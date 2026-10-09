// Builds test-data/external/*.json from two public Philippine SMS datasets, scrubbed for Rule Zero.
//
//   cd "AI Model/ai-local"
//   node scripts/build-external-tests.mjs
//
// Needs Node 18+ and internet (Kaggle + GitHub, pinned versions). The output is committed, so tests never download.
// Scrubbing: every digit is replaced with a random digit (same length; the "09" / "639" prefix of PH mobile numbers
// and the Luhn validity of card-length numbers are kept so the detector behaves the same), emails become
// someone@example.com, links become example.invalid/link, greeting names become "Juan", the sender column is dropped,
// and near-duplicates (same text once digits are hidden) are removed.
// Labels start from the dataset's own category and are then corrected by OVERRIDES (each one says why).

import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const OUT = path.join(ROOT, 'test-data', 'external');

const SOURCES = [
  {
    file: 'ph-sms-onzero0.json',
    name: 'onzero0/tagalog-sms',
    url: 'https://www.kaggle.com/api/v1/datasets/download/onzero0/tagalog-sms?datasetVersionNumber=1',
    page: 'https://www.kaggle.com/datasets/onzero0/tagalog-sms',
    license: 'CC0 1.0 (public domain dedication)',
    // otp = genuine one-time passwords (must warn); everything else is a received notice (must stay quiet)
    expect: { otp: 'PASSWORD', notifs: 'none', ads: 'none', gov: 'none', spam: 'none' },
    load: async (buf) => readXlsx(unzip(buf)['tagalog-sms.xlsx']),
  },
  {
    file: 'ph-sms-scottleechua.json',
    name: 'scottleechua/data spam-and-marketing-sms',
    url: 'https://raw.githubusercontent.com/scottleechua/data/ad50c348c53f738645f78ee8baa8be5082a1de91/spam-and-marketing-sms/text-messages.csv',
    page: 'https://github.com/scottleechua/data/tree/main/spam-and-marketing-sms',
    license: 'CC BY 4.0 (attribution: Scott Lee Chua)',
    // OTP and notifs texts are <REDACTED> in this dataset, so only spam / ads / gov are usable, all as negatives
    expect: { spam: 'none', ads: 'none', gov: 'none' },
    load: async (buf) => readCsv(buf.toString('utf8')),
  },
];

// Label corrections found by reviewing every disagreement between the dataset category and the detector.
// `match` runs on the text with digits shown as '#'. Only fix wrong labels here, never to hide a detector miss.
const OVERRIDES = [
  // Filed under ads / gov / notifs / spam, but the message itself carries a live code: forwarding it leaks the code
  { match: /^NEVER SHARE YOUR ONE-TIME PIN\. Your GOMO OTP is: #{6}/i, expect: 'PASSWORD', why: 'OTP filed under ads' },
  { match: /^Ang iyong One[- ]Time Pin ay:? #{6}/i, expect: 'PASSWORD', why: 'OTP filed under gov / notifs' },
  { match: /Virtual Pay security code is #{4}/i, expect: 'PASSWORD', why: 'card security code filed under notifs' },
  { match: /enter verification code: #{6}/i, expect: 'PASSWORD', why: 'verification code filed under notifs' },
  { match: /^Telegram code: #{5}/i, expect: 'PASSWORD', why: 'login code filed under spam' },
  { match: /^(?:G-)?#{6} is your (?:Google verification|authentication) code/i, expect: 'PASSWORD', why: 'OTP filed under notifs' },
  { match: /^Your One-Time-Pin is #{6}/i, expect: 'PASSWORD', why: 'OTP filed under gov' },
  // Filed under otp, but there is no code in the message: nothing to protect
  { match: /^#BDOStopScam|OTP = O, That's Private!|^#\/# (?:ADVISORY|Do not share your OTP)|^Huwag mag-pascam/i, expect: 'none', why: 'safety advisory, no code' },
  { match: /Maya will NEVER send links|no more SMS OTP|DoubleSafe|short survey/i, expect: 'none', why: 'announcement, no code' },
  { match: /GoJackpot|GJP!|Payday Blast/i, expect: 'none', why: 'casino spam filed under otp, no code' },
  { match: /SMGP Choice/i, expect: 'none', why: 'gift voucher code (promo codes stay quiet, like samples.json)' },
  { match: /^BDO-Online : Your account registered mobile number/i, expect: 'none', why: 'phishing text, no code' },
];

// ---- tiny zip / xlsx / csv readers (no dependencies) --------------------------------------------------------

function unzip(buf) {
  let eocd = buf.length - 22;
  while (eocd >= 0 && buf.readUInt32LE(eocd) !== 0x06054b50) eocd--;
  if (eocd < 0) throw new Error('not a zip file');
  const files = {};
  let p = buf.readUInt32LE(eocd + 16);
  for (let i = buf.readUInt16LE(eocd + 10); i > 0; i--) {
    if (buf.readUInt32LE(p) !== 0x02014b50) throw new Error('bad zip central directory');
    const method = buf.readUInt16LE(p + 10);
    const size = buf.readUInt32LE(p + 20);
    const nameLen = buf.readUInt16LE(p + 28);
    const local = buf.readUInt32LE(p + 42);
    const name = buf.toString('utf8', p + 46, p + 46 + nameLen);
    const start = local + 30 + buf.readUInt16LE(local + 26) + buf.readUInt16LE(local + 28);
    const data = buf.subarray(start, start + size);
    files[name] = method === 0 ? data : zlib.inflateRawSync(data);
    p += 46 + nameLen + buf.readUInt16LE(p + 30) + buf.readUInt16LE(p + 32);
  }
  return files;
}

const xmlText = (s) => s.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&amp;/g, '&');

function readXlsx(buf) {
  const files = unzip(buf);
  const strings = [...files['xl/sharedStrings.xml'].toString('utf8').matchAll(/<si>([\s\S]*?)<\/si>/g)]
    .map((m) => xmlText([...m[1].matchAll(/<t[^>]*>([\s\S]*?)<\/t>/g)].map((t) => t[1]).join('')));
  const rows = [...files['xl/worksheets/sheet1.xml'].toString('utf8').matchAll(/<row[^>]*>([\s\S]*?)<\/row>/g)].map((r) => {
    const cells = {};
    for (const c of r[1].matchAll(/<c r="([A-Z]+)\d+"([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const v = (c[3] ?? '').match(/<v>([\s\S]*?)<\/v>/)?.[1];
      cells[c[1]] = /t="s"/.test(c[2]) ? strings[+v] : xmlText((c[3] ?? '').match(/<t[^>]*>([\s\S]*?)<\/t>/)?.[1] ?? v ?? '');
    }
    return cells;
  });
  const header = rows.shift();
  const col = (name) => Object.keys(header).find((k) => header[k].trim().toLowerCase() === name);
  const [text, category] = [col('text'), col('category')];
  return rows.map((r) => ({ text: r[text] ?? '', category: (r[category] ?? '').trim().toLowerCase() }));
}

function readCsv(s) {
  const rows = [];
  let row = [], field = '', quoted = false;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (quoted) {
      if (c === '"') { if (s[i + 1] === '"') { field += '"'; i++; } else quoted = false; } else field += c;
    } else if (c === '"') quoted = true;
    else if (c === ',') { row.push(field); field = ''; }
    else if (c === '\n') { row.push(field.replace(/\r$/, '')); rows.push(row); row = []; field = ''; }
    else field += c;
  }
  if (field || row.length) { row.push(field); rows.push(row); }
  const header = rows.shift().map((h) => h.trim().toLowerCase());
  const [text, category] = [header.indexOf('text'), header.indexOf('category')];
  return rows.filter((r) => r.length > 1).map((r) => ({ text: r[text] ?? '', category: (r[category] ?? '').trim().toLowerCase() }));
}

// ---- scrubbing ------------------------------------------------------------------------------------------------

const PHONE = /(?<!\d)(?:\+?63|0)9\d{2}[ -]?\d{3}[ -]?\d{4}(?!\d)/g;     // same as the detector's PHONE
const CARD = /(?<!\d)(?:\d[ -]?){12,18}\d(?!\d)/g;                       // same as the detector's CARD
const EMAIL = /\b[a-z0-9._%+-]+@[a-z0-9-]+(?:\.[a-z0-9-]+)*\.[a-z]{2,}\b/gi;
const URL = /\bhttps?:\/\/\S+/gi;
const DOMAIN = /(?<![@\w.-])(?:[a-z0-9-]+\.)+(?:com|ph|pw|net|org|xyz|top|vip|cc|io|me|info|ly|app|shop|site|online|club|live|win|bet|link|asia|store|fun|icu|cyou|sbs|ca|fyi|cn|us|uk|co|tv|gg|ai|biz|pro|one|cloud|tech|space|website|today|best|life|world|help|run|work|page)\b(?:\/[^\s]*)?/gi;
// "Hi Juan", "Hi, Scott !", "Hi SCOTT !", "Dear Mr. Cruz"
const GREETING_NAME = /\b(Hi|Hello|Dear|Hey)(,?\s+(?:Mr|Ms|Mrs)\.?)?(,?\s+)([A-Z][a-z]{2,}|[A-Z]{3,})\b/g;
const NOT_A_NAME = new Set(['fam', 'maam', 'there', 'user', 'sir', 'mam', 'customer', 'ka', 'kabayan', 'po', 'member', 'everyone',
  'friend', 'ate', 'kuya', 'gomo', 'smart', 'globe', 'tnt', 'tm', 'all', 'guys']);
// Names a reviewer found in the data outside a greeting (the dataset owners' own first names, a family member)
const KNOWN_NAMES = /\b(?:scott|ruben(?:\s+l\b)?)\b/gi;

const luhn = (d) => {
  let sum = 0;
  for (let i = d.length - 1, dbl = false; i >= 0; i--, dbl = !dbl) {
    let x = d.charCodeAt(i) - 48;
    if (dbl) { x *= 2; if (x > 9) x -= 9; }
    sum += x;
  }
  return sum % 10 === 0;
};

function rng(key) {   // mulberry32, seeded from the digit-free template, so the output never depends on the real digits
  let a = crypto.createHash('sha256').update(key).digest().readUInt32LE(0);
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// Dates and times get random but REAL values (month 01-12, hour 00-23...). Random digits alone would turn
// "12-28-2024" into "97-08-8164", which the PII model reads as an ID number: a false alarm the real text never had.
const MONTH = '(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?';
const DATE_TIME = [
  { re: /(?<!\d)(\d{1,2}):(\d{2})(?::(\d{2}))?(?!\d)/gd, roles: ['hour', 'minute', 'minute'] },              // 9:05, 21:30:15
  { re: /(?<!\d)(\d{4})-(\d{1,2})-(\d{1,2})(?!\d)/gd, roles: ['year', 'monthOrDay', 'monthOrDay'] },        // 2024-12-28
  { re: new RegExp(`(?<!\\d)(\\d{4})-${MONTH}-(\\d{1,2})(?!\\d)`, 'gdi'), roles: ['year', 'day'] },          // 2024-Apr-15
  { re: /(?<![\d.])(\d{1,2})[-/](\d{1,2})(?:[-/](\d{4}|\d{2}))?(?!\d|\.\d|[-/]\d)/gd, roles: ['monthOrDay', 'monthOrDay', 'year'] }, // 12-28-24, 28/12
  { re: new RegExp(`(?<!\\d)(\\d{1,2})[- ]${MONTH}`, 'gdi'), roles: ['day'] },                                // 29-Dec
  { re: new RegExp(`\\b${MONTH}\\s+(\\d{1,2})(?:,?\\s+(\\d{4}))?(?!\\d)`, 'gdi'), roles: ['day', 'year'] },  // Dec 29, 2024
];
const ROLE_RANGE = { hour: [0, 23], minute: [0, 59], year: [2019, 2026], monthOrDay: [1, 12], day: [1, 28] };

function realDateDigits(text, rand) {
  const forced = new Map();                                 // position -> digit
  for (const { re, roles } of DATE_TIME) {
    for (const m of text.matchAll(re)) {
      roles.forEach((role, g) => {
        const span = m.indices[g + 1];
        if (!span) return;
        const len = span[1] - span[0];
        let [lo, hi] = ROLE_RANGE[role];
        if (role === 'year' && len === 2) [lo, hi] = [19, 26];
        if (role === 'hour' && /^\s*[ap]\.?m\b/i.test(text.slice(m.index + m[0].length))) [lo, hi] = [1, 12];
        if (len === 1) hi = Math.min(hi, 9), lo = Math.max(lo, role === 'minute' || role === 'hour' ? 0 : 1);
        const value = String(lo + Math.floor(rand() * (hi - lo + 1))).padStart(len, '0');
        for (let i = 0; i < len; i++) forced.set(span[0] + i, value[i]);
      });
    }
  }
  return forced;
}

function scrubDigits(text, key) {
  const keep = new Set();                                   // digit positions kept as-is (PH mobile prefix)
  for (const m of text.matchAll(PHONE)) {
    const prefix = m[0].match(/^(?:\+?63|0)9/)[0];
    for (let i = 0; i < prefix.length; i++) keep.add(m.index + i);
  }
  const cards = [...text.matchAll(CARD)].map((m) => {
    const digits = m[0].replace(/\D/g, '');
    return { start: m.index, end: m.index + m[0].length, valid: digits.length >= 13 && digits.length <= 19 && luhn(digits) };
  });
  for (let attempt = 0; ; attempt++) {
    const rand = rng(`${key}#${attempt}`);
    const forced = realDateDigits(text, rand);
    // split('') keeps UTF-16 indexes, the same ones regex match.index uses (emoji are two units)
    const out = text.split('').map((c, i) => {
      if (c < '0' || c > '9' || keep.has(i)) return c;
      return forced.get(i) ?? String(Math.floor(rand() * 10));
    });
    for (const card of cards) {                              // keep each card-length run's Luhn validity
      const positions = [];
      for (let i = card.start; i < card.end; i++) if (out[i] >= '0' && out[i] <= '9') positions.push(i);
      if (positions.length < 13 || positions.length > 19) continue;
      const last = positions[positions.length - 1];
      for (let d = 0; d < 10 && luhn(positions.map((i) => out[i]).join('')) !== card.valid; d++) out[last] = String((+out[last] + 1) % 10);
    }
    const scrubbed = out.join('');
    // never keep a real 4+ digit run by chance (dates and times may repeat: they are not secrets)
    const leaked = [...text.matchAll(/\d{4,}/g)].some((m) => scrubbed.slice(m.index, m.index + m[0].length) === m[0] && !keep.has(m.index) && !forced.has(m.index));
    if (!leaked || attempt > 20) return scrubbed;
  }
}

const template = (t) => t.replace(/\d/g, '#').replace(/\s+/g, ' ').trim().toLowerCase();

function clean(text) {
  // only \n and \t may stay escaped in the JSON: jvm-check's tiny unescape() knows nothing else
  return text.toWellFormed().replace(/\r\n?/g, '\n').replace(/[\u0000-\u0008\u000b-\u001f\u007f]/g, '').trim();
}

function scrub(text) {
  const key = template(text);
  let t = scrubDigits(text, key);
  t = t.replace(EMAIL, 'someone@example.com');
  t = t.replace(URL, 'https://example.invalid/link').replace(DOMAIN, (d) => (/^(?:someone@)?example\./i.test(d) ? d : 'example.invalid/link'));
  t = t.replace(GREETING_NAME, (all, hi, title, space, name) => (NOT_A_NAME.has(name.toLowerCase()) ? all : `${hi}${title ?? ''}${space}Juan`));
  return t.replace(KNOWN_NAMES, 'Juan');
}

// ---- build ----------------------------------------------------------------------------------------------------

async function download(url) {
  const res = await fetch(url, { redirect: 'follow' });
  if (!res.ok) throw new Error(`${url}: HTTP ${res.status}`);
  return Buffer.from(await res.arrayBuffer());
}

fs.mkdirSync(OUT, { recursive: true });
for (const src of SOURCES) {
  const rows = await src.load(await download(src.url));
  const seen = new Set();
  const out = [];
  const counts = {};
  for (const r of rows) {
    const text = clean(r.text);
    if (!text || /^<REDACTED>$/i.test(text) || !(r.category in src.expect)) continue;
    const key = template(text);
    if (seen.has(key)) continue;
    seen.add(key);
    const masked = text.replace(/\d/g, '#');
    const override = OVERRIDES.find((o) => o.match.test(masked));
    const entry = { text: scrub(text), expect: override?.expect ?? src.expect[r.category], tags: ['external', r.category] };
    if (override) entry.why = override.why;
    out.push(entry);
    counts[`${r.category}->${entry.expect}`] = (counts[`${r.category}->${entry.expect}`] ?? 0) + 1;
  }
  const doc = [
    '{',
    `  "_note": ${JSON.stringify(`REAL Philippine SMS from ${src.name}, SCRUBBED for Rule Zero (random digits, fake emails/links/names, no sender, near-duplicates removed). Generated by scripts/build-external-tests.mjs: do not edit by hand.`)},`,
    `  "_source": ${JSON.stringify(src.page)},`,
    `  "_license": ${JSON.stringify(src.license)},`,
    `  "_scoring": "expect PASSWORD = must warn (category PASSWORD, any severity). expect none = must not raise a red HIGH warning; a soft yellow MEDIUM 'Intended?' is allowed (phone numbers, emails).",`,
    '  "detect": [',
    out.map((e) => `    ${JSON.stringify(e)}`).join(',\n'),
    '  ]',
    '}',
    '',
  ].join('\n');
  fs.writeFileSync(path.join(OUT, src.file), doc);
  console.log(`${src.file}: ${out.length} unique messages (from ${rows.length} rows)`, counts);
}
