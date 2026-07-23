# Android release signing

Ncy production releases use one permanent signing certificate. Android accepts
an update only when its package name and signing certificate match the installed
application.

## Local configuration

Copy `android-signing.properties.example` to:

```text
%USERPROFILE%\.ncy\android-signing.properties
```

Fill in the absolute keystore path, alias, and passwords. Alternatively, set
`NCY_ANDROID_SIGNING_PROPERTIES` to an external properties file. Keep the
keystore and populated properties file outside this repository and maintain
encrypted offline backups.

## Build

Build all supported ABI releases:

```text
build-apks.bat release
```

Build only the primary arm64 release:

```text
build-apks.bat release --arm64
```

The builder verifies each APK with Android SDK `apksigner` and writes
`SHA256SUMS.txt` beside the release APKs under `release/android/<version>/release`.

Increase `versionCode` for every published update. Never replace the signing key
for an existing package unless performing a deliberately planned Android signing
key rotation.
