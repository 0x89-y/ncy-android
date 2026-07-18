param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"

$workerRoot = Join-Path $RepositoryRoot "pear-worker"
$bareKitRoot = Join-Path $RepositoryRoot "third_party/bare-kit/prebuilds/android"
$baseAar = Join-Path $bareKitRoot "bare-kit-v2.3.0.aar"
$workRoot = Join-Path $bareKitRoot "ncy-linked"
$temporaryZip = Join-Path $bareKitRoot "bare-kit-v2.3.0-linked.zip"

$resolvedBareKitRoot = [IO.Path]::GetFullPath($bareKitRoot).TrimEnd('\') + '\'
$resolvedWorkRoot = [IO.Path]::GetFullPath($workRoot).TrimEnd('\') + '\'
if (-not $resolvedWorkRoot.StartsWith($resolvedBareKitRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to use a work directory outside the Bare Kit cache: $resolvedWorkRoot"
}

if (-not (Test-Path -LiteralPath $baseAar)) {
    & (Join-Path $PSScriptRoot "setup_bare_kit.ps1") -RepositoryRoot $RepositoryRoot
}

Push-Location $workerRoot
try {
    npm ci
    if ($LASTEXITCODE -ne 0) { throw "npm ci failed for the Pear worker" }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Bare Pack failed for the Pear worker" }
} finally {
    Pop-Location
}

if (Test-Path -LiteralPath $workRoot) {
    Remove-Item -LiteralPath $workRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $workRoot -Force | Out-Null
& tar -xf $baseAar -C $workRoot
if ($LASTEXITCODE -ne 0) { throw "Could not unpack the Bare Kit AAR" }

$abis = @{
    "android-arm64" = "arm64-v8a"
    "android-arm" = "armeabi-v7a"
    "android-ia32" = "x86"
    "android-x64" = "x86_64"
}
$addons = @{
    "sodium-native" = "libsodium-native.5.1.0.so"
    "udx-native" = "libudx-native.1.20.7.so"
}

foreach ($hostName in $abis.Keys) {
    $jniDir = Join-Path $workRoot ("jni/" + $abis[$hostName])
    New-Item -ItemType Directory -Path $jniDir -Force | Out-Null
    foreach ($packageName in $addons.Keys) {
        $source = Join-Path $workerRoot (
            "node_modules/$packageName/prebuilds/$hostName/$packageName.bare"
        )
        if (-not (Test-Path -LiteralPath $source)) {
            throw "Missing linked addon prebuild: $source"
        }
        Copy-Item -LiteralPath $source -Destination (Join-Path $jniDir $addons[$packageName]) -Force
    }
}

if (Test-Path -LiteralPath $temporaryZip) {
    Remove-Item -LiteralPath $temporaryZip -Force
}
Compress-Archive -Path (Join-Path $workRoot "*") -DestinationPath $temporaryZip -Force
Move-Item -LiteralPath $temporaryZip -Destination $baseAar -Force
Remove-Item -LiteralPath $workRoot -Recurse -Force

Write-Host "Built Ncy Pear worker and linked Android addons into: $baseAar"
