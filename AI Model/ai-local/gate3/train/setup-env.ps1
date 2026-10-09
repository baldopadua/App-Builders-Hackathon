# Creates the isolated Gate 3 Python environment OUTSIDE the repo (default: <repo>\..\gate3-work).
# Everything it downloads (uv, Python, packages, cache) stays in that folder; delete the folder to undo.
#   powershell -ExecutionPolicy Bypass -File "AI Model\ai-local\gate3\train\setup-env.ps1"
# Override the location with $env:GATE3_WORK.
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = (Resolve-Path (Join-Path $here '..\..\..\..')).Path
$work = if ($env:GATE3_WORK) { $env:GATE3_WORK } else { Join-Path (Split-Path -Parent $repo) 'gate3-work' }
New-Item -ItemType Directory -Force $work | Out-Null
Write-Host "Gate 3 work folder: $work"

$uvVersion = '0.5.11'
$uvDir = Join-Path $work "tools\uv-$uvVersion"
$uv = Join-Path $uvDir 'uv.exe'
if (-not (Test-Path $uv)) {
    $zip = Join-Path $work "tools\uv-$uvVersion.zip"
    New-Item -ItemType Directory -Force (Split-Path $zip) | Out-Null
    $base = "https://github.com/astral-sh/uv/releases/download/$uvVersion/uv-x86_64-pc-windows-msvc.zip"
    Invoke-WebRequest "$base" -OutFile $zip -UseBasicParsing
    Invoke-WebRequest "$base.sha256" -OutFile "$zip.sha256" -UseBasicParsing
    $expected = ((Get-Content "$zip.sha256" -Raw).Trim() -split '\s+')[0]
    Remove-Item "$zip.sha256"
    $actual = (Get-FileHash $zip -Algorithm SHA256).Hash.ToLower()
    if ($actual -ne $expected.ToLower()) { throw "uv download hash mismatch: $actual vs $expected" }
    Expand-Archive $zip -DestinationPath $uvDir -Force
    Remove-Item $zip
}

$env:UV_CACHE_DIR = Join-Path $work 'uv-cache'
$env:UV_PYTHON_INSTALL_DIR = Join-Path $work 'python'
$venv = Join-Path $work 'venv'
& $uv python install 3.11
if ($LASTEXITCODE -ne 0) { throw 'uv python install failed' }
if (-not (Test-Path (Join-Path $venv 'Scripts\python.exe'))) {
    & $uv venv $venv --python 3.11
    if ($LASTEXITCODE -ne 0) { throw 'uv venv failed' }
}
& $uv pip install --python (Join-Path $venv 'Scripts\python.exe') --index-strategy unsafe-best-match -r (Join-Path $here 'requirements.txt')
if ($LASTEXITCODE -ne 0) { throw 'uv pip install failed' }
& (Join-Path $venv 'Scripts\python.exe') -c "import torch, transformers, onnx, onnxruntime, pyarrow; print('torch', torch.__version__, 'transformers', transformers.__version__, 'onnxruntime', onnxruntime.__version__)"
Write-Host "Ready. Python: $(Join-Path $venv 'Scripts\python.exe')"
