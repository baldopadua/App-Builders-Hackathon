# Gate 3 results (2026-10-10)

**Verdict: FAIL: keep the current model**

Baseline `shipped` vs candidate `control`, test split of the held-out private-note set (labels: pending), each at its own dev-tuned thresholds (baseline 0.61/0.58, candidate 0.61/0.59; similarity alone / anchor + similarity).

| Gate | Baseline | Candidate | Required | Pass |
|---|---|---|---|---|
| Tagalog/Taglish disclosure recall (test) | 47.0% | 48.5% | >= baseline + 10 points | no |
| False-alarm rate (test) | 14.6% | 13.5% | <= baseline and <= 10% | no |
| English disclosure recall (test) | 33.3% | 33.3% | >= baseline - 2 points | yes |
| Tatoeba tgl-eng top-1 (mean of both directions) | 4.6% | 4.7% | >= baseline + 10 points | no |
| Quantized model size (MB) | 23.0 | 23.0 | < 30 MB | yes |
| Warmed-up Gate 3 p95 on the Tecno Spark (ms) | - | - | < 10 ms (run the test app's speed test) | pending |

| Check | Value |
|---|---|
| parity_token_mismatches | 0 |
| parity_cosine_min | 0.9999994039535522 |
| alt_pairs_overlapping_eval_sets | 0 |
| notes_set_review | pending |

Full numbers (by language, anchor present/absent, confusion counts, defaults and dev-tuned): `gates.json`.
Every draft, both models: `private-notes-predictions.csv`. Validation during training: `training-log.csv`.
