# Downloads CANDIDATE models into lab-models/ (experiments only, never packaged in the APK).
# Usage: powershell -File scripts\download-lab-models.ps1 [-Only bge-small,nli-mobilebert]
# Check each model's license before bundling it; the Xenova repos are ONNX conversions of the original models.
param([string[]]$Only)
$root = Split-Path -Parent $PSScriptRoot
if ($Only) { $Only = $Only -split ',' }   # "-File" passes "a,b" as one string

$models = @(
  # embedding models (similarity)
  @{ repo = "Xenova/bge-small-en-v1.5";                      dir = "bge-small" },
  @{ repo = "Xenova/gte-small";                              dir = "gte-small" },
  @{ repo = "Xenova/all-MiniLM-L12-v2";                      dir = "minilm-l12" },
  @{ repo = "Xenova/paraphrase-multilingual-MiniLM-L12-v2";  dir = "paraphrase-multi" },
  @{ repo = "Xenova/bge-base-en-v1.5";                       dir = "bge-base" },
  # NLI models (does the draft imply the private fact?)
  @{ repo = "Xenova/mobilebert-uncased-mnli";                dir = "nli-mobilebert" },
  @{ repo = "Xenova/nli-deberta-v3-xsmall";                  dir = "nli-deberta-xsmall" },
  @{ repo = "Xenova/distilbert-base-uncased-mnli";           dir = "nli-distilbert" }
)

foreach ($m in $models) {
  if ($Only -and ($Only -notcontains $m.dir)) { continue }
  $dest = Join-Path $root "lab-models\$($m.dir)"
  New-Item -ItemType Directory -Force $dest | Out-Null
  foreach ($f in @("onnx/model_quantized.onnx", "tokenizer.json", "tokenizer_config.json", "config.json", "special_tokens_map.json", "vocab.txt")) {
    $name = Split-Path -Leaf $f
    try {
      Invoke-WebRequest -Uri "https://huggingface.co/$($m.repo)/resolve/main/$f" -OutFile (Join-Path $dest $name) -ErrorAction Stop
      Write-Host "ok      $($m.dir)/$name"
    } catch {
      Write-Host "missing $($m.dir)/$name"
    }
  }
}
