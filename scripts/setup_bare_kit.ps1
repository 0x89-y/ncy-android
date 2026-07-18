param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"

$version = "2.3.0"
$expectedSha256 = "A386063FA405B0BB4967490E84745075F007F95359C9871C5B7A45C18C2F49E2"
$prebuildRoot = Join-Path $RepositoryRoot "third_party/bare-kit/prebuilds"
$archive = Join-Path $prebuildRoot "prebuilds.zip"
$androidRoot = Join-Path $prebuildRoot "android"
$aar = Join-Path $androidRoot "bare-kit-v$version.aar"
$url = "https://github.com/holepunchto/bare-kit/releases/download/v$version/prebuilds.zip"

if (Test-Path -LiteralPath $aar) {
    Write-Host "Bare Kit Android artifact already exists: $aar"
    exit 0
}

New-Item -ItemType Directory -Path $prebuildRoot -Force | Out-Null
Write-Host "Downloading Bare Kit v$version prebuilds..."
Invoke-WebRequest -Uri $url -OutFile $archive

$actualSha256 = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
if ($actualSha256 -ne $expectedSha256) {
    throw "Bare Kit archive checksum mismatch. Expected $expectedSha256, got $actualSha256"
}

& tar -xf $archive -C $prebuildRoot android/bare-kit
if ($LASTEXITCODE -ne 0) { throw "Could not extract the Bare Kit Android artifact" }

New-Item -ItemType Directory -Path $androidRoot -Force | Out-Null
$temporaryZip = Join-Path $androidRoot "bare-kit-v$version.zip"
Compress-Archive -Path (Join-Path $androidRoot "bare-kit/*") -DestinationPath $temporaryZip -Force
Move-Item -LiteralPath $temporaryZip -Destination $aar -Force

Write-Host "Prepared Bare Kit Android artifact: $aar"
