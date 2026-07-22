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
    npm run notices
    if ($LASTEXITCODE -ne 0) { throw "Pear third-party notice generation failed" }
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
    "android-x64" = "x86_64"
}
$addons = @{
    "bare-inspect" = "libbare-inspect.3.1.4.so"
    "bare-type" = "libbare-type.1.1.0.so"
    "sodium-native" = "libsodium-native.5.1.0.so"
    "udx-native" = "libudx-native.1.20.7.so"
}

# The upstream AAR contains legacy 32-bit x86. Ncy intentionally publishes no
# x86 artifact, so remove it from the linked AAR as well; APK split filters alone
# do not remove an ABI from Android App Bundles.
$legacyX86Dir = Join-Path $workRoot "jni/x86"
if (Test-Path -LiteralPath $legacyX86Dir) {
    Remove-Item -LiteralPath $legacyX86Dir -Recurse -Force
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

# Compress-Archive writes ZIP entry names with Windows backslashes. Android's
# AAR extractor expects the ZIP-standard forward slash separator and can turn a
# directory such as META-INF/com into a zero-byte file otherwise. Add only file
# entries with normalized names; ZIP readers infer their parent directories.
Add-Type -AssemblyName System.IO.Compression
$zipStream = [IO.File]::Open(
    $temporaryZip,
    [IO.FileMode]::CreateNew,
    [IO.FileAccess]::ReadWrite,
    [IO.FileShare]::None
)
$archive = [IO.Compression.ZipArchive]::new(
    $zipStream,
    [IO.Compression.ZipArchiveMode]::Create,
    $false
)
try {
    foreach ($file in Get-ChildItem -LiteralPath $workRoot -File -Recurse) {
        $entryName = $file.FullName.Substring($resolvedWorkRoot.Length).Replace('\', '/')
        $entry = $archive.CreateEntry($entryName, [IO.Compression.CompressionLevel]::Optimal)
        $entryStream = $entry.Open()
        $sourceStream = [IO.File]::OpenRead($file.FullName)
        try {
            $sourceStream.CopyTo($entryStream)
        } finally {
            $sourceStream.Dispose()
            $entryStream.Dispose()
        }
    }
} finally {
    $archive.Dispose()
    $zipStream.Dispose()
}
Move-Item -LiteralPath $temporaryZip -Destination $baseAar -Force
Remove-Item -LiteralPath $workRoot -Recurse -Force

Write-Host "Built Ncy Pear worker and linked Android addons into: $baseAar"
