# εxodus Analyzer (Android)

Android app for running local [Exodus Privacy](https://exodus-privacy.eu.org/) static analysis on a user-selected APK file.

## Features

- Pick any APK from device storage using the system file picker
- Extract embedded Java classes from DEX files (including nested APKs)
- Download tracker signatures from the official Exodus API (cached locally)
- Match classes against Exodus `code_signature` rules
- Display app metadata, permissions, and detected trackers
- Export a JSON report compatible with `exodus_analyze.py -j`

## Build locally

```bash
cd android-app
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## CI/CD

GitHub Actions workflow [`.github/workflows/android.yml`](../.github/workflows/android.yml) builds the app on every push/PR that touches `android-app/` and uploads:

- `exodus-analyzer-debug-apk` — debug APK artifact
- `exodus-analyzer-release-apk` — unsigned release APK artifact
- `android-test-reports` — unit test reports

## Analysis approach

This app mirrors the logic in `exodus_analyze.py` / `exodus-core`:

1. Parse APK manifest metadata
2. Enumerate classes from `classes*.dex` using dexlib2 (instead of `dexdump`, which is Linux-only)
3. Fetch tracker signatures from `https://reports.exodus-privacy.eu.org/api/trackers`
4. Detect trackers by regex-matching class names against `code_signature` values

Tracker presence indicates a signature match, not proof of runtime activity — same caveat as the CLI tool.
