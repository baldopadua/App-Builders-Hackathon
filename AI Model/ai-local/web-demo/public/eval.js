import { loadModels, noteVectors, noteScores, notesResult, checkDetailed, hide, hideAll, NOTE_DEFAULTS, teachValues, fingerprint, taughtResult } from '/core.js';

const $ = (id) => document.getElementById(id);

const data = await (await fetch('/test-data/samples.json')).json();
const t = await loadModels();
$('status').textContent = `Models loaded: PII ${t.pii} ms, embeddings ${t.emb} ms. Scoring ${data.detect.length} messages and ${data.notes.length} private notes...`;

const esc = (s) => s.replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]));
const out = { detect: [], notes: [] };

// ---- 1. detection: rules + PII model --------------------------------------------------------------
let rows = '', ok = 0, fp = 0, miss = 0, wrong = 0;
for (const s of data.detect) {
  const r = await checkDetailed(s.text, 99, []);          // threshold 99 = private notes off
  const got = r.result?.category ?? 'none';
  const pass = got === s.expect;
  if (pass) ok++; else if (s.expect === 'none') fp++; else if (got === 'none') miss++; else wrong++;
  out.detect.push({ text: s.text, expect: s.expect, got, stage: r.stage, pass });
  rows += `<tr class="${pass ? 'pass' : 'fail'}"><td>${pass ? '✓' : '✗'}</td><td>${esc(s.text)}</td><td>${s.expect}</td><td>${got}${r.stage ? ` <span class="muted">(${r.stage})</span>` : ''}</td></tr>`;
}
const n = data.detect.length;
$('detect').innerHTML = `<h2>1. Detection (rules + PII model)</h2>
  <p><b>${ok}/${n} correct</b> · false alarms (should be quiet): <b>${fp}</b> · missed risks: <b>${miss}</b> · wrong category: <b>${wrong}</b></p>
  <table><tr><th></th><th>message</th><th>should be</th><th>got</th></tr>${rows}</table>`;
out.detectSummary = { total: n, correct: ok, falseAlarms: fp, missed: miss, wrongCategory: wrong };

// ---- 1b. "Itago": every risky sample, masked, must come back clean ---------------------------------
const noNotes = async (t) => (await checkDetailed(t, 99, [])).result;
let hidOk = 0;
const hidFails = [], examples = [];
const risky = data.detect.filter((s) => s.expect !== 'none');
for (const s of risky) {
  const after = await hideAll(s.text, noNotes);
  const left = await noNotes(after);
  if (!left) hidOk++; else hidFails.push({ after, kind: left.kind });
  if (examples.length < 14 && after !== s.text) examples.push([s.text, after]);
}
$('hide').innerHTML = `<h2>1b. Itago (hide it)</h2>
  <p><b>${hidOk}/${risky.length}</b> risky messages come back clean after masking.</p>
  <table><tr><th>before</th><th>after Itago</th></tr>${examples.map(([a, b]) => `<tr class="pass"><td>${esc(a)}</td><td>${esc(b)}</td></tr>`).join('')}</table>` +
  (hidFails.length ? `<p class="bad">Still warning: ${hidFails.map((f) => esc(`${f.kind}: ${f.after}`)).join('<br>')}</p>` : '');
out.hideSummary = { total: risky.length, clean: hidOk, fails: hidFails };

// ---- 2. private notes: anchor + similarity rule ---------------------------------------------------
let html = `<h2>2. Private notes (anchors + embedding similarity)</h2>
  <p class="muted">Flag when the draft contains one of the note's anchors (names / code words) and similarity ≥ ${NOTE_DEFAULTS.floor}, or when similarity alone ≥ ${NOTE_DEFAULTS.high}.</p>`;
out.noteSummary = [];
let totTp = 0, totFn = 0, totFp = 0, totTn = 0;
for (const set of data.notes) {
  const notes = [{ id: set.id, label: set.label, text: set.text, vectors: await noteVectors(set.text) }];
  const scored = [];
  for (const [kind, list] of [['should flag', set.positives], ['should NOT', set.negatives]]) {
    for (const text of list) {
      const s = (await noteScores(notes, text))[0];
      const res = notesResult([s]);
      scored.push({ kind, text, score: s.score, anchors: s.anchors, flagged: Boolean(res), via: res ? (s.anchorsNeeded > 0 && s.anchors.length >= s.anchorsNeeded && s.score >= NOTE_DEFAULTS.floor ? 'anchor' : 'similarity') : '' });
    }
  }
  const tp = scored.filter((x) => x.kind === 'should flag' && x.flagged).length;
  const fn = scored.filter((x) => x.kind === 'should flag' && !x.flagged).length;
  const fpc = scored.filter((x) => x.kind === 'should NOT' && x.flagged).length;
  const tn = scored.filter((x) => x.kind === 'should NOT' && !x.flagged).length;
  totTp += tp; totFn += fn; totFp += fpc; totTn += tn;

  html += `<h3>${esc(set.label)}</h3><p class="muted">${esc(set.text)}</p>
    <p>Flagged <b>${tp}/${tp + fn}</b> real paraphrases · false alarms <b>${fpc}/${fpc + tn}</b></p>
    <table><tr><th></th><th>score</th><th>anchor in draft</th><th>kind</th><th>result</th><th>message</th></tr>` +
    scored.sort((a, b) => b.score - a.score).map((x) => {
      const right = (x.kind === 'should flag') === x.flagged;
      return `<tr class="${right ? 'pass' : 'fail'}"><td>${right ? '✓' : '✗'}</td><td>${x.score.toFixed(3)}</td><td>${esc(x.anchors.join(', '))}</td><td>${x.kind}</td><td>${x.flagged ? 'flag (' + x.via + ')' : 'quiet'}</td><td>${esc(x.text)}</td></tr>`;
    }).join('') + '</table>';
  out.noteSummary.push({ label: set.label, flagged: `${tp}/${tp + fn}`, falseAlarms: `${fpc}/${fpc + tn}`,
    rows: scored.map((x) => ({ kind: x.kind, score: +x.score.toFixed(3), anchors: x.anchors, flagged: x.flagged, via: x.via, text: x.text })) });
}
const acc = (totTp + totTn) / (totTp + totFn + totFp + totTn);
html = html.replace('</p>', `</p><p><b>All sets: flagged ${totTp}/${totTp + totFn} real paraphrases, ${totFp}/${totFp + totTn} false alarms, ${(acc * 100).toFixed(0)}% accuracy.</b></p>`);
out.noteTotals = { flagged: `${totTp}/${totTp + totFn}`, falseAlarms: `${totFp}/${totFp + totTn}`, accuracy: +acc.toFixed(3) };
$('notes').innerHTML = html;

// ---- 3. taught secret values (MPIN, password, key): fingerprint only, exact match ------------------
let tOk = 0, tTotal = 0, trows = '';
for (const t of data.taught ?? []) {
  const values = teachValues(t.teach);
  const kind = values.length ? 'secret' : 'fact';
  tTotal++; if (kind === t.kind) tOk++;
  trows += `<tr class="${kind === t.kind ? 'pass' : 'fail'}"><td>${kind === t.kind ? '✓' : '✗'}</td><td><b>teach</b> ${esc(t.teach)}</td><td>${esc(t.label)}</td><td>${kind}</td></tr>`;
  const notes = values.length ? [{ label: t.label, fps: await Promise.all(values.map(fingerprint)) }] : [];
  for (const [should, text] of [...t.positives.map((x) => [true, x]), ...t.negatives.map((x) => [false, x])]) {
    const r = await taughtResult(notes, text);
    const ok = Boolean(r) === should;
    tTotal++; if (ok) tOk++;
    trows += `<tr class="${ok ? 'pass' : 'fail'}"><td>${ok ? '✓' : '✗'}</td><td>${esc(text)}</td><td>${should ? 'should catch' : 'should NOT'}</td><td>${r ? 'CATCH "' + esc(text.slice(r.start, r.end)) + '"' : 'quiet'}</td></tr>`;
  }
}
const demo = 'eto na yung 4821, bilisan mo';
const before = (await checkDetailed(demo, 99, [])).result;
const taughtNotes = [{ label: 'GCash MPIN', fps: [await fingerprint('4821')] }];
const after = (await checkDetailed(demo, 99, taughtNotes)).result;
const wow = { text: demo, before: before?.kind ?? 'no warning', after: after?.kind ?? 'no warning', itago: after ? hide(demo, after) : null };
$('taught').innerHTML = `<h2>3. Teach me: secret values (fingerprint only, exact match)</h2>
  <p><b>${tOk}/${tTotal}</b> checks correct · wow check: "${esc(demo)}" → before teaching: <b>${wow.before}</b>, after teaching 4821: <b>${wow.after}</b> → Itago: ${esc(wow.itago ?? '')}</p>
  <table><tr><th></th><th>text</th><th>expect</th><th>got</th></tr>${trows}</table>`;
out.taughtSummary = { correct: tOk, total: tTotal, wow };
$('status').textContent = 'Done.';
$('result').textContent = JSON.stringify(out, null, 1);
document.title = 'EVAL DONE';
