# 📦 TanggolIn Pre-Built APK

This directory contains the pre-compiled Android package for **TanggolIn (SecondLook Keyboard)**:

- **Filename:** `TanggolIn-SecondLook.apk`
- **Architecture:** Universal bundle (`arm64-v8a`, `armeabi-v7a`, `x86_64`)
- **Min SDK:** Android 8.0 (API 26+)
- **Target SDK:** Android 14 (API 34)
- **Features Included:**
  - Full Native Kotlin `InputMethodService` (SecondLook Keyboard)
  - Embedded Microsoft ONNX Runtime Mobile (`1.26.0`)
  - Embedded INT8 Quantized MiniLM Sentence Embedding Model (`~23 MB`)
  - Embedded INT8 Quantized PII Token Classification Model (`~28 MB`)
  - Full Flutter Companion Application & Safe Practice Playground

### Installation via ADB:
```bash
adb install TanggolIn-SecondLook.apk
```

*(The APK is also mirrored at the root of the repository as `./TanggolIn-SecondLook.apk`)*
