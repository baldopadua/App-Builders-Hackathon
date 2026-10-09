// NLI (entailment) lab: does the draft imply a fact from the private note?
// Usage: /nli.html?models=nli-mobilebert,nli-deberta-xsmall&tpl=0
import { pipeline } from '/vendor/transformers/transformers.min.js';
import '/core.js';   // sets up env (local models only, local wasm)
import { parts } from '/core.js';

const $ = (id) => document.getElementById(id);
const q = new URLSearchParams(location.search);
const names = (q.get('models') ?? 'nli-mobilebert').split(',').filter(Boolean);
const TEMPLATES = ['{}.', 'The message reveals that {}.'];
const data = await (await fetch('/test-data/samples.json')).json();
const auc = (pos, neg) => { let w = 0; for (const p of pos) for (const n of neg) w += p > n ? 1 : p === n ? 0.5 : 0; return w / (pos.length * neg.length); };
const claims = (text) => parts(text, 10).map(([a, b]) => text.slice(a, b).trim().replace(/[.!?]+$/, ''));

const results = [];
for (const name of names) {
  $('status').textContent = `Loading ${name}…`;
  let clf;
  try { clf = await pipeline('zero-shot-classification', name, { dtype: 'q8', device: 'wasm' }); } catch (e) { results.push({ model: name, error: String(e?.message ?? e) }); continue; }
  for (const [ti, tpl] of TEMPLATES.entries()) {
    const pooledPos = [], pooledNeg = [], perSet = [], rows = [];
    for (const set of data.notes) {
      const labels = claims(set.text);
      const score = async (draft) => {
        const out = await clf(draft, labels, { hypothesis_template: tpl, multi_label: true });
        return Math.max(...out.scores);
      };
      const pos = [], neg = [];
      for (const t of set.positives) { const s = await score(t); pos.push(s); rows.push([set.label, 'POS', s, t]); }
      for (const t of set.negatives) { const s = await score(t); neg.push(s); rows.push([set.label, 'neg', s, t]); }
      pooledPos.push(...pos); pooledNeg.push(...neg);
      perSet.push({ label: set.label, auc: +auc(pos, neg).toFixed(2), minPos: +Math.min(...pos).toFixed(3), maxNeg: +Math.max(...neg).toFixed(3) });
    }
    let best = { th: 0, acc: -1 };
    const total = pooledPos.length + pooledNeg.length;
    for (let th = 0.02; th <= 0.99; th += 0.01) {
      const tp = pooledPos.filter((s) => s >= th).length, tn = pooledNeg.filter((s) => s < th).length;
      const acc = (tp + tn) / total;
      if (acc > best.acc) best = { th: +th.toFixed(2), acc: +acc.toFixed(3), fn: pooledPos.length - tp, fp: pooledNeg.length - tn };
    }
    results.push({ model: name, template: ti, auc: +auc(pooledPos, pooledNeg).toFixed(3), best, perSet, rows: rows.map(([l, k, s, t]) => `${s.toFixed(3)} ${k} [${l}] ${t}`).sort().reverse() });
  }
}
$('status').textContent = 'Done.';
$('table').innerHTML = '<tr><th>model</th><th>template</th><th>AUC</th><th>best threshold</th><th>accuracy</th><th>missed / false alarms</th><th>per note</th></tr>' +
  results.map((r) => r.error ? `<tr><td>${r.model}</td><td colspan="6">ERROR ${r.error}</td></tr>` :
    `<tr><td>${r.model}</td><td>${r.template}</td><td><b>${r.auc}</b></td><td>${r.best.th}</td><td>${(r.best.acc * 100).toFixed(0)}%</td><td>${r.best.fn} / ${r.best.fp}</td><td>${r.perSet.map((s) => `${s.label}: ${s.auc} (${s.minPos}/${s.maxNeg})`).join('<br>')}</td></tr>`).join('');
$('result').textContent = JSON.stringify(results, null, 1);
document.title = 'NLI DONE';
