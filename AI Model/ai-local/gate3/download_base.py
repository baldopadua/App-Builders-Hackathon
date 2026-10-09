"""Download the pinned English MiniLM source checkpoint into ignored local storage."""

from huggingface_hub import snapshot_download

from data_pipeline import DATA


REPO = "sentence-transformers/all-MiniLM-L6-v2"
REVISION = "1110a243fdf4706b3f48f1d95db1a4f5529b4d41"


if __name__ == "__main__":
    print(snapshot_download(REPO, revision=REVISION, local_dir=DATA / "base", allow_patterns=[
        "config.json", "model.safetensors", "tokenizer.json", "tokenizer_config.json", "vocab.txt",
        "special_tokens_map.json", "modules.json", "1_Pooling/config.json",
    ]))
