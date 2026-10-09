# Putting `ai-local` into the Flutter app (Klarenz)

The team manual (`ARCHITECTURE.md`, sections 4, 9 and 10) has the full code. In short, inside the Flutter project (`flutter create --org com.secondlook secondlook`):

1. **Kotlin sources**: copy `kotlin/com/secondlook/` (`DetectionEngine.kt`, `SecretManager.kt` and the `ai/` folder) to `android/app/src/main/kotlin/com/secondlook/`. Don't edit them; ask Annie for changes.
2. **Models**: copy `assets/models/` to `android/app/src/main/assets/models/`.
3. **Gradle** (`android/app/build.gradle.kts`): `minSdk = 26`, `implementation("com.microsoft.onnxruntime:onnxruntime-android:1.26.0")` and `androidResources { noCompress += "onnx" }`. `androidx.security:security-crypto` is not needed.
4. **Keyboard**: call `DetectionEngine.get(this)` in `onCreate`. Call `detectAsync(text)` after a 500 ms typing pause **and** from `onUpdateSelection` (so pasted text is checked). Use `hide(text, warning)` for Itago. Never call `detect` on the main thread.
5. **Bridge**: paste the commented code in `ai/bridge/AiMethodChannel.kt` into the Flutter app and register it in `MainActivity.configureFlutterEngine`.
6. **Manifest**: use the fully-qualified name `com.secondlook.SecondLookIME`, and `import com.secondlook.secondlook.R` in the IME. No INTERNET permission.
7. **Release build**: if it crashes when the models load, add `-keep class ai.onnxruntime.** { *; }` to `android/app/proguard-rules.pro`. Check permissions with `aapt dump permissions`.

Until the models finish loading (about 1.3 s on the laptop; not yet measured on the phone), `detect` runs the rules only, so typing never waits.
