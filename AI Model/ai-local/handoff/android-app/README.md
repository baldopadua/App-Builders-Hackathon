# Handoff: put the ai-local AI core into the keyboard (Android App)

**For:** Klarenz. **From:** the AI side (`AI Model/ai-local`). Nothing in `Android App/` was edited by us; you copy, so there is no merge conflict.

## What you get

`DetectionEngine.discoverAnnieAiAdapter()` already looks for a class named `com.secondlook.ai.OnnxAiAdapter`. This folder provides that class. Once it is in the app, the keyboard switches to it by itself:

| | Before (today) | After |
|---|---|---|
| Engine | `RuleBasedAiAdapter` (regex) | `OnnxAiAdapter`: rules + PII model + private notes, the same code `jvm-check` and `test-app` measure |
| Notes | `SecretManager` value, exact substring | The same `SecretManager` notes. One value (MPIN, password) is caught anywhere; a sentence is caught by meaning and names; a value typed word for word is still always caught |
| Banner | `Warning(type, headline, tip)` | Same type. Filipino headline and tip from `SafetyTips` |
| Flutter UI, `SecretManager` storage, 5-note limit | yours | unchanged |

Measured on the same messages (`gradlew fallbackCheck` in `AI Model/ai-local/jvm-check`):

| | Risky caught (synthetic / real SMS) | Red false alarms on 63 safe synthetic | Red false alarms on 3,497 normal real PH SMS |
|---|---|---|---|
| Regex only (today) | 70/106 · 109/133 | 9 | 110 |
| AI core only (after steps 1-4) | **106/106 · 133/133** | **0** | **12** |
| AI core, then regex when it says nothing (after steps 1-3 only) | 106/106 · 133/133 | 9 | 117 |

## Try it first, without touching your app

**Your real app, with the AI, as a preview APK.** This is the same Flutter UI, keyboard, banner and Ingat Mode, built from a copy outside the repo with steps 1-5 below already applied:
```powershell
powershell -ExecutionPolicy Bypass -File "AI Model\ai-local\handoff\android-app\preview.ps1"
# -> ..\gate3-work\handoff-preview\Android App\build\app\outputs\flutter-apk\app-debug.apk   (adb install -r <apk>)
```
`Android App` is only read. Rerun it whenever your code or the AI changes. If one of your files changed so much that a snippet no longer fits, the script prints a warning for that step and builds anyway.

**Decision check on the laptop or phone.** The ai-local **test app** (`AI Model/ai-local/test-app`) builds your *current* `DetectionEngine.kt`, `SecretManager.kt`, `AiDetectionAdapter.kt`, `RuleBasedAiAdapter.kt` and `Warning` straight from `Android App`. They are copied read-only into `test-app/app/build/` at build time, never edited. The build adds these two adapter files. Its **Keyboard mirror** card shows the banner your keyboard would show for any draft, with your Practice-screen quick tests (Lola OTP, ML Password, Gemini Key, GCash No., Private Note) and a vault that uses your `SecretManager`. If you change your files, rebuild the test app and it follows.

```powershell
cd "AI Model\ai-local\test-app"; .\gradlew.bat installDebug     # phone on USB, then open "SecondLook AI test"
```

## Steps (about 10 minutes)

1. **Copy the code.** From the repo root, in PowerShell:
   ```powershell
   powershell -ExecutionPolicy Bypass -File "AI Model\ai-local\handoff\android-app\install.ps1"
   ```
   It copies `AI Model/ai-local/kotlin/com/secondlook/ai/**` plus `OnnxAiAdapter.kt` and `SecretManagerNotesStore.kt` into `android/app/src/main/kotlin/com/secondlook/ai/`. It only adds or overwrites files in that folder. Your `AiDetectionAdapter.kt` and `RuleBasedAiAdapter.kt` are not touched, and the empty placeholder files get their real content. It does not copy ai-local's `DetectionEngine.kt` or `SecretManager.kt`; yours stay. You can also copy by hand: same files, same places.

2. **Bundle the models.** No 50 MB copy: point Gradle at the committed models. In `android/app/build.gradle.kts`, inside `android { }`:
   ```kotlin
   sourceSets { getByName("main") { assets.srcDir("../../../AI Model/ai-local/assets") } }
   ```
   You already have `onnxruntime-android:1.26.0` and `noCompress += "onnx"`. The APK grows by about 50 MB.

3. **Build and check.** `flutter run`. The Practice screen's engine status (`getAiEngineStatus`) should say `OnnxAiAdapter` after 1-2 s. Logcat shows `OnnxAiAdapter: AI core ready in … ms`. If it still says `RuleBasedFallback`, check Logcat for `Candidate adapter … failed to instantiate`, which usually means the models are missing (step 2).

4. **Stop the regex fallback from adding false alarms.** In `DetectionEngine.kt`, in both `detectSync` and `detectAsync`, change `activeAdapter.detectSync(text) ?: fallbackAdapter.detectSync(text)` to `activeAdapter.detectSync(text)` ([`snippets/DetectionEngine.kt.txt`](snippets/DetectionEngine.kt.txt)). While the models load, `activeAdapter` *is* the regex, so it still runs; your `catch` blocks still fall back if the AI throws.

5. **Recommended: Flutter `check` off the main thread.** [`snippets/MainActivity-check.kt.txt`](snippets/MainActivity-check.kt.txt) moves the model work off the main thread. The Practice screen's category pill then names what the AI really found (`BABALA · OTP`, `BABALA · VAULT`, `PANSIN · INTENDED?`, …). `PlatformBridge.dart` does not change.

## How it behaves in the keyboard

- **Typing (500 ms pause):** `detectAsync` runs on `DetectionEngine`'s background thread. It takes tens of milliseconds. Measure it on the Tecno with the ai-local test app's speed test.
- **Send:** `detectSync` on the main thread. It reuses the answer from the pause check of the same text, so Send does not run the models again. If the user hits Send faster than the pause, it runs the full check once.
- **Itago:** masks every secret the AI finds: OTP and PIN become `••••••`, cards keep their last 4 digits. A private fact cannot be masked word by word; the banner warns instead. A saved value typed word for word is masked.
- **Teach me (Flutter):** unchanged. A new note works in the keyboard on the next check, because the answers are cached per saved-notes version.

## Keeping in sync

When the AI side changes something, run the same command again. `install.ps1 -Check` lists what differs without copying. Do not edit the copied files in `Android App`; ask the AI side to change them in `ai-local`, so the next copy does not undo your fix.

## Test it (synthetic text only, Rule Zero)

| Type in any chat | Expect |
|---|---|
| `Anak, eto yung code na pinadala sa akin: 482913` | red, OTP |
| `Bayad na po via GCash. Ref No. 1023 4567 89016, salamat!` | quiet (the regex alone used to warn) |
| `I forgot my password, can you help me reset it?` | quiet (the regex alone used to go red) |
| Teach `4821` labelled GCash MPIN, then type `eto na yung 4821, bilisan mo` | red, "Ito yung tinuro mong sikreto: GCash MPIN" |
| Teach `Si Jakim ay nakatira sa Taguig`, then type `Let's raid Jakim in Taguig` | red, private note |
| `Pa-GCash na lang sa 09171234567` | yellow, "Sinadya mo ba?" |

Release build crash on model load: add `-keep class ai.onnxruntime.** { *; }` to `android/app/proguard-rules.pro`.
