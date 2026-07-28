param(
    [Parameter(Mandatory = $true)]
    [string] $GradleCommand,
    [Parameter(Mandatory = $true)]
    [string] $Tasks,
    [string] $AbiArgument = ""
)

$ErrorActionPreference = "Stop"

function ConvertFrom-SecureValue([Security.SecureString] $Value) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$storeSecure = Read-Host "Keystore password" -AsSecureString
$keySecure = Read-Host "Key password (press Enter to reuse the keystore password)" -AsSecureString
$storePassword = ConvertFrom-SecureValue $storeSecure
$keyPassword = ConvertFrom-SecureValue $keySecure
if ([string]::IsNullOrEmpty($keyPassword)) {
    $keyPassword = $storePassword
}

try {
    $env:NCY_ANDROID_STORE_PASSWORD = $storePassword
    $env:NCY_ANDROID_KEY_PASSWORD = $keyPassword
    $arguments = @("--no-daemon", "--console=plain")
    if (-not [string]::IsNullOrWhiteSpace($AbiArgument)) {
        $arguments += $AbiArgument
    }
    $arguments += $Tasks.Split(' ', [StringSplitOptions]::RemoveEmptyEntries)
    & $GradleCommand @arguments
    $gradleExitCode = $LASTEXITCODE
} finally {
    Remove-Item Env:NCY_ANDROID_STORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:NCY_ANDROID_KEY_PASSWORD -ErrorAction SilentlyContinue
    $storePassword = $null
    $keyPassword = $null
}
exit $gradleExitCode