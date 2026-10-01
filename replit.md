# Resso Music Android project

## Android APK build

This repository contains a native Android app in `app/`. The supported build
path is the GitHub Actions workflow at `.github/workflows/build-apk.yml`, which
installs JDK 21 and Gradle, then builds the debug APK with:

```bash
gradle :app:assembleDebug --stacktrace --no-daemon
```

The APK is written to `app/build/outputs/apk/debug/` and the workflow also
uploads it as the `Resso-Music-Android-APK` artifact.

## Optional API configuration

`YOUTUBE_API_KEY`, `SPOTIFY_CLIENT_ID`, and `SPOTIFY_CLIENT_SECRET` are
injected at build time through a Gradle property or environment variable.
They must never be committed to the repository. YouTube search plays full
tracks. Spotify supplies track names, artists, and album art, and its preview
is used when matching videos are fetched. Without either key,
the app relies on YouTube and built-in offline music catalog.

The Android app is not a web server, so the Vite files at the repository root
are separate from the Android build and are not used to produce the APK.
