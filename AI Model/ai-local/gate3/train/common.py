"""Shared paths, pins and helpers for the Gate 3 scripts (laptop only, never shipped)."""
import hashlib
import json
import os
import re
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
AI_LOCAL = HERE.parent.parent                       # AI Model/ai-local
REPO = AI_LOCAL.parent.parent                       # App-Builders-Hackathon
WORK = Path(os.environ.get("GATE3_WORK", REPO.parent / "gate3-work")).resolve()   # outside the repo
DATA = WORK / "data"
MODELS = WORK / "models"
CHECKPOINTS = WORK / "checkpoints"
BASE_DIR = WORK / "base-model"
# Python never touches the network: on the team laptops Windows Smart App Control blocks the standalone Python's
# _ssl module, so files are fetched with the signed Windows curl.exe and loaded from disk.
os.environ["HF_HUB_OFFLINE"] = "1"
os.environ["TRANSFORMERS_OFFLINE"] = "1"
CURL = Path(os.environ.get("SystemRoot", r"C:\Windows")) / "System32" / "curl.exe"

MANIFEST = AI_LOCAL / "gate3" / "data-manifest.json"
NOTES_SET = AI_LOCAL / "test-data" / "gate3" / "private-notes.json"
SHIPPED_EMBEDDING = AI_LOCAL / "assets" / "models" / "embedding"

SEED = 42
MAX_TOKENS = 128    # same as the app's EmbeddingModel(maxTokens = 128)

# Student = the PyTorch weights the shipped Xenova ONNX model was converted from.
BASE_MODEL = "sentence-transformers/all-MiniLM-L6-v2"
BASE_REVISION = "1110a243fdf4706b3f48f1d95db1a4f5529b4d41"
SHIPPED_MODEL = "Xenova/all-MiniLM-L6-v2"
SHIPPED_REVISION = "751bff37182d3f1213fa05d7196b954e230abad9"

ALT = {
    "repo": "mutiyama/alt",
    "revision": "afbd92e198bbcf17f660e03076fd2938f5a4bbb2",
    "license": "CC BY 4.0",
    "attribution": "Asian Language Treebank (ALT) parallel corpus, NICT (Riza et al., 2016), via huggingface.co/datasets/mutiyama/alt, CC BY 4.0. Changes: kept the fil and en sides only, filtered and deduplicated (counts in this manifest).",
    "files": {
        "train": ("alt-parallel/train-00000-of-00001.parquet", "e2d95c41b90c0fe94226f9b424e63f476b6027015df4beba3e5d7e19092a3a44"),
        "validation": ("alt-parallel/validation-00000-of-00001.parquet", "646290f6bdb697763e8d00e98fb0487711d4b3ef948e199582197684739da6f8"),
    },
}
TATOEBA = {
    "repo": "mteb/tatoeba-bitext-mining",
    "revision": "4f65d2990c55534b6aa84eeb7da78b1f13c97426",
    "license": "CC BY 2.0 FR (Tatoeba)",
    "attribution": "Tatoeba (tatoeba.org) sentence pairs, CC BY 2.0 FR, via huggingface.co/datasets/mteb/tatoeba-bitext-mining (tgl-eng test split). Evaluation only, never trained on.",
    "file": "test/tgl-eng.jsonl.gz",
}


BASE_FILES = ["config.json", "model.safetensors", "tokenizer.json", "tokenizer_config.json", "vocab.txt",
              "special_tokens_map.json"]


def hf_url(repo: str, revision: str, filename: str, dataset: bool = False) -> str:
    return f"https://huggingface.co/{'datasets/' if dataset else ''}{repo}/resolve/{revision}/{filename}"


def fetch(url: str, dest: Path, expected_sha: str = None) -> str:
    """Download with curl.exe (skipped when the file is already there and matches). Returns the sha256."""
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not (dest.exists() and (expected_sha is None or sha256_file(dest) == expected_sha)):
        import subprocess
        tmp = dest.with_suffix(dest.suffix + ".part")
        subprocess.run([str(CURL), "-fsSL", "--retry", "3", "-o", str(tmp), url], check=True)
        tmp.replace(dest)
    digest = sha256_file(dest)
    if expected_sha and digest != expected_sha:
        raise SystemExit(f"{url}: sha256 {digest} does not match the pinned {expected_sha}")
    return digest


def ensure_base_model() -> Path:
    """The student's starting weights (pinned revision) in $GATE3_WORK/base-model."""
    for f in BASE_FILES:
        fetch(hf_url(BASE_MODEL, BASE_REVISION, f), BASE_DIR / f)
    return BASE_DIR


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def sha256_text(s: str) -> str:
    return hashlib.sha256(s.encode("utf-8")).hexdigest()


_PUNCT = re.compile(r"[^\w\s]", re.UNICODE)


def normalize(s: str) -> str:
    """For overlap checks only: NFKC, lowercase, accents and punctuation removed, whitespace collapsed."""
    s = unicodedata.normalize("NFKD", unicodedata.normalize("NFKC", s).lower())
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return " ".join(_PUNCT.sub(" ", s).split())


def read_tsv(path: Path):
    with open(path, encoding="utf-8") as f:
        return [tuple(line.rstrip("\n").split("\t")) for line in f if line.strip()]


def write_tsv(path: Path, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        for r in rows:
            f.write("\t".join(x.replace("\t", " ").replace("\n", " ") for x in r) + "\n")


def notes_set_texts():
    """Every note text and draft in the held-out private-note set (for the contamination check)."""
    d = json.loads(NOTES_SET.read_text(encoding="utf-8"))
    out = []
    for n in d["notes"]:
        out.append(n["text"])
        out += [x["text"] for x in n["disclosures"] + n["nonDisclosures"]]
    return out


def mean_pool(last_hidden, mask):
    """Mean over real tokens, then L2-normalise: the same pooling as the app's EmbeddingModel.embed."""
    import torch
    m = mask.unsqueeze(-1).to(last_hidden.dtype)
    v = (last_hidden * m).sum(1) / m.sum(1).clamp(min=1e-9)
    return torch.nn.functional.normalize(v, p=2, dim=-1)
