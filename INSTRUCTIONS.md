# 🛡️ TanggolIn (SecondLook Keyboard) — Judge & Evaluation Guide

> **"A second thought before you send."** · *Turuan ang phone mo na magtago ng sikreto.*  
> **AppBuildersPH Hackathon 2026** · Local AI Track · 🇵🇭 Identity: The Palawan Pangolin (*TanggolIn*)

---

## ⚡ Quick Start for Judges (2-Minute Fast Track)

We know your evaluation time is limited! Here is how to test **TanggolIn (SecondLook)** immediately:

### Option 1: Install the Pre-built APK (Recommended)
The pre-compiled, self-contained debug APK is ready at the root of this repository:
- **Root Path:** [`./TanggolIn-SecondLook.apk`](file:///c:/dev/App-Builders-Hackathon/TanggolIn-SecondLook.apk) *(also mirrored at [`./apk/TanggolIn-SecondLook.apk`](file:///c:/dev/App-Builders-Hackathon/apk/TanggolIn-SecondLook.apk))*

```bash
# Connect your Android phone or launch an emulator, then run:
adb install TanggolIn-SecondLook.apk
```
*Alternatively, transfer `TanggolIn-SecondLook.apk` to your phone via USB or Google Drive and tap to install.*

---

## 🚀 Setting Up the Keyboard (Step-by-Step)

1. **Launch the TanggolIn App**
   - Open **TanggolIn** from your app drawer.
   - Walk through the brief onboarding to meet our mascot, the **Palawan Pangolin** (TanggolIn), who curls up tightly to protect your secrets.

2. **Complete the 3-Step Setup Checklist**
   - **Step 1: Enable Keyboard**  
     Tap the button to open Android's **Manage Keyboards** settings, then toggle on **SecondLook Keyboard** (or TanggolIn). Tap *OK* on the standard Android warning.
   - **Step 2: Select Keyboard**  
     Tap the button and choose **SecondLook Keyboard** as your active input method.
   - **Step 3: Test Keyboard**  
     Tap into the integrated **Safe Test / Practice Playground** screen to begin typing!

---

## 🧪 Judge Evaluation Test Suite (Copy & Paste Scenarios)

You can test these scenarios directly inside the **TanggolIn Practice Playground** or in **any real Android app** (SMS, WhatsApp, Messenger, Notes, Browser, Slack).

### 🚨 Test Category 1: Passwords & PINs (Red Alert - High Risk)
*Everyday Filipinos often share passwords or PINs over chat without realizing the danger.*

| Test Phrase to Type / Paste | Expected Pangolin Reaction |
|---|---|
| `Password ng ML account ko: Lodi1234` | 🚨 **Red Warning Banner:** *"Teka! Password o PIN ito — huwag ibigay kahit kanino."* |
| `PIN ng ATM ko 1234` | 🚨 **Red Warning Banner:** Pangolin turns vigilant; flags 4-digit ATM PIN. |
| `GCash MPIN ko ay 9982` | 🚨 **Red Warning Banner:** Detects MPIN disclosure. |

👉 **Action to Test:** Tap the **"Itago" (Mask)** button on the suggestion bar.  
**Result:** The password/PIN is immediately redacted in-place inside your draft!

---

### 🔑 Test Category 2: OTP & Verification Codes (Red Alert - High Risk)
*The #1 attack vector in the Philippines: fake GCash agents asking for OTPs.*

| Test Phrase to Type / Paste | Expected Pangolin Reaction |
|---|---|
| `Anak, eto yung code na pinadala sa akin: 482913` | 🚨 **Red Warning:** Catches 6-digit OTP code in conversational Filipino. |
| `Your verification code is 829104` | 🚨 **Red Warning:** Catches English OTP format. |
| `Dito mo isend ang OTP: 391024` | 🚨 **Red Warning:** Catches targeted OTP handover. |

👉 **Action to Test:** Notice the **Send Key Guard**. The keyboard prevents quick accidental sends when high-risk secrets are active in the draft.

---

### 💻 Test Category 3: Developer Secrets & API Keys (Red Alert)
*Protects developers and freelancers from leaking expensive tokens and credentials into group chats.*

| Test Phrase to Type / Paste | Expected Pangolin Reaction |
|---|---|
| `AIzaSyB_1234567890abcdefghijklmnopqrstuv` | 🚨 **Red Warning:** Google / Gemini API Key detected. |
| `sk-proj-abc1234567890defghijklmnopqrstuvwxyz` | 🚨 **Red Warning:** OpenAI Secret Key detected. |
| `hf_abcdefghijklmnopqrstuvwxyz123456` | 🚨 **Red Warning:** Hugging Face Access Token detected. |
| `ghp_1234567890abcdefghijklmnopqrstuvwxyz` | 🚨 **Red Warning:** GitHub Personal Access Token detected. |

👉 **Action to Test:** Tap **"Itago"**. The key is replaced with protective masking.

---

### ⚠️ Test Category 4: Government IDs & Payment Cards (Amber Alert)

| Test Phrase to Type / Paste | Expected Pangolin Reaction |
|---|---|
| `TIN number ko: 123-456-789-000` | ⚠️ **Amber Warning:** Detects Philippine TIN format. |
| `SSS number: 34-1234567-8` | ⚠️ **Amber Warning:** Detects Philippine SSS format. |
| `Card: 4532 0150 1234 5678` | ⚠️ **Amber Warning:** Validates credit card pattern. |

---

### 🧠 Test Category 5: "Teach Me What's Private" (Custom Local AI Matching)
*Users can teach TanggolIn custom secrets, codewords, or sensitive terms.*

1. In the TanggolIn app, navigate to **Private Notes / Teach Privacy**.
2. Add a new private item:
   - **Keyword / Label:** `Secret Project`
   - **Secret Value / Phrase:** `Project Mayon`
3. Return to the typing area and type:
   - `"Let's talk about Project Mayon tomorrow"`
4. **Result:** TanggolIn's local embedding engine identifies the custom secret and raises an alert!

---

### ✈️ The Ultimate Proof: The Airplane Mode Test (100% Offline AI)

1. Turn on **Airplane Mode** on your device (disconnect both Wi-Fi and Mobile Data).
2. Type any of the test phrases above.
3. **Observation:** Every detection, embedding calculation, and redaction works **instantaneously and without delay**.
4. **Verification:** TanggolIn has **zero internet permissions** (`android.permission.INTERNET` is not even present in the native IME service). Your keystrokes never leave the silicon of your phone.

---

## 🏆 How TanggolIn Maps to the Judging Rubric

| Criteria | Weight | How TanggolIn Delivers |
|---|:---:|---|
| **Problem & Usefulness** | **25%** | Targets the real-world epidemic of phishing, social engineering, and accidental data leaks in the Philippines. Protects both everyday citizens (lolo, lola, teens, online sellers) and developers. |
| **Local AI** | **25%** | Multi-gate architecture running entirely on-device: Gate 1 (ultra-fast regex & heuristics), Gate 2 (quantized ONNX PII Token Classification model), Gate 3 (quantized Sentence-Transformers MiniLM embedding engine). Low memory footprint, sub-10ms response time. |
| **Technical Execution** | **20%** | Full Android native `InputMethodService` implemented in Kotlin, communicating smoothly with a high-performance ONNX Runtime Android engine and a polished Flutter companion application. |
| **Innovation** | **15%** | Active pre-send prevention at the keyboard layer: proactive warning banners, "Itago" one-tap draft sanitization, and send-holding guards before messages are ever sent over the wire. |
| **Product & Demo Quality** | **15%** | Deep Filipino cultural resonance centered on the endangered **Palawan Pangolin (*TanggolIn*)**, complete with responsive mascot animations, bilingual Tagalog/English safety guidance, and a pristine UI. |

---

## 🛠️ Building from Source (Optional)

If you prefer to compile from source code:

### Prerequisites
- **Flutter SDK:** 3.44.x (Channel stable)
- **Dart SDK:** 3.12.x
- **JDK:** 17+ (Java 17 required for Android Gradle Plugin)
- **Android SDK:** API Level 34 (compileSdk: 34, minSdk: 26)

### Build Commands
```powershell
# 1. Navigate to the Flutter app directory
cd "Android App"

# 2. Get dependencies
flutter pub get

# 3. Build the debug APK (includes on-device ONNX models)
flutter build apk --debug

# The generated APK will be at:
# Android App/build/app/outputs/flutter-apk/app-debug.apk
```

---

## ❓ Frequently Asked Questions & Troubleshooting

- **Q: Why is the APK file size ~330 MB?**  
  **A:** The debug APK bundles native ONNX Runtime libraries (`.so`) for all 3 major architectures (`arm64-v8a`, `armeabi-v7a`, `x86_64`) plus both full INT8 quantized neural models so it runs flawlessly on any physical device or emulator without downloading external dependencies.

- **Q: The keyboard didn't show up after installation.**  
  **A:** On Android, custom keyboards must be explicitly enabled once in **Settings → System → Languages & Input → On-screen keyboard / Manage Keyboards**. The TanggolIn onboarding app guides you through this in one tap.

- **Q: Does TanggolIn record or store what I type?**  
  **A:** Absolutely not. There is no keystroke logging, no cloud database, no tracking analytics, and no telemetry. The analysis runs ephemerally in RAM and is discarded instantly.

---

## 👥 The Team Behind TanggolIn

- **Klarenz** — Keyboard & IME Architecture Lead
- **Annie** — Local AI & Model Optimization Lead
- **Charles** — UI/UX & Interaction Design Lead
- **Jakim** — Testing, Identity & Cultural Resonance Lead

**Repository:** [github.com/baldopadua/App-Builders-Hackathon](https://github.com/baldopadua/App-Builders-Hackathon)  
*AppBuildersPH Hackathon 2026 · Cyberzone, SM Makati*
