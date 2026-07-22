@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem Builds Ncy Android APKs without Android Studio.
rem Usage: build-apks.bat [debug^|release^|all] [--arm64] [--skip-pear] [--no-open]

cd /d "%~dp0"
set "BUILD_MODE=debug"
set "SKIP_PEAR=0"
set "OPEN_OUTPUT=1"
set "ARM64_ONLY=0"

:parse_args
if "%~1"=="" goto :args_done
if /I "%~1"=="debug" (
  set "BUILD_MODE=debug"
  shift /1
  goto :parse_args
)
if /I "%~1"=="release" (
  set "BUILD_MODE=release"
  shift /1
  goto :parse_args
)
if /I "%~1"=="all" (
  set "BUILD_MODE=all"
  shift /1
  goto :parse_args
)
if /I "%~1"=="--skip-pear" (
  set "SKIP_PEAR=1"
  shift /1
  goto :parse_args
)
if /I "%~1"=="--arm64" (
  set "ARM64_ONLY=1"
  shift /1
  goto :parse_args
)
if /I "%~1"=="--no-open" (
  set "OPEN_OUTPUT=0"
  shift /1
  goto :parse_args
)
goto :usage

:args_done
if /I not "%BUILD_MODE%"=="debug" if /I not "%BUILD_MODE%"=="release" if /I not "%BUILD_MODE%"=="all" goto :usage

echo.
echo ============================================================
echo  Ncy Android APK builder
echo  Mode: %BUILD_MODE%
if "%ARM64_ONLY%"=="1" echo  ABI:  arm64-v8a only
echo ============================================================
echo.

where java.exe >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java was not found.
  echo Install JDK 17 or newer, or add its bin folder to PATH.
  goto :failed
)

set "GRADLE_CMD="
if defined GRADLE_HOME if exist "%GRADLE_HOME%\bin\gradle.bat" set "GRADLE_CMD=%GRADLE_HOME%\bin\gradle.bat"
if not defined GRADLE_CMD (
  for /f "delims=" %%G in ('where gradle.bat 2^>nul') do if not defined GRADLE_CMD set "GRADLE_CMD=%%G"
)
if not defined GRADLE_CMD (
  for /r "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.13-bin" %%G in (gradle.bat) do (
    if exist "%%~fG" if /I "%%~nxG"=="gradle.bat" if not defined GRADLE_CMD set "GRADLE_CMD=%%~fG"
  )
)
if not defined GRADLE_CMD (
  echo ERROR: Gradle 8.13 was not found.
  echo Expected it under %%USERPROFILE%%\.gradle\wrapper\dists\gradle-8.13-bin,
  echo or set GRADLE_HOME to a Gradle 8.13 installation.
  goto :failed
)

if not exist "local.properties" (
  if defined ANDROID_HOME (
    >"local.properties" echo sdk.dir=!ANDROID_HOME:\=\\!
  ) else if exist "%LOCALAPPDATA%\Android\Sdk" (
    >"local.properties" echo sdk.dir=!LOCALAPPDATA:\=\\!\\Android\\Sdk
  ) else (
    echo ERROR: Android SDK not found.
    echo Set ANDROID_HOME or create local.properties with sdk.dir=...
    goto :failed
  )
)

if "%SKIP_PEAR%"=="0" (
  echo [1/3] Rebuilding the Pear worker and native addon package...
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "scripts\build_pear_worker.ps1" -RepositoryRoot "%CD%"
  if errorlevel 1 (
    echo ERROR: Pear worker build failed.
    goto :failed
  )
) else (
  echo [1/3] Keeping the existing Pear worker assets ^(--skip-pear^).
)

set "GRADLE_TASKS="
if /I "%BUILD_MODE%"=="debug" set "GRADLE_TASKS=assembleProductionDebug"
if /I "%BUILD_MODE%"=="release" set "GRADLE_TASKS=assembleProductionRelease"
if /I "%BUILD_MODE%"=="all" set "GRADLE_TASKS=assembleProductionDebug assembleProductionRelease"

echo.
echo [2/3] Running !GRADLE_TASKS!...
set "GRADLE_ABI_ARG="
if "%ARM64_ONLY%"=="1" set "GRADLE_ABI_ARG=-PncyAbis=arm64-v8a"
call "%GRADLE_CMD%" --console=plain !GRADLE_ABI_ARG! !GRADLE_TASKS!
if errorlevel 1 (
  echo ERROR: Android build failed.
  goto :failed
)

for /f "tokens=2 delims== " %%V in ('findstr /R /C:"versionName =" app\build.gradle.kts') do set "APP_VERSION=%%~V"
set "APP_VERSION=!APP_VERSION:"=!"
if not defined APP_VERSION set "APP_VERSION=unknown"
set "OUTPUT_ROOT=%CD%\release\android\!APP_VERSION!"

echo.
echo [3/3] Collecting APKs in !OUTPUT_ROOT!...
if /I "%BUILD_MODE%"=="debug" call :collect debug
if /I "%BUILD_MODE%"=="release" call :collect release
if /I "%BUILD_MODE%"=="all" (
  call :collect debug
  if errorlevel 1 goto :failed
  call :collect release
)
if errorlevel 1 goto :failed

echo.
echo ============================================================
echo  Build complete
echo  APK folder: !OUTPUT_ROOT!
echo ============================================================
echo.
if /I "%BUILD_MODE%"=="release" call :unsigned_warning
if /I "%BUILD_MODE%"=="all" call :unsigned_warning

if "!OPEN_OUTPUT!"=="1" start "" explorer.exe "!OUTPUT_ROOT!"
exit /b 0

:collect
set "VARIANT=%~1"
set "OUTPUT_LABEL=!VARIANT!"
if /I "!VARIANT!"=="release" set "OUTPUT_LABEL=release-unsigned"
set "SOURCE_DIR=app\build\outputs\apk\production\!VARIANT!"
set "DEST_DIR=!OUTPUT_ROOT!\!VARIANT!"
if not exist "!DEST_DIR!" mkdir "!DEST_DIR!"
set "COPIED=0"
set "EXPECTED_APKS=3"
set "COLLECT_ABIS=arm64-v8a armeabi-v7a x86_64"
if "%ARM64_ONLY%"=="1" (
  set "EXPECTED_APKS=1"
  set "COLLECT_ABIS=arm64-v8a"
)
for %%A in (!COLLECT_ABIS!) do (
  for %%F in ("!SOURCE_DIR!\*%%A*.apk") do (
    if exist "%%~fF" (
      copy /Y "%%~fF" "!DEST_DIR!\Ncy-!APP_VERSION!-%%A-!OUTPUT_LABEL!%%~xF" >nul
      if errorlevel 1 exit /b 1
      echo   Ncy-!APP_VERSION!-%%A-!OUTPUT_LABEL!%%~xF
      set /a COPIED+=1
    )
  )
)
if not "!COPIED!"=="!EXPECTED_APKS!" (
  echo ERROR: Expected !EXPECTED_APKS! !VARIANT! APK^(s^) but found !COPIED! in !SOURCE_DIR!.
  exit /b 1
)
exit /b 0

:unsigned_warning
echo NOTE: Release APKs are optimized but unsigned because this repository has
echo no release keystore configured. Use the debug APKs for direct installation.
echo.
exit /b 0

:usage
echo Usage: %~nx0 [debug^|release^|all] [--arm64] [--skip-pear] [--no-open]
echo.
echo   debug    Installable production APKs, one per ABI. This is the default.
echo   release  Optimized but unsigned APKs, one per ABI.
echo   all      Build both sets.
echo.
echo --arm64 builds only the arm64-v8a APK for faster physical-device testing.
echo --skip-pear keeps the existing generated Pear bundle and linked AAR.
echo --no-open does not open Explorer after a successful build.
exit /b 2

:failed
echo.
echo Build stopped. No successful APK set was published to the output folder.
pause
exit /b 1
