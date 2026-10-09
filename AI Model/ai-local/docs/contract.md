# Contract: AI core ↔ keyboard ↔ Flutter

Everything is on-device. No HTTP, no cloud DB. Change this file only after all four owners agree.

## 1. `check(text)` (Kotlin, used by the keyboard directly)

```kotlin
fun check(text: String): RiskResult?   // null = nothing found
```

```kotlin
RiskResult(
  category: PASSWORD | API_KEY | CARD | PRIVATE_NOTE | OTHER_PII,   // API_KEY is new: tell Product/Flutter leads
  start: Int, end: Int,        // char offsets of the risky span in `text`
  reason: String,              // short, cautious, user-facing
  confidence: Float,           // 0..1
  noteLabel: String?           // PRIVATE_NOTE only
)
```

Rules: call off the main thread, on a typing pause; never log or persist `text`.

`RiskResult` also carries `kind` (password | otp | api_key | credit_card | gov_id | email | phone | bank_account | pii | private_match | my_secret)
and `severity` (HIGH red / MEDIUM soft yellow: email, phone, bank account). The team-facing wrapper is `com.secondlook.DetectionEngine`
(`detect(text): List<Warning>`, `Warning(type, message, severity, matchedText)`); see `architecture-reconciled.md`.

## 2. Flutter MethodChannel `secondlook/ai`

| Method | Args | Returns |
|---|---|---|
| `check` | `text` | `null` or `{type, message, severity, matchedText, start, end, tip, canHide, headline}` (see `bridge/AiMethodChannel.kt`) |
| `hideAll` | `text` | the text with every secret masked ("Itago") |
| `saveNote` | `text`, `label` | `{id, label, kind}`; kind `secret` (one value such as an MPIN: fingerprint only) or `fact` (a sentence: meaning + names). Error if 5 already |
| `listNotes` | – | `[{id, label, kind}]` (never the secret text) |
| `status` | – | `{ready, error}` |
| `deleteNote` | `id` | ok |
| `deleteAll` | – | ok |

## 3. Warning wording (cautious)
- PASSWORD: “This message appears to include a password. Sharing it could give someone access to your account.”
- PASSWORD (OTP/PIN variant): “This looks like a verification code or PIN. Sharing it could let someone into your account.”
- API_KEY: “This looks like an API key or access token. Anyone with it could use your account or run up charges.”
- OTHER_PII (government ID): “This looks like a government ID number. Sharing it could allow identity theft.”
- CARD: “This looks like a payment card number.”
- PRIVATE_NOTE: “This **may** reveal information you marked confidential: {noteLabel}.”
- Always offer Review / Dismiss (or Review / Continue). Never say “guaranteed safe”.

## 4. Storage
- Notes: max 5, AES-GCM with an Android Keystore key, embedding vectors stored with each fact.
- Taught secret values (`kind: secret`): only an HMAC-SHA256 fingerprint is stored (inside the encrypted blob); the HMAC key is itself encrypted with the Keystore key. The value is never stored.
- Models: bundled in the APK under `assets/models/`. No downloads at runtime.
- Release APK must not declare `INTERNET` (check: `aapt dump permissions app-release.apk`).
