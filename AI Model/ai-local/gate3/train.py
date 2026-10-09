"""Train one Tagalog-aligned MiniLM candidate using ALT train/validation with Memory Bank Contrastive Distillation."""

import argparse
import json
import math
import random
import shutil
from pathlib import Path

import numpy as np
import torch
import torch.nn.functional as F
from torch.utils.data import DataLoader
from transformers import AutoModel, AutoTokenizer

from data_pipeline import DATA


ROOT = Path(__file__).resolve().parent
BASE = DATA / "base"


def read_pairs(path):
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines()]


def pooled(model, batch):
    hidden = model(**batch).last_hidden_state
    mask = batch["attention_mask"].unsqueeze(-1)
    vectors = (hidden * mask).sum(dim=1) / mask.sum(dim=1).clamp(min=1)
    return F.normalize(vectors, p=2, dim=1)


@torch.no_grad()
def encode(model, tokenizer, texts, device, batch_size=32):
    model.eval()
    arrays = []
    for start in range(0, len(texts), batch_size):
        batch = tokenizer(texts[start:start + batch_size], padding=True, truncation=True,
                          max_length=128, return_tensors="pt").to(device)
        arrays.append(pooled(model, batch).cpu().numpy())
    return np.concatenate(arrays)


def validation_top1(model, tokenizer, pairs, device):
    from measure import retrieval_top1

    fil = encode(model, tokenizer, [p["fil"] for p in pairs], device)
    en = encode(model, tokenizer, [p["en"] for p in pairs], device)
    return retrieval_top1(fil, en)


class Export(torch.nn.Module):
    def __init__(self, model):
        super().__init__()
        self.model = model

    def forward(self, input_ids, attention_mask, token_type_ids):
        return self.model(input_ids=input_ids, attention_mask=attention_mask,
                          token_type_ids=token_type_ids).last_hidden_state


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "runs" / "candidate")
    parser.add_argument("--epochs", type=int, default=5)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--lr", type=float, default=2.5e-5)
    parser.add_argument("--queue-size", type=int, default=1024)
    args = parser.parse_args()
    if not 1 <= args.epochs <= 10:
        parser.error("--epochs must be 1 to 10")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    random.seed(42)
    np.random.seed(42)
    torch.manual_seed(42)
    torch.cuda.manual_seed_all(42)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    tokenizer = AutoTokenizer.from_pretrained(BASE, local_files_only=True)
    student = AutoModel.from_pretrained(BASE, local_files_only=True).to(device)
    teacher = AutoModel.from_pretrained(BASE, local_files_only=True).to(device).eval()
    teacher.requires_grad_(False)
    train = read_pairs(DATA / "train.jsonl")
    valid = read_pairs(DATA / "validation.jsonl")

    def collate(rows):
        return [r["fil"] for r in rows], [r["en"] for r in rows]

    loader = DataLoader(train, batch_size=args.batch_size, shuffle=True, collate_fn=collate,
                        generator=torch.Generator().manual_seed(42))

    # Initialize memory bank queue
    queue = []
    with torch.no_grad():
        for fil_text, en_text in loader:
            en = tokenizer(en_text, padding=True, truncation=True, max_length=128, return_tensors="pt").to(device)
            target = pooled(teacher, en)
            queue.append(target)
            if sum(x.size(0) for x in queue) >= args.queue_size:
                break
    queue_en = torch.cat(queue, dim=0)[:args.queue_size].detach()

    optimizer = torch.optim.AdamW(student.parameters(), lr=args.lr, weight_decay=0.01)
    steps = len(loader) * args.epochs
    warmup = max(1, math.ceil(steps * 0.1))
    scheduler = torch.optim.lr_scheduler.LambdaLR(
        optimizer, lambda step: min((step + 1) / warmup, max(0.0, (steps - step) / max(1, steps - warmup))))
    best = -1.0
    history = []
    checkpoint = output / "best_state.pt"
    queue_ptr = 0
    temp = 0.05

    for epoch in range(1, args.epochs + 1):
        student.train()
        total = 0.0
        for fil_text, en_text in loader:
            bs = len(fil_text)
            fil = tokenizer(fil_text, padding=True, truncation=True, max_length=128,
                            return_tensors="pt").to(device)
            en = tokenizer(en_text, padding=True, truncation=True, max_length=128,
                           return_tensors="pt").to(device)
            with torch.no_grad():
                target = pooled(teacher, en)
            fil_vec = pooled(student, fil)
            en_vec = pooled(student, en)

            # Positive logits
            l_pos = (fil_vec * target).sum(dim=1, keepdim=True) / temp

            # Negative logits against memory bank
            l_neg = fil_vec @ queue_en.T / temp

            # In-batch negatives (mask self)
            l_inbatch = fil_vec @ target.T / temp
            mask_self = torch.eye(bs, dtype=torch.bool, device=device)
            l_inbatch_neg = l_inbatch.masked_fill(mask_self, -1e4)

            logits = torch.cat([l_pos, l_neg, l_inbatch_neg], dim=1)
            labels = torch.zeros(bs, dtype=torch.long, device=device)

            loss_contrast = F.cross_entropy(logits, labels)
            loss_mse_fil = F.mse_loss(fil_vec, target)
            loss_mse_en = F.mse_loss(en_vec, target)

            loss = loss_contrast + 1.0 * loss_mse_fil + 0.5 * loss_mse_en

            optimizer.zero_grad(set_to_none=True)
            loss.backward()
            torch.nn.utils.clip_grad_norm_(student.parameters(), 1.0)
            optimizer.step()
            scheduler.step()
            total += float(loss.detach())

            # Update queue
            with torch.no_grad():
                end_ptr = queue_ptr + bs
                if end_ptr <= args.queue_size:
                    queue_en[queue_ptr:end_ptr] = target.detach()
                    queue_ptr = end_ptr % args.queue_size
                else:
                    first_part = args.queue_size - queue_ptr
                    queue_en[queue_ptr:] = target[:first_part].detach()
                    remainder = bs - first_part
                    queue_en[:remainder] = target[first_part:].detach()
                    queue_ptr = remainder

        top1 = validation_top1(student, tokenizer, valid, device)
        record = {"epoch": epoch, "training_loss": total / len(loader), "alt_validation_top1": top1}
        history.append(record)
        (output / "training_history.json").write_text(json.dumps(history, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(record), flush=True)
        if top1 > best:
            best = top1
            torch.save(student.cpu().state_dict(), checkpoint)
            student.to(device)

    student.load_state_dict(torch.load(checkpoint, map_location=device, weights_only=True))
    student.cpu().eval()
    export = Export(student)
    ids = torch.ones((1, 8), dtype=torch.long)
    mask = torch.ones_like(ids)
    onnx_path = output / "model_fp32.onnx"
    torch.onnx.export(export, (ids, mask, torch.zeros_like(ids)), str(onnx_path),
                      input_names=["input_ids", "attention_mask", "token_type_ids"],
                      output_names=["last_hidden_state"],
                      dynamic_axes={name: {0: "batch_size", 1: "sequence_length"} for name in
                                    ["input_ids", "attention_mask", "token_type_ids", "last_hidden_state"]},
                      opset_version=17, dynamo=False)
    from onnxruntime.quantization import QuantType, quantize_dynamic

    quantize_dynamic(str(onnx_path), str(output / "model_quantized.onnx"), weight_type=QuantType.QInt8)
    for filename in ("config.json", "tokenizer.json", "tokenizer_config.json", "vocab.txt", "special_tokens_map.json"):
        shutil.copy2(BASE / filename, output / filename)
    report = {"seed": 42, "epochs": args.epochs, "batch_size": args.batch_size,
              "learning_rate": args.lr, "temperature": temp, "queue_size": args.queue_size,
              "device": str(device), "history": history, "best_alt_validation_top1": best,
              "quantized_bytes": (output / "model_quantized.onnx").stat().st_size}
    (output / "training.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2), flush=True)


if __name__ == "__main__":
    main()
