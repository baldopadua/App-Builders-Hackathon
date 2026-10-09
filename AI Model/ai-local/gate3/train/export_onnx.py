"""Gate 3 step 3: export a checkpoint to the app's format (ONNX, int8 dynamic quantization, same inputs and
last_hidden_state output as the shipped Xenova model) and write probes.json for the Python <-> Kotlin parity check.

    python export_onnx.py                       selected checkpoint -> $GATE3_WORK/models/gate3-minilm-tl
    python export_onnx.py --control             original weights through the same pipeline -> models/control-minilm
The control run proves the export itself is faithful: its embeddings must match the shipped model.
"""
import argparse
import json
import shutil
from pathlib import Path

import numpy as np
import onnxruntime as ort
import torch
from onnxruntime.quantization import QuantType, quantize_dynamic
from transformers import AutoModel, AutoTokenizer

from common import (CHECKPOINTS, DATA, MAX_TOKENS, MODELS, NOTES_SET, SHIPPED_EMBEDDING, ensure_base_model, mean_pool,
                    read_tsv, sha256_file)

TOKENIZER_FILES = ["vocab.txt", "tokenizer.json", "tokenizer_config.json", "special_tokens_map.json"]

# Tokenizer edge cases on top of real sentences: accents, curly quotes, emoji, hyphens, numbers, very long text.
EDGE_PROBES = [
    "Señor Niño, ¿kumusta ka?", "“Sunflower” launch is next week…", "eto na yung 4821 😅 bilisan mo",
    "Mag-a-apply ako sa Banco Silangan ng loan — doble ang sahod!", "G-482913 is your Google verification code.",
    "pa-check naman: https://example.invalid/link?x=1&y=2", "TIN ko: 123-456-789-000", "  spaces   everywhere  ",
    "ÀÉÎÕÜ àéîõü ç ñ", "Nag-file na ako ng annulment against kay Jerome.",
    " ".join(["mahabang pangungusap tungkol sa lupa ni Lola"] * 30),
]


class LastHidden(torch.nn.Module):
    """Only last_hidden_state, like the shipped model; the app mean-pools it itself."""
    def __init__(self, model):
        super().__init__()
        self.model = model

    def forward(self, input_ids, attention_mask, token_type_ids):
        return self.model(input_ids=input_ids, attention_mask=attention_mask, token_type_ids=token_type_ids).last_hidden_state


def onnx_embed(session, tok, texts):
    out = []
    names = {i.name for i in session.get_inputs()}
    for t in texts:
        enc = tok(t, truncation=True, max_length=MAX_TOKENS, return_tensors="np")
        feeds = {k: v.astype(np.int64) for k, v in enc.items() if k in names}
        h = session.run(None, feeds)[0][0]
        v = h.mean(0)   # one text, no padding: mean over all tokens
        out.append(v / np.linalg.norm(v))
    return np.stack(out)


def probe_texts():
    val = read_tsv(DATA / "alt-validation.tsv")[:20]
    notes = json.loads(NOTES_SET.read_text(encoding="utf-8"))["notes"][:10]
    texts = [s for p in val for s in p] + [n["text"] for n in notes] + [n["disclosures"][1]["text"] for n in notes]
    return texts + EDGE_PROBES


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--control", action="store_true", help="export the ORIGINAL weights (pipeline check)")
    ap.add_argument("--checkpoint", default=str(CHECKPOINTS / "best"))
    args = ap.parse_args()

    src = ensure_base_model() if args.control else Path(args.checkpoint)
    name = "control-minilm" if args.control else "gate3-minilm-tl"
    out = MODELS / name
    out.mkdir(parents=True, exist_ok=True)

    tok = AutoTokenizer.from_pretrained(src)
    model = AutoModel.from_pretrained(src, attn_implementation="eager").eval()
    fp32 = out / "model_fp32.onnx"
    dummy = tok(["export example sentence", "isa pa"], padding=True, return_tensors="pt")
    dyn = {0: "batch_size", 1: "sequence_length"}
    torch.onnx.export(
        LastHidden(model).eval(), (dummy["input_ids"], dummy["attention_mask"], dummy["token_type_ids"]), str(fp32),
        input_names=["input_ids", "attention_mask", "token_type_ids"], output_names=["last_hidden_state"],
        dynamic_axes={"input_ids": dyn, "attention_mask": dyn, "token_type_ids": dyn, "last_hidden_state": dyn},
        opset_version=14, do_constant_folding=True,
    )
    model.eval()   # torch.onnx.export restores the wrapper's training mode; the reference embeddings need eval
    quantized = out / "model_quantized.onnx"
    # uint8 weights, one scale per output channel: closest to fp32 of the settings tried (mean cosine 0.993 on the
    # probes vs 0.991 for the shipped model, 0.978 per-tensor, 0.950 int8 per-tensor)
    quantize_dynamic(str(fp32), str(quantized), weight_type=QuantType.QUInt8, per_channel=True)
    fp32.unlink()
    base = ensure_base_model()
    for f in TOKENIZER_FILES:
        shutil.copyfile(base / f, out / f)   # the tokenizer never changes: same vocab as the shipped model
    shutil.copyfile(src / "config.json", out / "config.json")

    # ---- probes: what Python sees, for the Kotlin parity check (jvm-check gate3 reads models/<name>/probes.json) --
    texts = probe_texts()
    sess = ort.InferenceSession(str(quantized), providers=["CPUExecutionProvider"])
    q = onnx_embed(sess, tok, texts)
    with torch.no_grad():
        f = torch.cat([mean_pool(model(**(e := tok(t, truncation=True, max_length=MAX_TOKENS, return_tensors="pt"))).last_hidden_state,
                                 e["attention_mask"]) for t in texts]).numpy()
    probes = [{"text": t, "ids": tok(t, truncation=True, max_length=MAX_TOKENS)["input_ids"], "onnx": q[i].round(6).tolist()}
              for i, t in enumerate(texts)]
    (out / "probes.json").write_text(json.dumps(probes, ensure_ascii=False), encoding="utf-8")
    qcos = (q * f).sum(1)
    report = {"name": name, "source": str(src), "bytes": quantized.stat().st_size, "sha256": sha256_file(quantized),
              "probes": len(texts), "quantization_cos_min": float(qcos.min()), "quantization_cos_mean": float(qcos.mean())}
    shipped = ort.InferenceSession(str(SHIPPED_EMBEDDING / "model_quantized.onnx"), providers=["CPUExecutionProvider"])
    s = onnx_embed(shipped, tok, texts)
    scos = (q * s).sum(1)
    report["vs_shipped_cos_min"], report["vs_shipped_cos_mean"] = float(scos.min()), float(scos.mean())
    (out / "export-report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
