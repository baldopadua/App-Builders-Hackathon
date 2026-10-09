// Browser port of the ai-local core (models + checker). Shared by the demo page (app.js) and the scorer (eval.js).
// Same models and rules as the Kotlin code; keep them in sync.
import { env, AutoTokenizer, AutoModelForTokenClassification, pipeline } from '/vendor/transformers/transformers.min.js';
import { detectRules } from './rules.js';

// Everything is loaded from this machine. Remote model download is switched off.
env.allowRemoteModels = false;
env.allowLocalModels = true;
env.localModelPath = '/models/';
env.useBrowserCache = false;
env.backends.onnx.wasm.wasmPaths = '/vendor/transformers/';   // ort-wasm-simd-threaded.jsep.{mjs,wasm} live here
env.backends.onnx.wasm.numThreads = 1;

export const MIN_PII_SCORE = 0.6;

export const WARN = {
  PASSWORD: ['PASSWORD', 'a password'],
  CREDIT_CARD: ['CARD', 'a payment card number'],
  FINANCIAL: ['OTHER_PII', 'financial account details'],
  IBAN_CODE: ['OTHER_PII', 'a bank account number'],
  US_BANK_NUMBER: ['OTHER_PII', 'a bank account number'],
  EMAIL_ADDRESS: ['OTHER_PII', 'an email address'],
  PHONE_NUMBER: ['OTHER_PII', 'a phone number'],
  US_SSN: ['OTHER_PII', 'a government ID number'],
  US_PASSPORT: ['OTHER_PII', 'a passport number'],
  US_DRIVER_LICENSE: ['OTHER_PII', "a driver's license number"],
  US_ITIN: ['OTHER_PII', 'a tax ID number'],
};

let piiTok, piiModel, id2label, embedder;

export async function loadModels() {
  const t0 = performance.now();
  piiTok = await AutoTokenizer.from_pretrained('pii');
  piiModel = await AutoModelForTokenClassification.from_pretrained('pii', { dtype: 'q8', device: 'wasm' });
  id2label = piiModel.config.id2label;
  const t1 = performance.now();
  embedder = await pipeline('feature-extraction', 'embedding', { dtype: 'q8', device: 'wasm' });
  return { pii: Math.round(t1 - t0), emb: Math.round(performance.now() - t1) };
}
export const modelsReady = () => Boolean(piiModel && embedder);

/** Swap the embedding model (lab experiments). Name = folder under assets/models or lab-models. Returns load ms. */
export async function useEmbedder(name) {
  const t = performance.now();
  embedder = await pipeline('feature-extraction', name, { dtype: 'q8', device: 'wasm' });
  return Math.round(performance.now() - t);
}

// ---- PII model --------------------------------------------------------------------------------------

export async function piiEntities(text) {
  if (!text.trim()) return [];
  const tokens = piiTok.tokenize(text);
  const { logits } = await piiModel(piiTok(text));
  const [, seq, nl] = logits.dims;
  const data = logits.data;
  const lower = text.toLowerCase();
  const out = [];
  let cur = null, cursor = 0;
  const flush = () => { if (cur) out.push({ type: cur.type, start: cur.start, end: cur.end, score: cur.sum / cur.n, text: text.slice(cur.start, cur.end) }); cur = null; };

  for (let i = 1; i < seq - 1 && i - 1 < tokens.length; i++) {
    const tok = tokens[i - 1];
    let best = 0;
    for (let k = 1; k < nl; k++) if (data[i * nl + k] > data[i * nl + best]) best = k;
    let denom = 0;
    for (let k = 0; k < nl; k++) denom += Math.exp(data[i * nl + k] - data[i * nl + best]);
    const p = 1 / denom;

    // locate the token in the original text (Transformers.js does not return char offsets)
    const piece = tok.startsWith('##') ? tok.slice(2) : tok;
    let s, e;
    if (tok === '[UNK]') {
      s = cursor; while (s < text.length && /\s/.test(text[s])) s++;
      e = s; while (e < text.length && !/\s/.test(text[e])) e++;
    } else {
      const idx = lower.indexOf(piece, cursor);
      if (idx < 0) { s = cursor; e = Math.min(text.length, cursor + piece.length); } else { s = idx; e = idx + piece.length; }
    }
    cursor = e;

    const label = id2label[best] ?? 'O';
    if (label === 'O') { flush(); continue; }
    const type = label.slice(2);
    if (label.startsWith('B-') || !cur || cur.type !== type) { flush(); cur = { type, start: s, end: e, sum: 0, n: 0 }; }
    cur.end = e; cur.sum += p; cur.n++;
  }
  flush();
  return out;
}

const PII_KIND = { PASSWORD: 'password', CREDIT_CARD: 'credit_card', EMAIL_ADDRESS: 'email', PHONE_NUMBER: 'phone',
  US_SSN: 'gov_id', US_PASSPORT: 'gov_id', US_DRIVER_LICENSE: 'gov_id', US_ITIN: 'gov_id' };

export function piiResult(ents, text = '') {
  const best = ents
    .filter((e) => WARN[e.type] && e.score >= MIN_PII_SCORE && !isMasked(e.text) && !touchesMask(text, e.start))
    .sort((a, b) => b.score - a.score)[0];
  if (!best) return null;
  const [category, what] = WARN[best.type];
  const soft = best.type === 'EMAIL_ADDRESS' || best.type === 'PHONE_NUMBER';
  const tail = soft ? ' Intended?' : category === 'PASSWORD' ? ' Sharing it could give someone access to your account.' : ' Review it before sending.';
  return { category, kind: PII_KIND[best.type] ?? 'pii', severity: soft ? 'MEDIUM' : 'HIGH',
    start: best.start, end: best.end, confidence: best.score, reason: `This message appears to include ${what}.${tail}` };
}

// ---- "Itago" (hide it) + safety tips — Kotlin twins: ai/Redactor.kt, ai/SafetyTips.kt ------------------

export const DOT = '•';
export const KEY_REMOVED = '[private key removed]';
const MASK = '••••••';
const URL_PASSWORD = /(:\/\/[^\s:/@]+:)[^\s@/]+@/g;

const keepLastDigits = (s, keep) => {
  const total = (s.match(/\d/g) ?? []).length;
  let seen = 0;
  return [...s].map((c) => {
    if (!/\d/.test(c)) return c;
    seen++;
    return total > keep && seen > total - keep ? c : DOT;   // short numbers (CVV) fully masked
  }).join('');
};

/** Masked replacement for a risky span, or null when masking makes no sense (private notes). */
export function maskSpan(span, kind) {
  switch (kind) {
    case 'private_match': return null;
    case 'password': case 'otp': case 'my_secret': return MASK;
    case 'api_key':
      if (span.startsWith('-----BEGIN')) return KEY_REMOVED;
      if (/:\/\/[^\s:/@]+:[^\s@/]+@/.test(span)) return span.replace(URL_PASSWORD, (_, keep) => keep + MASK + '@');
      return span.slice(0, 4) + '••••';
    case 'credit_card': case 'bank_account': case 'phone': return keepLastDigits(span, 4);
    case 'gov_id': return keepLastDigits(span, 3);
    case 'email': { const at = span.indexOf('@'); return at > 0 ? '•••' + span.slice(at) : '•••'; }
    default: return '••••';
  }
}

/** The whole draft with this result's span masked, or null for private notes. */
export function hide(text, r) {
  const s = Math.max(0, Math.min(r.start, text.length));
  const e = Math.max(s, Math.min(r.end, text.length));
  const masked = maskSpan(text.slice(s, e), r.kind);
  return masked == null ? null : text.slice(0, s) + masked + text.slice(e);
}

/** "Itago lahat": masks every secret the checker finds. `check` may be async. */
export async function hideAll(text, check, maxPasses = 10) {
  let t = text;
  for (let i = 0; i < maxPasses; i++) {
    const r = await check(t);
    if (!r) return t;
    const next = hide(t, r);
    if (next == null) return t;
    t = next;
  }
  return t;
}

export const isMasked = (span) => span.includes(DOT) || span.includes(KEY_REMOVED);
export const touchesMask = (text, start) => text.slice(Math.max(0, start - 2), start).includes(DOT);

export const SAFETY_TIPS = {
  password: 'Huwag i-send ang password sa chat. Kung na-send na, palitan agad.',
  otp: 'Walang totoong bangko, GCash o Maya na hihingi ng OTP o PIN mo. Kapag may humingi, scam iyan.',
  api_key: 'Kahit sino sa chat ay puwedeng gumamit ng key na ito. Kung na-send na, i-revoke at gumawa ng bago.',
  credit_card: 'Hindi kailangan ang card number o CVV para makatanggap ng bayad.',
  gov_id: 'Ginagamit ang TIN, SSS at PhilSys sa identity theft at loan scams. I-send lang sa opisyal na channel.',
  bank_account: 'Okay lang kung pambayad ito. Siguraduhing tama ang taong padadalhan mo.',
  phone: 'Okay lang kung sinadya. Siguraduhing kilala mo ang kausap mo.',
  email: 'Okay lang kung sinadya. Siguraduhing kilala mo ang kausap mo.',
  private_match: 'Minarkahan mo itong pribado. Siguraduhing okay lang na malaman ito ng kausap mo.',
  my_secret: 'Ikaw mismo ang nagturo na sikreto ito. Pindutin ang Itago bago i-send.',
};
export const tipFor = (kind) => SAFETY_TIPS[kind] ?? 'Silipin muna bago i-send.';

export const HEADLINES = {   // Filipino-first banner title (Kotlin twin: SafetyTips.headline)
  password: 'Ingat! Mukhang password ito.',
  otp: 'Teka! OTP o PIN ito — huwag ibigay kahit kanino.',
  api_key: 'Teka lang — mukhang API key ito.',
  credit_card: 'Ingat! Detalye ng card ito.',
  gov_id: 'Ingat! Government ID number ito.',
  bank_account: 'Account number ito. Sinadya mo ba?',
  phone: 'Phone number ito. Sinadya mo ba?',
  email: 'Email address ito. Sinadya mo ba?',
  private_match: 'Pribado mo ito. Sigurado ka bang ise-send?',
  my_secret: 'Teka! Ito yung sikretong tinuro mo.',
};
export const headlineFor = (kind, label) =>
  kind === 'my_secret' && label ? `Teka! Ito yung tinuro mong sikreto: ${label}.` : HEADLINES[kind] ?? 'Teka lang, silipin muna.';

// ---- taught secret VALUES ("Teach me": your MPIN, passcode, password, API key) ----------------------------
// Caught anywhere, even with no clue words ("eto na yung 4821"). Only a keyed fingerprint (HMAC-SHA256) is kept.
// Exact match (ignoring case, and spaces / dashes inside numbers): deliberately not AI. Kotlin twin: notes/TaughtSecrets.kt

export const MIN_SECRET_CHARS = 4;
const SECRET_LEAD = `"'([{<“‘`, SECRET_TRAIL = `.,;:!?)]}>"'”’`;

/** Lowercase; a number loses its spaces and dashes ("4532 0151 1283 0366" = "4532015112830366"). */
export function normalizeSecret(v) {
  const t = v.trim();
  return (/\d/.test(t) && /^[\d -]+$/.test(t) ? t.replace(/\D/g, '') : t).toLowerCase();
}

/** The secret values in a "Teach me" entry: the whole entry if it is one word, else what the rules find. [] = a fact. */
export function teachValues(entry) {
  const t = entry.trim();
  if (!t) return [];
  if (!/\s/.test(t)) return normalizeSecret(t).length >= MIN_SECRET_CHARS ? [t] : [];
  const out = [];
  let rest = t;
  for (let i = 0; i < 10; i++) {
    const r = detectRules(rest);
    if (!r) break;
    const v = rest.slice(r.start, r.end);
    if (normalizeSecret(v).length >= MIN_SECRET_CHARS) out.push(v);
    const next = hide(rest, r);
    if (next == null) break;
    rest = next;
  }
  return out;
}

/** Where in a draft a taught value could be: words (as typed, trimmed, after ':' / '='), and digit runs. [[start, end]] */
export function secretCandidates(draft) {
  const out = [];
  const add = (s, e) => { if (e - s >= MIN_SECRET_CHARS) out.push([s, e]); };
  for (const m of draft.matchAll(/\S+/g)) {
    const s = m.index, e = s + m[0].length;
    add(s, e);
    let a = s, b = e;
    while (a < b && SECRET_LEAD.includes(draft[a])) a++;
    while (b > a && SECRET_TRAIL.includes(draft[b - 1])) b--;
    if (a !== s || b !== e) add(a, b);
    const cut = Math.max(draft.lastIndexOf(':', b - 1), draft.lastIndexOf('=', b - 1));
    if (cut >= a && cut < b - 1) add(cut + 1, b);
  }
  for (const m of draft.matchAll(/(?<!\d)\d(?:[ -]?\d)+(?!\d)/g)) add(m.index, m.index + m[0].length);
  return out.sort((x, y) => x[0] - y[0]);
}

let fpKey = null;   // browser demo: the key lives in this browser (the phone keeps it encrypted with the Keystore)
async function fingerprintKey() {
  if (fpKey) return fpKey;
  let raw = null;
  try { raw = JSON.parse(localStorage.getItem('sl_fp_key')); } catch { /* storage blocked */ }
  if (!Array.isArray(raw) || raw.length !== 32) {
    raw = Array.from(crypto.getRandomValues(new Uint8Array(32)));
    try { localStorage.setItem('sl_fp_key', JSON.stringify(raw)); } catch { /* storage blocked */ }
  }
  fpKey = await crypto.subtle.importKey('raw', new Uint8Array(raw), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return fpKey;
}
export async function fingerprint(value) {
  const sig = await crypto.subtle.sign('HMAC', await fingerprintKey(), new TextEncoder().encode(normalizeSecret(value)));
  return [...new Uint8Array(sig)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

/** Notes with `fps` are taught secret values. Returns a result for the first taught value found in the draft. */
export async function taughtResult(notes, text) {
  const taught = notes.filter((n) => n.fps?.length);
  if (!taught.length || !text.trim()) return null;
  for (const [s, e] of secretCandidates(text)) {
    const span = text.slice(s, e);
    if (isMasked(span)) continue;
    const f = await fingerprint(span);
    const n = taught.find((x) => x.fps.includes(f));
    if (n) return { category: 'PRIVATE_NOTE', kind: 'my_secret', severity: 'HIGH', start: s, end: e, confidence: 0.99, label: n.label,
      reason: `This is a secret you taught SecondLook: ${n.label}.` };
  }
  return null;
}

// ---- private notes ("Teach me what's private") ------------------------------------------------------

const SENTENCE = /[^.!?\n]+[.!?]*/g;
export function parts(text, minChars) {
  return [...text.matchAll(SENTENCE)]
    .map((m) => [m.index, m.index + m[0].length])
    .filter(([a, b]) => text.slice(a, b).trim().length >= minChars);
}
export async function embed(text) {
  const o = await embedder(text, { pooling: 'mean', normalize: true });
  return Array.from(o.data);
}
export const cosine = (a, b) => { let s = 0; for (let i = 0; i < a.length; i++) s += a[i] * b[i]; return s; };

/** Embeds a note as: the whole note + each sentence. */
export async function noteVectors(text) {
  const sentences = parts(text, 10).map(([a, b]) => text.slice(a, b).trim());
  const vs = [await embed(text.trim())];
  if (sentences.length > 1) for (const s of sentences.slice(0, 6)) vs.push(await embed(s));
  return vs;
}

// ---- anchors: the names / code words the user is really protecting -------------------------------------
// A note like "Our client is buying the bakery next door. The project is called Sunflower." has one anchor: Sunflower.
// Anchors are capitalised words (not common sentence starters) and quoted phrases ('blue heron at midnight').
// Kotlin twin: kotlin/.../notes/NoteAnchors.kt. Keep in sync.

const STOP = new Set(('our the we this that these those there here it its if when what who how why only also then so but and or because after before ' +
  'while since please thanks thank hello hi yes no not do does did dont can could will would should may might must lets let i my your his her their ' +
  'they them he she you one all any some each both most more many much few other another such even just now new old good great today tomorrow ' +
  'monday tuesday wednesday thursday friday saturday sunday january february march april may june july august september october november december ' +
  'ang ng sa si ni kay yung mga ako ikaw siya kami tayo sila ito iyon po opo hindi oo ay na pa din rin lang naman ba ko mo niya namin natin nila ' +
  'kapag kung para pero may wala kasi dahil tapos pag sana ayan eto ayun ' +
  'project plan note notes deal meeting team client customer boss').split(' '));

const stem = (w) => (w.length > 4 && w.endsWith('s') && !w.endsWith('ss') ? w.slice(0, -1) : w);
const wordsOf = (s) => (s.toLowerCase().match(/\p{L}[\p{L}\d'-]*/gu) ?? []).map((w) => stem(w.replace(/'s$/, '')));   // "Jakim's" -> "jakim"

/** A note with several anchors (e.g. a person AND a place) needs up to 2 of them to appear in the draft. */
export const anchorsNeeded = (anchors) => Math.min(2, anchors.length);

export function anchorsOf(text) {
  const anchors = [], seen = new Set();
  for (const m of text.matchAll(/(?:^|[\s(])['"“‘]([^'"“”‘’]{4,60})['"”’](?=$|[\s.,;:!?)])/g)) {
    const words = [...new Set(wordsOf(m[1]).filter((w) => w.length >= 4 && !STOP.has(w)))];
    if (words.length) anchors.push({ words, need: Math.min(2, words.length), shown: m[1].trim() });
  }
  for (const m of text.matchAll(/\p{Lu}\p{L}{2,}/gu)) {
    const w = stem(m[0].toLowerCase());
    if (STOP.has(w) || STOP.has(m[0].toLowerCase()) || seen.has(w)) continue;
    seen.add(w);
    anchors.push({ words: [w], need: 1, shown: m[0] });
  }
  return anchors;
}

/** Which anchors (as the user wrote them) appear in the draft. */
export function anchorHits(anchors, draft) {
  const words = new Set(wordsOf(draft));
  return anchors.filter((a) => a.words.filter((w) => words.has(w)).length >= a.need).map((a) => a.shown);
}

/** Best similarity of the draft to each note: [{label, score, start, end, anchors}] */
export async function noteScores(allNotes, text) {
  const notes = allNotes.filter((n) => n.vectors?.length);   // facts only; secret values: taughtResult
  if (!notes.length || text.trim().length < 12) return [];
  const ps = parts(text, 12).slice(0, 4);
  if (!(ps.length === 1 && ps[0][0] === 0 && ps[0][1] === text.length)) ps.push([0, text.length]);
  const pv = [];
  for (const [a, b] of ps) pv.push(await embed(text.slice(a, b).trim()));
  return notes.map((n) => {
    let best = -1, range = ps[ps.length - 1];
    pv.forEach((v, i) => n.vectors.forEach((nv) => { const c = cosine(v, nv); if (c > best) { best = c; range = ps[i]; } }));
    n.anchors ??= anchorsOf(n.text);
    return { label: n.label, score: best, start: range[0], end: range[1], anchors: anchorHits(n.anchors, text), anchorsNeeded: anchorsNeeded(n.anchors) };
  });
}

/**
 * Decision rule. A draft is flagged when
 *   - it contains one of the note's anchors AND is at least a little similar (>= floor), or
 *   - it is very similar to the note with no anchor at all (>= high).
 * Wording stays cautious: similarity can suggest a connection, never prove a disclosure.
 */
export const NOTE_DEFAULTS = { high: 0.75, floor: 0.60 };

export function notesResult(scores, high = NOTE_DEFAULTS.high, floor = NOTE_DEFAULTS.floor) {
  let best = null;
  for (const s of scores) {
    const viaAnchor = s.anchorsNeeded > 0 && s.anchors.length >= s.anchorsNeeded && s.score >= floor;
    if (!viaAnchor && s.score < high) continue;
    const rank = (viaAnchor ? 1 : 0) + s.score;
    if (!best || rank > best.rank) best = { ...s, viaAnchor, rank };
  }
  if (!best) return null;
  return { category: 'PRIVATE_NOTE', kind: 'private_match', severity: 'HIGH', start: best.start, end: best.end, confidence: Math.min(1, Math.max(0, best.score)),
    reason: best.viaAnchor
      ? `This mentions "${best.anchors[0]}", which is part of a note you marked confidential: ${best.label}.`
      : `This may reveal information you marked confidential: ${best.label}.` };
}

// ---- the checker (same stage order as SecondLookChecker.checkDetailed) ------------------------------

export async function checkDetailed(text, threshold, notes) {
  const stages = [];
  const timed = async (name, fn) => { const t = performance.now(); const r = await fn(); stages.push({ name, result: r.result, ms: Math.round(performance.now() - t) }); return r; };
  await timed('taught-secrets', async () => ({ result: await taughtResult(notes, text) }));
  await timed('rules', async () => ({ result: detectRules(text) }));
  const p = await timed('pii-model', async () => { const ents = await piiEntities(text); return { result: piiResult(ents, text), ents }; });
  const n = await timed('private-notes', async () => { const scores = await noteScores(notes, text); return { result: notesResult(scores, threshold), scores }; });
  const first = stages.find((s) => s.result);
  return { result: first?.result ?? null, stage: first?.name ?? null, stages, ents: p.ents, scores: n.scores };
}
