# How to download the model assets

The generated build outputs and experiment models are not part of the submission.
Only the runtime models under `assets/models/` are used by the Android app and
are intentionally kept in the repository so it can build offline.

## Runtime models

From this directory (`AI Model/ai-local`), run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\download-models.ps1
```

This downloads the two runtime models into:

```text
assets/models/pii/
assets/models/embedding/
```

The script downloads the model files from Hugging Face, including the
quantized ONNX files and tokenizer configuration. Review the model licenses
before distributing the app.

## Experimental models

Experimental models are ignored under `lab-models/`. Download them only when
running the browser experiments:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\download-lab-models.ps1
```

To download only selected experiments:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\download-lab-models.ps1 `
  -Only bge-small,nli-mobilebert
```

These models must not be packaged into the APK or committed to GitHub.

## Rebuild generated outputs locally

Build outputs are ignored and should be regenerated on each developer machine:

```powershell
cd .\test-app
.\gradlew.bat assembleDebug
```

The generated APK will be under `test-app\app\build\` and is intentionally not
tracked.
