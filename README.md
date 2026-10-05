# 🎵 Resso Music - Native Android App

TikTok-style vertical music streaming app with video vibes, synchronized lyrics, beat-synchronized flash effects, spatial 8D audio, and background playback built using Jetpack Compose, Kotlin, Media3 ExoPlayer, and Room Database.

---

## 🚀 Features (Native Android)
- **TikTok-Style Vertical Vibe Feed:** Swipe vertically between songs with seamless video loops, atmospheric album artwork, and animated disc visuals.
- **Synced Lyrics:** Dynamic lyrics synchronized with playback timestamps, featuring tap-to-seek lyric lines and custom Lyric Poster generator.
- **Interactive Community & Danmaku:** Floating bullet comments overlay (danmaku) with real-time comments sheet, likes, and timestamp tagging.
- **Audio Processing Engine:**
  - **Spatial 8D Audio:** Binaural panning audio processor rotating sound in real-time.
  - **Equalizer & Bass Boost:** Built-in Android audio equalizer with bass enhancement and presets.
  - **Flash Sync:** Real-time beat detection synchronized with hardware flashlight and on-screen strobe pulses.
- **ExoPlayer & Media3 Foreground Playback Service:** Background playback that continues even when the screen is locked or the app is minimized, with notification media controls.
- **Local Persistence with Room:** Offline persistence for favorites, custom created vibes, playlists, and user comments.
- **Explore & Real-time Search:** Search across high-fidelity 320kbps streams, genres, trending hits, and artists.

---

## 🛠️ Architecture & Tech Stack
- **UI Framework:** Jetpack Compose with Material 3 theming
- **Audio & Media:** AndroidX Media3 ExoPlayer (`media3-exoplayer`, `media3-session`)
- **Database:** AndroidX Room (`room-ktx`, `room-runtime`)
- **Networking & Async:** OkHttp, Retrofit, Kotlin Coroutines & Flow
- **Image Loading:** Coil Compose
- **Target SDK:** Android SDK 36 (Min SDK 24)

