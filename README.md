<p align="center">
  <img src="Bread Logo.png" alt="Bread Logo" width="120"/>
</p>

<h1 align="center">Bread</h1>
<p align="center">A clean, offline-first Bible reading app for Android</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue?logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/Min%20SDK-30-orange" />
</p>

---

## Features

- **Bible Reader** — Read all 66 books of the Bible (KJV) with chapter navigation
- **Offline Caching** — Chapters are cached locally via Room after first load, no internet needed after that
- **Downloads** — Download entire books for offline use, with live download status per book
- **Search** — Search through Bible content
- **Community** — Community feed screen
- **Chats** — Chat screen
- **Profile** — Manage your profile photo and settings

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Networking | Retrofit 2 + Gson |
| Local Cache | Room Database |
| Images | Coil |
| ViewModel | Lifecycle ViewModel Compose |
| Bible API | [bible-api.com](https://bible-api.com) (KJV) |

---

## Project Structure

```
app/src/main/java/com/bibleread/bread/
├── data/
│   ├── BibleApiService.kt       # Retrofit API + models
│   ├── BibleDatabase.kt         # Room entity, DAO, database
│   └── BibleRepository.kt       # Cache-first data logic
├── viewmodel/
│   ├── BibleViewModel.kt        # Reader state management
│   └── DownloadViewModel.kt     # Per-book download state
├── ui/
│   ├── screens/
│   │   ├── ReaderScreen.kt      # Bible reader with book/chapter dropdowns
│   │   ├── DownloadsScreen.kt   # Download manager for all 66 books
│   │   ├── ProfileScreen.kt     # Profile + settings
│   │   ├── HomeScreen.kt        # Community feed
│   │   ├── SearchScreen.kt      # Search
│   │   ├── ChatsScreen.kt       # Chats
│   │   └── SplashScreen.kt      # Splash / onboarding
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator with API 30+
- Internet connection for first-time Bible content load

### Setup

1. Clone the repository
   ```bash
   git clone https://github.com/your-username/bread.git
   ```
2. Open the project in Android Studio
3. Sync Gradle
4. Run on a device or emulator

---

## How Offline Works

1. First time you open a chapter → fetched from `bible-api.com` and saved to Room
2. Next time you open the same chapter → loaded directly from Room, no network needed
3. In the **Downloads** tab (Profile → Downloads) you can download an entire book at once — all chapters cached in one go

---

## Bible Translation

Currently using **King James Version (KJV)** via [bible-api.com](https://bible-api.com).

---

## License

This project is for personal/educational use. Bible content is provided by [bible-api.com](https://bible-api.com).
