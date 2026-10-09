# PII model (token classification)

Candidate: `onnx-community/bert-small-pii-detection-ONNX` (Apache-2.0)

Needed files (put them here, keep names):
- `model_quantized.onnx` (~28.7 MB)  ← from the repo's `onnx/` folder
- `tokenizer.json`, `vocab.txt`, `config.json` (labels in `id2label`)

Get them with `scripts/download-models.ps1`. Large binary: commit with Git LFS or keep out of git and download on each machine.
Verify accuracy and speed on the demo phone before committing to this model.
