<div align="center">

# SubStream

**A native Android music client for your own Subsonic-compatible server.**

Stream the library you host yourself — Navidrome, Airsonic, Gonic, Symfonium's server, anything
speaking the Subsonic API. No account, no cloud, no closed format.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/ExoPlayer-Media3%201.11-FF6D00?logo=googleplay&logoColor=white)](https://developer.android.com/media/media3)
[![License: TBD](#license)

</div>

---

## Why

Self-hosted music deserves a first-class phone app. SubStream is a single-module, fully-Compose
Android app that talks straight to your server over the Subsonic REST API — no intermediate
service, no sync proxy, nothing of yours stored outside the device.

## Features

- **Home** — a shelf-style layout: quick picks, recently added, frequently played, made-for-you
  (random), favorites, and your artists. Everything hidden that has no cover art.
- **Library** — albums, artists (alphabetical index, main artists only), playlists, favorites.
- **Search** — artists, albums, and tracks in one query (`search3`).
- **Now Playing** — swipeable full-screen sheet with queue, shuffle, and repeat
  (off → all → one).
- **Background playback** — a Media3 `MediaSessionService` keeps audio alive outside the app with
  a real notification, lock-screen art, and hardware button support.
- **Favorites everywhere** — star/unstar albums and artists, toggled straight from the UI and
  optimistic on screen.
- **Album & artist detail** — tracklists, play-anything, per-artist song lookup.
- **Dynamic server login** — point the app at any Subsonic host, username, and password. Swapping
  servers clears cached art URLs and stops playback so credentials never leak across hosts.
- **Material 3** — light/dark, edge-to-edge, adaptive bottom nav, extended material icons.

## Tech stack

| Layer | Choice |
| --- | --- |
| UI | Jetpack Compose + Material 3 + Adaptive Navigation Suite |
| Architecture | MVVM with `ViewModel` + `StateFlow`, sealed UI states |
| DI | Koin |
| Networking | Retrofit 3 + OkHttp 5 + kotlinx.serialization |
| Playback | AndroidX Media3 (ExoPlayer + MediaSessionService) |
| Images | Coil |
| Navigation | Navigation Compose |
| Min SDK | 24 (Android 7.0) · Target/Compile SDK 37 |
| Build | Gradle 9.6 · AGP 9.4.1 · Kotlin 2.2.10 · JDK 11 |

## Getting started

**Requirements:** Android Studio with a JDK 11+ toolchain, and a reachable Subsonic server.

```bash
git clone https://github.com/linconkusunoki/substream.git
cd substream
./gradlew installDebug
```

Or open the project in Android Studio and hit ▶. On first launch the app asks for a **server URL**
(`music.example.com`), **username**, and **password** — credentials are salted and MD5-hashed per
the Subsonic token scheme, never stored in plaintext. Settings lets you re-test the connection,
switch servers, or log out.

## Project structure

```
app/src/main/java/com/substream/
├── MainActivity.kt, SubstreamApp.kt
├── data/
│   ├── api/         Subsonic REST models, Retrofit service, auth + interceptors
│   ├── preferences/ Server config and credential storage
│   └── repository/  SubsonicRepository — single source of truth for the API
├── di/              Koin module
├── player/          PlaybackService (MediaSessionService), PlayerManager, cache
└── ui/
    ├── components/  PlayerBar
    ├── navigation/  Routes and MainScreen scaffold
    ├── screens/     Login, Home, Library, Search, Settings, Album/Artist detail, Now Playing
    └── theme/       Material 3 color, type, theme
```

Every call funnels through `SubsonicRepository`, which returns `Result<T>` — screens never touch
Retrofit or OkHttp directly.

## Tests

```bash
./gradlew test
```

Unit tests cover URL/auth construction, server config parsing, main-artist filtering, and home shelf
composition; Compose UI tests cover the home screen states.

## Status

Actively built and usable. No releases are published yet — build from source.

## License

Not yet licensed. All rights reserved until a license is chosen.