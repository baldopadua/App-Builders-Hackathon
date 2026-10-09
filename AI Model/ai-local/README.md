# ai-local — SecondLook's on-device AI core

Owner: Annie (AI Lead). Everything here runs **on the phone**: rules, two small ONNX models and encrypted storage. No server, no network, no cloud.

- How to test: [TESTING.md](TESTING.md)
- How to download models: [HOW_TO_DOWNLOAD_MODELS.md](HOW_TO_DOWNLOAD_MODELS.md)
- How to put this into the Flutter app: [docs/integration.md](docs/integration.md)
- Gate 3 Filipino measurement and training experiment: [gate3/README.md](gate3/README.md) (Candidate v2 passed all shipping gates and was promoted)

## Download models on Linux or macOS

From any cloned copy of this repository, run the download script from the
`ai-local` directory. The script finds its own location, so no absolute path
needs to be configured:

```bash
cd "AI Model/ai-local"
pwsh ./scripts/download-models.ps1
```

If the repository is in another directory, use its path instead:

```bash
cd "/path/to/App-Builders-Hackathon/AI Model/ai-local"
pwsh ./scripts/download-models.ps1
```

The runtime models are downloaded to:

```text
assets/models/pii/
assets/models/embedding/
```

If PowerShell is not installed on Ubuntu or Debian:

```bash
sudo apt update
sudo apt install -y powershell
```

Then run the download command again. See
[HOW_TO_DOWNLOAD_MODELS.md](HOW_TO_DOWNLOAD_MODELS.md) for Windows commands,
experimental models, and local rebuild instructions.

- API contract (keyboard ↔ AI ↔ Flutter): [docs/contract.md](docs/contract.md)

## How it decides (cheapest first; the first hit wins)

0. **Taught secrets**: values the user taught (an MPIN, a password, a key), matched exactly through a keyed fingerprint (HMAC-SHA256). The value itself is never stored. Warning type `my_secret`.
1. **Rules**: regex + Luhn + Filipino / Taglish phrasing. Red for PINs and passcodes, passwords, OTP / 2FA codes, API keys and tokens, SSH private keys, database URLs, cards + CVV and PH government IDs. A soft yellow "Intended?" for phone numbers, emails and bank accounts.
2. **PII model**: `bert-small-pii-detection` (27 MB, quantized ONNX), a second net for shapes the rules don't know.
3. **Private facts**: `all-MiniLM-L6-v2` (22 MB, quantized ONNX), matching by meaning plus the names / code words in the user's note.

Then **Itago** masks the secret inside the draft (`••••••`, `AIza••••`, last 4 digits kept for cards), and every warning carries a Filipino-first headline and a Taglish tip.

## Results so far (laptop: synthetic test set + real PH SMS; not a benchmark)

| Check                                            | Result                                      |
| ------------------------------------------------ | ------------------------------------------- |
| Detection, rules + PII model (169 synthetic messages) | 169/169 |
| Itago: risky messages masked, then checked again | 106/106 synthetic + 133/133 real clean |
| **Real PH SMS** (3,630 public messages, scrubbed): OTPs caught | **133/133** (rules + model) |
| Real PH SMS: red false alarms on 3,497 normal messages | rules only **0**; rules + PII model **12** (0.3%) |
| Taught secrets                                   | 27/27                                       |
| Private facts (5 notes, 40 drafts)               | 14/18 paraphrases caught, 3/22 false alarms |

Kotlin (`jvm-check`) and the browser port (`web-demo`) give the same results. **Not yet measured on the Tecno phone or inside the keyboard.** The same person wrote the rules and the test messages, so this shows the rules do what was intended, not that they generalize. The real-SMS rules were also tuned on that same data (no held-out split yet), so treat 133/133 as an upper bound. Full report: `eval/results/report.txt`.

## Folders

```
kotlin/com/secondlook/
├─ DetectionEngine.kt   team-facing API: get(), detect / detectAsync, hide / hideAll, Warning
├─ SecretManager.kt     "Teach me": save / list / delete (encrypted)
└─ ai/
   ├─ SecondLookChecker.kt, Detector.kt, RiskResult.kt   pipeline + hand-over format
   ├─ rules/RuleDetector.kt        regex + Luhn + Taglish phrasing (works with no model)
   ├─ Redactor.kt, SafetyTips.kt   Itago masking, Filipino headlines and tips
   ├─ model/                       WordPiece tokenizer, PII detector, sentence embeddings (ONNX Runtime)
   ├─ notes/                       private facts (meaning + anchors), taught secrets (fingerprints)
   ├─ android/                     Keystore AES-GCM, encrypted store, model loading from assets
   └─ bridge/AiMethodChannel.kt    Flutter MethodChannel "secondlook/ai" (commented; paste into the Flutter app)
assets/models/{pii,embedding}/     quantized .onnx + tokenizer files (committed, ~50 MB)
test-data/samples.json             synthetic messages, private-note sets, taught secrets
jvm-check/                         laptop check: same Kotlin + real models, scores everything
web-demo/                          browser demo + scorer, fully local (Transformers.js 3.8.1)
test-app/                          Android test app: compiles ../kotlin and ../assets directly
scripts/                           download-models.ps1 (re-download the bundled models), download-lab-models.ps1 (experiments only)
docs/                              contract.md, integration.md, architecture-reconciled.md
```

## Models and licenses (for the submission disclosures)

| Model / library                                                                                    | Used for                        | License    |
| -------------------------------------------------------------------------------------------------- | ------------------------------- | ---------- |
| `Xenova/all-MiniLM-L6-v2` (ONNX export of `sentence-transformers/all-MiniLM-L6-v2`), quantized | private facts                   | Apache-2.0 |
| `onnx-community/bert-small-pii-detection-ONNX`, quantized                                        | PII model                       | Apache-2.0 |
| ONNX Runtime 1.26.0 (`onnxruntime-android`; `onnxruntime` for the laptop check)                | runs the models                 | MIT        |
| Transformers.js 3.8.1                                                                              | browser demo on the laptop only | Apache-2.0 |

Pretrained models, not custom-trained. Re-check each license page before submitting.

## Rules

- Synthetic data only. The keys in the test data are invented look-alikes (`AKIAIOSFODNN7EXAMPLE` is AWS's documentation example).
- Never log or store the draft text. The release app has no INTERNET permission.
- Twins must change together: `RuleDetector.kt` ↔ `web-demo/public/rules.js`; `Redactor.kt`, `SafetyTips.kt` and the notes code ↔ `web-demo/public/core.js`. Run both checks after a change.
