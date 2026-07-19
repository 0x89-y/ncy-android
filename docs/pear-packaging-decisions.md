# Pear packaging decisions

Status: agreed direction. Pear was promoted into the single production Android
application on 2026-07-19; ABI-specific and desktop/server packaging remain pending.

## Product direction

- Ship one Ncy application with Pear included as an optional, off-by-default feature.
- Do not maintain separate Ncy and Ncy Pear editions.
- Keep Local only as the default transport policy. Pear runtime/network activity starts
  only after the user selects a Pear-capable policy and accepts the privacy notice.
- Do not make Pear an in-app downloadable executable component.
- Do not maintain a custom stripped-down fork of Bare without evidence of a supported
  upstream configuration that produces meaningful savings.

## Android

- Replace the universal/fat Pear APK with dedicated ABI artifacts (or an Android App
  Bundle where the distribution channel supports it).
- Make `arm64-v8a` the primary direct-download APK for current physical devices.
- Retain `armeabi-v7a` as the legacy-phone build.
- Retain `x86_64` as the emulator build.
- Drop 32-bit `x86`.
- Keep all ABI artifacts generated from the same source, version and release job; they
  are not separate product editions.
- Do not pursue additional debug-symbol stripping: the inspected Bare Kit libraries
  were unchanged by Gradle's stripping stage.
- Do not remove Bare/Hyperswarm native modules. The separately visible modules are
  small, while sodium and UDX are required for crypto/networking and `libbare-kit.so`
  dominates the size.

Measured development artifacts on 2026-07-19:

| Artifact/component | Size |
| --- | ---: |
| Production debug APK without Pear | 62.80 MB |
| Universal dev debug APK with Pear | 307.29 MB |
| Universal APK increase | 244.49 MB |
| `arm64-v8a` `libbare-kit.so` | 61.88 MB |
| `armeabi-v7a` `libbare-kit.so` | 48.69 MB |
| `x86_64` `libbare-kit.so` | 63.82 MB |

Measured unsigned production release artifacts after ABI splitting on 2026-07-19:

| Artifact | Size |
| --- | ---: |
| `arm64-v8a` APK | 69.55 MB |
| `armeabi-v7a` APK | 55.81 MB |
| `x86_64` APK | 71.54 MB |
| Three-ABI Android App Bundle | 58.10 MB |

Expected Pear overhead for a per-ABI artifact is roughly 50–66 MB instead of
244 MB. Final release artifacts must be measured rather than inferred from debug builds.

## Desktop and server

- Package exactly one Bare runtime matching each release target's OS and architecture.
- Package the worker bundle and its required native addons alongside that runtime.
- Use Tauri's NSIS LZMA compression for Windows desktop installers and measure both
  installer download size and installed size.
- Do not build a custom reduced Bare runtime unless upstream supports it and measured
  savings justify the maintenance cost.
- Do not install a shared system-wide Bare runtime for Ncy Desktop and ncy-server.
  Keeping private, version-pinned copies avoids cross-application update, permission and
  uninstall coupling.

The main plausible same-machine use case is an always-on ncy-server service plus an
occasionally opened desktop UI, potentially under different user accounts. In normal
desktop use this is largely redundant because Ncy Desktop can close to the tray and keep
sync running. The remaining niche case does not justify shared-runtime complexity for an
estimated maximum saving of about 46 MB.

Measured Windows Pear payload on 2026-07-19:

| Component | Size |
| --- | ---: |
| Bare Windows x64 runtime | 43.05 MB |
| Ncy Pear worker bundle | 0.85 MB |
| Offloaded native addons | 2.45 MB |
| Total uncompressed payload | 46.35 MB |

## Packaging work to schedule

1. **Completed 2026-07-19:** Configure Android App Bundle and per-ABI APK outputs for
   `arm64-v8a`, `armeabi-v7a` and `x86_64`, with no `x86` or universal APK artifact.
2. **Completed 2026-07-19:** Merge the Pear-enabled Android flavor into the single
   production Ncy product while preserving the off-by-default runtime toggle and consent
   behavior.
3. Add target-specific Bare runtime, worker bundle and addons to desktop release resources.
4. Add the equivalent target-specific resource layout to ncy-server release packaging.
5. Confirm NSIS LZMA configuration and measure compressed versus installed Windows sizes.
6. Record signed release APK/AAB, installer and installed-size measurements in this note.
