"""Gate 3 step 2: fine-tune all-MiniLM-L6-v2 so Filipino sentences land next to their English translations.

Student: the pinned all-MiniLM-L6-v2 weights (same tokenizer, 384-d mean-pooled output, so the app code is unchanged).
Loss per batch of ALT (fil, en) pairs:
  contrastive  symmetric in-batch InfoNCE (scale 20) between student(fil) and student(en)
  english      ENGLISH_WEIGHT * ||student(en) - frozen_original(en)||^2   (keeps English close to the shipped model)
At most 3 epochs, seed 42. Evaluated on the ALT validation split every EVAL_EVERY steps; the selected checkpoint is
the best fil<->en top-1 retrieval among checkpoints whose English output stays within MIN_ENGLISH_COS of the original.
Tatoeba and the private-note set are never read here.

Resumable: progress is saved to $GATE3_WORK/checkpoints/last.pt; rerunning continues where it stopped.
    <venv>\\Scripts\\python.exe "AI Model\\ai-local\\gate3\\train\\train.py" [--bench 5]
"""
import argparse
import json
import math
import random
import time

import numpy as np
import torch
from transformers import AutoModel, AutoTokenizer, get_linear_schedule_with_warmup

from common import CHECKPOINTS, DATA, MAX_TOKENS, SEED, ensure_base_model, mean_pool, read_tsv

EPOCHS = 3
BATCH = 32
LR = 3e-5
WARMUP = 0.1
SCALE = 20.0
ENGLISH_WEIGHT = 5.0
EVAL_EVERY = 250
MIN_ENGLISH_COS = 0.97


def seed_everything(seed):
    random.seed(seed); np.random.seed(seed); torch.manual_seed(seed)


def batches_for_epoch(pairs, epoch, tok_len):
    """Deterministic per epoch: shuffle, sort chunks by length (less padding), shuffle the batch order."""
    rng = random.Random(SEED + epoch)
    idx = list(range(len(pairs)))
    rng.shuffle(idx)
    chunk = BATCH * 50
    batches = []
    for s in range(0, len(idx), chunk):
        part = sorted(idx[s:s + chunk], key=lambda i: tok_len[i])
        batches += [part[b:b + BATCH] for b in range(0, len(part), BATCH) if len(part[b:b + BATCH]) == BATCH]
    rng.shuffle(batches)
    return batches


def encode(tok, texts):
    return tok(texts, padding=True, truncation=True, max_length=MAX_TOKENS, return_tensors="pt")


def embed(model, enc):
    return mean_pool(model(**enc).last_hidden_state, enc["attention_mask"])


@torch.no_grad()
def evaluate(student, teacher, tok, val):
    student.eval()
    fil, en, ten = [], [], []
    for s in range(0, len(val), 64):
        part = val[s:s + 64]
        fil.append(embed(student, encode(tok, [p[0] for p in part])))
        e = encode(tok, [p[1] for p in part])
        en.append(embed(student, e)); ten.append(embed(teacher, e))
    fil, en, ten = torch.cat(fil), torch.cat(en), torch.cat(ten)
    sim = fil @ en.T
    target = torch.arange(len(val))
    f2e = (sim.argmax(1) == target).float().mean().item()
    e2f = (sim.argmax(0) == target).float().mean().item()
    eng_cos = (en * ten).sum(1).mean().item()
    student.train()
    return {"fil_to_en": f2e, "en_to_fil": e2f, "top1_mean": (f2e + e2f) / 2, "english_cos": eng_cos}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--bench", type=int, default=0, help="only time this many training steps, save nothing")
    args = ap.parse_args()

    seed_everything(SEED)
    base = ensure_base_model()
    tok = AutoTokenizer.from_pretrained(base)
    # No dropout: the English term then measures real drift from the original (0 at step 0), not dropout noise.
    student = AutoModel.from_pretrained(base, hidden_dropout_prob=0.0, attention_probs_dropout_prob=0.0)
    teacher = AutoModel.from_pretrained(base).eval()
    for p in teacher.parameters():
        p.requires_grad_(False)
    student.gradient_checkpointing_enable(gradient_checkpointing_kwargs={"use_reentrant": False})
    student.train()

    train = read_tsv(DATA / "alt-train.tsv")
    val = read_tsv(DATA / "alt-validation.tsv")
    tok_len = [len(tok(p[0], truncation=True, max_length=MAX_TOKENS)["input_ids"]) for p in train]
    lens = sorted(tok_len)
    print(f"ALT train {len(train)} pairs, validation {len(val)}. Filipino tokens: median {lens[len(lens) // 2]}, "
          f"p95 {lens[int(len(lens) * .95)]}, truncated at {MAX_TOKENS}: {sum(1 for x in lens if x >= MAX_TOKENS)}", flush=True)

    steps_per_epoch = len(batches_for_epoch(train, 0, tok_len))
    total = steps_per_epoch * EPOCHS
    opt = torch.optim.AdamW(student.parameters(), lr=LR, weight_decay=0.01)
    sched = get_linear_schedule_with_warmup(opt, int(total * WARMUP), total)

    CHECKPOINTS.mkdir(parents=True, exist_ok=True)
    last, best_dir, log_path = CHECKPOINTS / "last.pt", CHECKPOINTS / "best", CHECKPOINTS / "train-log.jsonl"
    step, best, history = 0, None, []
    if last.exists() and not args.bench:
        st = torch.load(last, weights_only=False)
        student.load_state_dict(st["student"]); opt.load_state_dict(st["opt"]); sched.load_state_dict(st["sched"])
        step, best, history = st["step"], st["best"], st["history"]
        random.setstate(st["py_rng"]); np.random.set_state(st["np_rng"]); torch.set_rng_state(st["torch_rng"])
        print(f"Resuming at step {step}/{total} (best so far: {best})", flush=True)
    elif not args.bench:
        base_eval = evaluate(student, teacher, tok, val)
        history.append({"step": 0, **base_eval})
        print(f"step 0 (original model) validation: {base_eval}", flush=True)

    t_start, s_start = time.time(), step
    while step < total:
        epoch, k = divmod(step, steps_per_epoch)
        batch = batches_for_epoch(train, epoch, tok_len)[k]
        fil = encode(tok, [train[i][0] for i in batch])
        en = encode(tok, [train[i][1] for i in batch])
        s_fil, s_en = embed(student, fil), embed(student, en)
        with torch.no_grad():
            t_en = embed(teacher, en)
        logits = SCALE * s_fil @ s_en.T
        target = torch.arange(len(batch))
        contrastive = (torch.nn.functional.cross_entropy(logits, target) + torch.nn.functional.cross_entropy(logits.T, target)) / 2
        english = ((s_en - t_en) ** 2).sum(1).mean()
        loss = contrastive + ENGLISH_WEIGHT * english
        opt.zero_grad(set_to_none=True)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(student.parameters(), 1.0)
        opt.step(); sched.step()
        step += 1

        if step % 10 == 0 or args.bench:
            rate = (time.time() - t_start) / (step - s_start)
            print(f"step {step}/{total} epoch {epoch + 1} loss {loss.item():.4f} (contrastive {contrastive.item():.4f}, "
                  f"english {english.item():.5f}) {rate:.2f} s/step, ETA {(total - step) * rate / 60:.0f} min", flush=True)
        if args.bench and step - s_start >= args.bench:
            return
        if step % EVAL_EVERY == 0 or step == total:
            ev = {"step": step, "epoch": round(step / steps_per_epoch, 2), **evaluate(student, teacher, tok, val)}
            history.append(ev)
            eligible = ev["english_cos"] >= MIN_ENGLISH_COS
            if eligible and (best is None or ev["top1_mean"] > best["top1_mean"]):
                best = ev
                student.save_pretrained(best_dir); tok.save_pretrained(best_dir)
            with open(log_path, "a", encoding="utf-8") as f:
                f.write(json.dumps(ev) + "\n")
            print(f"validation {ev} {'(new best)' if best is ev else '' if eligible else '(English drifted, not eligible)'}", flush=True)
            torch.save({"student": student.state_dict(), "opt": opt.state_dict(), "sched": sched.state_dict(), "step": step,
                        "best": best, "history": history, "py_rng": random.getstate(), "np_rng": np.random.get_state(),
                        "torch_rng": torch.get_rng_state()}, last)

    summary = {"config": {"epochs": EPOCHS, "batch": BATCH, "lr": LR, "warmup": WARMUP, "scale": SCALE,
                          "english_weight": ENGLISH_WEIGHT, "eval_every": EVAL_EVERY, "min_english_cos": MIN_ENGLISH_COS,
                          "seed": SEED, "max_tokens": MAX_TOKENS, "steps": total},
               "selected": best, "history": history}
    (CHECKPOINTS / "train-summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(f"Done. Selected checkpoint: {best}", flush=True)


if __name__ == "__main__":
    main()
