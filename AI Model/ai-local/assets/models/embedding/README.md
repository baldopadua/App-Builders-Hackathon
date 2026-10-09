# Embedding model (for “Teach me what’s private”)

Candidate: `Xenova/all-MiniLM-L6-v2` (Apache-2.0)

Needed files (put them here, keep names):
- `model_quantized.onnx` (~23 MB)  ← from the repo's `onnx/` folder
- `tokenizer.json`, `vocab.txt`, `config.json`

Alternatives: `Xenova/bge-small-en-v1.5` (~34 MB, license unconfirmed), `Xenova/paraphrase-multilingual-MiniLM-L12-v2` (~118 MB, license unconfirmed, handles Tagalog).
Check each model’s license before bundling and list it in the submission disclosure.
