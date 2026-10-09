# Copies the ai-local AI core into the keyboard app (Klarenz runs this; it never deletes anything of his).
#   powershell -ExecutionPolicy Bypass -File "AI Model\ai-local\handoff\android-app\install.ps1"          copy
#   powershell -ExecutionPolicy Bypass -File "AI Model\ai-local\handoff\android-app\install.ps1" -Check   only list what differs
# Copies:  AI Model/ai-local/kotlin/com/secondlook/ai/**   ->  Android App/android/app/src/main/kotlin/com/secondlook/ai/
#          handoff/android-app/OnnxAiAdapter.kt, SecretManagerNotesStore.kt -> same folder
# Never copies ai-local's own DetectionEngine.kt / SecretManager.kt (the keyboard keeps Klarenz's).
# Never touches AiDetectionAdapter.kt, RuleBasedAiAdapter.kt or any file outside that ai/ folder.
param([string]$AndroidApp, [switch]$Check)
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$aiLocal = (Resolve-Path (Join-Path $here '..\..')).Path
if (-not $AndroidApp) { $AndroidApp = Join-Path (Split-Path -Parent (Split-Path -Parent $aiLocal)) 'Android App' }
$dest = Join-Path $AndroidApp 'android\app\src\main\kotlin\com\secondlook\ai'
if (-not (Test-Path (Join-Path $AndroidApp 'android\app\build.gradle.kts'))) { throw "Not the keyboard app: $AndroidApp" }

$core = Join-Path $aiLocal 'kotlin\com\secondlook\ai'
$files = [ordered]@{}
Get-ChildItem $core -Recurse -File -Filter *.kt | Sort-Object FullName | ForEach-Object { $files[$_.FullName.Substring($core.Length + 1)] = $_.FullName }
foreach ($f in 'OnnxAiAdapter.kt', 'SecretManagerNotesStore.kt') { $files[$f] = Join-Path $here $f }

$changed = 0
foreach ($rel in $files.Keys) {
    $src = $files[$rel]; $dst = Join-Path $dest $rel
    $same = (Test-Path $dst) -and ((Get-FileHash $src).Hash -eq (Get-FileHash $dst).Hash)
    if ($same) { continue }
    $changed++
    if ($Check) { Write-Host "differs: ai\$rel" ; continue }
    New-Item -ItemType Directory -Force (Split-Path $dst) | Out-Null
    Copy-Item $src $dst -Force
    Write-Host "copied:  ai\$rel"
}
if ($Check) { Write-Host "$changed file(s) differ from ai-local." } else { Write-Host "$changed file(s) copied, $($files.Count - $changed) already up to date." }

$gradle = Get-Content (Join-Path $AndroidApp 'android\app\build.gradle.kts') -Raw
if ($gradle -notmatch 'ai-local/assets') {
    Write-Host ''
    Write-Host 'Models are not bundled yet. In Android App\android\app\build.gradle.kts, inside android { }, add:' -ForegroundColor Yellow
    Write-Host '    sourceSets { getByName("main") { assets.srcDir("../../../AI Model/ai-local/assets") } }' -ForegroundColor Yellow
}
