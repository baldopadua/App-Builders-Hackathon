# Downloads the candidate model files from Hugging Face into assets/models/. Not run automatically (~52 MB total).
# Usage: powershell -File scripts\download-models.ps1
$root = Split-Path -Parent $PSScriptRoot

$models = @(
  @{ repo = "onnx-community/bert-small-pii-detection-ONNX"; dir = "pii";       onnx = "onnx/model_quantized.onnx" },
  @{ repo = "Xenova/all-MiniLM-L6-v2";                      dir = "embedding"; onnx = "onnx/model_quantized.onnx" }
)

foreach ($m in $models) {
  $dest = Join-Path $root "assets\models\$($m.dir)"
  New-Item -ItemType Directory -Force $dest | Out-Null
  $files = @($m.onnx, "tokenizer.json", "vocab.txt", "config.json", "tokenizer_config.json", "special_tokens_map.json")
  foreach ($f in $files) {
    $name = Split-Path -Leaf $f
    $url = "https://huggingface.co/$($m.repo)/resolve/main/$f"
    Write-Host "Downloading $($m.repo) / $f"
    Invoke-WebRequest -Uri $url -OutFile (Join-Path $dest $name)
  }
}
Write-Host "Done. Check licenses and verify the models on the demo phone."
