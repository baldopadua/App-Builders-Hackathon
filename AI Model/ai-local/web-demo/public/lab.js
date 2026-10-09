// Compares embedding models on the private-note samples. Usage: /lab.html?models=embedding,bge-small,gte-small
import { useEmbedder, noteVectors, noteScores } from '/core.js';

const $ = (id) => document.getElementById(id);
const names = (new URLSearchParams(location.search).get('models') ?? 'embedding').split(',').filter(Boolean);
const data = await (await fetch('/test-data/samples.json')).json();
const results = [];

const auc = (pos, neg) => { let w = 0; for (const p of pos) for (const n of neg) w += p > n ? 1 : p === n ? 0.5 : 0; return w / (pos.length * neg.length); };

for (const name of names) {
  $('status').textContent = `Loading ${name}…`;
  let loadMs;
  try { loadMs = await useEmbedder(name); } catch (e) { results.push({ model: name, error: String(e?.message ?? e) }); continue; }

  const pooledPos = [], pooledNeg = [], perSet = [];
  let totalMs = 0, calls = 0;
  for (const set of data.notes) {
    const notes = [{ id: set.id, label: set.label, text: set.text, vectors: await noteVectors(set.text) }];
    const score = async (t) => { const s = performance.now(); const r = (await noteScores(notes, t))[0].score; totalMs += performance.now() - s; calls++; return r; };
    const pos = [], neg = [];
    for (const t of set.positives) pos.push(await score(t));
    for (const t of set.negatives) neg.push(await score(t));
    pooledPos.push(...pos); pooledNeg.push(...neg);
    perSet.push({ label: set.label, auc: +auc(pos, neg).toFixed(2), minPos: +Math.min(...pos).toFixed(3), maxNeg: +Math.max(...neg).toFixed(3) });
  }
  let best = { th: 0, acc: -1 };
  const total = pooledPos.length + pooledNeg.length;
  for (let th = 0.05; th <= 0.95; th += 0.01) {
    const tp = pooledPos.filter((s) => s >= th).length, tn = pooledNeg.filter((s) => s < th).length;
    const acc = (tp + tn) / total;
    if (acc > best.acc) best = { th: +th.toFixed(2), acc: +acc.toFixed(3), fn: pooledPos.length - tp, fp: pooledNeg.length - tn };
  }
  results.push({ model: name, loadMs, msPerCheck: Math.round(totalMs / calls), auc: +auc(pooledPos, pooledNeg).toFixed(3), best, perSet });
}

$('status').textContent = 'Done.';
$('table').innerHTML = '<tr><th>model</th><th>AUC</th><th>best single threshold</th><th>accuracy</th><th>missed / false alarms</th><th>per note: AUC (lowest real / highest false)</th><th>ms per check</th></tr>' +
  results.map((r) => r.error ? `<tr><td>${r.model}</td><td colspan="6">ERROR ${r.error}</td></tr>` :
    `<tr><td>${r.model}</td><td><b>${r.auc}</b></td><td>${r.best.th}</td><td>${(r.best.acc * 100).toFixed(0)}%</td><td>${r.best.fn} / ${r.best.fp}</td><td>${r.perSet.map((s) => `${s.label}: ${s.auc} (${s.minPos}/${s.maxNeg})`).join('<br>')}</td><td>${r.msPerCheck}</td></tr>`).join('');
$('result').textContent = JSON.stringify(results, null, 1);
document.title = 'LAB DONE';
