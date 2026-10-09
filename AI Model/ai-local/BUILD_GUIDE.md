# SecondLook AI Test App - Build & Test Guide

**For the whole team:** Quick, portable builds on Windows. Works on any dev machine with Java + Android SDK.

---

## Quick Start (Pick One)

### 🚀 Fastest Option: Interactive Menu
```powershell
cd "AI Model\ai-local"
.\BUILD_AND_TEST.ps1
```
Choose option **2** to build + install on phone.

### ⚡ Command-Line (No Menu)
```powershell
cd "AI Model\ai-local"
.\BUILD_AND_TEST.ps1 -Install        # Build debug APK and install on phone
.\BUILD_AND_TEST.ps1 -Quick          # Fast build with low memory
.\BUILD_AND_TEST.ps1 -Clean          # Clean cache and rebuild
```

### 🔧 Manual (Old Way, Still Works)
```powershell
cd "AI Model\ai-local\test-app"
.\gradlew.bat assembleDebug
```

---

## Before First Build

### 1. **Install Prerequisites**

- **JDK 17+** → Download from [Adoptium](https://adoptium.net/)
- **Android SDK** → Already have it? Run Android Studio once, it sets up automatically
- **USB Cable** → For phone testing

### 2. **Setup Phone (One Time)**

1. Connect phone via USB
2. Go to **Settings → About Phone**
3. Tap **Build Number** 7 times (rapid taps)
4. Back up → **Developer Options** → Enable **USB Debugging**
5. On computer, you'll see a prompt on the phone: tap **Allow**

### 3. **Test Prerequisites**

```powershell
java -version                    # Should show JDK 17+
adb devices                      # Should show your phone
echo $env:ANDROID_HOME           # Should show Android SDK path
```

If `adb` command not found, Android SDK isn't in PATH. Android Studio usually fixes this automatically.

---

## Build Profiles (Choose Your Speed)

| Profile | Memory | Build Time | Best For |
|---------|--------|-----------|----------|
| **Balanced** (default) | 3GB | 2-3 min | Most dev machines |
| **Conservative** (`-Quick`) | 2GB | 3-5 min | Older laptops / low RAM |
| **Aggressive** | 5GB | 1-2 min | High-end machines (8GB+ RAM) |

The script auto-selects **Balanced**. If you get `out of memory` errors, use `-Quick`.

---

## What Happens During Build

1. **Gradle daemon** starts with memory limits
2. **Kotlin code** from `kotlin/` is compiled
3. **ONNX models** from `assets/` are packaged
4. **APK** (~160 MB debug, 85 MB release) is generated
5. **ADB** pushes APK to phone (if `-Install`)
6. App launches automatically

First build is slower (gradle setup). Subsequent builds are cached.

---

## Troubleshooting

### ❌ "Out of Memory" Error
```powershell
.\BUILD_AND_TEST.ps1 -Quick          # Use conservative memory settings
# or
.\BUILD_AND_TEST.ps1 -Clean          # Clean cache, retry
```
**Close other apps** (VS Code, browser tabs) to free RAM.

### ❌ "ADB not found"
```powershell
$env:ANDROID_HOME                   # Check if set
# If not set, add to PowerShell profile:
$env:ANDROID_HOME = "C:\Users\<YourUsername>\AppData\Local\Android\Sdk"
```

### ❌ "No devices found"
1. Check `adb devices` in terminal (not in script)
2. Unlock phone (sometimes stays locked in timeout)
3. On phone, tap **Allow** if USB Debugging prompt appears
4. Try `adb kill-server` then `adb devices` again

### ❌ "daemon disappeared unexpectedly"
```powershell
.\BUILD_AND_TEST.ps1 -Clean
```

---

## Testing on Phone

After install, app opens automatically. Follow **TESTING.md** for test cases:

1. **Model load time** (should show in status)
2. **Airplane mode** (test that offline works)
3. **Tap samples** (test detection)
4. **Itago** (hide secrets)
5. **Teach** (teach new secrets)

---

## For CI/CD or Headless Build

```powershell
cd "AI Model\ai-local"

# Build only (no install)
.\gradlew.bat assembleDebug -Pprofile=balanced

# Output
# APK: test-app\app\build\outputs\apk\debug\app-debug.apk

# Or use script's exit code
.\BUILD_AND_TEST.ps1 -Quick
if ($LASTEXITCODE -eq 0) { Write-Host "Success" }
```

---

## Performance Tips

- **First build:** ~3-5 min (gradle downloads deps)
- **Incremental build:** ~30-60 sec (changes only)
- **Clean build:** ~3 min (everything from scratch)

**Speed up incremental builds:**
- Change only Kotlin code (avoid model changes)
- Keep phone connected (ADB install is instant)
- Run script multiple times (gradle caches compile outputs)

---

## What Gets Tested

| Layer | Test | Output |
|-------|------|--------|
| **Kotlin code** | Compiles & links | No build errors |
| **Models** | Bundled in APK | App can load them on phone |
| **Performance** | Real phone execution | "All stages: X ms" |
| **Offline** | Airplane mode works | No network calls |
| **Storage** | Taught secrets persist | Survives app restart |

---

## Share With Team

**Copy these files to team repo:**
- `BUILD_AND_TEST.ps1` → Use this to build locally
- `BUILD_GUIDE.md` → This doc
- `TESTING.md` → What to test after install

**Team members do:**
```powershell
git pull
cd "AI Model\ai-local"
.\BUILD_AND_TEST.ps1 -Install
```

Done. No setup docs needed after first pull.

---

## Questions?

- **Build fails:** Run `.\BUILD_AND_TEST.ps1 -Clean` then check logs in `test-app\.gradle\daemon\`
- **Phone not detected:** Troubleshooting above → Check `adb devices`
- **Need USB driver:** Android Studio usually installs automatically. If not: download from phone maker's website.

---

## Advanced: Manual Build (Without Script)

```powershell
cd "AI Model\ai-local\test-app"

# Adjust memory in gradle.properties if needed
# org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=768m

# Build
.\gradlew.bat assembleDebug

# Install
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

**Last updated:** Oct 9, 2026  
**Tested on:** Windows 11, JDK 17, Android SDK 35
