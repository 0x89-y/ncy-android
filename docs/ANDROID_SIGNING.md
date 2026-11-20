# Android releases and signing

Ncy production releases use one permanent signing certificate. Android accepts
an update only when its package name and signing certificate match the installed
application.

## Releasing

Releases are built, signed and drafted by GitHub Actions
(`.github/workflows/release.yml`) when a `v*` tag is pushed. To release:

1. Increase `versionCode`, set `versionName`, and add the matching
   `## <version> — <date>` section to `CHANGELOG.md` and the in-app changelog
   (`ui/settings/Changelog.kt`). `ChangelogSyncTest` fails if the two disagree.
2. Commit to `main`, then tag and push: `git tag v0.9.0` and
   `git push origin main v0.9.0`.
3. Review the draft release on GitHub and publish it.

The workflow fails unless the tag matches `versionName`, the unit tests pass, and
the APK is signed by the pinned release certificate. It uploads the arm64-v8a APK
and `SHA256SUMS.txt`, with the version's `CHANGELOG.md` section as the notes.

## Signing secrets

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

Keep the keystore outside this repository and maintain encrypted offline backups.
Increase `versionCode` for every published update. Never replace the signing key
for an existing package unless performing a deliberately planned Android signing
key rotation.

## Local builds

Test APKs need no signing setup: `gradle assembleProductionDebug` builds them.

A signed release build on your own machine is only needed when GitHub Actions
cannot be used. Gradle reads the keystore path and alias from
`%USERPROFILE%\.ncy\android-signing.properties`, or from the file that
`NCY_ANDROID_SIGNING_PROPERTIES` names (see `android-signing.properties.example`),
and the passwords from the `NCY_ANDROID_STORE_PASSWORD` and
`NCY_ANDROID_KEY_PASSWORD` environment variables. Passwords never go in the
properties file.
