# Resso Music Android project

## Android APK build

This repository contains a native Android app in `app/`. The supported build
path is the GitHub Actions workflow at `.github/workflows/build-apk.yml`, which
installs JDK 17 and Gradle, then builds the debug APK with:

```bash
gradle :app:assembleDebug --stacktrace --no-daemon
```

The APK is written to `app/build/outputs/apk/debug/` and the workflow also
uploads it as the `Resso-Music-Android-APK` artifact.

## Optional API configuration

`YOUTUBE_API_KEY` is injected at build time through a Gradle property or
environment variable. It must never be committed to the repository. If it is
not configured, YouTube search is skipped and the app uses the iTunes catalog
fallback.

The Android app is not a web server, so the Vite files at the repository root
are separate from the Android build and are not used to produce the APK.