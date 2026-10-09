@echo off
REM ============================================================================
REM SecondLook AI Test App - Simple Build Wrapper
REM For Windows Command Prompt (cmd.exe)
REM Usage:
REM   build.bat                  - Interactive menu
REM   build.bat debug            - Build debug APK
REM   build.bat release          - Build release APK
REM   build.bat install          - Build debug + install on phone
REM   build.bat clean            - Clean build artifacts
REM ============================================================================

setlocal enabledelayedexpansion

REM Get script directory
set SCRIPT_DIR=%~dp0
set TEST_APP_DIR=%SCRIPT_DIR%test-app

REM Colors (Windows 10+)
set GREEN=[92m
set YELLOW=[93m
set RED=[91m
set BLUE=[94m
set RESET=[0m

REM Ensure we're in the right directory
pushd "%TEST_APP_DIR%"

REM Parse arguments
if "%1"=="" goto menu
if "%1"=="debug" goto build_debug
if "%1"=="release" goto build_release
if "%1"=="install" goto build_install
if "%1"=="clean" goto do_clean
goto menu

:menu
cls
echo.
echo [94m===================================================[0m
echo [94mSecondLook AI Test App Builder[0m
echo [94m===================================================[0m
echo.
echo 1. Build Debug APK (default, faster)
echo 2. Build ^& Install on Phone
echo 3. Build Release APK
echo 4. Clean ^& Rebuild
echo 5. Exit
echo.
set /p choice="Choose (1-5): "

if "%choice%"=="1" goto build_debug
if "%choice%"=="2" goto build_install
if "%choice%"=="3" goto build_release
if "%choice%"=="4" goto do_clean
if "%choice%"=="5" goto end
echo Invalid choice
goto menu

:stop_daemons
echo [93mStopping Gradle daemons...[0m
call gradlew.bat --stop >nul 2>&1
timeout /t 2 /nobreak >nul
goto :eof

:build_debug
echo [94m===================================================[0m
echo [94mBuilding Debug APK[0m
echo [94m===================================================[0m
echo [93mConfiguring memory: 2GB (conservative)[0m
(
echo org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
echo org.gradle.parallel=false
echo org.gradle.workers.max=1
echo org.gradle.caching=true
echo android.useAndroidX=false
echo android.nonTransitiveRClass=true
echo kotlin.code.style=official
) > gradle.properties
call :stop_daemons
echo.
echo [93mBuilding APK (this may take 2-5 minutes)...[0m
call gradlew.bat assembleDebug
if %errorlevel% equ 0 (
    echo [92m✓ Build successful![0m
    echo [92m✓ APK: %TEST_APP_DIR%\app\build\outputs\apk\debug\app-debug.apk[0m
) else (
    echo [91m✗ Build failed[0m
    echo [93mTroubleshooting:[0m
    echo   1. Close other apps and try again
    echo   2. Run: build.bat clean
    echo   3. Check: %USERPROFILE%\.gradle\daemon for logs
)
pause
if "%1"=="" goto menu
goto end

:build_release
echo [94m===================================================[0m
echo [94mBuilding Release APK[0m
echo [94m===================================================[0m
echo [93mConfiguring memory: 3GB (balanced)[0m
(
echo org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8
echo org.gradle.parallel=true
echo org.gradle.workers.max=2
echo org.gradle.caching=true
echo android.useAndroidX=false
echo android.nonTransitiveRClass=true
echo kotlin.code.style=official
) > gradle.properties
call :stop_daemons
echo.
echo [93mBuilding APK (this may take 2-5 minutes)...[0m
call gradlew.bat assembleRelease
if %errorlevel% equ 0 (
    echo [92m✓ Build successful![0m
    echo [92m✓ APK: %TEST_APP_DIR%\app\build\outputs\apk\release\app-release-unsigned.apk[0m
) else (
    echo [91m✗ Build failed[0m
)
pause
if "%1"=="" goto menu
goto end

:build_install
echo [94m===================================================[0m
echo [94mBuild ^& Install on Phone[0m
echo [94m===================================================[0m
echo [93mConfiguring memory: 2GB[0m
(
echo org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
echo org.gradle.parallel=false
echo org.gradle.workers.max=1
echo org.gradle.caching=true
echo android.useAndroidX=false
echo android.nonTransitiveRClass=true
echo kotlin.code.style=official
) > gradle.properties
call :stop_daemons
echo.
echo [93mBuilding APK...[0m
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo [91m✗ Build failed[0m
    pause
    goto end
)
echo [92m✓ Build successful[0m
echo.

REM Find ADB
set ADB_PATH=
if defined ANDROID_HOME (
    if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
        set ADB_PATH=%ANDROID_HOME%\platform-tools\adb.exe
    )
)
if not defined ADB_PATH (
    for /f "delims=" %%A in ('where adb 2^>nul') do set ADB_PATH=%%A
)

if not defined ADB_PATH (
    echo [91m✗ ADB not found[0m
    echo [93mSetup:[0m
    echo   1. Set ANDROID_HOME environment variable
    echo   2. Or install Android Studio
    pause
    goto end
)

echo [93mLooking for connected devices...[0m
"%ADB_PATH%" devices > nul
echo.

echo [93mInstalling APK...[0m
set APK=%TEST_APP_DIR%\app\build\outputs\apk\debug\app-debug.apk
"%ADB_PATH%" install -r "%APK%"

if %errorlevel% equ 0 (
    echo [92m✓ Installation successful![0m
    echo [92m✓ App should open automatically[0m
    echo.
    echo [93mNext: Follow TESTING.md for test cases[0m
) else (
    echo [91m✗ Installation failed[0m
    echo [93mHints:[0m
    echo   1. Unlock your phone
    echo   2. Check USB debugging is enabled
    echo   3. Run: "%ADB_PATH%" devices
)
pause
if "%1"=="" goto menu
goto end

:do_clean
echo [94m===================================================[0m
echo [94mCleaning Build[0m
echo [94m===================================================[0m
call :stop_daemons
echo [93mDeleting build artifacts...[0m
call gradlew.bat clean
if %errorlevel% equ 0 (
    echo [92m✓ Clean successful[0m
) else (
    echo [93m⚠ Clean completed with warnings (non-critical)[0m
)
pause
if "%1"=="" goto menu
goto end

:end
popd
endlocal
