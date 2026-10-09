# Testing the SecondLook AI core

**For:** Jakim (Test & Identity Lead), Klarenz, Charles, and any AI assistant helping them.
**Rule zero:** all test data is synthetic. Never type a real PIN, password, OTP, card number, ID or key, not even "just to test". Screenshots, videos and git history keep them.

## 1. Four ways to test

| Where | What it proves | Time | Needs |
| --- | --- | --- | --- |
| Laptop check (`jvm-check`) | The real Kotlin code and the real models pass every test case | ~2 min | JDK 17 |
| Browser demo + scorer (`web-demo`) | Same results in a browser; try messages by hand; proof that nothing goes online | ~1 min after `npm install` | Node.js 18+ |
| Android test app (`test-app`) | The AI core runs on the phone: load time, speed, airplane mode, Itago, Teach | ~2 min build | Android SDK, USB cable |
| The real keyboard (Klarenz's app) | Warnings inside Messages / Messenger, the Send hold, paste | after Checkpoint 3 | the keyboard APK |

All commands are for Windows PowerShell, starting from the repo root (`App-Builders-Hackathon`). The folder `AI Model` has a space in its name, so keep the quotes.

## 2. Laptop check (Kotlin + real models)

```powershell
cd "AI Model\ai-local\jvm-check"
.\gradlew.bat run
```

The end of the output should look like this (results as of Oct 9, 9:15 PM):

```
   rules + model: 146/146 correct
   87/87 come back clean after Itago
   flagged 14/18 real paraphrases, 3/22 false alarms
   27/27 taught-secret checks correct
   wow check: "eto na yung 4821, bilisan mo"
      before teaching: no warning
      after teaching '4821' as GCash MPIN: my_secret
ALL DETECTION CHECKS PASSED
```

The `FAIL` lines inside section 3 (private notes) are the known misses and false alarms, so they are expected. Any other `FAIL` means something broke: tell Annie.

If it breaks:
- `Incremental compilation failed` or `Truncated class file`: run `.\gradlew.bat clean run '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false'`
- `insufficient memory` or `paging file is too small`: close other apps, run `.\gradlew.bat --stop`, then try again.

## 3. Browser demo and scorer

```powershell
cd "AI Model\ai-local\web-demo"
npm install        # first time only
npm start          # then open http://127.0.0.1:5173
```

- **http://127.0.0.1:5173**: type a message or tap a sample, try **Itago**, and try **Teach**: "Fill MPIN example (4821)" → **Save** → tap the "No clue words" sample. The line *"Requests to other sites since page load: 0 ✓"* proves nothing went online.
- **http://127.0.0.1:5173/eval.html**: scores every test case and should give the same numbers as the laptop check. Wait for "Done." at the top.
- `lab.html` and `nli.html` are model experiments. They need `scripts\download-lab-models.ps1` first (those models are not in git).

Stop the server with Ctrl+C. "Port 5173 in use" means an old server is still running.

## 4. Android test app (on the Tecno)

On the phone, turn on USB debugging once: Settings → About phone → tap **Build number** 7 times → Developer options → **USB debugging**. Then build and install:

```powershell
cd "AI Model\ai-local\test-app"
.\gradlew.bat assembleDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

The debug APK is big (~160 MB): it carries ONNX Runtime for every phone CPU type plus the 50 MB of models.

On the phone, write every result in the bug log:

1. Open **SecondLook AI test**. The status line shows the model load time ("PII … ms, embeddings … ms") and "INTERNET permission: not declared". **Write both down.**
2. Turn **airplane mode ON**. Everything below must still work.
3. Tap each sample button. Check the Filipino headline, the colour (red = secret, yellow = "Intended?") and the tip. Under the result, "All stages: N ms" is the time of one check on this phone. **Write down a typical N.**
4. Tap **Itago** on a red warning: the secret becomes `••••` and the warning goes away.
5. **Teach a secret value:** "Fill MPIN example (4821)" → **Save** → tap "No clue words (teach 4821 first)" → red *"Teka! Ito yung tinuro mong sikreto: GCash MPIN."* Delete it afterwards.
6. **Teach a private fact:** "Fill Jakim example" → **Save** → type *"Let's raid Jakim in Taguig"* → warning; *"Jakim is a great developer"* → no warning.
7. Close the app fully and reopen it: what you taught is still listed (the encrypted storage works).

Only quote numbers you measured yourself. The briefing warns that fake benchmarks can get results disputed.

## 5. Inside the real keyboard (after Klarenz's Checkpoint 3)

Run the team manual's section 8 cases (`ARCHITECTURE.md`), groups **I, J and K first**, then A–H, in Google Messages and Messenger with airplane mode ON. Also test **paste**: copy *"Anak, eto yung code na pinadala sa akin: 482913"*, paste it into a chat, and expect a red warning within about a second.

Log every case like this:

| # | App | PASS / FAIL | What happened | Fixed? |
| --- | --- | --- | --- | --- |
| 59 | Messages | PASS | red warning, Itago worked | – |

## 6. Add a test case (when you find a miss or a false alarm)

1. Open `test-data/samples.json` and add a line to `"detect"`:

   ```json
   { "text": "Passcode ko 0517, ikaw na bahala", "expect": "PASSWORD", "tags": ["gcash", "pin", "filipino"] }
   ```

   `expect` is one of: `PASSWORD` (passwords, PINs, OTPs), `API_KEY`, `CARD`, `OTHER_PII` (IDs, phone, email, bank account), `PRIVATE_NOTE`, or `none` (must stay quiet).
2. For every new message that should warn, add a near-miss that must **not** warn (`"expect": "none"`), e.g. *"Paano palitan ang MPIN sa GCash?"*.
3. Run the laptop check **and** the browser scorer. Both must agree. A new `FAIL` means the rules need a change. Tell Annie: the rules live in `kotlin/com/secondlook/ai/rules/RuleDetector.kt` and its twin `web-demo/public/rules.js`, and both must change together.

The other sections of `samples.json` work the same way: `"notes"` (a private fact plus drafts that should or should not warn) and `"taught"` (a taught value plus drafts).

## 7. Before demo day

- The release APK has **no INTERNET permission** (manual, section 17: `aapt dump permissions`).
- Airplane mode ON for the whole demo. Teach only the fake MPIN `4821` on a phone that is mirrored on stage.
- Never log or save what is typed, and never commit real keys. If GitHub blocks a push because a fake test key "looks real", mark it as a test value on the page GitHub shows.
