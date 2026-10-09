# Gate 3: Tagalog / Taglish private-note matching

**Goal (lead's plan):** measure the current "Teach me what's private" matcher, train one Filipino–English candidate, and replace the on-device embedding model only if it passes every gate. Gate 2 (PII model) is out of scope. Training data never goes on the phone.

**Status and numbers:** [`results/gates.md`](results/gates.md).

## What is where

| Piece | Where | In the repo? |
|---|---|---|
| Held-out private-note set: 36 invented notes × (4 disclosures + 4 non-disclosures), 12 notes each in English, Tagalog and Taglish, dev/test split | `test-data/gate3/private-notes.json` | yes (invented, Rule Zero) |
| Dataset revisions, licenses, attribution, filter counts, hashes, overlap check | `gate3/data-manifest.json` | yes |
| Scripts: data, training, ONNX export, gates | `gate3/train/*.py` | yes |
| Evaluation with the app's own Kotlin code | `jvm-check`: `gradlew gate3` (`src/main/kotlin/Gate3.kt`) | yes |
| Same evaluation through the browser twin (`core.js`), compared decision by decision | `web-demo/gate3-eval.mjs` | yes |
| Phone speed test (warmed-up p95, 5 notes) | test app → "Gate 3 speed test" | yes |
| Downloads, venv, checkpoints, exported models, raw results | `$GATE3_WORK` (default `..\gate3-work` next to the repo) | **no** |

## Data

- **Training:** ALT parallel corpus (`mutiyama/alt`, revision `afbd92e`, CC BY 4.0), fil–en pairs, train split (18,084 after filtering); validation split (998) for checkpoint selection.
- **Untouched test:** Tatoeba tgl–eng (`mteb/tatoeba-bitext-mining`, revision `4f65d29`, CC BY 2.0 FR), 1,000 pairs, plus the private-note test split.
- **Overlap check:** no ALT pair matches any evaluation sentence after normalisation (exact, or an evaluation sentence of 6+ words contained in a pair). `data-manifest.json` records 0.
- FLORES+ is not used (gated, evaluation-only; the plan does not depend on it). The multilingual MiniLM teacher is excluded (no Tagalog in its language list).

## Training (one candidate)

`all-MiniLM-L6-v2` (pinned revision `1110a24`), same tokenizer, same 384-d mean-pooled output, so the app's Kotlin code does not change. Loss: symmetric in-batch contrastive (scale 20) between Filipino and English sentences, plus 5 × squared distance between the student's English output and a frozen copy of the original (limits English regression). At most 3 epochs, batch 32, lr 3e-5, seed 42, dropout off. The checkpoint is chosen on ALT validation (fil↔en top-1) among checkpoints whose English output stays at cosine ≥ 0.97 of the original. Tatoeba and the private-note set are never read during training.

**Export:** ONNX opset 14, `last_hidden_state` output like the shipped model, dynamic int8 quantization (uint8 weights, per channel). On the probes this is closer to fp32 (mean cosine 0.993) than the shipped Xenova file (0.991). The same pipeline applied to the original weights (`control-minilm`) reproduces the shipped model's results, so the export itself does not move the numbers.

**Parity:** `export_onnx.py` writes `probes.json` (Python token ids + ONNX embeddings for 71 texts, including accents, emoji, curly quotes, URLs, a >128-token text). `gradlew gate3` re-tokenizes and re-embeds them with the app's `WordPieceTokenizer` + ONNX + pooling. Control run: 0 token-id differences, cosine 1.000.

## Gates (from the plan)

The candidate is integrated only if all of these hold on the test split. Both models use thresholds tuned on the dev split only: the highest dev recall with dev false alarms ≤ 10%.

1. Tagalog/Taglish disclosure recall ≥ baseline + 10 points
2. False-alarm rate ≤ baseline and ≤ 10%
3. English disclosure recall ≥ baseline − 2 points
4. Tatoeba top-1 ≥ baseline + 10 points
5. Quantized model < 30 MB
6. Warmed-up Gate 3 p95 < 10 ms on the Tecno Spark

Otherwise the current model stays and the findings are published in `results/`.

## Run it again

```powershell
# once: isolated Python env OUTSIDE the repo (uv + Python 3.11 + pinned CPU packages)
powershell -ExecutionPolicy Bypass -File "AI Model\ai-local\gate3\train\setup-env.ps1"
$py = "..\gate3-work\venv\Scripts\python.exe"        # from the repo root; or set $env:GATE3_WORK

cd "AI Model\ai-local\gate3\train"
& $py prepare_data.py            # ALT + Tatoeba (pinned, sha256-checked), filters, overlap check, data-manifest.json
& $py export_onnx.py --control   # optional: export pipeline check on the original weights
& $py train.py                   # about 2 h on a 4-core laptop CPU; resumable (rerun to continue)
& $py export_onnx.py             # selected checkpoint -> gate3-work\models\gate3-minilm-tl (+ probes.json)

cd ..\..\jvm-check
.\gradlew.bat gate3                                                                               # current model
.\gradlew.bat gate3 "-Pargs=model=C:/dev/hackathon/gate3-work/models/gate3-minilm-tl name=gate3-minilm-tl"

cd ..\web-demo
node gate3-eval.mjs                                                                               # JS twin = Kotlin?
node gate3-eval.mjs model=C:/dev/hackathon/gate3-work/models/gate3-minilm-tl

cd ..\gate3\train
& $py gates.py [--phone-p95 <baseline ms> <candidate ms>]   # -> gate3\results\gates.md / gates.json
```

**Phone gate:** build the test app with the candidate bundled next to the current model:
`cd "AI Model\ai-local\test-app"; .\gradlew.bat assembleDebug -Pgate3Candidate=C:/dev/hackathon/gate3-work/models/gate3-minilm-tl`,
install it, turn on airplane mode, tap **Gate 3 speed test**, and pass the two p95 values to `gates.py --phone-p95`.

**Windows note:** Smart App Control blocks the standalone Python's `_ssl` module on the team laptop, so the scripts never use the network from Python. Files are fetched with the signed `C:\Windows\System32\curl.exe` and checked against pinned sha256 values.
