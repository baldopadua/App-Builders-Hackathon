"""Compare production Kotlin embeddings from gate3Parity with Python ONNX inference."""

import argparse
from pathlib import Path

import numpy as np

from measure import BASELINE, OnnxEncoder


ROOT = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", type=Path, default=BASELINE)
    parser.add_argument("--kotlin-log", type=Path, default=ROOT / "runs" / "baseline-parity.log")
    args = parser.parse_args()
    samples = dict(line.split("\t", 1) for line in (ROOT / "parity_texts.tsv").read_text(encoding="utf-8").splitlines()[1:])
    kotlin = {}
    for line in args.kotlin_log.read_text(encoding="utf-8").splitlines():
        if line.startswith("PARITY\t"):
            _, name, values = line.split("\t", 2)
            kotlin[name] = np.fromstring(values, sep=",")
    if set(kotlin) != set(samples):
        raise RuntimeError(f"Expected {set(samples)}, got {set(kotlin)} from Kotlin")
    python = OnnxEncoder(args.model).encode(list(samples.values()), batch_size=1)
    errors = {name: float(np.max(np.abs(python[i] - kotlin[name]))) for i, name in enumerate(samples)}
    print(errors)
    if max(errors.values()) > 1e-3:
        raise SystemExit("Kotlin/Python embedding parity failed")


if __name__ == "__main__":
    main()
