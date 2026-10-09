# SecondLook: architecture, reconciled with the AI core that is already built

This merges the team manual ("Full Architecture & Build Manual") with what the AI lead has built and **measured** in `ai-local/`.
The product, roles, schedule and Flutter + native-Kotlin split in the manual all stand. Where the manual and the measurements disagree, the table in section 4 says what we do and who has to decide.

> **Superseded by the team manual `ARCHITECTURE.md`** (roles, plan, API). This file is the earlier reconciliation; "Jkim" below is the keyboard owner in that older manual version, now **Klarenz**. Numbers updated Oct 9, 9:15 PM.

**Status (Oct 9, ~7:50 PM):** the AI core is built and tested **on a laptop (Kotlin/JVM + browser) and as an Android test APK that has not yet been run on a phone.** Nothing here has been tested inside the keyboard service or on the Tecno phone.

## 1. Architecture (unchanged shape, AI layer filled in)

```
Flutter app (Charles/Jkim)                 Native Kotlin keyboard (Jkim)
  onboarding / secrets / practice            SecondLookIME (InputMethodService)
        │  MethodChannel "secondlook/ai"            │  detectAsync(text) on typing pause
        ▼                                           ▼
 ┌──────────────────────── DetectionEngine  (one instance per process, DetectionEngine.get(ctx)) ────────────────────────┐
 │  SecondLookChecker.check(text)  — cheapest stage first, first hit wins                                                   │
 │   1. RuleDetector      regex + Luhn: password, OTP/PIN, API keys/tokens, cards, gov IDs, email, PH phone   (no model)    │
 │   2. PiiDetector       bert-small-pii (ONNX, 27 MB)  — extra catch for email / unseen shapes                             │
 │   3. NoteMatcher       all-MiniLM-L6-v2 (ONNX, 22 MB) + anchors vs. the user's private notes ("Teach me")                │
 │  SecretManager  → EncryptedNotesStore (Android Keystore AES-GCM, max 5 notes, label shown, text never leaves)            │
 └──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
 No INTERNET permission. Models are inside the APK (assets/models). Nothing is logged or uploaded.
```

## 2. The manual's files → where they are

| Manual says | Now | Owner |
|---|---|---|
| `DetectionEngine.kt` (`detect(text): List<Warning>`) | `kotlin/com/secondlook/DetectionEngine.kt`. Same API and `Warning` shape; adds `get(ctx)`, `detectAsync`, `awaitReady`, `isReady` | AI (Annie) |
| `RuleDetector.kt` | `kotlin/com/secondlook/ai/rules/RuleDetector.kt` (replaces the skeleton; see section 4) | AI |
| `EmbeddingEngine.kt` | `ai/model/EmbeddingModel.kt` + `WordPieceTokenizer.kt` (the manual omitted the tokenizer; it is written) | AI |
| `SecretManager.kt` (was Jkim) | `kotlin/com/secondlook/SecretManager.kt` facade over `ai/android/EncryptedNotesStore.kt` | AI owns storage, Jkim wires the channel |
| `minilm_l6_v2.onnx` | `assets/models/embedding/model_quantized.onnx` (+ `vocab.txt`) — already downloaded, quantized | AI |
| Flutter MethodChannel | `ai/bridge/AiMethodChannel.kt` (ready-to-paste, commented because it needs Flutter) | Jkim pastes |
| `SecondLookIME.kt`, `KeyboardView.kt`, `WarningBanner.kt`, layouts, Flutter screens | not in `ai-local` | Jkim / Charles |

## 3. The API the keyboard and Flutter call

```kotlin
// in SecondLookIME.onCreate()  — returns immediately; models load in the background
val engine = DetectionEngine.get(this)

// on the typing pause (never on the main thread — detectAsync does that for you)
engine.detectAsync(text) { warnings ->
    val w = warnings.firstOrNull()
    if (w == null) hideWarning() else showWarning(w)   // w.type, w.message, w.severity, w.matchedText
}

// private notes (call off the main thread; the Flutter bridge already does)
engine.secrets.save("Si Jakim ay nakatira sa Taguig", "Jakim location")
engine.secrets.list(); engine.secrets.delete(id); engine.secrets.deleteAll()
```

`Warning.type` (key your `strings.xml` on this; `message` is the English default):
`password`, `otp`, `api_key`, `credit_card`, `gov_id`, `email`, `phone`, `bank_account`, `pii`, `private_match`, `my_secret` (a value the user taught, e.g. their MPIN).
`Warning.severity`: **`high` = red banner**, **`medium` = soft yellow "Intended?"** (email, phone, bank account).
Until the models finish loading, `detect` uses the rules only, so the keyboard is never blocked.

## 4. Where the manual and the measurements disagree

All numbers: `ai-local/test-data/samples.json` (146 messages + 5 private-note sets + 5 taught secrets), run in Kotlin and in a browser port, same results.

| # | Manual | What we found | Resolution | Decide |
|---|---|---|---|---|
| 1 | Test case 3 `4532 1234 5678 9012` should warn | It **fails the Luhn checksum**, so no Luhn detector (including the manual's own) can flag it | Use the Luhn-valid `4532 0151 1283 0366` in the demo and test list. Real card numbers pass Luhn | Jakim |
| 2 | Private match if similarity > 0.65–0.75; claims 0.80 for "Let's raid Jakim in Taguig" and 0.35 for "Jakim is a great developer" | Measured **0.53** and **0.26**. At 0.65 the demo example would **not** warn. Pure similarity also over-fires ("buying bread at the bakery" 0.66 vs a real paraphrase 0.56) | **Anchors + similarity**: the names/code words in the note (Jakim, Taguig) must appear (up to 2), plus ≥ 0.35 similarity; with no anchor, ≥ 0.75. Cases 7, 8 warn, case 13 stays quiet | decided (AI lead) |
| 3 | "MiniLM was trained on multilingual data; Filipino/Taglish partially works" | all-MiniLM-L6-v2 is English. Tagalog drafts scored low; the multilingual model we tried scored worse | Don't claim Taglish semantic matching. Say: works on English meaning; names and code words work in any language | Jakim (pitch) |
| 4 | Phone numbers: warn (case 5). Email: maybe (case 14) | Both are things small businesses share on purpose (GCash number, email to receive payment) | **Soft** warning: `severity = medium`, "Intended?", yellow banner. Bank **account** numbers get the same soft warning. Switchable: `RuleDetector(flagPhoneNumbers, flagEmails, flagBankAccounts)` | Annie + Charles (banner colours) |
| 5 | Rule regex skeletons | `OTP mo ay 5832` (case 15) does **not** match the skeleton (it allows one connector word). `pin`/`secret` + `.{3,}` and no word boundaries would flag ordinary text | Use the new `RuleDetector`. It passes all of the manual's test cases (12/12 as written in `samples.json`) and 146/146 overall | done |
| 6 | Categories: card, password, otp, private match, phone | Real chat leaks are mostly API keys/tokens and admin logins | Added `api_key` (Gemini/OpenAI/AWS/GitHub/Hugging Face/PayMongo/Xendit/JWT/PEM/DB URLs…), `gov_id` (SSS, TIN, PhilSys, PhilHealth, Pag-IBIG, UMID, passport, licence), Taglish phrasing | needs Charles/Jakim to add strings + icon for the new types |
| 7 | `onnxruntime-android:1.16.3`, `security-crypto:1.1.0-alpha06` | We built and ran with **onnxruntime-android 1.26.0**, minSdk 26. Notes use Keystore AES-GCM directly, so `security-crypto` is not needed | Use 1.26.0, drop `security-crypto` | Jkim |
| 8 | Export the model with `optimum-cli` | The quantized model (22 MB, vs ~90 MB fp32) and its vocab are already in `assets/models/` | Skip the export | Annie |
| 9 | Disclosures: MiniLM "(MIT license)" | The Hugging Face model page lists **Apache-2.0**. The PII model `bert-small-pii-detection` (Apache-2.0) is also used and was missing from the list. ONNX Runtime is MIT | Fix the disclosure list; re-check each license page before submitting | Jakim |
| 10 | `SecretManager` is Jkim's | Saving a secret needs the embedding model, so storage must sit with the AI core (one owner per code area) | AI core owns it; Jkim wires the MethodChannel | agreed by playbook rule |
| 11 | `checkCurrentText()` runs on the main thread | Model inference can take tens of ms on a phone (laptop: ~1–10 ms) | Use `detectAsync` | Jkim |
| 12 | Read text with `getExtractedText(...)` | **Not tested by us.** Many apps return null for it; `getTextBeforeCursor(n,0)` + `getTextAfterCursor(n,0)` is the usual fallback | Try both on the demo apps | Jkim / Jakim |
| 13 | `override fun onEditorAction(...)` in the IME | **Not verified**, but as far as we know this is not an `InputMethodService` callback; the usual place is your own Enter key handler, then `sendDefaultEditorAction(true)` | Check on the phone before building on it | Jkim |

## 5. What each person does now

**Jkim (keyboard):** copy `ai-local/kotlin/` into `android/app/src/main/kotlin/` and `ai-local/assets/models/` into `android/app/src/main/assets/models/`; add `implementation("com.microsoft.onnxruntime:onnxruntime-android:1.26.0")`, `minSdk 26`, and `androidResources { noCompress += "onnx" }` (see `docs/integration.md`). Call `DetectionEngine.get(this)` and `detectAsync`. Paste `AiMethodChannel` into `MainActivity`. Confirm the release APK has no `INTERNET` (`aapt dump permissions`).
**Annie (AI):** done: rules, tokenizer, both ONNX models, notes store, anchors, adapter, test sets, laptop + browser + APK test harnesses. Left: run on the Tecno phone (load time, ms per check, airplane mode), tune with Jakim, help Jkim integrate.
**Charles (UI):** two banner styles (red `high`, yellow `medium`); strings/icons for the new `type`s; show the saved note **label** only.
**Jakim (test):** use `test-data/samples.json` (146 messages) plus the manual's 15 cases, with the Luhn fix in #1. Update the demo script: the "Jakim / Taguig" demo works because both names appear; a paraphrase with **no** name and only moderate similarity is **missed** (e.g. "We should celebrate after the bakery acquisition closes!").

## 6. Known limits (say these honestly in the pitch)
- Private-note matching on 4 note sets / 33 drafts: flags **13 of 15** real paraphrases, **3 of 18** false alarms (drafts that literally contain the protected word). Small, hand-written set: it shows direction, not general accuracy.
- A meaning-only paraphrase without any of the note's names is usually missed.
- Rules are patterns: new phrasings will slip through; the Filipino stop-list is a heuristic.
- Not yet verified on a phone: model load time, per-check latency, text access from the keyboard, the IME Enter-key interception.
- The keyboard cannot block the host app's own Send button.
