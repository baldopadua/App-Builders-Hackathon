# Architecture

---

# 🛡️ SecondLook Keyboard — Full Architecture & Build Manual

> **"A second thought before you send."** · *Turuan ang phone mo na magtago ng sikreto.*
> 
> 
> AppBuildersPH Hackathon 2026 · Local AI · Privacy Tools · 🇵🇭 Identity: the Palawan Pangolin
> 
> **Team:** Klarenz (Keyboard Lead) · Annie (AI Lead) · Charles (UI/Design Lead) · Jakim (Test & Identity Lead)
> 
> **GitHub:** baldopadua · **LinkedIn:** jakim-lopez
> 
> **Updated Oct 9, 9:00 PM:** concept matches the AI core Annie has already built and tested (`ai-local/`). Roles unchanged. **Everything DONE by 8:00 AM, SUBMITTED before 9:00 AM.** Official details from the participant briefing are in the Time Budget.
> 
> 🤖 **Using an AI assistant to code?** Paste **section 18** into it first.
> 

---

## 🆕 What Changed in This Version (read this first)

- **Who it's for.** Everyday Filipinos first — **lolo & lola, kids & teens, parents, small online sellers** — the people who "send lang nang send" and never get reminded. Developers and techy users second. Same protection for both.
- **What it protects first:** **passwords & PINs**, **OTP / verification codes**, and **API & secret keys** (Gemini, Hugging Face, OpenAI, SSH keys, database passwords). Then cards, government IDs, and a soft check for GCash numbers, emails and bank accounts.
- **Inspiration (section 1):** leaked passwords and keys get used — in 2026 even AI agents in testing reached real companies' systems, one of them using passwords found online.
- **Real prevention, not just a warning:** (1) warns while you type, with a **Filipino-first headline** (*"Teka! OTP o PIN ito — huwag ibigay kahit kanino."*), (2) **"Itago"** hides the secret inside the draft with one tap, (3) **holds the keyboard's own Send key** until you confirm.
- **Everyday phrasing works:** *"Anak, eto yung code na pinadala sa akin: 482913"*, *"Password ng ML account ko: Lodi1234"*, *"PIN ng ATM ko 1234"*.
- **"Teach me what's private"** understands **meaning** (no names needed for close wording) **and** remembers the **names / code words** you protect.
- **Strict deadline:** done **8:00 AM**, submitted **before 9:00 AM**, feature freeze **7:00 AM**.
- **Official briefing facts:** pitch = **5 min pitch + live demo, then 3 min Q&A**, starting **1:40 PM** (12:00 PM is registration); demo video **~1 minute**; **X or LinkedIn video post required — tag Devin / Cognition, #AppBuildersPH**; **one submission only, no edits**; public repo with rebuild instructions.
- **Identity:** the Palawan pangolin + Grammarly-style keyboard references (section 1).
- **Verified so far (laptop only):** detection **128/128** synthetic test messages, Itago **77/77** risky messages come back clean, Kotlin and browser give identical results, the Android test APK builds. **Not yet run on the Tecno phone or inside the keyboard.**

Details: `ai-local/docs/architecture-reconciled.md` · Test data: `ai-local/test-data/samples.json`

---

## ⏰ Time Budget

**Our deadlines (stricter than the official ones):**

| Milestone | Time | Notes |
| --- | --- | --- |
| Start | 7:30 PM Oct 9 | **~12.5 hours** until everything is done |
| ⭐ Full flow works offline | **3:00 AM** | Checkpoint 5 (one hour earlier than the first plan) |
| 🔒 Feature freeze | **7:00 AM** | Bug fixes only after this |
| ✅ Everything DONE: release APK, ~1-minute video, README, description, screenshots | **8:00 AM — STRICT** |  |
| 📤 Everything SUBMITTED: Cerebral Valley form, public GitHub, video link, X/LinkedIn post | **before 9:00 AM — STRICT** | Official deadline 10:00 AM is only our buffer |
| 🚶 At the venue, rehearsed | **11:00 AM** | One hour before registration opens |

**Official schedule (from the participant briefing):**

| Oct 10 | What | Where / rule |
| --- | --- | --- |
| **10:00 AM** | **Submission deadline — no extensions.** One submission per team, **no edits or resubmits**. GitHub must be **public** by then; code freezes at 10:00 AM (judges review the repo as of the deadline) | [cerebralvalley.ai/e/appbuildersph-hackathon-2026](http://cerebralvalley.ai/e/appbuildersph-hackathon-2026) |
| 12:00 PM | Registration and participant arrival | **Cyberzone, SM Makati** (in person) |
| 12:15 PM | Demo and AV technical checks | Wi-Fi, power, HDMI and USB-C available — bring our own laptop |
| 1:00 PM | Official opening ·**finalists announced** (10-15 teams) | Finalists must be on site |
| **1:40 PM** | **Finalist pitching begins** (continues 3:40 PM after a break) | **5 min pitch + live demo, then 3 min judge Q&A** |
| 5:45 PM | Awards ceremony | Event ends 7:00 PM |

Questions during the build: the official Telegram group. Concerns on Demo Day: Bryl Lim or any organizer. Judging: 25% problem & usefulness · 25% local AI · 20% technical execution · 15% innovation · 15% product & demo quality.

---

## 1. What We're Building (30-second Explanation)

SecondLook **teaches your phone to keep secrets.** It is an **Android keyboard** that runs **AI entirely on-device** and catches the moment you're about to send something that should never leave your phone — the OTP a fake "GCash agent" asked for, your ATM PIN, your Mobile Legends password, a Gemini API key pasted from a `.env` — or something you personally marked as private.

**It warns you WHILE you type, hides the secret with one tap, and holds the Send key until you're sure.** No cloud. No internet needed. Works offline.

**The problem:** people **send lang nang send**. Nobody reminds lola that a "GCash agent" should never get her OTP, or reminds a kid that a classmate shouldn't get his game password, or reminds a busy developer that the `.env` he just pasted has a live key. By the time they notice, the message is already sent.

### Who It's For (in priority order)

| Priority | Who | What usually leaks |
| --- | --- | --- |
| **1** | **Everyday Filipinos** — lolo & lola, kids & teens, parents, small online sellers | OTP / GCash MPIN given to a "GCash agent" or "promo"; ATM PIN sent to a relative; game / Facebook password shared with a classmate; TIN or ID number sent to a "loan agent" |
| **2** | **Developers, freelancers, IT students** | API keys (Gemini, Hugging Face, OpenAI…), SSH private keys, database passwords pasted into group chats |

### What It Protects (in priority order)

1. **Passwords & PINs** — account passwords, GCash MPIN, ATM PIN, game and Facebook passwords
2. **OTP / verification / authentication / 2FA codes**
3. **API & secret keys** — Gemini, Hugging Face, OpenAI, AWS, GitHub, PayMongo, Xendit…, **SSH private keys**, **database passwords**
4. **Cards + CVV**, **government IDs** (TIN, SSS, PhilSys, PhilHealth, Pag-IBIG, UMID, passport, driver's license)
5. 🟡 Soft "Intended?" check: **GCash / phone number, email, bank account** (people share these on purpose to get paid)
6. **Anything you teach it** — "Teach me what's private"

### Inspiration: leaked secrets get used — even by AI

- **OpenAI — "Hugging Face incident and the road ahead":** [https://openai.com/index/hugging-face-incident-and-the-road-ahead/](https://openai.com/index/hugging-face-incident-and-the-road-ahead/) — OpenAI disclosed (July 21, 2026) that an internal research AI model under test got past its sandbox and reached Hugging Face's systems. Hugging Face's account of how it happened differs. ([OpenAI technical report](https://cdn.openai.com/pdf/67869394-cb91-4c12-888c-5cbd85c7814c/OpenAI-Hugging-Face%20Incident-Technical-Report.pdf))
- **ANC News — "Google's consumer AI model Gemini hacked…":** [https://www.facebook.com/ANCalerts/posts/pfbid0SGaQU6jw8d2NhYPQBeLJpkpSzxiMZkACiykLML7efCu3aakUbcMZZNS9paAdHJMnl](https://www.facebook.com/ANCalerts/posts/pfbid0SGaQU6jw8d2NhYPQBeLJpkpSzxiMZkACiykLML7efCu3aakUbcMZZNS9paAdHJMnl) — per CNN / AFP reports (Sept 19, 2026), Google said Gemini, while being tested, got into three real companies **using passwords it found online or by guessing logins**. ([CNN report via Atlanta News First](https://www.atlantanewsfirst.com/2026/09/19/google-says-ai-model-gemini-hacked-into-3-companies-while-undergoing-testing/))
- **Our takeaway:** once a password, OTP or key is out, anyone — even an AI agent — can find it and use it. The cheapest place to stop a leak is **before it is sent**, on the phone, without sending your typing to anyone.

*(Summaries are from news coverage; open the originals before quoting them on stage.)*

### Why a Phone Keyboard

- Most Filipinos — especially lolo, lola and kids — do everything on the phone: chats, GCash, OTP texts, games.
- A keyboard is in **every** app, so one install protects Messenger, Viber, SMS, games and browsers on the phone. No copy-paste into a separate checker.
- Developers also paste keys on desktop; a browser/desktop version is on the roadmap. The phone is where the people who never get reminded do their typing — and where a curious kid or a borrowed phone needs the reminder most.

### Sound Familiar? (the relatable part)

| Situation | What gets typed | SecondLook |
| --- | --- | --- |
| 👵 Lola, texting her "apo" who is really a scammer | `Anak, eto yung code na pinadala sa akin: 482913` | 🔴*"Teka! OTP o PIN ito — huwag ibigay kahit kanino."* → **Itago** → `…sa akin: ••••••` |
| 🎁 "Nanalo ka sa promo! Ibigay lang ang OTP" | `Ma'am eto po yung OTP: 902114, para ma-claim ko na yung premyo` | 🔴 + tip:*"Walang totoong bangko, GCash o Maya na hihingi ng OTP o PIN mo. Kapag may humingi, scam iyan."* |
| 🎮 Kid trading accounts with a classmate | `Password ng ML account ko: Lodi1234` | 🔴*"Ingat! Mukhang password ito."* → **Itago** |
| 🏧 Pinapakuha ang pera sa kamag-anak | `PIN ng ATM ko 1234, pakikuha mo na lang yung pera` | 🔴 →**Itago** |
| 🧾 "Loan agent" asking for your TIN | `TIN ko: 123-456-789-000` | 🔴 →**Itago** → `TIN ko: •••-•••-•••-000` |
| 💳 Online seller "verification" | `card: 5555555555554444 cvv 123` | 🔴 →**Itago** → `card: ••••••••••••4444 cvv •••` |
| 👩‍💻 Dev in the barkada GC: "paste ko na lang yung .env" | `GEMINI_API_KEY=AIza…` | 🔴 →**Itago** → `GEMINI_API_KEY=AIza••••` |
| 🖥️ "Eto yung SSH key ng server" / "pa-send ng DB password" | `-----BEGIN OPENSSH PRIVATE KEY-----` · `postgres://admin:Sup3rS3cret!@db…` | 🔴 →`[private key removed]` · `postgres://admin:••••••@db…` |
| 🏪 Small business getting paid | `Pa-GCash na lang sa 09171234567` · `BDO account no. 0012345678` | 🟡*"Sinadya mo ba?"* — normal to share, so a soft check, not an alarm |

### How SecondLook Prevents the Mistake (3 steps)

1. **Warn while typing** — a red or yellow banner above the keys with a **Filipino-first headline**, the reason, and a tip.
2. **Itago (one tap)** — the secret is replaced *inside the draft* (`•••••`, `AIza••••`, `••• •••• •••• 0366`), so the rest of the message can still be sent. SecondLook checks again; on our tests every masked message comes back clean.
3. **Hold the Send key** — if a red warning is still there, SecondLook's own Send/Enter key asks first: **[Itago] [Review] [Send anyway]**.

### The Killer Feature: "Teach Me What's Private"

Users save a private note like *"Si Jakim ay nakatira sa Taguig"* — and if they later type *"Let's raid Jakim in Taguig"* in any messaging app, SecondLook warns them. **No other keyboard we know of does this.**

**It is smart in two ways:**

- **Meaning (the AI):** save *"We are raising our prices by 20 percent next month. Do not tell customers yet."* → typing *"Prices will go up by 20% next month"* warns, **with no names at all**. An on-device AI model compared the meaning.
- **Names and code words:** SecondLook also remembers the names in your note (*Jakim*, *Taguig*). When they show up together, it warns even on looser wording. Just one name in an unrelated sentence (*"Jakim is a great developer"*) → no warning.`

### Identity: the Palawan Pangolin 🇵🇭 (Jakim + Charles)

**Why a pangolin?** When a pangolin senses danger, it curls into a tight, armored ball. SecondLook does the same for a person's data: when sensitive information is about to be sent, it steps in to protect it before anything is exposed. Because all detection runs on the device with a local AI model, nothing leaves the phone — just as the pangolin keeps everything safely tucked inside its shell.

The **Palawan pangolin is endemic to the Philippines**, which gives us an authentically Filipino identity that is distinctive and rarely used in branding. Its gentle, shy nature lets us present the app as a **caring guardian rather than a surveillance tool**, which fits the Filipino values of **ingat** (care) and **malasakit** (concern for others).

**How the identity shows up in the product:**

| Where | What |
| --- | --- |
| Keyboard top strip | A small pangolin button (like Grammarly's "G" button):**calm** when the draft is safe, **peeking** on a yellow "Intended?", **curled into a ball** on a red warning, **uncurls** after Itago. *Polish — build after Checkpoint 4.* |
| Voice (strings, Jakim) | Gentle and caring, never scolding — the built-in headlines already follow this:*"Ingat! Mukhang password ito."* · *"Teka! OTP o PIN ito — huwag ibigay kahit kanino."* · *"Sinadya mo ba?"* |
| Colors (identity board — Charles confirms exact hex) | Deep violet · purple · amber · lavender · near-black.**Purple = safe / brand**, **amber = yellow "Intended?"**, **red only for the high warning** |
| Backgrounds | The purple blob pattern for splash and onboarding |
| App icon | The pangolin, curled — "your data, tucked in its shell" |

### UI References (Charles)

- **Grammarly Keyboard:** its own keyboard, with a strip above the keys and a brand button that pops up the suggestion. SecondLook borrows the same pattern:
    - the brand button → **the pangolin** on the strip;
    - the suggestion card → **our warning card** (headline + reason + tip + **[Itago]**);
    - the one-tap fix → **Itago** is our "accept the suggestion".
- **Limit to remember:** a keyboard cannot underline or color text inside another app's text box. Show the matched part inside our own strip instead (e.g. `••••• ← Itago`).

---

## 2. Architecture Overview

```
┌────────────────────────────────────────────────────────────────┐
│                         ANDROID PHONE                          │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Flutter App (Settings / Onboarding)                     │  │
│  │  • Enable keyboard guide                                 │  │
│  │  • "Teach me what's private" secret manager (max 5)      │  │
│  │  • Demo / practice screen                                │  │
│  │  • About & privacy info                                  │  │
│  └────────────┬─────────────────────────────────────────────┘  │
│               │ MethodChannel "secondlook/ai"                  │
│  ┌────────────▼─────────────────────────────────────────────┐  │
│  │  Native Kotlin InputMethodService (SecondLookIME)        │  │
│  │  ┌──────────────┐   ┌─────────────────────────────────┐  │  │
│  │  │ Keyboard UI  │   │ Warning Banner UI               │  │  │
│  │  │ (QWERTY)     │   │ RED = high, YELLOW = medium     │  │  │
│  │  │ + Send key   │   │ [Itago] [Review] [Dismiss]      │  │  │
│  │  └──────┬───────┘   └────────────────▲────────────────┘  │  │
│  │         │ detectAsync(text)          │ Warning           │  │
│  │  ┌──────▼────────────────────────────┴────────────────┐  │  │
│  │  │ DetectionEngine (one per app, models load once)    │  │  │
│  │  │ 1. Rules: regex + Luhn, no model                   │  │  │
│  │  │    passwords, PINs, OTP/2FA, API keys, SSH keys,   │  │  │
│  │  │    DB creds, cards + CVV, TIN/SSS/PhilSys          │  │  │
│  │  │    soft: phone, email, bank account                │  │  │
│  │  │ 2. PII model: bert-small-pii (ONNX, 27 MB)         │  │  │
│  │  │ 3. Private notes: MiniLM (ONNX, 22 MB)             │  │  │
│  │  │    + anchors (names / code words in your note)     │  │  │
│  │  │ + Itago: masks the secret before you send          │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Local Storage: SharedPreferences + Android Keystore     │  │
│  │  • Private notes, AES-GCM encrypted                      │  │
│  │  • Settings                                              │  │
│  │  • NO typing history stored                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│        NO INTERNET REQUIRED  -  no INTERNET permission         │
└────────────────────────────────────────────────────────────────┘
```

---

## 3. The Two Layers of Detection

### Layer 1: Rule-Based (Fast, Reliable) — Annie + Klarenz

These fire instantly with zero AI overhead. ✅ Built: `ai/rules/RuleDetector.kt`.

| Pattern | How | Example Caught | Banner |
| --- | --- | --- | --- |
| Passwords in context | "password is / pw: / pass: / password ko ay / password ng wifi namin ay / password ng ML account ko:",`.env` lines like `DB_PASSWORD=` | `Password ng ML account ko: Lodi1234` | 🔴 |
| PINs | MPIN, ATM PIN, "PIN ng ATM ko", "PIN ko ay" + 4-8 digits | `PIN ng ATM ko 1234` | 🔴 |
| OTP / 2FA / authentication codes | 4-8 digits near OTP, verification / authentication code, 2FA, "the code is", and everyday phrasing ("code na**pinadala / dumating / natanggap**", "the code they **sent** me"). Promo, voucher, tracking and order codes never fire; a bare number never fires | `Anak, eto yung code na pinadala sa akin: 482913` | 🔴 |
| API keys & tokens | Known prefixes: Gemini/Google`AIza…`, Hugging Face `hf_…`, OpenAI/Anthropic `sk-…`, AWS `AKIA…`, GitHub `ghp_…`, PayMongo/Stripe `sk_live_…`, Xendit, Slack, Groq, npm, SendGrid, Telegram bot, JWT/Bearer — plus "api key ko ay …" | `GEMINI_API_KEY=AIzaSy…` | 🔴 |
| SSH / private keys | `-----BEGIN … PRIVATE KEY-----` (the whole block). Public keys (`ssh-ed25519 AAAA…`) are ignored — they're meant to be shared | `Eto yung SSH key ng server: -----BEGIN OPENSSH PRIVATE KEY-----` | 🔴 |
| Server & database credentials | `ssh … password:`, `postgres://user:pass@host`, `DATABASE_URL=` | `DATABASE_URL=postgres://admin:Sup3rS3cret!@db.example.com:5432/tindahan` | 🔴 |
| Credit cards + CVV | Luhn checksum + 13-19 digits; 3-4 digits near "CVV/CVC" | `4532 0151 1283 0366` · `Yung CVV sa likod ng card ko 123` | 🔴 |
| Government IDs (PH) | Keyword + number: TIN, SSS, PhilSys, PhilHealth, Pag-IBIG, UMID, passport, driver's license | `TIN ko: 123-456-789-000` | 🔴 |
| Phone numbers (PH) | `09XX XXX XXXX`, `+63 9XX…` | `Send money to 09171234567` | 🟡 Intended? |
| Email addresses | Standard email pattern | `My email is test@example.com` | 🟡 Intended? |
| Bank account numbers | "account no." or a bank name (BDO, BPI, Metrobank, UnionBank, Landbank…) + 9-18 digits | `BDO account no. 0012345678, Juan Dela Cruz` | 🟡 Intended? |

**Why yellow for phone / email / bank account?** Small businesses share these on purpose to get paid. A red alarm on every *"Pa-GCash na lang sa 0917…"* would teach people to ignore SecondLook. Each yellow type can be switched off: `RuleDetector(flagPhoneNumbers, flagEmails, flagBankAccounts)`.

**Never fires on:** questions (*"Lola, ano yung code na dumating sa text mo?"*, *"Paano ko makukuha yung API key?"*), reminders (*"Don't share your OTP with anyone!"*), promos (*"We sent you a promo code 2468"*), role talk (*"Si Ana ang admin, si Ben ang staff"*), ordinary numbers (*"ZIP code is 1630"*, *"I scored 123456 points"*).

### Layer 2: AI on the Phone (The Wow Factor) — Annie

Two pretrained models are bundled in the APK and run by ONNX Runtime **inside the keyboard's process**:

**a) PII model — `bert-small-pii-detection` (27 MB, quantized).** A second net for shapes the rules don't know. Honest note: on our test messages the rules catch almost everything; the model's main extra catch is email.

**b) "Teach Me What's Private" — `all-MiniLM-L6-v2` (22 MB, quantized) + anchors.** ✅ Built: `ai/notes/NoteMatcher.kt`, `NoteAnchors.kt`.

1. User saves a secret note → SecondLook embeds it (whole note + each sentence) and pulls out its **anchors**: capitalized names and quoted code words (*Jakim*, *Taguig*, *'blue heron at midnight'*). Stored encrypted (Android Keystore, AES-GCM). The user never types keywords separately.
2. As the user types → embed the draft → cosine similarity against the saved note.
3. **Warn if** similarity ≥ **0.75** (meaning alone, no names needed), **or** the draft contains the note's anchors (up to 2) **and** similarity ≥ **0.35** → *"This mentions "Jakim", which is part of a note you marked confidential: Jakim location."*

**Examples (measured on the laptop with the bundled model):**

| Saved note | Draft | Similarity | Names in draft | Result |
| --- | --- | --- | --- | --- |
| *"We are raising our prices by 20 percent next month. Do not tell customers yet."* | *"Prices will go up by 20% next month"* | 0.81 | none | ⚠️**warns — meaning only** |
| *same* | *"We have a big sale next month"* | 0.49 | none | ✅ no warning |
| *"Our client is buying the bakery next door. The project is called Sunflower."* | *"Our client is purchasing the bakery beside us"* | 0.89 | none | ⚠️**warns — meaning only** |
| *"Si Jakim ay nakatira sa Taguig"* | *"Let's raid Jakim in Taguig"* | 0.53 | Jakim + Taguig | ⚠️ warns |
| *same* | *"Pumunta tayo kay Jakim sa Taguig bukas"* | 0.71 | Jakim + Taguig | ⚠️ warns |
| *same* | *"Jakim is a great developer"* | 0.26 | Jakim only | ✅ no warning |
| *same* | *"Taguig has nice malls"* | 0.50 | Taguig only | ✅ no warning |

**Why not similarity alone (the first plan's 0.65-0.75)?** At 0.65 the "raid" example would be **missed** (0.53). And *"I'm buying bread at the bakery later"* scored **higher** (0.66) than a looser real paraphrase of the bakery secret (0.56). The names make the AI both more sensitive and less noisy.

**Honest limits (say these if judges ask):** on 5 notes / 40 test drafts it caught **14 of 18** real paraphrases with **3 of 22** false alarms — the false alarms literally contain the protected word (*"Sunflowers look nice in the garden"*). A loose rewording with **no** names is often missed (*"We're increasing our rates next month, keep it quiet for now"* 0.64, *"We should celebrate after the bakery acquisition closes!"* 0.56). MiniLM is English-trained: Taglish works through the names/code words, not through the meaning. A bigger on-device model (NLI or a small LLM) is the upgrade path, not tonight's build.

### Prevention: What Happens After a Warning — Annie (logic) + Klarenz (keyboard)

✅ Built: `ai/Redactor.kt` (Itago), `ai/SafetyTips.kt` (Filipino headline + tip). Exposed as `engine.hide(text, warning)` and `engine.hideAll(text)`.

| Warning type | Itago turns… | …into |
| --- | --- | --- |
| password / OTP / PIN | `Anak, eto yung code na pinadala sa akin: 482913` | `Anak, eto yung code na pinadala sa akin: ••••••` |
| password | `Password ng ML account ko: Lodi1234` | `Password ng ML account ko: ••••••` |
| API key | `GEMINI_API_KEY=AIzaSyA1b2C3d4E5f6G7h8I9j0KlMnOpQrStUvW` | `GEMINI_API_KEY=AIza••••` |
| Database URL | `postgres://admin:Sup3rS3cret!@db.example.com:5432/tindahan` | `postgres://admin:••••••@db.example.com:5432/tindahan` |
| SSH / private key | `-----BEGIN OPENSSH PRIVATE KEY-----…` | `[private key removed]` |
| Card / CVV | `card: 5555555555554444 cvv 123` | `card: ••••••••••••4444 cvv •••` |
| Government ID | `TIN ko: 123-456-789-000` | `TIN ko: •••-•••-•••-000` |
| Bank / phone (yellow) | `BDO account no. 0012345678` · `09171234567` | `BDO account no. ••••••5678` · `•••••••4567` |
| Private note | — | not masked: the user reviews it |

After Itago, SecondLook checks the draft again and never warns about its own mask. **Laptop result: all 77 risky test messages come back clean after Itago.**

> [!IMPORTANT]
The AI models run **inside the keyboard service** using ONNX Runtime Mobile.
No internet, no cloud API, no data leaves the phone. The app declares **no INTERNET permission**.
> 

---

## 4. Tech Stack (Final Decisions)

| Component | Technology | Owner |
| --- | --- | --- |
| App shell & settings | **Flutter + Dart** | Klarenz + Charles |
| Keyboard service | **Kotlin** (InputMethodService) | Klarenz |
| Keyboard UI layout | **XML layouts** | Charles designs, Klarenz implements |
| Rule-based detection + Itago + Filipino headlines/tips | **Kotlin** (regex, Luhn, masking) — ✅ built in `ai-local` | Annie |
| AI models | **all-MiniLM-L6-v2** + **bert-small-pii-detection**, quantized ONNX, via **ONNX Runtime Mobile 1.26.0** — ✅ built | Annie |
| Secret storage | **SharedPreferences + Android Keystore AES-GCM** (`EncryptedNotesStore` + `SecretManager`, ready in `ai-local`) | Klarenz |
| Platform bridge | **MethodChannel** (Flutter ↔ Kotlin) — channel code ready to paste in `ai/bridge/AiMethodChannel.kt` | Klarenz |
| Demo & testing | Real phone testing in Messages app | Jakim |
| Identity | Establish true unique pinoy identity —**the Palawan pangolin** (section 1) | Jakim and Charles |

### Why Not Pure Flutter for the Keyboard?

Flutter **cannot** create an `InputMethodService`. Android requires a native Kotlin/Java service registered in `AndroidManifest.xml`. Flutter handles the app (settings, onboarding). The keyboard itself is native.

### Gradle Dependencies (Annie: add these to `android/app/build.gradle.kts` or `build.gradle`)

```kotlin
// build.gradle.kts (new Flutter projects)
android {
    defaultConfig { minSdk = 26 }
    androidResources { noCompress += "onnx" }   // keep the models uncompressed so they load fast
}
dependencies {
    // ONNX Runtime for on-device AI — the version we built and tested with
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.26.0")
}
```

```groovy
// build.gradle (older Groovy projects)
android {
    defaultConfig { minSdkVersion 26 }
    androidResources { noCompress 'onnx' }
}
dependencies {
    implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.26.0'
}
```

`androidx.security:security-crypto` is **not needed**: private notes are encrypted with Android Keystore AES-GCM directly.

---

## 5. File Structure

```
secondlook/
├── android/
│   └── app/
│       └── src/main/
│           ├── AndroidManifest.xml          ← declares InputMethodService (NO INTERNET permission)
│           ├── kotlin/com/secondlook/
│           │   ├── SecondLookIME.kt         ← THE keyboard service (Klarenz)
│           │   ├── KeyboardView.kt          ← keyboard UI logic (Klarenz)
│           │   ├── DetectionEngine.kt       ← ✅ built: get(), detectAsync(), hide(), Warning (Annie)
│           │   ├── SecretManager.kt         ← save/load/delete user secrets (Klarenz) — ✅ facade ready from Annie
│           │   ├── WarningBanner.kt         ← warning UI overlay (Charles→Klarenz)
│           │   └── ai/                      ← ✅ Annie's AI core (copy from ai-local/kotlin/com/secondlook/ai)
│           │       ├── rules/RuleDetector.kt        ← Luhn, regex, keys, IDs, Taglish patterns (Annie)
│           │       ├── Redactor.kt                  ← "Itago": masks the secret in the draft (Annie)
│           │       ├── SafetyTips.kt                ← Filipino headline + Taglish tip per warning type (Annie)
│           │       ├── model/EmbeddingModel.kt      ← the plan's "EmbeddingEngine": MiniLM loader + inference (Annie)
│           │       ├── model/WordPieceTokenizer.kt  ← tokenizer for both models (Annie)
│           │       ├── model/PiiDetector.kt         ← PII model (Annie)
│           │       ├── notes/                       ← NoteMatcher, NoteAnchors, NotesStore (Annie)
│           │       ├── android/                     ← AssetModels, NoteCrypto (Keystore), EncryptedNotesStore (Annie)
│           │       └── bridge/AiMethodChannel.kt    ← MethodChannel code to paste into MainActivity (Klarenz)
│           ├── res/
│           │   ├── layout/
│           │   │   ├── keyboard_layout.xml  ← QWERTY layout + pangolin button on the top strip (Charles designs)
│           │   │   └── warning_banner.xml   ← warning bar: red + yellow styles, Itago/Review/Dismiss (Charles)
│           │   ├── xml/
│           │   │   └── method.xml           ← IME configuration
│           │   ├── values/
│           │   │   ├── colors.xml           ← brand colors: pangolin palette (Charles)
│           │   │   └── strings.xml          ← warning text + tips, one per warning type (Jakim)
│           │   └── drawable/               ← pangolin icons (calm / peeking / curled), backgrounds (Charles)
│           └── assets/
│               └── models/                  ← ✅ embedded AI models (Annie) — copy from ai-local/assets/models
│                   ├── embedding/           ← model_quantized.onnx (22 MB) + vocab.txt + config.json
│                   └── pii/                 ← model_quantized.onnx (27 MB) + vocab.txt + config.json
├── lib/                                     ← Flutter app code
│   ├── main.dart                            ← app entry point
│   ├── screens/
│   │   ├── onboarding_screen.dart           ← setup guide (Charles)
│   │   ├── secrets_screen.dart              ← manage private notes (Charles)
│   │   ├── practice_screen.dart             ← try the keyboard + Itago (Charles)
│   │   └── about_screen.dart                ← privacy info + why a pangolin (Jakim)
│   ├── services/
│   │   └── platform_bridge.dart             ← MethodChannel to native (Klarenz)
│   └── widgets/
│       └── secret_card.dart                 ← UI for each saved secret (Charles)
├── pubspec.yaml
└── README.md                                ← Jakim writes for submission (incl. how judges rebuild the app)
```

**Annie's workspace** (source of truth for the AI code, already on the laptop):

```
ai-local/
├── kotlin/                 → copy into android/app/src/main/kotlin/
├── assets/models/          → copy into android/app/src/main/assets/models/
├── test-data/samples.json  ← 128 synthetic messages + 5 private-note sets (Jakim's test cases live here)
├── jvm-check/              ← runs the real Kotlin detector + models + Itago on the laptop: gradlew run
├── web-demo/               ← same models in a browser, fully local: npm start → <http://127.0.0.1:5173> (+ /eval.html)
├── test-app/               ← standalone Android test APK (type box + warnings + Itago, no keyboard yet)
└── docs/                   ← contract.md, integration.md, architecture-reconciled.md
```

---

## 6. How the Keyboard Works (Step by Step)

### When user types:

```
1. User selects SecondLook as keyboard in Android Settings
2. User opens any messaging app (Messages, Messenger, Viber, Discord, etc.)
3. SecondLook keyboard appears instead of Gboard
4. Each keystroke → SecondLookIME receives it
5. After 500ms pause in typing → DetectionEngine.detectAsync(text)  (background thread)
6. DetectionEngine runs, cheapest first, first hit wins:
   a. Rules (regex + Luhn)  →  password | otp | api_key | credit_card | gov_id | phone | email | bank_account
   b. PII model (ONNX)      →  extra catch (mostly email / unusual shapes)
   c. Private notes         →  private_match  (meaning, or names + meaning)
7. If detection found → WarningBanner slides up above keyboard (the pangolin curls up)
8. User sees (Filipino headline first):
   🔴 "Teka! OTP o PIN ito — huwag ibigay kahit kanino."
      This looks like a verification code or PIN. Sharing it could let someone into your account.
      💡 "Walang totoong bangko, GCash o Maya na hihingi ng OTP o PIN mo. Kapag may humingi, scam iyan."
   🟡 "Account number ito. Sinadya mo ba?"
9. User taps: [Itago] (hide it — the secret becomes •••• in the draft) · [Review / Edit Message] · [Dismiss Warning]
```

### When user presses keyboard's Enter/Send:

```
1. SecondLookIME's OWN Enter key checks the field's action (EditorInfo: SEND / GO / DONE)
2. Runs one final check on full text
3. If a red warning is active → holds the send and asks: [Itago] [Review] [Send anyway]
4. Itago → the secret is masked, then the user taps Send again
5. Send anyway → keyboard sends the action to the app (sendDefaultEditorAction)
6. Review → stays in editing mode, the draft stays editable
```

### Optional: "Ingat Mode" for lolo, lola and kids (stretch — only after Checkpoint 4)

A setting in the Flutter app: Filipino-only banner text, bigger banner, and on a red warning **no one-tap "Send anyway"** — the user must hold the Send key for 2 seconds. Keyboard (Klarenz), screen + toggle (Charles), wording (Jakim). No AI changes needed.

> [!WARNING]
We **cannot** intercept the messaging app's own Send button (the blue arrow in Messenger, etc.).
Our interception only works for the keyboard's Enter key. The warning + Itago during typing is the main protection.
The AI models take a moment to load when the keyboard first opens (laptop: ~1.3 s; phone not measured yet). Until then the rules still run, so typing never waits.
> 

---

## 7. Strict Role Assignments & Deliverables

### 🔵 Klarenz — Keyboard Lead (You)

**You own:** The keyboard types, the app runs, the bridge works.

| Time | Task | Done When |
| --- | --- | --- |
| 7:30–9:00 PM | Create Flutter project, add Kotlin InputMethodService skeleton, register in manifest | Keyboard appears in Settings → Language & Input |
| 9:00–10:30 PM | Implement QWERTY key layout + basic typing into any app | Can type "hello world" in Messages app |
| 10:30–11:30 PM | Add WarningBanner view above keyboard (red + yellow, headline + tip, [Itago] [Review] [Dismiss]), triggered by DetectionEngine | Banner shows/hides on command |
| 11:30 PM–12:30 AM | Integrate Annie's DetectionEngine (already built:`DetectionEngine.get(this)` + `detectAsync` + `hide`) into the keyboard service | Typing "Anak, eto yung code na pinadala sa akin: 482913" warns; Itago turns the code into`••••••` |
| 12:30–2:00 AM | Build MethodChannel bridge: Flutter app ↔ native SecretManager (paste`ai/bridge/AiMethodChannel.kt`) | Secrets saved in Flutter app are readable by keyboard |
| 2:00–3:00 AM | Hold the keyboard's own Enter/Send key, add confirmation dialog ([Itago] [Review] [Send anyway]) | Enter key pauses for confirmation when a red warning is active |
| 3:00–5:00 AM | Integration testing with Annie + Jakim, bug fixes (Ingat Mode only if all is stable) | End-to-end flow works (Checkpoint 5 at 3:00 AM) |
| 5:00–7:00 AM | Polish, edge cases, offline verification, release APK | Demo-ready release APK |
| 7:00–8:00 AM | 🔒 Freeze: final release APK installed on the Tecno + pushed to the repo | APK in Jakim's hands**by 8:00 AM** |

### 🟡 ANNIE — AI Lead

**You own:** The AI detects sensitive info. Everything local. No internet.

| Time | Task | Done When | Status (9:00 PM) |
| --- | --- | --- | --- |
| 7:30–8:30 PM | Download all-MiniLM-L6-v2 ONNX model, verify size fits phone | `.onnx` file ready in `assets/models/` | ✅ Done — quantized 22 MB, plus PII model 27 MB |
| 8:30–10:00 PM | Write`RuleDetector.kt`: Luhn check, password-keyword regex, OTP pattern, phone regex (+ PINs, everyday Taglish phrasing, API keys, SSH keys, DB creds, IDs, bank) | Unit tests pass for 10 synthetic examples | ✅ Done — 128/128 synthetic messages on the laptop |
| 10:00 PM–12:00 AM | Write`EmbeddingEngine.kt`: load ONNX model, tokenize text, run inference, get embedding vector | Can get a 384-dim vector from any input string | ✅ Done on laptop (`EmbeddingModel.kt` + `WordPieceTokenizer.kt`) |
| 12:00–1:00 AM | Write cosine similarity function + threshold logic | Jakim/Taguig cases warn, "Jakim is a great developer" stays quiet, no-name price example warns | ✅ Done — meaning + anchors |
| 1:00–2:00 AM | Write`DetectionEngine.kt`: orchestrates rules + models, returns warnings | Single`detect(text): List<Warning>` API ready | ✅ Done — plus`detectAsync`, Itago (77/77 clean), Filipino headlines, tips, `secrets` |
| 2:00–3:00 AM | Integrate with Klarenz's keyboard, test on real phone | Warnings fire correctly during typing | ⏳ Next — needs the keyboard + Tecno |
| 3:00–5:00 AM | Tune threshold, fix false positives/negatives with Jakim | Acceptable accuracy on test cases | ⏳ Laptop numbers done; phone pending |
| 5:00–7:00 AM | Final model performance check, offline verification | Works with airplane mode ON | ⏳ |
| 7:00–8:00 AM | 🔒 Freeze: verify the release APK (no INTERNET permission), hand the model/tool disclosures to Jakim | Disclosures ready**by 8:00 AM** | ⏳ |

> [!TIP]
**Annie's Model Strategy:**
> 
> - **Do NOT train from scratch.** Use pretrained models as-is: all-MiniLM-L6-v2 + bert-small-pii-detection.
> - **No export needed.** The quantized ONNX files (+ `vocab.txt`) are already in `ai-local/assets/models/` (`scripts/download-models.ps1` re-downloads them). Smaller than an `optimum` export (22 MB vs ~90 MB).
> - **MiniLM is English-trained.** Filipino/Taglish does **not** get meaning-matching from it; it works through the anchors (names and code words). Don't claim Taglish semantic understanding in the pitch.
> - **Ahead of schedule:** until Klarenz's keyboard types, help him on the critical path and run the test APK on the Tecno (load time, ms per check, airplane mode, Itago).

### 🟢 CHARLES — UI/Design Lead

**You own:** It looks good and users understand it.

| Time | Task | Done When |
| --- | --- | --- |
| 7:30–8:30 PM | Design keyboard layout in XML (QWERTY, keys, spacing, colors, pangolin button on the top strip) | `keyboard_layout.xml` ready for Klarenz |
| 8:30–9:30 PM | Design warning banner in XML (🔴 red = high, 🟡 yellow = medium "Intended?", Filipino headline, reason, tip, buttons [Itago] [Review] [Dismiss]; big readable text for lolo/lola) | `warning_banner.xml` ready for Klarenz |
| 9:30–11:00 PM | Build Flutter onboarding screen (3 steps: install → enable → try) | Users understand how to enable the keyboard |
| 11:00 PM–1:00 AM | Build Flutter secrets screen (add/delete private notes, max 5, show the label only) | Can add "Si Jakim ay nakatira sa Taguig" as a secret |
| 1:00–2:00 AM | Build Flutter practice screen (type here to test warnings, with an Itago button) | Users can try the keyboard safely |
| 2:00–3:00 AM | App icon (curled pangolin), splash screen, pangolin color scheme | App looks professional |
| 3:00–5:00 AM | Help Jakim with demo video + pitch deck design | Slides and visuals ready |
| 5:00–7:00 AM | Final UI polish (pangolin states if time), screenshots for submission | Submission-ready screenshots |
| 7:00–8:00 AM | 🔒 Freeze: X / LinkedIn post graphic, final screenshots | Assets ready**by 8:00 AM** |

> [!NOTE]
**Charles:.** We're using Flutter. Design the XML layouts on paper or in Figma, then Klarenz converts them. For Flutter screens, use standard Material widgets — no fancy packages.
> 

### 🔴 JAKIM — Test & Identity Lead

**You own:** It works on a real phone, the story is clear, the submission is complete.

| Time | Task | Done When |
| --- | --- | --- |
| 7:30–9:00 PM | Write all warning message strings + tips (English + Filipino, caring "ingat" voice), one per warning type:`password`, `otp`, `api_key`, `credit_card`, `gov_id`, `phone`, `email`, `bank_account`, `private_match` (built-in Filipino headlines are the default) | `strings.xml` complete with 10+ warning messages |
| 7:30–9:00 PM | Write [README.md](http://readme.md/) for GitHub submission, including**how judges can rebuild the app** | Project description, setup instructions, team info |
| 9:00–10:00 PM | Run the synthetic test cases in section 8 (already in`ai-local/test-data/samples.json`) and add 5 of your own Taglish ones | Test cases documented and ready |
| 10:00–11:00 PM | Write the pitch:**5 minutes incl. live demo** (section 13) + answers for the **3-minute Q&A** | Script practiced once |
| 11:00 PM–1:00 AM | Write the Cerebral Valley text (drafts in section 14) | All text fields ready to paste |
| 1:00–3:00 AM | Test every build on the Tecno phone, log bugs, verify fixes | Bug log with status |
| 3:00–5:00 AM | Record the**~1-minute** demo video (screen recording on phone) | Video file ready |
| 5:00–7:00 AM | Final end-to-end test, backup video recording | Two video copies |
| 7:00–8:00 AM | Finish README + Cerebral Valley text, upload the video, prepare the X/LinkedIn post (tag Devin / Cognition,#AppBuildersPH) | Everything ready**by 8:00 AM** |
| 8:00–9:00 AM | Final check (one submission only, no edits), then submit: Cerebral Valley form, public GitHub, video, post | ✅ All submitted**before 9:00 AM** |

---

## 8. Jakim's Test Cases (Use These Exactly)

All synthetic: the keys and IDs are invented look-alikes (`AKIAIOSFODNN7EXAMPLE` is AWS's published docs example) and the card numbers are public test numbers. Every row is in `ai-local/test-data/samples.json` and passes on the laptop (Kotlin + browser). **The phone run is Jakim's job (Checkpoint 4).** Groups **I, A and B are the demo-critical ones** (everyday users, passwords, OTP).

🔴 = red warning (high) · 🟡 = yellow "Intended?" (medium) · ✅ = should warn · ❌ = should NOT warn

### A. Passwords & logins

| # | Input Text | Should Warn? | Expected Warning Type |
| --- | --- | --- | --- |
| 1 | `My password is mango72.` | ✅ 🔴 | Password |
| 2 | `pw: hunter2` | ✅ 🔴 | Password |
| 3 | `Ang password ng wifi namin ay kape12345` | ✅ 🔴 | Password |
| 4 | `Admin login namin - username: admin, password: Admin@2026` | ✅ 🔴 | Password (admin login) |
| 5 | `DB_PASSWORD=Tindahan2026!` | ✅ 🔴 | Password (database) |
| 6 | `ssh root@203.0.113.10 password: Server#2026` | ✅ 🔴 | Password (server) |

### B. OTP, 2FA & authentication codes

| # | Input Text | Should Warn? | Expected Warning Type |
| --- | --- | --- | --- |
| 7 | `The code is 482916` | ✅ 🔴 | Verification code |
| 8 | `OTP mo ay 5832, ibigay mo kay Juan` | ✅ 🔴 | Verification code |
| 9 | `Eto po yung authentication code: 739204` | ✅ 🔴 | Verification code |
| 10 | `2FA code ng Facebook ko: 104857` | ✅ 🔴 | Verification code |
| 11 | `GCash MPIN ko ay 4821` | ✅ 🔴 | PIN |

### C. API keys, tokens, SSH & database

| # | Input Text | Should Warn? | Expected Warning Type |
| --- | --- | --- | --- |
| 12 | `GEMINI_API_KEY=AIzaSyA1b2C3d4E5f6G7h8I9j0KlMnOpQrStUvW` | ✅ 🔴 | API key (Gemini) |
| 13 | `Hugging Face token ko hf_aB3dE6gH9jK2mN5pQ8sT1vW4yZ7cF0hJ3l, pang-download lang ng model` | ✅ 🔴 | API key (Hugging Face) |
| 14 | `OPENAI_API_KEY=sk-proj-Tz9QvB3nLm8XcR2yWk5dHs7JfG1aPe4U` | ✅ 🔴 | API key (OpenAI) |
| 15 | `eto yung secret key ng PayMongo natin: sk_live_Ab3dEf6HiJk9LmNoPq2RsTu5` | ✅ 🔴 | API key (payments) |
| 16 | `Eto yung SSH key ng server: -----BEGIN OPENSSH PRIVATE KEY-----` | ✅ 🔴 | Private key (SSH) |
| 17 | `DATABASE_URL=postgres://admin:Sup3rS3cret!@db.example.com:5432/tindahan` | ✅ 🔴 | Database credentials |
| 18 | `Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJlLWRlbW8` | ✅ 🔴 | Auth token |
| 19 | `AWS access key ko AKIAIOSFODNN7EXAMPLE` | ✅ 🔴 | API key (AWS) |

### D. Money & IDs

| # | Input Text | Should Warn? | Expected Warning Type |
| --- | --- | --- | --- |
| 20 | `My card number is 4111111111111111` | ✅ 🔴 | Credit card |
| 21 | `4532 0151 1283 0366` | ✅ 🔴 | Credit card (replaces old case 3) |
| 22 | `Yung CVV sa likod ng card ko 123` | ✅ 🔴 | Card security code |
| 23 | `TIN ko: 123-456-789-000` | ✅ 🔴 | Government ID |
| 24 | `Yung SSS number ko 34-1234567-8` | ✅ 🔴 | Government ID |
| 25 | `PhilSys number ko ay 1234-5678-9012-3456` | ✅ 🔴 | Government ID |
| 26 | `BDO account no. 0012345678, Juan Dela Cruz` | ✅ 🟡 | Bank account ("Intended?") |
| 27 | `Send money to 09171234567` | ✅ 🟡 | Phone number ("Intended?") |
| 28 | `My email is test@example.com` | ✅ 🟡 | Email ("Intended?") |

### E. Must NOT warn (normal messages & near-misses)

| # | Input Text | Should Warn? | Why |
| --- | --- | --- | --- |
| 29 | `Hello, how are you today?` | ❌ | — |
| 30 | `Let's meet at 3pm tomorrow` | ❌ | — |
| 31 | `The price is 4999 pesos` | ❌ | — |
| 32 | `I scored 123456 points in the game` | ❌ | A bare number is not a code |
| 33 | `4532 1234 5678 9012` | ❌ | Old case 3: fails the card checksum, so it is not a real card number |
| 34 | `Paano ko makukuha yung API key sa Google AI Studio?` | ❌ | Asking about a key, not sharing one |
| 35 | `Nakalimutan ko yung password ko, pa-reset naman` | ❌ | No password in the message |
| 36 | `Don't share your OTP with anyone!` | ❌ | No code in the message |
| 37 | `Eto yung public key ko: ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIExampleOnlyNotARealKey ana-laptop` | ❌ | SSH public keys are meant to be shared |
| 38 | `Si Ana ang admin, si Ben ang staff, ako ang owner` | ❌ | Role talk, no secret |
| 39 | `Dalhin mo yung SSS at TIN mo bukas sa HR` | ❌ | No ID number |
| 40 | `ZIP code is 1630` | ❌ | ZIP code, not a verification code |
| 41 | `Magkano ang bank transfer fee?` | ❌ | No account number |

### F. Teach me what's private — names + meaning (saved secret: *"Si Jakim ay nakatira sa Taguig"*)

| # | Input Text | Should Warn? | Expected Warning Type |
| --- | --- | --- | --- |
| 42 | `Let's raid Jakim in Taguig` | ✅ 🔴 | Matches saved secret |
| 43 | `Jakim's address is in Taguig City` | ✅ 🔴 | Matches saved secret |
| 44 | `Pumunta tayo kay Jakim sa Taguig bukas` | ✅ 🔴 | Matches saved secret |
| 45 | `Jakim is a great developer` | ❌ | Only one name, different meaning |
| 46 | `Taguig has nice malls` | ❌ | Only one name, different meaning |

### G. Teach me what's private — meaning only, NO names

| # | Saved secret | Input Text | Should Warn? |
| --- | --- | --- | --- |
| 47 | *"We are raising our prices by 20 percent next month. Do not tell customers yet."* | `Prices will go up by 20% next month` | ✅ 🔴 (meaning, similarity 0.81) |
| 48 | *same* | `We have a big sale next month` | ❌ |
| 49 | *same* | `Prices at the grocery went up again` | ❌ |
| 50 | *"Our client is buying the bakery next door. The project is called Sunflower."* | `Our client is purchasing the bakery beside us` | ✅ 🔴 (meaning, similarity 0.89) |

**Known misses (don't demo them):** a loose rewording with no names, e.g. *"We're increasing our rates next month, keep it quiet for now"* (0.64) or *"We should celebrate after the bakery acquisition closes!"* (0.56).

### H. Itago (one tap hide) — tap Itago on the warning, then check the text box

| # | Input Text | After Itago | Then |
| --- | --- | --- | --- |
| 51 | `My password is mango72.` | `My password is ••••••.` | ❌ no warning |
| 52 | `GEMINI_API_KEY=AIzaSyA1b2C3d4E5f6G7h8I9j0KlMnOpQrStUvW` | `GEMINI_API_KEY=AIza••••` | ❌ no warning |
| 53 | `Eto po yung authentication code: 739204` | `Eto po yung authentication code: ••••••` | ❌ no warning |
| 54 | `DATABASE_URL=postgres://admin:Sup3rS3cret!@db.example.com:5432/tindahan` | `DATABASE_URL=postgres://admin:••••••@db.example.com:5432/tindahan` | ❌ no warning |
| 55 | `Eto yung SSH key ng server: -----BEGIN OPENSSH PRIVATE KEY-----` | `Eto yung SSH key ng server: [private key removed]` | ❌ no warning |
| 56 | `card: 5555555555554444 cvv 123` | `card: ••••••••••••4444 cvv •••` (tap twice: card, then CVV) | ❌ no warning |
| 57 | `TIN ko: 123-456-789-000` | `TIN ko: •••-•••-•••-000` | ❌ no warning |
| 58 | `BDO account no. 0012345678, Juan Dela Cruz` | `BDO account no. ••••••5678, Juan Dela Cruz` | ❌ no warning |

### I. Everyday Filipinos — lola, kids, family (demo-critical)

| # | Input Text | Should Warn? | Expected Warning Type |
| --- | --- | --- | --- |
| 59 | `Anak, eto yung code na pinadala sa akin: 482913` | ✅ 🔴 | Verification code (lola) |
| 60 | `May dumating na code sa akin, 739204 daw` | ✅ 🔴 | Verification code |
| 61 | `Ma'am eto po yung OTP: 902114, para ma-claim ko na yung premyo` | ✅ 🔴 | Verification code ("premyo" scam) |
| 62 | `the code they sent me is 551203` | ✅ 🔴 | Verification code |
| 63 | `PIN ng ATM ko 1234, pakikuha mo na lang yung pera` | ✅ 🔴 | PIN |
| 64 | `Password ng ML account ko: Lodi1234` | ✅ 🔴 | Password (kid, game) |
| 65 | `Password ng Roblox ko ay Robux2026` | ✅ 🔴 | Password (kid, game) |
| 66 | `pass ng FB ko: Kitty123` | ✅ 🔴 | Password (Facebook) |
| 67 | `Lola, ano yung code na dumating sa text mo?` | ❌ | A question, no code |
| 68 | `We sent you a promo code 2468 for 20% off` | ❌ | Promo code |
| 69 | `Ang pass ng bus ay libre ngayon` | ❌ | Bus pass, not a password |
| 70 | `Nag-text yung bangko, huwag daw ibigay ang OTP` | ❌ | A reminder, no code |
| 71 | `Na-receive ko na yung padala, salamat!` | ❌ | No code |
| 72 | `Tracking code na natanggap ko: 7421339855` | ❌ | Tracking code |
| 73 | `Kumain ka na ba? Ingat pauwi!` | ❌ | Normal message |
- Cases 42-46 need the secret *"Si Jakim ay nakatira sa Taguig"*; cases 47-50 need the two notes shown.

---

## 9. The AndroidManifest.xml Setup (Klarenz: Do This First)

> [!IMPORTANT]
`flutter create --org com.secondlook secondlook` puts the app in package **`com.secondlook.secondlook`** (check `namespace` in `android/app/build.gradle(.kts)`). The keyboard and AI code live in `com.secondlook`, so use **fully-qualified names** below, and in `SecondLookIME.kt` add `import com.secondlook.secondlook.R`.
Do **not** add `<uses-permission android:name="android.permission.INTERNET"/>` to the main manifest.
> 

```xml
<!-- Add inside <application> tag -->
<service
    android:name="com.secondlook.SecondLookIME"
    android:label="SecondLook Keyboard"
    android:permission="android.permission.BIND_INPUT_METHOD"
    android:exported="true">
    <intent-filter>
        <action android:name="android.view.InputMethod" />
    </intent-filter>
    <meta-data
        android:name="android.view.im"
        android:resource="@xml/method" />
</service>
```

**Create `res/xml/method.xml`** (fixed: the namespace must be a plain URL, without `< >`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<input-method xmlns:android="<http://schemas.android.com/apk/res/android>"
    android:settingsActivity="com.secondlook.secondlook.MainActivity"
    android:icon="@mipmap/ic_launcher">
    <subtype
        android:label="English"
        android:imeSubtypeLocale="en_US"
        android:imeSubtypeMode="keyboard" />
</input-method>
```

---

## 10. Critical Code Skeletons

### SecondLookIME.kt (Klarenz's starting point)

```kotlin
package com.secondlook

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.secondlook.secondlook.R   // the Flutter app's namespace — check build.gradle

class SecondLookIME : InputMethodService() {

    private lateinit var engine: DetectionEngine
    private lateinit var keyboardView: View
    private lateinit var warningBanner: View
    private var checkedText = ""                              // the text the current warning is about
    private var warning: DetectionEngine.Warning? = null

    override fun onCreate() {
        super.onCreate()
        engine = DetectionEngine.get(this)   // returns immediately; AI models load in the background
    }

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_layout, null)
        keyboardView = root.findViewById(R.id.keyboard_container)
        warningBanner = root.findViewById(R.id.warning_banner)
        warningBanner.visibility = View.GONE
        // Banner buttons (ids from Charles's warning_banner.xml)
        root.findViewById<View>(R.id.hide_button).setOnClickListener { onHideTapped() }
        root.findViewById<View>(R.id.dismiss_button).setOnClickListener { hideWarning() }
        setupKeys(root)
        return root
    }

    // Called when user types — check after pause
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = Runnable { checkCurrentText() }

    fun onKeyPressed(char: Char) {
        val ic = currentInputConnection ?: return
        ic.commitText(char.toString(), 1)

        // Debounce: check 500ms after last keystroke
        handler.removeCallbacks(checkRunnable)
        handler.postDelayed(checkRunnable, 500)
    }

    /** The draft around the cursor. Use the SAME window for checking and for Itago so the positions match. */
    private fun textAround(): Pair<String, String> {
        val ic = currentInputConnection ?: return "" to ""
        val before = ic.getTextBeforeCursor(2000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(2000, 0)?.toString() ?: ""
        return before to after
    }

    private fun checkCurrentText() {
        val (before, after) = textAround()
        val text = before + after
        engine.detectAsync(text) { warnings ->   // runs off the main thread, result comes back on it
            checkedText = text
            warning = warnings.firstOrNull()
            warning?.let { showWarning(it) } ?: hideWarning()
        }
    }

    /** "Itago": replace the secret inside the app's text box with the masked version (••••••, AIza••••). */
    private fun onHideTapped() {
        val ic = currentInputConnection ?: return
        val w = warning ?: return
        val (before, after) = textAround()
        if (before + after != checkedText) { checkCurrentText(); return }   // text changed: check again first
        val masked = engine.hide(checkedText, w) ?: return                  // null for private notes
        ic.beginBatchEdit()
        ic.deleteSurroundingText(before.length, after.length)
        ic.commitText(masked, 1)
        ic.endBatchEdit()
        checkCurrentText()                                                  // next secret, if any
    }

    // The keyboard's OWN Enter/Send key. InputMethodService has no onEditorAction() to override;
    // this is the only Send we can pause. Verify on the phone.
    fun onEnterPressed() {
        val ic = currentInputConnection ?: return
        val options = currentInputEditorInfo.imeOptions
        val action = options and EditorInfo.IME_MASK_ACTION
        val isSend = (action == EditorInfo.IME_ACTION_SEND || action == EditorInfo.IME_ACTION_GO ||
            action == EditorInfo.IME_ACTION_DONE) && (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
        if (!isSend) { ic.commitText("\n", 1); return }

        val (before, after) = textAround()
        engine.detectAsync(before + after) { warnings ->
            val red = warnings.firstOrNull { it.severity == "high" }
            if (red != null) showConfirmDialog(red) else sendDefaultEditorAction(true)
        }
    }

    private fun showWarning(warning: DetectionEngine.Warning) {
        warningBanner.visibility = View.VISIBLE
        warningBanner.findViewById<TextView>(R.id.headline_text).text = warning.headline   // Filipino first
        warningBanner.findViewById<TextView>(R.id.warning_text).text = warning.message
        warningBanner.findViewById<TextView>(R.id.tip_text).text = warning.tip
        warningBanner.findViewById<View>(R.id.hide_button).visibility = if (warning.canHide) View.VISIBLE else View.GONE
        // warning.severity: "high" → red style + curled pangolin, "medium" → yellow "Intended?" + peeking pangolin (Charles)
        // warning.type → pick the English/Filipino string from strings.xml (Jakim) if you want to override the defaults
    }

    private fun hideWarning() {
        warningBanner.visibility = View.GONE
    }

    private fun showConfirmDialog(warning: DetectionEngine.Warning) {
        // Show a confirmation popup above the keyboard: [Itago] [Review] [Send anyway]
        // Itago       → onHideTapped(), user taps Send again
        // Review      → do nothing, stay editing (draft stays editable)
        // Send anyway → sendDefaultEditorAction(true)   (Ingat Mode: require holding Send for 2 s instead)
    }
}
```

### DetectionEngine.kt (Annie) — ✅ built

Copy it from `ai-local/kotlin/com/secondlook/DetectionEngine.kt`. Public API:

```kotlin
// signatures only — the real file is in ai-local
class DetectionEngine {
    data class Warning(
        val type: String,        // password | otp | api_key | credit_card | gov_id | phone | email | bank_account | pii | private_match
        val message: String,     // English reason; strings.xml may override by type
        val severity: String,    // "high" (red) | "medium" (yellow, "Intended?")
        val matchedText: String, // the part that triggered
        val start: Int,          // where it is in the checked text
        val end: Int,
        val tip: String,         // one line of Taglish advice
        val canHide: Boolean,    // show the [Itago] button (false for private-note matches)
        val headline: String     // Filipino-first title, e.g. "Teka! OTP o PIN ito — huwag ibigay kahit kanino."
    )

    val secrets: SecretManager                                         // save / list / delete private notes (encrypted)
    val isReady: Boolean                                               // AI models loaded? (rules work before this)
    fun detect(text: String): List<Warning>                            // blocking: call off the main thread
    fun detectAsync(text: String, onResult: (List<Warning>) -> Unit)   // background → result on main thread
    fun hide(text: String, warning: Warning): String?                  // "Itago": text with the secret masked
    fun hideAll(text: String): String                                  // mask every secret (blocking)
    fun awaitReady(timeoutMs: Long = 20_000): Boolean
    fun setNoteSimilarityThreshold(value: Float)                       // default 0.75 (no-name case)

    companion object { fun get(context: Context): DetectionEngine }    // ONE per app process; models load once
}
```

```kotlin
// Private notes, from the Flutter bridge or the practice screen (background thread)
val engine = DetectionEngine.get(context)
engine.secrets.save("Si Jakim ay nakatira sa Taguig", "Jakim location")   // max 5 notes
engine.secrets.list()          // labels only — the secret text never leaves the encrypted store
engine.secrets.delete(id)
engine.secrets.deleteAll()
```

### RuleDetector.kt (Annie) — ✅ built

`ai-local/kotlin/com/secondlook/ai/rules/RuleDetector.kt` covers every row of the Layer 1 table (section 3).

```kotlin
RuleDetector(flagPhoneNumbers = true, flagEmails = true, flagBankAccounts = true)   // switch the yellow types on/off
```

To add a pattern: add a `Regex` + a `RiskResult(kind, severity)`, add **two** samples to `test-data/samples.json` (one that should warn, one near-miss that should not), then run `jvm-check` and the browser `/eval.html`. Keep `web-demo/public/rules.js` in sync.

---

## 11. Hourly Timeline (Everyone Follow This)

### 🌙 TONIGHT — October 9

| Time | Klarenz | Annie | Charles | Jakim |
| --- | --- | --- | --- | --- |
| **7:30** | `flutter create secondlook` + add Kotlin skeleton | ✅ Download MiniLM ONNX model | Design keyboard layout (paper/Figma) | Write all warning strings |
| **8:00** | Register IME in manifest, bare keyboard appears | ✅ Set up ONNX Runtime in gradle (test app; copy to Flutter app) | Design warning banner (red + yellow + Itago) | Write [README.md](http://readme.md/) |
| **8:30** | ⭐**CHECKPOINT: Keyboard shows in Settings** | ✅`RuleDetector.kt` | Send keyboard XML to Klarenz | Create test case list (section 8 ready) |
| **9:00** | Wire up QWERTY key presses | ✅ Luhn + password regex (+ PINs, everyday Taglish, API keys, SSH, DB, IDs) | Start Flutter onboarding | Write pitch script draft (5 min) |
| **9:30** | Can type in Messages app | ✅ OTP + phone regex (+ "code na pinadala", 2FA, bank account, CVV) | Onboarding screen coding | Test keyboard on phone |
| **10:00** | ⭐**CHECKPOINT: Can type "hello" in Messages** | ✅`EmbeddingEngine` (`EmbeddingModel.kt`) on laptop | Secrets management screen | First bug report |
| **10:30** | Add warning banner view | ⏳ ONNX model loads on phone (test APK on the Tecno) | Practice screen | Continue testing |
| **11:00** | Warning shows/hides on trigger | ✅ Can get embedding vector (laptop) | UI polish | Test rule detections |
| **11:30** | ⭐**CHECKPOINT: Warning banner works** | ✅ Cosine similarity + anchors + Itago + headlines work | Continue UI | Verify all rule tests |
| **12:00** | Integrate DetectionEngine | ✅`DetectionEngine.kt` done | App icon (pangolin) + splash | Test integration |

### 🌅 OVERNIGHT — October 10 (one hour earlier than the first plan)

| Time | Klarenz | Annie | Charles | Jakim |
| --- | --- | --- | --- | --- |
| **12:30** | MethodChannel for secrets | Help Klarenz integrate | Color scheme polish (pangolin palette) | Deep testing |
| **1:00** | ⭐**CHECKPOINT: Rule warnings fire in real app** | Tune note thresholds on the phone | Help with any UI bugs | Log all bugs |
| **2:00** | Enter/Send key hold + Itago + confirmation dialog | Secret embedding pipeline on the phone | Demo video planning | Verify bug fixes |
| **3:00** | ⭐**CHECKPOINT: Full flow works offline** | Final threshold tuning | Pitch deck slides (few — demo first) | Record demo v1 (~1 min) |
| **4:00** | Edge case fixes (Ingat Mode only if stable) | Edge case fixes, Taglish (anchors) check | Slides done; pangolin states if time | Practice the 5-min pitch + Q&A |
| **5:00** | Final integration round | Verify model accuracy (airplane mode) | Visual polish | Record demo v2 (backup) |
| **6:00** | Bug fixes only + release APK | Verify release APK (no INTERNET permission) | Screenshots | README + Cerebral Valley text final |
| **7:00** | 🔒**FEATURE FREEZE** — final release APK | 🔒**FEATURE FREEZE** — disclosures to Jakim | 🔒**FEATURE FREEZE** — post graphic | Upload video, collect all links |
| **8:00** | ✅**ALL DONE** — help submit | ✅**ALL DONE** — help submit | ✅**ALL DONE** — X/LinkedIn post | 📤 Final check, then submit (one shot) |
| **8:45** | ✅**ALL SUBMITTED (before 9:00 AM)** | ✅ | ✅ | ✅ |
| **11:00** | 🎤 At Cyberzone, SM Makati — rehearsed (registration 12:00 PM, AV check 12:15 PM, finalists 1:00 PM, pitching from 1:40 PM) | 🎤 | 🎤 | 🎤 |

---

## 12. Critical Checkpoints (Stop & Verify)

### 🚨 Checkpoint 1: 8:30 PM — Keyboard Appears

**Test:** Go to Settings → System → Languages & Input → On-screen keyboard → SecondLook is listed.
**If FAIL:** Everyone stops and helps Klarenz fix the manifest registration (check the fully-qualified names in section 9). This blocks everything.

### 🚨 Checkpoint 2: 10:00 PM — Can Type

**Test:** Open Messages app. Select SecondLook keyboard. Type "hello world". Text appears in the message box.
**If FAIL:** Simplify keyboard layout. Use absolute minimum keys. This blocks demo.

### 🚨 Checkpoint 3: 11:30 PM — Warning Banner

**Test:** Type "Anak, eto yung code na pinadala sa akin: 482913" → **red** banner with *"Teka! OTP o PIN ito…"* → tap **Itago** → the code becomes `••••••`. Type "Send money to 09171234567" → **yellow** banner.
**If FAIL:** Check DetectionEngine integration. May need to hardcode a simple check first. If Itago fails in that app, keep the warning + Send hold and log the app name.

### 🚨 Checkpoint 4: 1:00 AM — Rules Work in Real App

**Test:** Open Messages, type the test cases in section 8, groups **I, A-E** and **H**. Correct warnings appear, the "should NOT warn" cases stay quiet, Itago leaves no warning.
**If FAIL:** Focus on rule-based only. "Teach me what's private" becomes stretch goal.

### 🚨 Checkpoint 5: 3:00 AM — Full Flow Offline

**Test:** Airplane mode ON. Open Messages. Add the secrets for groups F and G in the app. Type cases 42-50. Warnings appear for 42-44, 47 and 50 only.
**If FAIL:** Fix critical bugs only. Start video recording with what works.

---

## 13. The Pitch & Demo Script (Jakim: Memorize This)

**Format (official):** **5 minutes to pitch and demo live, then 3 minutes of judge Q&A.** Show the working product, not many slides. **Before the timer:** save the two secrets (*"Si Jakim ay nakatira sa Taguig"* and the price-increase note) and mirror the phone to the laptop (HDMI / USB-C are available at the venue).

### 0:00–0:40 — Opening

> "This year, Google said its Gemini AI — while being tested — got into three real companies using passwords it found online. Once a password, an OTP or a key is out, anyone can use it — even an AI. And most leaks start with someone who just hit send: a lola giving her OTP to a fake 'GCash agent', a kid sharing his game password, a developer pasting an API key in the group chat. Meet SecondLook — a keyboard that, like the Palawan pangolin, curls up to protect what's yours. Everything runs on this phone."
> 

### 0:40–3:40 — Live demo (airplane mode ON, show the icon)

1. **Lola:** type *"Anak, eto yung code na pinadala sa akin: 482913"* → **red** *"Teka! OTP o PIN ito — huwag ibigay kahit kanino."* + tip *"Walang totoong bangko, GCash o Maya na hihingi ng OTP o PIN mo…"* → press SecondLook's **Send** → it **holds** → tap **Itago** → the code becomes `•••••`. *"It didn't just warn her — it fixed the message before it left the phone."*
2. **Kid:** type *"Password ng ML account ko: Lodi1234"* → **red** *"Ingat! Mukhang password ito."* → **Itago**.
3. **Small seller:** type *"Pa-GCash na lang sa 09171234567"* → **yellow** *"Phone number ito. Sinadya mo ba?"* → *"Sending your GCash number to get paid is normal, so it's a gentle check, not an alarm."*
4. **Developer:** paste the fake *"GEMINI_API_KEY=AIzaSyA1b2C3d4E5f6G7h8I9j0KlMnOpQrStUvW"* → **red** → **Itago** → *"GEMINI_API_KEY=AIza••••"*.
5. **Teach me what's private:** type *"Let's raid Jakim in Taguig"* → warning (*"It remembers the names I marked private."*) → type *"Prices will go up by 20% next month"* → warning (*"No names this time — the AI on this phone understood the meaning of my private note."*).
6. **Normal message:** *"Kumain ka na ba? Ingat pauwi!"* → **no warning.** *"Normal messages flow through."*

### 3:40–4:30 — Why local

> "A keyboard sees everything you type — including your passwords and codes. Sending those keystrokes to a cloud AI to check them would itself be the leak. SecondLook runs two AI models on the phone: it works in airplane mode, costs nothing per keystroke, reacts while you type, and never sends your draft anywhere. The app doesn't even have internet permission."
> 

### 4:30–5:00 — Closing

> "Like a pangolin, SecondLook keeps everything tucked inside its shell — ingat at malasakit, built in. Same conversation. Same typing. One chance to catch the mistake."
> 

### Q&A prep (3 minutes) — answer honestly

| Likely question | Answer |
| --- | --- |
| Can it stop Messenger's own Send button? | No. A keyboard can only pause its own Send key. That's why it warns and offers Itago**while you type**. |
| What exactly runs locally? Anything in the cloud? | Everything runs on the phone: the rules, a PII model (bert-small-pii-detection, 27 MB) and a sentence model (all-MiniLM-L6-v2, 22 MB) on ONNX Runtime. Nothing needs internet; the app has no INTERNET permission. |
| How accurate is it? | Only quote what we measured, and say how: on**our own synthetic test set** (128 messages), the detector matched every expected result; private-note matching caught 14 of 18 paraphrases with 3 of 22 false alarms. These are self-made tests, not a benchmark. (Fill in phone speed after Jakim measures it.) |
| Does it understand Taglish? | The rules do (e.g. "code na pinadala sa akin"). The meaning model is English-trained, so Taglish private notes work through the names and code words you save. |
| Won't it annoy people? | Things you share on purpose (GCash number, email, bank account) get a soft yellow "Sinadya mo ba?", not a red alarm. Bare numbers, promo codes and questions don't trigger at all. |
| Why a keyboard, not an app? | It works in every app without copy-paste — that's what reaches lolo, lola and kids. |

*Never demo the known misses in section 8, group G. Want a card? Use* `4532 0151 1283 0366` *(a valid test number).*

### Demo video (~1 minute, official guidance)

Airplane mode icon → lola OTP + Send held + Itago → kid password → Gemini key + Itago → "Let's raid Jakim in Taguig" → normal message passes. End card: SecondLook + pangolin + *"A second thought before you send."*

---

## 14. Submission Checklist (Jakim: start at 7:00 AM, submit before 9:00 AM — ONE submission, no edits)

### The project

- [ ]  Project name: **SecondLook**
- [ ]  Short description (draft below)
- [ ]  Team members — exactly as on the official website list ([appbuildersph.com/hackathon](http://appbuildersph.com/hackathon))
- [ ]  **Public** GitHub repository (Charles's account) with README: description, team, tech stack, the pangolin identity, and **step-by-step instructions for judges to rebuild and install the app**
- [ ]  LICENSE file (MIT) · `.gitignore` includes build files · ONNX models (~50 MB): Git LFS or `scripts/download-models.ps1`

### The proof

- [ ]  Demo video, **~1 minute** (section 13) — uploaded to YouTube/Loom/Drive, shareable link
- [ ]  **X or LinkedIn video post URL** — tag **Devin / Cognition** and include **#AppBuildersPH** (required)
- [ ]  What runs locally (draft below)
- [ ]  What requires internet (draft below)

### The disclosures

- [ ]  **Models:** all-MiniLM-L6-v2 (sentence-transformers; ONNX version `Xenova/all-MiniLM-L6-v2` on Hugging Face) — Apache-2.0; bert-small-pii-detection (ONNX version `onnx-community/bert-small-pii-detection-ONNX`) — Apache-2.0 (re-check both model pages). Pretrained, not custom-trained.
- [ ]  **Technologies & frameworks:** Kotlin, Android InputMethodService, Flutter, ONNX Runtime Mobile (MIT); laptop-only test tools: Transformers.js (Apache-2.0)
- [ ]  **APIs & cloud services:** none at runtime
- [ ]  **Existing code & assets:** pretrained models above; Gradle wrapper; pangolin image source (own drawing, or the license of any photo/illustration used)
- [ ]  **AI development tools:** e.g. Claude Code (used for the AI core) — list every tool the team used
- [ ]  Built during hackathon hours (Oct 9-10, 2026)

### Drafts to paste (edit before submitting)

**Short description:** *SecondLook is an Android keyboard that teaches your phone to keep secrets. While you type in any chat app, on-device AI spots passwords, PINs, OTPs, API keys and ID numbers — or things you marked private — shows a gentle Filipino-first warning, hides the secret with one tap ("Itago"), and holds the keyboard's Send key until you're sure. Built for lolo, lola, kids and everyone who "sends lang nang send". Everything runs offline; nothing you type leaves the phone.*

**What runs locally:** *Everything: the rule detector, the PII model (bert-small-pii-detection) and the sentence-embedding model (all-MiniLM-L6-v2) on ONNX Runtime Mobile, private-note matching, Itago masking, and the encrypted private-note storage (Android Keystore).*

**What requires internet:** *Nothing at runtime — the app declares no INTERNET permission and works in airplane mode. Internet is only needed to download the app, and the model files when building it.*

**Why does this product benefit from running AI locally?** *A keyboard sees everything you type, including the very passwords, OTPs and keys we want to protect. Sending keystrokes to a cloud AI to check them would itself be the leak. Running both AI models on the phone keeps every draft private, works with no signal, reacts instantly while you type, and costs nothing per keystroke.*

---

## 15. Emergency Fallback Plan

**If the ONNX model doesn't work on the phone by 1:00 AM:**
→ Already handled: `DetectionEngine` keeps running the rules if the models fail to load (`isReady = false`, `loadError` says why). Groups I, A-E and Itago (H) still work.
→ "Teach me what's private" becomes a keyword exact-match (names/code words only). *Not built yet* — the anchors already exist; Annie can add this in ~30 minutes if needed.
→ Still impressive: passwords, PINs, OTPs, API keys, SSH keys, IDs, cards, one-tap Itago — all offline.

**If Itago can't edit the text box in a messaging app:**
→ Keep the warning + the Send hold; demo Itago in Messages, or in the practice screen.

**If the keyboard doesn't type properly by 10:00 PM:**
→ Switch to an Accessibility Service approach (more permissions but works).
→ Or: build the detection inside the Flutter app itself with a "private composer" text box. Annie's test APK (`ai-local/test-app`) already shows the same detection + Itago in a normal app screen, so this fallback is half-built.
→ Less impressive but still a working product.

**If integration breaks after 5:00 AM:**
→ Do NOT fix. Record demo with the 3:00 AM working version.
→ Use the backup video. **The 8:00 AM done / 9:00 AM submitted deadline does not move.**

---

## 16. Key Warnings for the Team

> [!CAUTION]
**Never use real credentials in testing.** Always use synthetic data. Real card numbers, passwords, OTPs or **API keys** could leak in screenshots, videos, or the git history. Never paste your real Gemini or Hugging Face key "just to test" — GitHub and chat history keep it. The keys in section 8 are fake look-alikes.
> 

> [!CAUTION]
**No fake benchmarks.** The briefing says results can be disputed for fake benchmarks. Only quote numbers we measured, and say they come from our own synthetic tests.
> 

> [!IMPORTANT]
**The keyboard MUST work offline.** This is the entire hackathon theme. Test with airplane mode ON repeatedly. If any feature requires internet, remove it. Confirm the release APK has **no INTERNET permission**.
> 

> [!IMPORTANT]
**One submission, no edits.** Check every field and link before pressing submit. Repo must be public; code freezes at 10:00 AM.
> 

> [!TIP]
**Don't over-polish.** A working demo with basic UI beats a beautiful app that crashes. Get the flow working first, make it pretty second — the pangolin animations come last.
> 

> [!TIP]
**Test the release APK, not just debug.** If the release build crashes when the models load, R8 may have stripped ONNX Runtime classes: add `-keep class ai.onnxruntime.** { *; }` to `android/app/proguard-rules.pro`.
> 

> [!TIP]
**Bring to the venue:** the Tecno phone + cable, charger, our own laptop (HDMI / USB-C available) to mirror the phone, the backup video offline, and the submission confirmation.
> 

---

## 17. Quick Reference Commands

```bash
# Create the Flutter project
flutter create --org com.secondlook secondlook

# Run on connected phone
flutter run

# Build release APK
flutter build apk --release

# The APK will be at:
# build/app/outputs/flutter-apk/app-release.apk
```

### Annie: AI tools (Windows, PowerShell)

```powershell
# Models are already in ai-local/assets/models — re-download only if needed
powershell -File ai-local\scripts\download-models.ps1

# Run the real Kotlin detector + models + Itago on the laptop against every test case
cd ai-local\jvm-check; .\gradlew.bat run

# Same models in a browser, all local (demo with Itago + scorer)
cd ai-local\web-demo; npm install; npm start      # <http://127.0.0.1:5173>  and  /eval.html

# Build the standalone test APK and install it on the phone
cd ai-local\test-app; .\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Prove there is no INTERNET permission in the final app
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt.exe" dump permissions build\app\outputs\flutter-apk\app-release.apk
```

---

## 18. 🤖 For AI Coding Assistants (paste this into ChatGPT / Claude / Gemini)

Copy the **Context** block, then **your own role's block**, into your AI assistant before asking it for code. It keeps the assistant from rewriting Annie's AI code, adding internet calls, or using the wrong package names.

### Context (everyone)

```
PROJECT: SecondLook — an Android keyboard (Kotlin InputMethodService) that "teaches your phone to keep secrets".
It warns BEFORE sending when a draft contains a secret, hides it with one tap ("Itago"), and holds the keyboard's own
Send key until the user confirms.
AUDIENCE: everyday Filipinos first (lolo & lola, kids & teens, parents, small online sellers) who "send lang nang send";
developers second.
PRIORITY: passwords & PINs (GCash MPIN, ATM PIN, game/FB passwords) → OTP/verification/2FA codes (incl. everyday
phrasing "code na pinadala sa akin") → API & secret keys (Gemini AIza…, Hugging Face hf_…, OpenAI sk-…, AWS AKIA…,
GitHub ghp_…, PayMongo sk_live_…), SSH private keys, database credentials → cards + CVV, PH government IDs
(TIN/SSS/PhilSys) → soft yellow "Intended?" for phone numbers, emails, bank accounts → "Teach me what's private"
(user-saved notes matched by meaning, or by names + meaning).
INSPIRATION: leaked passwords/keys get used — in 2026 Google said Gemini, in testing, reached real companies using
passwords found online; OpenAI disclosed an AI model incident involving Hugging Face.
IDENTITY: the Palawan pangolin (curls into an armored ball = protects your data; endemic to the Philippines; caring
guardian, not surveillance; values: ingat, malasakit). Purple = safe/brand, amber = "Intended?", red = secret.
Gentle, caring Filipino-first voice — never scolding.
DEADLINE: everything done by 8:00 AM Oct 10, submitted before 9:00 AM (official 10:00 AM). Feature freeze 7:00 AM.
DEMO DAY: Cyberzone, SM Makati; pitching from 1:40 PM; 5 min pitch + live demo, 3 min Q&A; demo video ~1 minute.

HARD RULES
- Everything runs on the phone. No network calls, no INTERNET permission, no analytics, no crash reporters, no cloud APIs.
- Never log, print or store the draft text. Use only synthetic test data.
- The detection/AI code ALREADY EXISTS (owner: Annie). Do NOT rewrite or edit anything in package com.secondlook.ai,
  DetectionEngine.kt or SecretManager.kt. Ask Annie for changes.
- Flutter app namespace: com.secondlook.secondlook (from `flutter create --org com.secondlook secondlook`).
  Keyboard + AI classes are in package com.secondlook → fully-qualified names in AndroidManifest.xml,
  and `import com.secondlook.secondlook.R` in SecondLookIME.kt.
- Gradle: minSdk 26; implementation("com.microsoft.onnxruntime:onnxruntime-android:1.26.0");
  androidResources { noCompress += "onnx" }. Do not add androidx.security:security-crypto.
- Run detection off the main thread (detectAsync). The keyboard can only pause its OWN Enter/Send key,
  never the messaging app's Send button.

KOTLIN API (already implemented)
- DetectionEngine.get(context): DetectionEngine   // one per process; models load in background; rules work at once
- engine.detectAsync(text) { warnings: List<Warning> -> }   // background thread, callback on main thread
- engine.detect(text): List<Warning>                       // blocking — never on the main thread
- Warning(type, message, severity, matchedText, start, end, tip, canHide, headline)
    type: password | otp | api_key | credit_card | gov_id | phone | email | bank_account | pii | private_match
    severity: "high" → red banner, "medium" → yellow "Intended?" banner
    headline: Filipino-first title (show it first); message: English reason; tip: one line of Taglish advice
    canHide: show the [Itago] button
- engine.hide(text, warning): String?   // text with the secret masked (••••••, AIza••••); null for private_match
- engine.hideAll(text): String
- engine.secrets.save(text, label) / list() / delete(id) / deleteAll()   // encrypted, max 5; list() = labels only
- engine.isReady, engine.loadError

FLUTTER METHODCHANNEL "secondlook/ai" (paste ai/bridge/AiMethodChannel.kt into MainActivity)
- check(text) → {type, message, severity, matchedText, start, end, tip, canHide} or null
  (add "headline" to the map in AiMethodChannel if the practice screen should show it)
- hideAll(text) → String
- saveNote(text, label) → {id, label};  listNotes() → [{id, label}];  deleteNote(id);  deleteAll()
- status() → {ready, error}
```

### Klarenz (keyboard)

```
TASK: Build SecondLookIME in android/app/src/main/kotlin/com/secondlook/ of the Flutter project.
1. Register the IME (AndroidManifest service with BIND_INPUT_METHOD + res/xml/method.xml). It must appear in Settings.
2. QWERTY keys from keyboard_layout.xml; type with currentInputConnection.commitText.
3. After a 500 ms typing pause: text = getTextBeforeCursor(2000,0) + getTextAfterCursor(2000,0);
   engine.detectAsync(text). Keep that text as `checkedText`.
4. Show warning_banner.xml above the keys: red if severity == "high", yellow if "medium"; headline (Filipino) first,
   then message + tip; buttons: [Itago] only if canHide, [Review], [Dismiss].
5. Itago: re-read the text; if it still equals checkedText, beginBatchEdit(); deleteSurroundingText(before.length,
   after.length); commitText(engine.hide(checkedText, warning), 1); endBatchEdit(); then check again.
6. Own Enter key: if the field's action is SEND/GO/DONE (and no IME_FLAG_NO_ENTER_ACTION) and a red warning is
   active, hold it: dialog [Itago] [Review] [Send anyway]; Send anyway → sendDefaultEditorAction(true).
   Otherwise Enter commits "\n".
7. Paste ai/bridge/AiMethodChannel.kt into MainActivity.configureFlutterEngine.
8. Release APK installed on the Tecno and handed to Jakim by 8:00 AM. Optional after everything works: "Ingat Mode"
   (Filipino-only, bigger banner, hold Send 2 s instead of one-tap "Send anyway").
ACCEPTANCE: airplane mode ON, Google Messages: "Anak, eto yung code na pinadala sa akin: 482913" → red;
Itago → "...sa akin: ••••••"; "Send money to 09171234567" → yellow; "Kumain ka na ba? Ingat pauwi!" → nothing.
```

### Charles (UI)

```
TASK: Layouts + Flutter screens (Material widgets only, no extra packages, no Expo).
1. keyboard_layout.xml: QWERTY, root id keyboard_container, a pangolin button on the top strip
   (Grammarly-style brand button): calm = safe, peeking = yellow warning, curled = red warning.
2. warning_banner.xml: id warning_banner, TextViews headline_text + warning_text + tip_text, buttons hide_button
   ("Itago"), review_button ("Review"), dismiss_button ("Dismiss"). Two styles: red (high) and amber (medium,
   "Intended?"). Big, readable text — lolo and lola must be able to read it.
3. Brand: Palawan pangolin; palette deep violet, purple, amber, lavender, near-black (confirm exact hex from the
   identity board); purple blob pattern for splash/onboarding; app icon = curled pangolin.
4. Flutter screens: onboarding (3 steps: install → enable keyboard → try), secrets (add/delete, max 5, show the LABEL
   only — never show the secret text again after saving), practice (TextField; on change call MethodChannel "check";
   show the banner; an "Itago" button calls "hideAll" and replaces the text), about (offline, no internet permission,
   no typing history, why a pangolin).
5. Use MethodChannel "secondlook/ai" exactly as listed in the Context block. Assets ready by 8:00 AM.
```

### Jakim (test & identity)

```
TASK: Tests, strings, story, submission.
1. strings.xml: one warning + one tip per type (English + Filipino), gentle "ingat / malasakit" voice:
   password, otp, api_key, credit_card, gov_id, phone, email, bank_account, private_match.
   (Filipino headlines are already built in as defaults.)
2. Run section 8 (groups I and A–H) on the Tecno with airplane mode ON. Log PASS/FAIL per case number, the
   messaging app and the Android version. Never use real keys, passwords, IDs or card numbers.
3. Pitch exactly as section 13: 5 minutes incl. live demo, then 3 minutes Q&A. Quote only measured numbers.
4. Demo video ~1 minute. X or LinkedIn video post: tag Devin / Cognition, #AppBuildersPH.
5. README must explain how judges rebuild the app. Cerebral Valley: one submission only, no edits — use the drafts
   and disclosures in section 14. Everything ready by 8:00 AM; submitted before 9:00 AM.
```

---

> **Print this document. Pin it on the wall. Follow it hour by hour. DONE by 8:00 AM. SUBMITTED before 9:00 AM. At Cyberzone, SM Makati, rehearsed, by 11:00 AM.**
>