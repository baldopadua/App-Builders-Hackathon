"""Check pinned local dataset integrity and private-note fixture structure."""

import hashlib
import json
from pathlib import Path

from data_pipeline import DATA, normalized


ROOT = Path(__file__).resolve().parent


def load(name):
    path = DATA / f"{name}.jsonl"
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def main():
    manifest = json.loads((DATA / "manifest.json").read_text(encoding="utf-8"))
    splits = {name: load(name) for name in ("train", "validation", "tatoeba_test")}
    for name, rows in splits.items():
        path = DATA / f"{name}.jsonl"
        assert hashlib.sha256(path.read_bytes()).hexdigest() == manifest["sha256"][name]
        assert len(rows) == manifest["counts"][name]
    held_out = {normalized(pair[lang]) for name in ("validation", "tatoeba_test")
                for pair in splits[name] for lang in ("fil", "en")}
    assert all(normalized(pair[lang]) not in held_out for pair in splits["train"] for lang in ("fil", "en"))

    lines = (ROOT / "private_notes_draft.tsv").read_text(encoding="utf-8").splitlines()
    assert lines[0] == "id\tlanguage\tnote\tpositives\tnegatives"
    cases = [line.split("\t") for line in lines[1:]]
    assert len(cases) >= 30 and len({row[0] for row in cases}) == len(cases)
    assert {language: sum(row[1] == language for row in cases) for language in ("en", "fil", "taglish")} == \
           {"en": 10, "fil": 10, "taglish": 10}
    assert all(len(row) == 5 and len(row[3].split("||")) == 4 and len(row[4].split("||")) == 4 for row in cases)
    drafts = [text for row in cases for group in row[3:5] for text in group.split("||")]
    assert len({normalized(text) for text in drafts}) == len(drafts)
    training_texts = {normalized(pair[lang]) for pair in splits["train"] for lang in ("fil", "en")}
    assert all(normalized(text) not in held_out | training_texts
               for row in cases for text in [row[2], *row[3].split("||"), *row[4].split("||")])
    print(f"PASS: {len(splits['train'])} ALT training pairs, {len(splits['validation'])} validation pairs, "
          f"{len(splits['tatoeba_test'])} Tatoeba pairs; {len(cases)} notes and {len(drafts)} unique drafts")


if __name__ == "__main__":
    main()
