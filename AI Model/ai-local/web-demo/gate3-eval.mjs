// Gate 3 twin check: runs the held-out private-note set through core.js (noteVectors / noteScores / notesResult, the
// JS twin of NoteMatcher) and compares every decision with the Kotlin run (jvm-check `gradlew gate3`).
// Node only, no browser, no network: transformers.js runs the same ONNX file through onnxruntime-node.
//   node gate3-eval.mjs                                        shipped model vs $GATE3_WORK/results/shipped.json
//   node gate3-eval.mjs model=<folder> kotlin=<results.json>   any candidate (e.g. ../../../gate3-work/models/gate3-minilm-tl)
import fs from 'node:fs';
import path from 'node:path';
import { register } from 'node:module';
import { fileURLToPath } from 'node:url';

// core.js imports transformers.js from the demo server's /vendor path; point that at the npm package instead.
register('data:text/javascript,' + encodeURIComponent(
  "export async function resolve(s, c, next) { return next(s === '/vendor/transformers/transformers.min.js' ? '@huggingface/transformers' : s, c); }",
), import.meta.url);

const here = path.dirname(fileURLToPath(import.meta.url));
const AI_LOCAL = path.resolve(here, '..');
const WORK = path.resolve(process.env.GATE3_WORK ?? path.join(AI_LOCAL, '..', '..', '..', 'gate3-work'));
const args = Object.fromEntries(process.argv.slice(2).filter((a) => a.includes('=')).map((a) => [a.slice(0, a.indexOf('=')), a.slice(a.indexOf('=') + 1)]));
const modelDir = path.resolve(args.model ?? path.join(AI_LOCAL, 'assets', 'models', 'embedding'));
const name = args.name ?? (args.model ? path.basename(modelDir) : 'shipped');
const kotlinFile = args.kotlin ?? path.join(WORK, 'results', `${name}.json`);

// transformers.js wants <root>/<name>/{config.json, tokenizer*.json, onnx/model_quantized.onnx}
const stage = path.join(WORK, 'js-models', name);
fs.mkdirSync(path.join(stage, 'onnx'), { recursive: true });
for (const f of ['config.json', 'tokenizer.json', 'tokenizer_config.json', 'special_tokens_map.json']) {
  if (fs.existsSync(path.join(modelDir, f))) fs.copyFileSync(path.join(modelDir, f), path.join(stage, f));
}
fs.copyFileSync(path.join(modelDir, 'model_quantized.onnx'), path.join(stage, 'onnx', 'model_quantized.onnx'));

const core = await import('./public/core.js');
const { env } = await import('@huggingface/transformers');
env.localModelPath = path.join(WORK, 'js-models') + path.sep;
env.allowLocalModels = true;
env.allowRemoteModels = false;
await core.useEmbedder(name, 'cpu');

const set = JSON.parse(fs.readFileSync(path.join(AI_LOCAL, 'test-data', 'gate3', 'private-notes.json'), 'utf8'));
const kotlin = fs.existsSync(kotlinFile) ? JSON.parse(fs.readFileSync(kotlinFile, 'utf8')) : null;
const tuned = kotlin?.thresholds?.devTuned;
const byId = new Map((kotlin?.rows ?? []).map((r) => [r.id, r]));

const rows = [];
for (const split of ['dev', 'test']) {
  const notes = [];
  for (const n of set.notes.filter((x) => x.split === split)) notes.push({ id: n.id, label: n.label, text: n.text, vectors: await core.noteVectors(n.text) });
  for (const n of set.notes.filter((x) => x.split === split)) {
    for (const [key, disclosure, tag] of [['disclosures', true, 'd'], ['nonDisclosures', false, 'n']]) {
      for (const [j, d] of n[key].entries()) {
        const scores = await core.noteScores(notes, d.text);
        const own = scores.find((s) => s.label === n.label);
        rows.push({
          id: `${n.id}-${tag}${j}`, split, lang: d.lang, disclosure, score: own.score,
          ownRank: 1 + scores.filter((s) => s.score > own.score).length,
          flaggedDefault: Boolean(core.notesResult([own])),
          flaggedTuned: tuned ? Boolean(core.notesResult([own], tuned.high, tuned.floor)) : null,
        });
      }
    }
  }
}

const pct = (n, d) => (d ? `${((100 * n) / d).toFixed(1)}%` : '-');
const summary = (rs, key) => {
  const pos = rs.filter((r) => r.disclosure), neg = rs.filter((r) => !r.disclosure);
  const tp = pos.filter((r) => r[key]).length, fp = neg.filter((r) => r[key]).length;
  return `recall ${pct(tp, pos.length)} (${tp}/${pos.length})  false alarms ${pct(fp, neg.length)} (${fp}/${neg.length})`;
};
const test = rows.filter((r) => r.split === 'test');
console.log(`== Gate 3 JS twin (core.js): ${name}`);
for (const [label, key] of [['defaults', 'flaggedDefault'], [`dev-tuned ${tuned ? `${tuned.high}/${tuned.floor}` : '(no Kotlin run)'}`, 'flaggedTuned']]) {
  if (key === 'flaggedTuned' && !tuned) continue;
  console.log(`   -- test split, ${label}`);
  console.log(`   all             ${summary(test, key)}`);
  for (const lang of ['en', 'tl', 'taglish']) console.log(`   draft in ${lang.padEnd(7)} ${summary(test.filter((r) => r.lang === lang), key)}`);
}
if (!kotlin) {
  console.log(`\nNo Kotlin results at ${kotlinFile}: run jvm-check "gradlew gate3" first to compare.`);
} else {
  let decisions = 0, rank = 0, maxDiff = 0;
  for (const r of rows) {
    const k = byId.get(r.id);
    if (!k) { decisions++; continue; }
    if (k.flaggedDefault !== r.flaggedDefault || k.flaggedTuned !== r.flaggedTuned) { decisions++; console.log(`   differs: ${r.id} js ${r.score.toFixed(4)} kotlin ${k.score.toFixed(4)}`); }
    if (k.ownRank !== r.ownRank) rank++;
    maxDiff = Math.max(maxDiff, Math.abs(k.score - r.score));
  }
  console.log(`\nKotlin vs JS on ${rows.length} drafts: ${decisions} decision(s) differ, ${rank} retrieval rank(s) differ, largest score difference ${maxDiff.toFixed(5)}`);
  if (decisions) process.exitCode = 1;
}
