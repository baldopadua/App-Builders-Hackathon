// JS port of kotlin/com/secondlook/ai/rules/RuleDetector.kt (same regexes, same wording). Keep both in sync.
// Returns { category, kind, severity, start, end, reason, confidence } or null.
// Email, PH mobile numbers and bank account numbers get a SOFT warning (severity MEDIUM, "Intended?"):
// small businesses share them on purpose to get paid.

export function luhnValid(digits) {
  if (!digits || /\D/.test(digits)) return false;
  let sum = 0, dbl = false;
  for (let i = digits.length - 1; i >= 0; i--) {
    let d = digits.charCodeAt(i) - 48;
    if (dbl) { d *= 2; if (d > 9) d -= 9; }
    sum += d; dbl = !dbl;
  }
  return sum % 10 === 0;
}

// Not glued to letters: "Your parcel PH5918957249418" is a tracking number, not a card
const CARD = /(?<![\dA-Za-z])(?:\d[ -]?){12,18}\d(?!\d)/g;

const PASSWORD = new RegExp(
  String.raw`\b(?:password|passcode|passwd|pwd|pw|pasword|paswrd|passwrd|pass(?=\s*[:=]|\s+(?:ng|sa)\s+[\w-]+(?:\s+(?:ko|mo|namin|natin))?\s*[:=]))\b` +
  String.raw`(?:\s+(?:ko|mo|nya|niya|namin|natin|po|(?:for|ng|sa|para\s+sa)\s+(?:the\s+|my\s+|our\s+|ang\s+)?[\w-]+(?:\s+[\w-]+)?)){0,3}` +
  String.raw`\s*(?:(?:is|ay|ang)\s*[:=]?|[:=]|->|-)\s*["']?([^\s"']{3,})`, 'gid');
const PASSWORD_WORD = String.raw`(?:password|passcode|passwd|pwd|pw|pasword|paswrd|passwrd)`;
const PASSWORD_CANDIDATE = String.raw`(?<![\w])(?:\d{3,8}|[A-Za-z][A-Za-z0-9_#@!.-]*\d[A-Za-z0-9_#@!.-]*)(?![\w])`;
const CONVERSATIONAL_PASSWORD_AFTER = new RegExp(
  String.raw`\b${PASSWORD_WORD}\b[^\d\n]{0,40}?(${PASSWORD_CANDIDATE})`, 'gid');
const CONVERSATIONAL_PASSWORD_BEFORE = new RegExp(
  String.raw`(${PASSWORD_CANDIDATE})[^\d\n]{0,40}?\b${PASSWORD_WORD}\b`, 'gid');

const EMAIL = /\b[a-z0-9._%+-]+@[a-z0-9-]+(?:\.[a-z0-9-]+)*\.[a-z]{2,}\b/gi;
const PHONE = /(?<!\d)(?:\+?63|0)9\d{2}[ -]?\d{3}[ -]?\d{4}(?!\d)/g;                       // Philippine mobile
// A code as apps and people write it: 4-8 digits, or 3 + 3 split by a space or dash ("843 833", "687-781")
const OTP_DIGITS = String.raw`(?:\d{4,8}(?!\d)|\d{3}[ -]\d{3}(?![ -]?\d))`;
// "The code is 482916" / "code mo ay 482916" / "code ko: 482916" / "WhatsApp code: 687-781", but not zip / promo / tracking / order codes
const BARE_CODE = new RegExp(String.raw`(?<!\b(?:zip|postal|area|promo|tracking|order|product|country|bar|qr)\s)\bcode\s*(?:(?:is|ay)\s*[:=]?|[:=]|(?:mo|ko)(?:\s+ay|\s*[:=]))\s*(${OTP_DIGITS})`, 'gid');

// "ko / mo / namin" plus Nokia-era text speak: "pin q 5521", "pin mu 1234"
const PIN_OWNER = String.raw`(?:ko|mo|namin|q|ku|mu|nmin)`;

// OTP / PIN / passcode / 2FA words followed (within a short distance) by 4-8 digits: "Passcode ko 0517", "6-digit code 739204"
const CODE = new RegExp(String.raw`\b(?:otp|one[- ]time[-\s]+(?:password|passcode|code|pin)|verification\s+code|security\s+code|authentication\s+code|auth\s+code|confirmation\s+code|login\s+code|2fa|mfa|two[- ]factor(?:\s+code)?|backup\s+code|recovery\s+code|m-?pin|passcode|atm\s+pin|pin\s+code|(?:my|your)\s+pin|pin\s+${PIN_OWNER}|pin\s+(?:ng|sa)\s+[\w-]+|\d[- ]?digits?\s+(?:code|pin|otp))\b[^\d\n]{0,25}?(\d{4,8})(?!\d)`, 'gid');

// Number first, then the PIN word and its owner (elderly / text-speak order): "cge nak 4821 mpin q"
const CODE_BEFORE = new RegExp(String.raw`(?<![\d.,])(\d{4,8})(?!\d)[^\d\n,.;]{0,8}?\b(?:m-?pin|atm\s+pin|pin)\s+${PIN_OWNER}\b`, 'gid');

// E-wallet / bank / app names: "GCash code ko 553201", "code galing GCash: 482913", "Maya OTP 739204".
// The digits must not be glued to letters ("GCash code PROMO2026" stays quiet).
const APPS = String.raw`(?:g-?cash|maya|paymaya|gotyme|seabank|shopee(?:pay)?|lazada|grab(?:pay)?|coins(?:\.ph)?|bdo|bpi|unionbank|metrobank|landbank|facebook|fb|gmail|google|messenger|viber|telegram|tiktok|roblox)`;
const CODE_WORD = String.raw`(?:code|otp|m?pin|passcode|auth(?:entication)?\s+code|verification\s+code)`;
const APP_CODE = new RegExp(String.raw`\b${APPS}\s+${CODE_WORD}\b[^\d\n]{0,25}?(?<![A-Za-z])(\d{4,8})(?!\d)`, 'gid');

// Words just before a long number that make it a transaction reference, not a card: "Ref No.", "Trace no."
const REFERENCE = /\b(?:ref(?:erence)?|trace|txn|transaction|tracking|order|invoice|parcel|package|shipment|waybill|ticket|booking)\b[^\d\n]{0,12}$/i;

// Everyday phrasing (lola, kids): "eto yung code na pinadala sa akin: 482913", "may dumating na code, 739204",
// "the code they sent me is 551203". Never promo / voucher / tracking / order codes.
const SENT = String.raw`(?:pinadala|ipinadala|dumating|natanggap|na-?receive|sinend|sinent|tinext|nag-?text|sent|received|texted|got)`;
const NOT_OTP = String.raw`(?<!\b(?:zip|postal|area|promo|tracking|order|product|country|bar|qr|voucher|discount|referral)\s)`;
const OTP_SENT = new RegExp(String.raw`${NOT_OTP}\b(?:code|otp|pin)\b[^\d\n]{0,40}?\b${SENT}\b[^\d\n]{0,25}?(\d{4,8})(?!\d)`, 'gid');
const OTP_SENT_BEFORE = new RegExp(String.raw`\b${SENT}\b[^\d\n]{0,25}?${NOT_OTP}\b(?:code|otp|pin)\b[^\d\n]{0,15}?(\d{4,8})(?!\d)`, 'gid');
const CODE_FROM_APP = new RegExp(String.raw`${NOT_OTP}\b${CODE_WORD}\s+(?:ng|sa|galing(?:\s+sa)?|from|para\s+sa)\s+${APPS}\b[^\d\n]{0,25}?(?<![A-Za-z])(\d{4,8})(?!\d)`, 'gid');

// Forwarded OTP texts put the code FIRST: "482913 is your GRAB verification code", "G-482913 is your Google verification
// code", "843 833 is your Instagram code", "482913 - Grab OTP for Udemy". Needs "is" / "ay" / "-" right after the number.
const CODE_FIRST = new RegExp(String.raw`(?<![\w.,#-])(?:g-)?(${OTP_DIGITS})[^\S\n]*(?:is|ay|-|–)[^\S\n]+(?:(?:your|ang\s+iyong|ang)[^\S\n]+)?(?:[A-Za-z][\w.]*[^\S\n]+){0,4}?${NOT_OTP}(?:(?:verification|security|authentication|auth|login|confirmation|activation|one[- ]time)[^\S\n]+)?(?:code|otp|pin|passcode)\b`, 'gid');
// "Use 482913 for two-factor authentication on Facebook."
const USE_CODE = new RegExp(String.raw`\buse\s+(${OTP_DIGITS})\s+(?:for|to|as)\b[^\d\n]{0,40}?\b(?:two[- ]factor|2fa|verification|authenticat\w*|log\s*in|sign\s*in|confirm\w*|code|otp)\b`, 'gid');
// Code word, a longer gap, then "is" / ":" right before the code: "Your One-Time PIN for your BDO Securities login is 482913",
// "The OTP code for your GCash Bank Transfer is 482913", "Use this code to confirm: 482913"
const CODE_IS = new RegExp(String.raw`\b(?:otp|one[- ]time[-\s]+(?:password|passcode|code|pin)|(?:verification|security|confirmation|login|activation|auth(?:entication)?)\s+code|(?:this|the)\s+code|m-?pin|passcode)\b[^\d\n]{0,40}?(?:\bis|\bay|[:=])[^\S\n]*(${OTP_DIGITS})`, 'gid');

// CVV / CVC: "CVV 123", "yung CVV sa likod ng card ko 123"
const CVV = /\b(?:cvv2?|cvc2?|csc)\b[^\d\n]{0,25}?(\d{3,4})(?!\d)/gid;

// Bank account numbers (soft warning): "BDO account no. 0012345678", "Account no. 0012345678 BDO"
const BANK_ACCOUNT = new RegExp(
  String.raw`\b(?:account|acct|acc|bank|bdo|bpi|metrobank|unionbank|landbank|pnb|rcbc|security\s*bank|china\s*bank|gotyme|seabank|cimb|eastwest|psbank)\b` +
  String.raw`[^\d\n]{0,25}?(\d[\d -]{7,16}\d)(?!\d)`, 'gid');

const KEY_CONTEXT = new RegExp(
  String.raw`\b(?:api[ _-]?key|apikey|api[ _-]?secret|secret[ _-]?key|client[ _-]?secret|access[ _-]?token|refresh[ _-]?token|auth(?:orization)?[ _-]?token|bearer|token|private[ _-]?key|service[ _-]?account[ _-]?key|susi)\b` +
  String.raw`(?:\s+(?:ko|mo|nya|niya|namin|natin|po|(?:ng|sa|para\s+sa)\s+[\w-]+))?` +
  String.raw`[^\S\n]*(?:(?:is|ay|ang)\s*)?[:=]?[^\S\n]*["']?([A-Za-z0-9_\-./+=]{16,})`, 'gid');

const ENV_KEY = /\b[A-Z][A-Z0-9_]*(?:API_KEY|APIKEY|SECRET|TOKEN|PRIVATE_KEY|ACCESS_KEY)\s*=\s*["']?([^\s"']{8,})/gd;
const ENV_PASSWORD = /\b[A-Z][A-Z0-9_]*(?:PASSWORD|PASSWD|PWD)\s*=\s*["']?([^\s"']{4,})/gd;

const KEY_PATTERNS = [
  [/\bAIza[0-9A-Za-z_-]{35}\b/g, 0.97],                                       // Google / Gemini
  [/\bsk-(?:ant-|proj-)?[A-Za-z0-9_-]{20,}/g, 0.97],                          // OpenAI / Anthropic style
  [/\b(?:AKIA|ASIA)[0-9A-Z]{16}\b/g, 0.97],                                   // AWS access key id
  [/\bgh[pousr]_[A-Za-z0-9]{36,}/g, 0.97],                                    // GitHub
  [/\b(?:sk|rk)_(?:live|test)_[A-Za-z0-9]{16,}/g, 0.97],                      // Stripe / PayMongo style
  [/\bxnd_(?:development|production)_[A-Za-z0-9]{20,}/g, 0.97],              // Xendit
  [/\bxox[abprs]-[A-Za-z0-9-]{10,}/g, 0.97],                                  // Slack
  [/\bGOCSPX-[A-Za-z0-9_-]{20,}/g, 0.97],                                     // Google OAuth client secret
  [/\bhf_[A-Za-z0-9]{30,}/g, 0.97],                                           // Hugging Face
  [/\bgsk_[A-Za-z0-9]{40,}/g, 0.97],                                          // Groq
  [/\bnpm_[A-Za-z0-9]{36}\b/g, 0.97],                                         // npm
  [/\bSG\.[A-Za-z0-9_-]{16,}\.[A-Za-z0-9_-]{16,}/g, 0.97],                    // SendGrid
  [/\b\d{8,10}:[A-Za-z0-9_-]{35}\b/g, 0.95],                                  // Telegram bot token
  [/\beyJ[A-Za-z0-9_-]{8,}\.eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}/g, 0.95], // JWT
  // PEM / SSH private key: the whole block (to the END line, or to the end of the text) so "Itago" removes it all
  [/-----BEGIN (?:[A-Z]+ )*PRIVATE KEY-----(?:[\s\S]*?-----END (?:[A-Z]+ )*PRIVATE KEY-----|[\s\S]*)/g, 0.99],
  [/\b[a-z][a-z0-9+.-]{1,15}:\/\/[^\s:/@]+:[^\s@/]{3,}@\S+/g, 0.95],          // URL with user:password@
];

const GOV_ID = new RegExp(
  String.raw`\b(?:sss|tin|philhealth|phil\s?health|pag-?ibig|hdmf|umid|crn|philsys|psn|national\s+id|passport|driver'?s?\s+licen[sc]e|lisensya)\b` +
  String.raw`[^\d\n]{0,25}?([A-Za-z]?\d[\d -]{5,18}\d[A-Za-z]?)`, 'gid');

const NOT_A_SECRET = new Set(['required', 'wrong', 'incorrect', 'invalid', 'correct', 'expired', 'changed', 'hidden',
  'secret', 'safe', 'strong', 'weak', 'not', 'the', 'your', 'this', 'that', 'here', 'reset',
  'hindi', 'dapat', 'kailangan', 'mali', 'tama', 'nasa', 'para', 'sikreto', 'lihim',
  'mahina', 'malakas', 'ishare', 'i-share', 'nag-expire', 'naka-save', 'nakalimutan']);

const looksLikeSecret = (v) => v.length >= 16 && (/\d/.test(v) || (/[A-Z]/.test(v) && /[a-z]/.test(v)));
// Already hidden with "Itago" (Kotlin twin: Redactor.isMasked)
const isMaskedSpan = (s) => s.includes('•') || s.includes('[private key removed]');

const hit = (category, kind, start, end, confidence, reason, severity = 'HIGH') => ({ category, kind, severity, start, end, confidence, reason });
const password = (start, end, hasDigit) => hit('PASSWORD', 'password', start, end, hasDigit ? 0.95 : 0.85,
  'This message appears to include a password. Sharing it could give someone access to your account.');
const passwordReminder = (start, end, hasDigit) => hit('PASSWORD', 'password', start, end, hasDigit ? 0.75 : 0.7,
  'This message may include a password. Intended?', 'MEDIUM');
const apiKey = (start, end, confidence) => hit('API_KEY', 'api_key', start, end, confidence,
  'This looks like an API key or access token. Anyone with it could use your account or run up charges.');
const OTP_REASON = 'This looks like a verification code or PIN. Sharing it could let someone into your account.';

export function detectRules(text, { flagPhoneNumbers = true, flagEmails = true, flagBankAccounts = true } = {}) {
  if (!text.trim()) return null;
  const hits = [];

  for (const m of text.matchAll(CARD)) {
    const digits = m[0].replace(/\D/g, '');
    // "Ref No. 1023 4567 89012": GCash / bank transaction numbers are shared as proof of payment, not card numbers
    const isReference = REFERENCE.test(text.slice(Math.max(0, m.index - 25), m.index));
    if (digits.length >= 13 && digits.length <= 19 && luhnValid(digits) && !isReference) {
      hits.push(hit('CARD', 'credit_card', m.index, m.index + m[0].length, 0.95,
        'This looks like a payment card number. Sharing it could let someone make charges.'));
    }
  }
  for (const m of text.matchAll(CVV)) {
    const [gs, ge] = m.indices[1];
    hits.push(hit('CARD', 'credit_card', gs, ge, 0.9, 'This looks like a card security code (CVV). Sharing it could let someone use your card.'));
  }
  for (const [re, conf] of KEY_PATTERNS) for (const m of text.matchAll(re)) hits.push(apiKey(m.index, m.index + m[0].length, conf));
  for (const m of text.matchAll(KEY_CONTEXT)) {
    const [gs, ge] = m.indices[1];
    if (looksLikeSecret(m[1])) hits.push(apiKey(gs, ge, 0.9));
  }
  for (const m of text.matchAll(ENV_KEY)) { const [gs, ge] = m.indices[1]; hits.push(apiKey(gs, ge, 0.95)); }

  for (const m of text.matchAll(PASSWORD)) {
    const [gs] = m.indices[1];
    const token = m[1].replace(/[.,;!?)]+$/, '');
    if (token.length < 3 || NOT_A_SECRET.has(token.toLowerCase())) continue;
    hits.push(password(gs, gs + token.length, /\d/.test(token)));
  }
  for (const re of [CONVERSATIONAL_PASSWORD_AFTER, CONVERSATIONAL_PASSWORD_BEFORE]) {
    for (const m of text.matchAll(re)) {
      const [gs] = m.indices[1];
      const token = m[1].replace(/[.,;!?)]+$/, '');
      if (token.length < 3 || NOT_A_SECRET.has(token.toLowerCase())) continue;
      if (!hits.some((x) => x.start === gs && x.end === gs + token.length)) {
        hits.push(passwordReminder(gs, gs + token.length, /\d/.test(token)));
      }
    }
  }
  for (const m of text.matchAll(ENV_PASSWORD)) { const [gs, ge] = m.indices[1]; hits.push(password(gs, ge, true)); }

  for (const m of text.matchAll(CODE)) { const [gs, ge] = m.indices[1]; hits.push(hit('PASSWORD', 'otp', gs, ge, 0.85, OTP_REASON)); }
  for (const re of [CODE_BEFORE, CODE_FIRST, USE_CODE, CODE_IS, BARE_CODE, OTP_SENT, OTP_SENT_BEFORE, APP_CODE, CODE_FROM_APP]) {
    for (const m of text.matchAll(re)) { const [gs, ge] = m.indices[1]; hits.push(hit('PASSWORD', 'otp', gs, ge, 0.8, OTP_REASON)); }
  }

  for (const m of text.matchAll(GOV_ID)) {
    const [gs, ge] = m.indices[1];
    hits.push(hit('OTHER_PII', 'gov_id', gs, ge, 0.9, 'This looks like a government ID number. Sharing it could allow identity theft.'));
  }
  // MEDIUM severity ("Intended?"): small businesses share these on purpose, so a soft warning
  if (flagEmails) for (const m of text.matchAll(EMAIL)) {
    hits.push(hit('OTHER_PII', 'email', m.index, m.index + m[0].length, 0.7, 'This contains an email address. Intended?', 'MEDIUM'));
  }
  if (flagPhoneNumbers) for (const m of text.matchAll(PHONE)) {
    hits.push(hit('OTHER_PII', 'phone', m.index, m.index + m[0].length, 0.7, 'This contains a phone number. Intended?', 'MEDIUM'));
  }
  if (flagBankAccounts) for (const m of text.matchAll(BANK_ACCOUNT)) {
    const [gs, ge] = m.indices[1];
    hits.push(hit('OTHER_PII', 'bank_account', gs, ge, 0.7, 'This contains a bank account number. Intended?', 'MEDIUM'));
  }

  let best = null;
  for (const h of hits) {
    if (isMaskedSpan(text.slice(h.start, h.end))) continue;            // already hidden with "Itago"
    if (!best || h.confidence > best.confidence) best = h;             // first max wins on ties
  }
  return best;
}
