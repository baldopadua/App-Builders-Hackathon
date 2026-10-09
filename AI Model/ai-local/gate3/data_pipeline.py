"""Download pinned Filipino/English pairs into ignored local storage.

ALT trains the model. Tatoeba is loaded solely for overlap removal and evaluation.
"""

import argparse
import hashlib
import json
import unicodedata
from pathlib import Path


ROOT = Path(__file__).resolve().parent
DATA = ROOT / "data"
ALT_REVISION = "afbd92e198bbcf17f660e03076fd2938f5a4bbb2"
TATOEBA_REVISION = "4f65d2990c55534b6aa84eeb7da78b1f13c97426"


def normalized(text):
    return " ".join(unicodedata.normalize("NFC", text).casefold().split())


def pair_key(left, right):
    return tuple(sorted((normalized(left), normalized(right))))


def clean_pairs(rows):
    seen = set()
    result = []
    for left, right in rows:
        if not isinstance(left, str) or not isinstance(right, str):
            continue
        left, right = " ".join(left.split()), " ".join(right.split())
        key = pair_key(left, right)
        if not all(key) or key in seen:
            continue
        seen.add(key)
        result.append((left, right))
    return result


def remove_overlap(train, evaluation):
    held_out = {normalized(text) for pair in evaluation for text in pair}
    kept = [(a, b) for a, b in train if normalized(a) not in held_out and normalized(b) not in held_out]
    return kept, len(train) - len(kept)


def write_jsonl(path, pairs):
    with path.open("w", encoding="utf-8", newline="\n") as file:
        for fil, en in pairs:
            file.write(json.dumps({"fil": fil, "en": en}, ensure_ascii=False) + "\n")
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    from datasets import load_dataset
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=DATA)
    parser.add_argument("--alt-revision", default=ALT_REVISION)
    parser.add_argument("--tatoeba-revision", default=TATOEBA_REVISION)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    alt_rev = args.alt_revision
    tatoeba_rev = args.tatoeba_revision
    cache = str(output / "cache")

    def alt(split):
        ds = load_dataset("mutiyama/alt", "alt-parallel", split=split, revision=alt_rev, cache_dir=cache)
        return clean_pairs((row["translation"]["fil"], row["translation"]["en"]) for row in ds), len(ds)

    train, raw_train = alt("train")
    valid, raw_valid = alt("validation")
    ds = load_dataset("mteb/tatoeba-bitext-mining", "tgl-eng", split="test", revision=tatoeba_rev, cache_dir=cache)
    tatoeba = clean_pairs((row["sentence1"], row["sentence2"]) for row in ds)
    raw_tatoeba = len(ds)
    cleaned_train, cleaned_valid = len(train), len(valid)
    train, train_removed = remove_overlap(train, valid + tatoeba)
    valid, valid_removed = remove_overlap(valid, tatoeba)
    hashes = {name: write_jsonl(output / f"{name}.jsonl", rows) for name, rows in
              (("train", train), ("validation", valid), ("tatoeba_test", tatoeba))}
    manifest = {
        "seed": 42,
        "datasets": {
            "alt": {"repo": "mutiyama/alt", "config": "alt-parallel", "revision": alt_rev,
                    "license": "CC BY 4.0", "usage": "train and validation",
                    "citation": "Riza et al. (2016), Introduction of the Asian Language Treebank"},
            "tatoeba": {"repo": "mteb/tatoeba-bitext-mining", "config": "tgl-eng", "revision": tatoeba_rev,
                        "license": "CC BY 2.0", "usage": "untouched evaluation only"},
        },
        "counts": {"train_raw": raw_train, "train_clean": cleaned_train, "train": len(train),
                   "validation_raw": raw_valid, "validation_clean": cleaned_valid, "validation": len(valid),
                   "tatoeba_raw": raw_tatoeba, "tatoeba_test": len(tatoeba),
                   "train_overlap_removed": train_removed, "validation_overlap_removed": valid_removed},
        "sha256": hashes,
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
