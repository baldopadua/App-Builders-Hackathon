import { loadModels, modelsReady, noteVectors, checkDetailed, hide, tipFor, headlineFor, teachValues, fingerprint, useEmbedder } from '/core.js';

const $ = (id) => document.getElementById(id);
const MAX_NOTES = 5;
const AVAILABLE_EMBEDDERS = ['embedding', 'paraphrase-multi', 'bge-small', 'minilm-l12'];  // models available in assets/models and lab-models

const SAMPLES = [   // all synthetic. Everyday Filipinos first; developer secrets last (secondary audience)
  ['GCash MPIN', 'GCash MPIN ko ay 4821'],
  ['Passcode', 'Passcode ko 0517, ikaw na bahala'],
  ['No clue words (teach 4821 first)', 'eto na yung 4821, bilisan mo'],
  ['Lola: OTP', 'Anak, eto yung code na pinadala sa akin: 482913'],
  ['"Premyo" scam', "Ma'am eto po yung OTP: 902114, para ma-claim ko na yung premyo"],
  ['GCash ref no. (quiet)', 'Bayad na po via GCash. Ref No. 1023 4567 89016, salamat!'],
  ['ATM PIN', 'PIN ng ATM ko 1234, pakikuha mo na lang yung pera'],
  ['Kid: game password', 'Password ng ML account ko: Lodi1234'],
  ['FB password', 'pass ng FB ko: Kitty123'],
  ['TIN', 'TIN ko: 123-456-789-000'],
  ['Card + CVV', 'card: 5555555555554444 cvv 123'],
  ['GCash number', 'Pa-GCash na lang sa 09171234567, salamat!'],
  ['Bank account', 'BDO account no. 0012345678, Juan Dela Cruz'],
  ['Harmless', 'Kumain ka na ba? Ingat pauwi!'],
  ['Jakim (save note first)', "Let's raid Jakim in Taguig"],
  ['No names (save note first)', 'Prices will go up by 20% next month'],
  ['Dev: Gemini key', 'GEMINI_API_KEY=AIzaSyA1b2C3d4E5f6G7h8I9j0KlMnOpQrStUvW'],
  ['Dev: SSH key', 'Eto yung SSH key ng server: -----BEGIN OPENSSH PRIVATE KEY-----'],
];

// Quick test cases (subset of samples.json for quick verification)
const QUICK_TESTS = [
  { input: 'My password is mango72.', expect: 'PASSWORD' },
  { input: 'PIN ng ATM ko 1234', expect: 'PASSWORD' },
  { input: 'GEMINI_API_KEY=AIzaSyA1b2C3d4E5f6G7h8I9j0KlMnOpQrStUvW', expect: 'API_KEY' },
  { input: 'card: 4532 0151 1283 0366 cvv 123', expect: 'CARD' },
  { input: 'TIN ko: 123-456-789-000', expect: 'OTHER_PII' },
  { input: 'email: juan@example.com', expect: 'OTHER_PII' },
  { input: 'Kumain ka na ba? Ingat pauwi!', expect: 'none' },
];

let notes = loadNotes();           // [{id, label, text, vectors:[[...]]}]  (demo only: NOT encrypted)
let version = 0;
let currentEmbedder = 'embedding';  // default embedder model

function loadNotes() { try { return JSON.parse(localStorage.getItem('sl_notes') ?? '[]'); } catch { return []; } }
function saveNotes() { try { localStorage.setItem('sl_notes', JSON.stringify(notes)); } catch { /* storage blocked */ } }

/** Parse URL parameters to get requested embedder model */
function getEmbedderFromURL() {
  const params = new URLSearchParams(location.search);
  const model = params.get('embedder');
  return model && AVAILABLE_EMBEDDERS.includes(model) ? model : 'embedding';
}

/** Re-embed all private notes with the new embedder model */
async function reEmbedNotes(embedder) {
  const notesWithText = notes.filter((n) => n.text && !n.fps);
  if (!notesWithText.length) return;
  
  for (const note of notesWithText) {
    note.vectors = await noteVectors(note.text);
  }
  saveNotes();
  renderNotes();
  schedule();
}

// ---- UI ---------------------------------------------------------------------------------------------

const NAMES = { PASSWORD: 'PASSWORD / CODE', API_KEY: 'API KEY / TOKEN', CARD: 'CARD', PRIVATE_NOTE: 'PRIVATE NOTE', OTHER_PII: 'PERSONAL INFO' };
const threshold = () => 0.2 + Number($('th').value) / 100;

function esc(s) { return s.replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c])); }

let shown = null;   // { text, result } currently on screen, for the "Itago" button

function render(text, r, totalMs) {
  const v = $('verdict');
  shown = r.result ? { text, result: r.result } : null;
  $('tip').textContent = r.result ? '💡 ' + tipFor(r.result.kind) : '';
  $('hideBtn').hidden = !r.result || r.result.kind === 'private_match';
  if (!text.trim()) { v.className = 'verdict idle'; v.textContent = 'Type a message above, or tap a sample.'; $('preview').innerHTML = ''; }
  else if (!r.result) { v.className = 'verdict ok'; v.textContent = '✓ No risk found'; $('preview').textContent = text; }
  else {
    const x = r.result;
    v.className = 'verdict ' + (x.severity === 'MEDIUM' ? 'soft' : 'risk');
    v.innerHTML = `${x.severity === 'MEDIUM' ? '🟡' : '⚠'} ${esc(headlineFor(x.kind, x.label))}<br><span class="reason">${esc(x.reason)} · ${NAMES[x.category]} ${x.confidence.toFixed(2)}</span>`;
    $('preview').innerHTML = esc(text.slice(0, x.start)) + '<mark>' + esc(text.slice(x.start, x.end)) + '</mark>' + esc(text.slice(x.end));
  }
  $('details').textContent = (r.stage ? `Decided by: ${r.stage}\n` : '') +
    r.stages.map((s) => `• ${s.name}: ${s.result ? `HIT ${s.result.category} ${s.result.confidence.toFixed(2)}` : 'no hit'} (${s.ms} ms)`).join('\n') +
    `\nAll stages: ${totalMs} ms`;
  $('debug').textContent = 'PII model entities (all, before filtering):\n' +
    (r.ents.length ? r.ents.map((e) => `  ${e.type} "${e.text}" ${e.score.toFixed(2)}`).join('\n') : '  none') +
    `\n\nPrivate-note similarity (threshold ${threshold().toFixed(2)}):\n` +
    (r.scores.length ? r.scores.map((s) => `  ${s.label}: ${s.score.toFixed(3)}` + (s.anchors.length ? `  [private word in draft: ${s.anchors.join(', ')}]` : '')).join('\n') : '  no notes saved, or text too short');
  netProof();
}

function netProof() {
  const foreign = performance.getEntriesByType('resource').filter((e) => new URL(e.name).origin !== location.origin);
  $('net').textContent = `Requests to other sites since page load: ${foreign.length}` + (foreign.length ? ' → ' + foreign.map((e) => new URL(e.name).host).join(', ') : ' ✓') +
    `   |   browser online: ${navigator.onLine}`;
  $('net').className = foreign.length ? 'bad' : 'good';
}

let timer;
function schedule() { clearTimeout(timer); timer = setTimeout(run, 250); }
async function run() {
  if (!modelsReady()) return;
  const text = $('input').value;
  const v = ++version;
  const t0 = performance.now();
  const r = await checkDetailed(text, threshold(), notes);
  if (v === version) render(text, r, Math.round(performance.now() - t0));
}

function renderNotes() {
  const box = $('notes');
  box.innerHTML = notes.length ? '' : '<div class="muted">Nothing taught yet.</div>';
  for (const n of notes) {
    const row = document.createElement('div'); row.className = 'noterow';
    row.innerHTML = n.fps ? `<b>🔑 ${esc(n.label)}</b> <span class="muted">secret value (fingerprint only)</span>`
      : `<b>🔒 ${esc(n.label)}</b> <span class="muted">private fact (meaning + names)</span>`;
    const del = document.createElement('button'); del.textContent = 'Delete';
    del.onclick = () => { notes = notes.filter((x) => x.id !== n.id); saveNotes(); renderNotes(); schedule(); };
    row.append(del); box.append(row);
  }
}

async function saveNote() {
  const text = $('noteText').value.trim();
  if (!modelsReady()) return alert('Models are still loading');
  if (notes.length >= MAX_NOTES) return alert(`You can keep at most ${MAX_NOTES} private notes.`);
  const label = $('noteLabel').value.trim();
  const values = teachValues(text);   // one word, or a value the rules recognise -> a secret value (fingerprint only)
  if (values.length) {
    const fps = [];
    for (const v of values) fps.push(await fingerprint(v));
    notes.push({ id: crypto.randomUUID(), label: label || 'My secret', fps });
  } else {
    if (text.length < 10) return alert('Write a private fact (a sentence), or one secret value such as your MPIN');
    notes.push({ id: crypto.randomUUID(), label: label || 'Private note', text, vectors: await noteVectors(text) });
  }
  saveNotes(); renderNotes();
  $('noteText').value = ''; $('noteLabel').value = '';
  schedule();
}

function renderQuickTests(testCases) {
  const testsBox = $('quickTests');
  if (!testsBox || !testCases || testCases.length === 0) return;
  
  testsBox.innerHTML = '';
  const results = {};
  
  for (const test of testCases) {
    const row = document.createElement('div');
    row.className = 'testrow';
    row.innerHTML = `<code>${esc(test.input.substring(0, 40))}</code> → <span class="muted">${test.expect}</span>`;
    const runBtn = document.createElement('button');
    runBtn.textContent = 'Run';
    runBtn.onclick = async () => {
      $('input').value = test.input;
      schedule();
      // Wait for result to be rendered
      await new Promise(r => setTimeout(r, 350));
      const resultSpan = runBtn.nextElementSibling;
      // Check what we got
      const resultCategory = shown?.result?.category;
      const passed = test.expect === 'none' 
        ? !shown?.result 
        : resultCategory === test.expect;
      resultSpan.textContent = passed ? '✓' : '✗';
      resultSpan.className = 'result ' + (passed ? 'pass' : 'fail');
    };
    const resultSpan = document.createElement('span');
    resultSpan.className = 'result';
    resultSpan.textContent = '–';
    row.append(runBtn, resultSpan);
    testsBox.append(row);
  }
}

function renderEmbedderSelector() {
  const selectorBox = $('embedderSelector');
  if (!selectorBox) return;
  
  selectorBox.innerHTML = '';
  for (const model of AVAILABLE_EMBEDDERS) {
    const label = document.createElement('label');
    label.style.marginRight = '12px';
    label.innerHTML = `<input type="radio" name="embedder" value="${model}" ${model === currentEmbedder ? 'checked' : ''}>
      <span style="margin-left:4px">${model === 'embedding' ? 'English (default)' : model === 'paraphrase-multi' ? 'Tagalog (paraphrase-multi)' : model}</span>`;
    label.onchange = async (e) => {
      if (!e.target.checked) return;
      const oldEmbedder = currentEmbedder;
      currentEmbedder = e.target.value;
      try {
        const ms = await useEmbedder(currentEmbedder);
        $('embedderStatus').textContent = `Model switched to ${currentEmbedder} (${ms} ms)`;
        $('embedderStatus').className = 'good';
        // Re-embed all private notes
        await reEmbedNotes(currentEmbedder);
        schedule();
        // Update URL
        const url = new URL(location);
        url.searchParams.set('embedder', currentEmbedder);
        history.replaceState({}, '', url);
      } catch (e) {
        currentEmbedder = oldEmbedder;
        e.target.checked = false;
        $('embedderStatus').textContent = `Failed to load ${e.target.value}: ${e?.message ?? e}`;
        $('embedderStatus').className = 'bad';
      }
    };
    selectorBox.append(label);
  }
}

function init() {
  // Initialize embedder selector
  renderEmbedderSelector();
  
  // Initialize quick tests
  renderQuickTests(QUICK_TESTS);
  
  for (const [name, t] of SAMPLES) {
    const b = document.createElement('button'); b.textContent = name;
    b.onclick = () => { $('input').value = t; schedule(); };
    $('samples').append(b);
  }
  $('input').addEventListener('input', schedule);
  $('hideBtn').onclick = () => {
    if (!shown || $('input').value !== shown.text) return;   // only hide what is on screen
    const masked = hide(shown.text, shown.result);
    if (masked != null) { $('input').value = masked; schedule(); }
  };
  $('th').addEventListener('input', () => { $('thLabel').textContent = `Similarity needed when no private word appears: ${threshold().toFixed(2)} (lower = warns more)`; schedule(); });
  $('save').onclick = saveNote;
  $('fill').onclick = () => { $('noteText').value = 'Our client is buying the bakery next door. The project is called Sunflower.'; $('noteLabel').value = 'Project Sunflower'; };
  $('fillJakim').onclick = () => { $('noteText').value = 'Si Jakim ay nakatira sa Taguig'; $('noteLabel').value = 'Jakim location'; };
  $('fillPrice').onclick = () => { $('noteText').value = 'We are raising our prices by 20 percent next month. Do not tell customers yet.'; $('noteLabel').value = 'Price increase'; };
  $('fillMpin').onclick = () => { $('noteText').value = '4821'; $('noteLabel').value = 'GCash MPIN'; };
  $('delAll').onclick = () => { notes = []; saveNotes(); renderNotes(); schedule(); };
  renderNotes();
  
  // Load embedder from URL if specified
  currentEmbedder = getEmbedderFromURL();
}

init();

async function startup() {
  try {
    const t = await loadModels();
    $('status').textContent = `Models loaded from this machine: PII ${t.pii} ms, embeddings ${t.emb} ms.`;
    $('status').className = 'good';
    
    // Load the requested embedder model if not the default
    if (currentEmbedder !== 'embedding') {
      const ms = await useEmbedder(currentEmbedder);
      $('embedderStatus').textContent = `Using ${currentEmbedder} embedder (${ms} ms)`;
      $('embedderStatus').className = 'good';
    }
    
    schedule();
  } catch (e) {
    $('status').textContent = `Model load FAILED: ${e?.message ?? e}`;
    $('status').className = 'bad';
  }
}

startup();
