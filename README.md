# 🛡️ TanggolIn (SecondLook Keyboard)

<p align="center">
  <img src="Resources/Tanggolin%20Icon.svg" alt="TanggolIn Logo" width="140" height="140" />
</p>

<p align="center">
  <strong>"A second thought before you send."</strong><br>
  <em>Turuan ang phone mo na magtago ng sikreto.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Engine-Kotlin%20IME%20%2B%20Flutter-7952B3?style=flat-square&logo=kotlin&logoColor=white" alt="Engine" />
  <img src="https://img.shields.io/badge/Local%20AI-ONNX%20Runtime%20Mobile-005CED?style=flat-square&logo=onnx&logoColor=white" alt="AI" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline%20%2F%20Zero%20Cloud-success?style=flat-square" alt="Privacy" />
  <img src="https://img.shields.io/badge/Hackathon-AppBuildersPH%202026-F59E0B?style=flat-square" alt="Hackathon" />
</p>

---

> 📖 **Judges & Evaluators:** For a complete, step-by-step test guide and copy-pasteable test scenarios, please see [**INSTRUCTIONS.md**](file:///c:/dev/App-Builders-Hackathon/INSTRUCTIONS.md).  
> 📦 **Pre-Built APK:** Available immediately at repository root: [**`./TanggolIn-SecondLook.apk`**](file:///c:/dev/App-Builders-Hackathon/TanggolIn-SecondLook.apk) *(and [`./apk/TanggolIn-SecondLook.apk`](file:///c:/dev/App-Builders-Hackathon/apk/TanggolIn-SecondLook.apk))*.

---

## 🌟 The Problem: "Send Lang Nang Send"

Every day, millions of Filipinos fall victim to digital scams, social engineering, and accidental disclosures:
- **Lolo and Lola** sharing their one-time pin (OTP) with fake "GCash assistance" agents.
- **Teens and Gamers** sending account passwords or ATM PINs over unsecured chats.
- **Developers and Freelancers** accidentally pasting `.env` secrets, OpenAI keys, or Gemini API tokens into group threads.

By the time the sender realizes what happened, **the message is already sent**.

## 💡 The Solution: TanggolIn (SecondLook)

Named after the **Palawan Pangolin** (*TanggolIn* — from *tanggol* meaning "defend" / "protect"), this smart Android keyboard acts as an on-device digital guardian:
1. **Warns WHILE you type:** Instantly identifies risky disclosures before you hit send.
2. **"Itago" (Hide) in one tap:** Replaces credentials and private data with safe masking (`[NAKATAGO]`).
3. **Send-Key Guard:** Holds the keyboard's dispatch key when sensitive information is exposed.
4. **100% Local AI:** Operates with **zero internet access** — all neural network inferences run directly on your phone's processor using ONNX Runtime.

---

## 🛡️ Key Features

### 1. 🚨 Multi-Tier Risk Classification
- **High Risk (Red Alert):** Passwords, PINs, OTP codes, 2FA tokens, API keys (Gemini, Hugging Face, OpenAI, GitHub, AWS), and private keys.
- **Medium Risk (Amber Alert):** Government ID numbers (TIN, SSS, PhilSys, PhilHealth), credit/debit card numbers.
- **Intent Check (Yellow Notice):** GCash numbers, phone numbers, and bank account numbers (allowing intentional transfers while preventing accidental leaks).

### 2. 🇵🇭 Filipino-First Safety Guidance
Safety warnings are delivered in empathetic, conversational Tagalog and English:
- *"Teka! OTP o PIN ito — huwag ibigay kahit kanino."*
- *"Paalala: Huwag ibahagi ang password ng iyong account."*

### 3. 🙈 "Itago" (Instant Draft Sanitization)
With a single tap on the keyboard bar, sensitive tokens and credentials are redacted in-place inside the active text field.

### 4. 🧠 "Teach Me What's Private" (Local Semantic Memory)
Teach TanggolIn personal secrets, internal project names, or code words. Using an on-device sentence embedding model, TanggolIn understands the contextual meaning and flags mentions even if phrased differently.

### 5. 🔒 Absolute Zero-Cloud Guarantee
- **No network calls:** The app and keyboard service don't request or use the Android `INTERNET` permission.
- **No telemetry or keylogging:** Text analysis is processed in volatile memory and discarded immediately.
- **Airplane-mode ready:** Operates seamlessly with no Wi-Fi or cellular connection.

---

## 🏗️ Architecture & Technology Stack

```
┌─────────────────────────────────────────────────────────────┐
│                   Android Host Application                  │
│       (WhatsApp, Telegram, Messenger, Notes, SMS, Browser)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ Keystrokes & Text Commits
┌──────────────────────────────▼──────────────────────────────┐
│       TanggolIn Native Android InputMethodService (Kotlin)   │
│  ┌───────────────────────────────────────────────────────┐  │
│  │               Low-Latency Multi-Gate Engine           │  │
│  │                                                       │  │
│  │  [Gate 1] Heuristic & Regex Scanner (Sub-1ms)         │  │
│  │           OTP, PINs, Passwords, API Keys, RegEx       │  │
│  │                                                       │  │
│  │  [Gate 2] ONNX PII Token Classifier (Quantized INT8)  │  │
│  │           Contextual entity detection (Microsoft ORT) │  │
│  │                                                       │  │
│  │  [Gate 3] ONNX Semantic Embedding Engine (INT8)      │  │
│  │           "Teach Me What's Private" contextual memory │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  Keyboard UI: Smart Banner, Pangolin Shield, "Itago" Action  │
└──────────────────────────────┬──────────────────────────────┘
                               │ MethodChannels & IPC
┌──────────────────────────────▼──────────────────────────────┐
│           Flutter Companion Application (Flutter 3.44)       │
│   • 3-Step Guided Activation & Keyboard Settings             │
│   • Interactive Safe Practice Playground                     │
│   • Private Notes & Custom Secret Vault                      │
│   • About TanggolIn & Privacy Architecture Breakdown         │
└─────────────────────────────────────────────────────────────┘
```

- **Keyboard Layer:** Native Android `InputMethodService` written in **Kotlin**.
- **Companion / Settings UI:** Built with **Flutter 3.44 / Dart 3.12**.
- **Inference Runtime:** **Microsoft ONNX Runtime Android** (`onnxruntime-android:1.26.0`).
- **AI Models:** Quantized INT8 ONNX models:
  - MiniLM Sentence Embedding Model (`assets/models/embedding/model_quantized.onnx`)
  - PII Token Classification Model (`assets/models/pii/model_quantized.onnx`)

---

## ⚡ Quick Start: Testing the App

### Option A: Install Pre-Built APK (Fastest)

```bash
# 1. From the repository root:
adb install TanggolIn-SecondLook.apk

# 2. Or install directly on your device from the root file:
# TanggolIn-SecondLook.apk
```

### Option B: Build from Source

```powershell
# Prerequisites: Flutter 3.44+, JDK 17+, Android SDK 34
cd "Android App"
flutter pub get
flutter build apk --debug
```

### 3-Step Setup Checklist
1. Open the **TanggolIn** app.
2. Tap **Step 1: Enable Keyboard** and activate *SecondLook Keyboard*.
3. Tap **Step 2: Select Keyboard** and pick *SecondLook Keyboard*.
4. Tap **Step 3: Test Keyboard** to jump into the built-in practice playground!

---

## 🧪 Try These Sample Inputs!

| What to Type | What Happens |
|---|---|
| `Anak, eto yung code: 482913` | 🚨 Catches 6-digit OTP code |
| `Password ng ML ko: Lodi1234` | 🚨 Catches password in Tagalog |
| `PIN ng ATM ko 1234` | 🚨 Catches 4-digit banking PIN |
| `sk-proj-abc1234567890defghijkl` | 🚨 Catches OpenAI API Key |
| `AIzaSyB_1234567890abcdefghijkl` | 🚨 Catches Gemini / Google API Key |
| `TIN: 123-456-789-000` | ⚠️ Catches Philippine Government TIN |

*Tap **"Itago"** to see the secret masked in-place!*

---

## 📁 Repository Directory Structure

```
App-Builders-Hackathon/
├── TanggolIn-SecondLook.apk        # 📦 Pre-compiled ready-to-test APK (Root)
├── app-debug.apk                   # 📦 Mirror APK binary
├── apk/                            # 📁 Dedicated APK distribution directory
│   └── TanggolIn-SecondLook.apk
├── INSTRUCTIONS.md                 # 📋 Detailed Judge & Evaluator Guide
├── README.md                       # 📖 Main Project Documentation
├── ARCHITECTURE.md                 # 🏛️ Comprehensive Architecture & Build Manual
├── TANGGOLIN_MOBILE_UI_DESIGN_REVIEW.md # 🎨 Design System & UI Specifications
├── Resources/                      # 🎨 High-resolution vector assets & logos
│   ├── App icon.svg
│   └── Tanggolin Icon.svg
├── Android App/                    # 📱 Flutter Companion App & Native Kotlin IME
│   ├── android/                    # Native Android project with Kotlin IME service
│   │   └── app/src/main/
│   │       ├── assets/models/      # On-device quantized ONNX models
│   │       └── kotlin/com/secondlook/ # Kotlin InputMethodService & AI adapters
│   └── lib/                        # Flutter companion application screens & widgets
└── AI Model/                       # 🤖 Model training, conversion & evaluation pipelines
    └── ai-local/                   # Python quantization scripts, test data & docs
```

---

## 👥 The TanggolIn Team

Created with ❤️ for **AppBuildersPH Hackathon 2026**:

- **Klarenz** — *Keyboard & IME Architecture Lead*
- **Annie** — *Local AI & Model Optimization Lead*
- **Charles** — *UI/UX & Design Lead*
- **Jakim** — *Testing, Identity & Cultural Lead*

**GitHub Repository:** [baldopadua/App-Builders-Hackathon](https://github.com/baldopadua/App-Builders-Hackathon)  
*Venue: Cyberzone, SM Makati · October 10, 2026*
