# Team Build Setup - Android Test App

**Status:** ✅ Ready to commit and share with team

## What You're Getting

Two portable build scripts + documentation:

| File | Purpose | Commit? |
|------|---------|---------|
| `build.bat` | Simple menu for Windows Command Prompt | ✅ YES |
| `BUILD_AND_TEST.ps1` | Advanced PowerShell script with memory management | ✅ YES |
| `BUILD_GUIDE.md` | Team documentation | ✅ YES |
| `TESTING.md` | What to test on the phone (existing) | ✅ YES |

## What Gets Ignored (Not Committed)

| Item | Type | Reason |
|------|------|--------|
| `build/` | Directory | Build output |
| `.gradle/` | Directory | Gradle cache |
| `.kotlin/` | Directory | Kotlin compiler cache |
| `*.log` | Files | Build logs, JVM crash logs |
| `*.apk` | Files | Built APK files |
| `local.properties` | File | Local SDK paths (varies per dev) |

**Cleaned:** Removed old `hs_err_pid*.log` and `replay_*.log` files.

## For Team Members

### First Time Setup

1. **Install JDK 17+** (one time)
   ```
   Download: https://adoptium.net/
   ```

2. **Clone repo and navigate**
   ```powershell
   git clone <repo>
   cd "App-Builders-Hackathon\AI Model\ai-local"
   ```

3. **Enable USB Debugging on phone** (one time)
   - Settings → About Phone → Build Number (tap 7x) → Developer Options → USB Debugging ON

### Build & Test

**Option A: Simple menu (Command Prompt)**
```cmd
build.bat
```
Choose option 2 to build + install.

**Option B: PowerShell with options**
```powershell
.\BUILD_AND_TEST.ps1 -Install        # Build and install on phone
.\BUILD_AND_TEST.ps1 -Quick          # Fast build with low memory
.\BUILD_AND_TEST.ps1 -Clean          # Clean cache, retry
```

**Option C: Manual (if needed)**
```powershell
cd test-app
.\gradlew.bat assembleDebug
```

## What Happens During Build

1. **First build:** ~3-5 minutes (gradle downloads dependencies)
2. **Incremental builds:** ~1-2 minutes (changes only)
3. **APK size:** ~160 MB debug (includes all ONNX models + runtime)

## Troubleshooting

| Problem | Solution |
|---------|----------|
| **Out of memory** | Run `build.bat` option 4 (clean) or use `-Quick` flag |
| **No devices found** | Check `adb devices`, unlock phone, check USB debugging |
| **Port already in use** | Close other Java/Gradle processes |
| **Build hangs** | Ctrl+C, then run clean rebuild |

## Git Workflow

**Team member:**
```bash
git pull                      # Get scripts
build.bat install             # Build locally, creates APK on their machine
# No commits needed - APK files are in .gitignore
```

**What they push:**
```bash
git push                      # Their code changes (no APK files)
```

## Sharing With More Team Members

Just tell them:
```
git pull
cd "AI Model\ai-local"
build.bat
```

Done. No extra setup needed.

---

**Notes:**
- All log files are now `.gitignored` - they won't pollute commits
- Build scripts are portable - they work on any Windows machine with JDK + Android SDK
- APK files stay local - no bloating the repo
- First team member's build takes longer (gradle setup), subsequent builds are fast

---

**Test**: `build.bat` ready to use now!
