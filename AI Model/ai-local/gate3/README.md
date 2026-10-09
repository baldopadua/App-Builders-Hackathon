# Gate 3 Filipino Measurement and Training Experiment

> **Release Decision: Shipped Baseline Model Retained; Candidate v2 Classified as Experimental Prototype**
>
> Following rigorous gate criteria and external review, **Candidate v2 is classified as an experimental research prototype, not an approved production release**. The previous report claim of production approval has been corrected.
> 
> While Candidate v2 achieved a +11.8 percentage point gain on Tatoeba bi-text mining, it did not satisfy the strict production release criteria:
> 1. **False Alarm Regression:** On the held-out 240-draft evaluation, false alarms rose from 3/120 (2.5%) to 12/120 (10.0%), violating the requirement to improve recall *without increasing false alarms*.
> 2. **Hardware Latency:** Warmed-up p95 latency on physical hardware (TECNO LH8n) was 17.42 ms, exceeding the <10.0 ms target.
> 3. **Unreviewed Draft Labels:** Draft test annotations in `private_notes_draft.tsv` have not yet undergone native Filipino speaker linguistic review.
>
> In accordance with the engineering gate criteria, **the baseline embedding model (`all-MiniLM-L6-v2`, SHA-256 `AFDB6F1A...`) is retained in production**, paired with an interruption-safe, background vector migration engine.

---

## 1. Saved-Note Vector Migration Engine

Before considering any future embedding model switch, the system must guarantee that user private notes stored under prior vector spaces are not corrupted or silently weakened. We implemented and verified an atomic, interruption-safe vector migration system in `PersistentNotesStore.kt` and `NoteMigration.kt`.

### Architecture & Safety Invariants
* **Embedding Model Versioning:** Every stored note persists a `modelVersion` field (legacy unversioned notes default to `"legacy"`). The active model version is deterministically derived from the SHA-256 fingerprint of the active model weights (`computeModelVersion`).
* **Background Re-Embedding:** `PersistentNotesStore.migrate()` iterates over stored notes in a background worker thread (`AnnieAiAdapter.kt`), decrypts note plaintext with Keystore AES-GCM, calculates the new 384-dimensional embedding vector using the active model, and atomically updates the note record.
* **Taught-Secret Isolation:** Taught secrets (`KIND_SECRET`) store exact-match HMAC-SHA256 fingerprints and no embedding vectors; the migration engine strictly skips them to prevent tampering.
* **Atomic Persistence:** Each note is committed to encrypted key-value storage immediately upon re-embedding. If the Android process or OS terminates during migration, migrated notes remain valid while remaining notes are safely picked up on the next launch.
* **Interruption & Retry Idempotence:** The engine supports cancellation checks (`interrupter: () -> Boolean`) between notes. Subsequent runs inspect `alreadyUpToDate` and resume without redundant computations.

### Unit Test Verification
Automated test suite (`AI Model/ai-local/jvm-check/src/test/kotlin/NoteMigrationTest.kt`):
1. `testLegacyNoteMigration()`: Verifies legacy notes migrate from `"legacy"` to the active model version with updated 384-dim unit vectors while preserving encrypted note content.
2. `testMigrationInterruptionAndRetry()`: Simulates process termination after processing 1 of 3 notes, asserts partial progress is saved, and verifies that re-running completes the remaining 2 notes without re-embedding the first.
3. `testSecretFingerprintIsolation()`: Confirms taught secrets retain their exact HMAC fingerprints and are not altered by the migration runner.

---

## 2. Remeasuring the Full Matcher on Held-Out Notes

Both models were evaluated on the held-out dataset (30 user notes, 240 draft messages: 120 true disclosures / positives, 120 harmless benign / negatives) at calibrated anchor sensitivity (`anchorFloor = 0.60f`).

### Complete 6-Segment Breakdown Matrix

| Segment | Positives (Disclosures) | Negatives (Harmless) | Shipped Baseline Flags | Candidate v2 Prototype Flags |
| :--- | :---: | :---: | :---: | :---: |
| **English / with anchor** | 16 | 9 | **TP: 9/16 (56.3%)**<br>FP: 0/9 (0.0%) | **TP: 14/16 (87.5%)**<br>FP: 4/9 (44.4%) |
| **English / no anchor** | 24 | 31 | **TP: 3/24 (12.5%)**<br>FP: 0/31 (0.0%) | **TP: 4/24 (16.7%)**<br>FP: 0/31 (0.0%) |
| **Filipino / with anchor** | 26 | 7 | **TP: 13/26 (50.0%)**<br>FP: 1/7 (14.3%) | **TP: 23/26 (88.5%)**<br>FP: 5/7 (71.4%) |
| **Filipino / no anchor** | 14 | 33 | **TP: 2/14 (14.3%)**<br>FP: 0/33 (0.0%) | **TP: 1/14 (7.1%)**<br>FP: 1/33 (3.0%) |
| **Taglish / with anchor** | 17 | 7 | **TP: 8/17 (47.1%)**<br>FP: 2/7 (28.6%) | **TP: 10/17 (58.8%)**<br>FP: 2/7 (28.6%) |
| **Taglish / no anchor** | 23 | 33 | **TP: 1/23 (4.3%)**<br>FP: 0/33 (0.0%) | **TP: 5/23 (21.7%)**<br>FP: 0/33 (0.0%) |
| **OVERALL TOTAL** | **120** | **120** | **TP: 36/120 (30.0%)**<br>**FP: 3/120 (2.5%)** | **TP: 57/120 (47.5%)**<br>**FP: 12/120 (10.0%)** |

### Analysis of Matcher Behavior
1. **High Semantic Drift on Anchor Negatives:** While Candidate v2 increases true positive recall on anchor notes (e.g. Filipino anchor TP rose from 13 to 23), its cross-lingual representation space compresses benign drafts containing anchor words into high-similarity regions. For Filipino anchor notes, 5 out of 7 harmless drafts were flagged (71.4% FP). For English anchor notes, 4 out of 9 harmless drafts were flagged (44.4% FP).
2. **False Alarm Quadrupling:** Overall false alarms increased from 3 (2.5%) to 12 (10.0%). In a privacy-protecting keyboard context, false alarms directly undermine user trust and cause warning fatigue.
3. **Pending Native Filipino Review:** All 240 draft annotations in `private_notes_draft.tsv` are synthetic drafts created by the team. They have **not yet undergone formal native Filipino speaker linguistic review**. Release decisions must not be based on unverified ground truth.

---

## 3. Shipping Gates Evaluation & Release Decision

| Gate Metric | Shipped Baseline | Candidate v2 Prototype | Gate Target | Gate Status |
| :--- | :---: | :---: | :---: | :---: |
| **Tatoeba Filipino → English Top-1** | 3.2% | 15.0% (+11.8 pts) | $\ge$ +10.0 percentage point gain | **PASSED** |
| **NoteMatcher False Alarms** | **3/120 (2.5%)** | 12/120 (10.0%) | No increase vs. baseline (must not add FPs) | **FAILED** (+9 FPs) |
| **NoteMatcher Disclosure Recall** | 36/120 (30.0%) | 57/120 (47.5%) | $\ge$ +10.0 pts without more false alarms | **FAILED** (Condition not met) |
| **TECNO LH8n Warmed-up p95 Latency** | p50: 9.20 ms | p50: 10.76 ms, **p95: 17.42 ms** | Warmed-up p95 < 10.0 ms | **FAILED** |
| **Native Filipino Speaker Review** | Unverified | Unverified | Must be signed off prior to release | **PENDING** |
| **Background Vector Migration** | **Ready & Tested** | Ready & Tested | Zero user note degradation across updates | **PASSED** |
| **Model Footprint** | 22.97 MB | 22.86 MB | < 30 MB on-device | **PASSED** |
| **Deterministic Runtime Parity** | < 1.0e-7 | < 3.83e-8 | Max float diff < 1.0e-3 | **PASSED** |

### Release Decision: Retain Baseline Model in Production
Candidate v2 failed two primary release gates:
* It quadrupled false alarms on the held-out draft suite (from 3 to 12).
* Its hardware execution latency p95 (17.42 ms) exceeds the 10.0 ms budget.

Therefore:
1. **Production Deployment:** Retain `all-MiniLM-L6-v2` (`AFDB6F1A...`, 22,972,370 bytes) in `AI Model/ai-local/assets/models/embedding/` and `Android App/android/app/src/main/assets/models/embedding/`.
2. **Prototype Status:** Preserve Candidate v2 training checkpoints and evaluation scripts in `runs/candidate/` as an experimental prototype for further negative-mining research and distillation.
3. **Migration Readiness:** Ship the background note migration engine into production now, ensuring future candidate models have a safe migration path once gates are cleared.

---

## 4. Technical Training Log (Candidate v2 Prototype)

For ongoing research, the Candidate v2 training architecture is documented below:

### Architecture: Memory Bank Queue & Multi-Objective Distillation
* **Teacher Network:** Frozen `sentence-transformers/all-MiniLM-L6-v2` (Apache-2.0).
* **Negative Mining:** MoCo-style Memory Bank Queue of 1,024 teacher English sentence representations, decoupling negative sample diversity from mini-batch GPU RAM constraints.
* **Loss Function:**
  $$\mathcal{L} = \mathcal{L}_{\text{InfoNCE}}^{\text{fil}\to\text{en}} + 0.5 \cdot \mathcal{L}_{\text{InfoNCE}}^{\text{en}\to\text{fil}} + 0.5 \cdot \mathcal{L}_{\text{MSE}}(z_{\text{fil}}, z_{\text{en}}) + 0.1 \cdot \mathcal{L}_{\text{MSE}}(z_{\text{en}}^{\text{student}}, z_{\text{en}}^{\text{teacher}})$$

### Data Provenance & Cryptographic Invariants

| Dataset | Split & Revision | License | Item Count | SHA-256 Digest |
| :--- | :--- | :--- | :---: | :--- |
| **Asian Language Treebank (ALT)** | `afbd92e198bb...` | CC BY 4.0 | 18,077 clean train pairs; 998 validation pairs | Train: `31071f28...`<br>Valid: `03b505c3...` |
| **MTEB Tatoeba (`tgl-eng`)** | `4f65d2990c55...` | CC BY 2.0 | 1,000 zero-leakage test pairs | `c523b181...` |
| **Base Checkpoint** | `1110a243fdf4...` | Apache-2.0 | 384-dim WordPiece uncased checkpoint | Teacher snapshot |

Strict zero-leakage partitioning is cryptographically enforced by `audit.py`.

---

## 5. Experimental Reproduction Commands

```powershell
# 1. Audit and train experimental prototype (in AI Model/ai-local/gate3)
python data_pipeline.py
python download_base.py
python audit.py
python train.py
python measure.py --model runs/candidate --output runs/candidate-tatoeba.json
python parity.py

# 2. Run Kotlin runtime evaluation and unit tests (in AI Model/ai-local/jvm-check)
./gradlew test         # Executes NoteMigrationTest (interruption, retry, and legacy migration)
./gradlew gate3Eval    # Scores held-out 240 draft matrix across both models
./gradlew run          # Production detection smoke test on shipped baseline model
```
