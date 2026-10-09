"""Gate 3 step 4: compare the candidate with the current model on the plan's gates and publish the findings.

Reads $GATE3_WORK/results/<name>.json written by `gradlew gate3` (jvm-check, the app's own Kotlin code) and writes
gate3/results/gates.json + gates.md (committed). Both models are compared at their own dev-tuned thresholds (fair);
the shipped defaults are reported too. Phone latency is typed in from the test app's speed test:
    python gates.py [--baseline shipped] [--candidate gate3-minilm-tl] [--phone-p95 <baseline ms> <candidate ms>]
"""
import argparse
import csv
import datetime
import json

from common import AI_LOCAL, CHECKPOINTS, MANIFEST, NOTES_SET, WORK

OUT = AI_LOCAL / "gate3" / "results"


def load(name):
    return json.loads((WORK / "results" / f"{name}.json").read_text(encoding="utf-8"))


def metric(r, which, group, key):
    return r["test"][which][group][key]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--baseline", default="shipped")
    ap.add_argument("--candidate", default="gate3-minilm-tl")
    ap.add_argument("--phone-p95", nargs=2, type=float, metavar=("BASELINE_MS", "CANDIDATE_MS"))
    args = ap.parse_args()
    b, c = load(args.baseline), load(args.candidate)
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))

    rows = []

    def gate(name, base, cand, ok, need):
        rows.append({"gate": name, "baseline": base, "candidate": cand, "required": need, "pass": ok})

    for which in ("devTuned",):
        rb = metric(b, which, "lang_tl_or_taglish", "recall"); rc = metric(c, which, "lang_tl_or_taglish", "recall")
        gate("Tagalog/Taglish disclosure recall (test)", rb, rc, None if rb is None else rc >= rb + 0.10, ">= baseline + 10 points")
        fb = metric(b, which, "all", "falseAlarmRate"); fc = metric(c, which, "all", "falseAlarmRate")
        gate("False-alarm rate (test)", fb, fc, fc <= fb and fc <= 0.10, "<= baseline and <= 10%")
        eb = metric(b, which, "lang_en", "recall"); ec = metric(c, which, "lang_en", "recall")
        gate("English disclosure recall (test)", eb, ec, ec >= eb - 0.02, ">= baseline - 2 points")
    tb, tc = b["tatoeba"]["mean"], c["tatoeba"]["mean"]
    gate("Tatoeba tgl-eng top-1 (mean of both directions)", tb, tc, tc >= tb + 0.10, ">= baseline + 10 points")
    gate("Quantized model size (MB)", round(b["modelBytes"] / 1e6, 1), round(c["modelBytes"] / 1e6, 1), c["modelBytes"] < 30e6, "< 30 MB")
    if args.phone_p95:
        pb, pc = args.phone_p95
        gate("Warmed-up Gate 3 p95 on the Tecno Spark (ms)", pb, pc, pc < 10, "< 10 ms")
    else:
        gate("Warmed-up Gate 3 p95 on the Tecno Spark (ms)", None, None, None, "< 10 ms (run the test app's speed test)")

    checks = {
        "parity_token_mismatches": c.get("parity", {}).get("tokenIdMismatches"),
        "parity_cosine_min": c.get("parity", {}).get("cosineMin"),
        "alt_pairs_overlapping_eval_sets": manifest["overlap_check"]["alt_pairs_overlapping_after_filtering"],
        "notes_set_review": c.get("notesSetReview"),
    }
    measured = [r for r in rows if r["pass"] is not None]
    verdict = ("PASS" if all(r["pass"] for r in rows) else "PENDING (phone gate or label review)" if all(r["pass"] for r in measured)
               else "FAIL: keep the current model")

    def model_summary(r):
        keep = {k: r[k] for k in ("name", "modelBytes", "tatoeba", "thresholds", "latencyPcMs") if k in r}
        keep["test"] = {w: {g: r["test"][w][g] for g in ("all", "lang_en", "lang_tl", "lang_taglish", "lang_tl_or_taglish", "anchor_present", "anchor_absent")}
                        for w in ("default", "devTuned")}
        keep["test"]["auc"], keep["test"]["ownNoteTop1"] = r["test"]["auc"], r["test"]["ownNoteTop1"]
        keep["dev"] = {w: r["dev"][w]["all"] for w in ("default", "devTuned")}
        return keep

    OUT.mkdir(parents=True, exist_ok=True)
    report = {"date": datetime.date.today().isoformat(), "verdict": verdict, "gates": rows, "checks": checks,
              "baseline": model_summary(b), "candidate": model_summary(c)}
    (OUT / "gates.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")

    def f(x, pct=True):
        if x is None: return "-"
        if isinstance(x, bool): return "yes" if x else "no"
        return f"{100 * x:.1f}%" if pct and isinstance(x, float) and x <= 1 else f"{x}"

    md = [f"# Gate 3 results ({report['date']})", "", f"**Verdict: {verdict}**", "",
          f"Baseline `{args.baseline}` vs candidate `{args.candidate}`, test split of the held-out private-note set "
          f"(labels: {checks['notes_set_review']}), each at its own dev-tuned thresholds "
          f"(baseline {b['thresholds']['devTuned']['high']}/{b['thresholds']['devTuned']['floor']}, "
          f"candidate {c['thresholds']['devTuned']['high']}/{c['thresholds']['devTuned']['floor']}; similarity alone / anchor + similarity).", "",
          "| Gate | Baseline | Candidate | Required | Pass |", "|---|---|---|---|---|"]
    for r in rows:
        pct = "MB" not in r["gate"] and "ms" not in r["gate"]
        md.append(f"| {r['gate']} | {f(r['baseline'], pct)} | {f(r['candidate'], pct)} | {r['required']} | {'pending' if r['pass'] is None else f(r['pass'])} |")
    md += ["", "| Check | Value |", "|---|---|"] + [f"| {k} | {v} |" for k, v in checks.items()]
    md += ["", "Full numbers (by language, anchor present/absent, confusion counts, defaults and dev-tuned): `gates.json`.",
           "Every draft, both models: `private-notes-predictions.csv`. Validation during training: `training-log.csv`."]
    (OUT / "gates.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    write_predictions_csv(b, c, args.baseline, args.candidate)
    write_training_log()
    print("\n".join(md))


def write_predictions_csv(b, c, bname, cname):
    """One row per draft of the held-out set: text, labels, and each model's score and decisions."""
    texts, note_text = {}, {}
    for n in json.loads(NOTES_SET.read_text(encoding="utf-8"))["notes"]:
        note_text[n["id"]] = n["text"]
        for key, tag in (("disclosures", "d"), ("nonDisclosures", "n")):
            for j, d in enumerate(n[key]):
                texts[f"{n['id']}-{tag}{j}"] = d["text"]
    crow = {r["id"]: r for r in c["rows"]}
    cols = ["id", "split", "note_lang", "draft_lang", "expected", "anchor_in_draft",
            f"{bname}_score", f"{bname}_default", f"{bname}_tuned", f"{cname}_score", f"{cname}_default", f"{cname}_tuned", "draft", "note"]
    with open(OUT / "private-notes-predictions.csv", "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(cols)
        flag = lambda x: "flag" if x else "quiet"
        for r in b["rows"]:
            k = crow[r["id"]]
            w.writerow([r["id"], r["split"], r["noteLang"], r["lang"], "disclosure" if r["disclosure"] else "non-disclosure",
                        "|".join(r["anchors"]), f"{r['score']:.4f}", flag(r["flaggedDefault"]), flag(r["flaggedTuned"]),
                        f"{k['score']:.4f}", flag(k["flaggedDefault"]), flag(k["flaggedTuned"]), texts[r["id"]], note_text[r["id"].split("-")[0]]])


def write_training_log():
    summary = CHECKPOINTS / "train-summary.json"
    if not summary.exists():
        return
    s = json.loads(summary.read_text(encoding="utf-8"))
    best = s["selected"]["step"] if s.get("selected") else None
    with open(OUT / "training-log.csv", "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["step", "epoch", "alt_val_fil_to_en_top1", "alt_val_en_to_fil_top1", "alt_val_top1_mean", "english_cos_to_original", "selected"])
        for h in s["history"]:
            w.writerow([h["step"], h.get("epoch", 0), f"{h['fil_to_en']:.4f}", f"{h['en_to_fil']:.4f}", f"{h['top1_mean']:.4f}",
                        f"{h['english_cos']:.4f}", "yes" if h["step"] == best else ""])
    (OUT / "training-config.json").write_text(json.dumps(s["config"], indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
