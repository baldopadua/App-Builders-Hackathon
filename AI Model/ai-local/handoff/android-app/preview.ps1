# Builds a PREVIEW of the real keyboard app (Klarenz's Flutter UI, keyboard, banner, Ingat Mode) with the AI handoff
# applied, in a copy OUTSIDE the repo, so the team can see and test the result before anyone pastes anything.
#   powershell -ExecutionPolicy Bypass -File "AI Model\ai-local\handoff\android-app\preview.ps1"
#   -> <repo>\..\gate3-work\handoff-preview\Android App\build\app\outputs\flutter-apk\app-debug.apk
# Steps on the copy: install.ps1, the two snippets (DetectionEngine, MainActivity "check"), the Gradle assets line.
# Android App itself is only read. Rerun after either side changes. Needs Flutter + the Android SDK.
param([string]$Out, [string]$GradleHeap = '2G', [switch]$NoBuild)
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$aiLocal = (Resolve-Path (Join-Path $here '..\..')).Path
$repo = Split-Path -Parent (Split-Path -Parent $aiLocal)
$work = if ($env:GATE3_WORK) { $env:GATE3_WORK } else { Join-Path (Split-Path -Parent $repo) 'gate3-work' }
if (-not $Out) { $Out = Join-Path $work 'handoff-preview' }
$app = Join-Path $Out 'Android App'

# 1. fresh copy of the keyboard app (no build outputs)
if (Test-Path $app) { Remove-Item $app -Recurse -Force }
robocopy (Join-Path $repo 'Android App') $app /E /XD build .dart_tool .gradle .idea /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw "copy failed ($LASTEXITCODE)" }

# 2. the handoff files
& powershell -ExecutionPolicy Bypass -File (Join-Path $here 'install.ps1') -AndroidApp $app | Out-Null

$src = Join-Path $app 'android\app\src\main\kotlin\com\secondlook'
$utf8 = New-Object System.Text.UTF8Encoding $false   # no BOM (a BOM breaks the first key of gradle.properties)
function Patch([string]$file, [string]$pattern, [string]$replacement, [string]$what) {
    $text = [IO.File]::ReadAllText($file)
    $new = [regex]::Replace($text, $pattern, $replacement)
    if ($new -eq $text) { Write-Warning "$what : pattern not found, left as is"; return }
    [IO.File]::WriteAllText($file, $new, $utf8)
    Write-Host "applied: $what"
}

# 3. snippets/DetectionEngine.kt.txt: the AI core answers alone once active
Patch (Join-Path $src 'DetectionEngine.kt') 'activeAdapter\.detectSync\(text\) \?: fallbackAdapter\.detectSync\(text\)' 'activeAdapter.detectSync(text)' 'DetectionEngine: no regex after the AI'

# 4. snippets/MainActivity-check.kt.txt: Flutter "check" off the main thread + the AI's category
$main = Join-Path $src 'secondlook\MainActivity.kt'
Patch $main '(?s)"check" -> \{.*?(?=\s*"hide" -> \{)' @'
"check" -> {
                    val text = call.argument<String>("text") ?: ""
                    checkWorker.execute {
                        val warning = DetectionEngine.detectSync(text)
                        val reply = warning?.let {
                            mapOf(
                                "type" to it.type.name,
                                "headline" to it.headline,
                                "tip" to it.tip,
                                "category" to (com.secondlook.ai.OnnxAiAdapter.categoryFor(text) ?: legacyCategory(text)),
                            )
                        }
                        mainHandler.post { result.success(reply) }
                    }
                }
'@ 'MainActivity: check off the main thread'
Patch $main '(class MainActivity : FlutterActivity\(\) \{\s*\n\s*private val CHANNEL = "secondlook/ai")' @'
$1
    private val checkWorker = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun legacyCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("otp") || lower.contains("pin") || Regex("""\b\d{4,8}\b""").containsMatchIn(text) -> "OTP / PIN protection"
            lower.contains("password") -> "Password protection"
            lower.contains("aiza") || lower.contains("api key") || lower.contains("gemini") -> "API Key protection"
            SecretManager.containsProtectedSecret(text) -> "Vault Secret protection"
            else -> "Data protection"
        }
    }
'@ 'MainActivity: worker + legacyCategory'

# 5. bundle the models (absolute path: this copy is not inside the repo)
$assets = (Join-Path $aiLocal 'assets') -replace '\\', '/'
Patch (Join-Path $app 'android\app\build.gradle.kts') '(\n\s*buildTypes \{)' "`n    sourceSets { getByName(`"main`") { assets.srcDir(`"$assets`") } }`n`$1" 'build.gradle.kts: models from ai-local/assets'
Patch (Join-Path $app 'android\gradle.properties') '(?m)^org\.gradle\.jvmargs=.*$' "org.gradle.jvmargs=-Xmx$GradleHeap -XX:MaxMetaspaceSize=768m" 'gradle.properties: smaller Gradle heap'

if ($NoBuild) { Write-Host "Prepared $app"; return }
Push-Location $app
try {
    flutter pub get; if ($LASTEXITCODE -ne 0) { throw 'flutter pub get failed' }
    flutter build apk --debug; if ($LASTEXITCODE -ne 0) { throw 'flutter build failed' }
} finally { Pop-Location }
Write-Host "APK: $(Join-Path $app 'build\app\outputs\flutter-apk\app-debug.apk')"
