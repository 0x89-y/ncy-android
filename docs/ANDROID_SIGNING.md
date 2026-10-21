# Android release signing

Ncy production releases use one permanent signing certificate. Android accepts
an update only when its package name and signing certificate match the installed
application.

## Local configuration

Copy `android-signing.properties.example` to:

```text
%USERPROFILE%\.ncy\android-signing.properties
```

Fill in only the absolute keystore path and key alias. Alternatively, set
`NCY_ANDROID_SIGNING_PROPERTIES` to an external properties file. Keep the
keystore and populated properties file outside this repository and maintain
encrypted offline backups.

Release builds prompt invisibly for the keystore password and then the key
password. Press Enter at the second prompt when both passwords are the same.
Passwords are passed to a no-daemon Gradle process through temporary environment
variables and are cleared when that process exits. They are not stored in the
properties file or placed in command-line arguments. Debug builds do not prompt.

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

## GitHub releases

`.github/workflows/release.yml` builds, signs and drafts a GitHub release when a
`v*` tag is pushed. To release:

1. Increase `versionCode`, set `versionName`, and add the matching
   `## <version> — <date>` section to `CHANGELOG.md` and the in-app changelog.
2. Commit, then tag and push: `git tag v0.7.6` and `git push origin main v0.7.6`.
3. Review the draft release and publish it.

The workflow fails unless the tag matches `versionName`, the unit tests pass, and
the APK is signed by the pinned release certificate. It uploads the arm64-v8a APK
and `SHA256SUMS.txt`, with the version's `CHANGELOG.md` section as the notes.

The signing material lives in a GitHub environment named `release`
(Settings → Environments). Restrict its deployment branches and tags to the tag
pattern `v*`, and add these environment secrets:

| Secret | Value |
| --- | --- |
| `NCY_ANDROID_KEYSTORE_BASE64` | The keystore, Base64-encoded |
| `NCY_ANDROID_KEY_ALIAS` | The key alias, e.g. `ncy-release` |
| `NCY_ANDROID_STORE_PASSWORD` | The keystore password |
| `NCY_ANDROID_KEY_PASSWORD` | The key password (repeat the keystore password if they match) |

Copy the Base64 keystore to the clipboard in PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("D:\Ncy Secrets\ncy-android-release.jks")) | Set-Clipboard
```

Increase `versionCode` for every published update. Never replace the signing key
for an existing package unless performing a deliberately planned Android signing
key rotation.
