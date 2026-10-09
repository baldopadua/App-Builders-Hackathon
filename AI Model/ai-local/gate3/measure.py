"""Measure Tatoeba bitext retrieval with the same ONNX pooling used by Android."""

import argparse
import hashlib
import json
import time
from pathlib import Path

import numpy as np

from data_pipeline import DATA


ROOT = Path(__file__).resolve().parents[1]
BASELINE = ROOT / "assets" / "models" / "embedding"


def retrieval_top1(source, target):
    scores = source @ target.T
    return float(np.mean(np.argmax(scores, axis=1) == np.arange(len(source))))


class OnnxEncoder:
    def __init__(self, folder):
        import onnxruntime as ort
        from transformers import AutoTokenizer

        self.folder = Path(folder)
        self.tokenizer = AutoTokenizer.from_pretrained(self.folder, local_files_only=True)
        options = ort.SessionOptions()
        options.intra_op_num_threads = 2
        self.session = ort.InferenceSession(str(self.folder / "model_quantized.onnx"), options,
                                            providers=["CPUExecutionProvider"])
        self.inputs = {item.name for item in self.session.get_inputs()}

    def encode(self, texts, batch_size=1):
        output = []
        for start in range(0, len(texts), batch_size):
            batch = self.tokenizer(texts[start:start + batch_size], padding=True, truncation=True,
                                   max_length=128, return_tensors="np")
            feeds = {key: value.astype(np.int64) for key, value in batch.items() if key in self.inputs}
            hidden = self.session.run(None, feeds)[0]
            mask = batch["attention_mask"][..., None]
            vectors = (hidden * mask).sum(axis=1) / np.maximum(mask.sum(axis=1), 1)
            vectors /= np.maximum(np.linalg.norm(vectors, axis=1, keepdims=True), 1e-12)
            output.append(vectors.astype(np.float32))
        return np.concatenate(output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", type=Path, default=BASELINE)
    parser.add_argument("--dataset", type=Path, default=DATA / "tatoeba_test.jsonl")
    parser.add_argument("--output", type=Path, default=ROOT / "gate3" / "runs" / "baseline.json")
    args = parser.parse_args()
    pairs = [json.loads(line) for line in args.dataset.read_text(encoding="utf-8").splitlines()]
    encoder = OnnxEncoder(args.model)
    started = time.perf_counter()
    fil = encoder.encode([pair["fil"] for pair in pairs])
    en = encoder.encode([pair["en"] for pair in pairs])
    elapsed = time.perf_counter() - started
    weights = args.model / "model_quantized.onnx"
    report = {
        "model": str(args.model.resolve()),
        "model_sha256": hashlib.sha256(weights.read_bytes()).hexdigest(),
        "model_bytes": weights.stat().st_size,
        "dataset_sha256": hashlib.sha256(args.dataset.read_bytes()).hexdigest(),
        "pairs": len(pairs),
        "fil_to_en_top1": retrieval_top1(fil, en),
        "en_to_fil_top1": retrieval_top1(en, fil),
        "desktop_seconds": round(elapsed, 3),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
