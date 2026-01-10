# PhantomGPS

A modern Android app for GPS location spoofing. Patch installed apps to use custom GPS coordinates.

## Features

- **App Discovery** - Automatically detects installed apps that use location permissions
- **APK Patching** - Injects location hooks into target apps
- **Custom Locations** - Set any GPS coordinates for spoofed location
- **Material You** - Modern UI with dynamic theming support

## Screenshots

*Coming soon*

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture (Data → Domain → Presentation)
- **DI**: Koin
- **Navigation**: Navigation3 with per-tab back stacks
- **Async**: Kotlin Coroutines + Flow

## Project Structure

```
app/src/main/java/com/navi/phantom/
├── data/                   # Data layer
│   └── local/
│       ├── datasource/     # Data sources
│       ├── dto/            # Data transfer objects
│       ├── mapper/         # DTO to domain mappers
│       └── repository/     # Repository implementations
├── domain/                 # Domain layer
│   ├── models/             # Domain models
│   ├── repository/         # Repository interfaces
│   └── errors/             # Domain errors
├── features/               # Feature modules
│   ├── apps/               # App listing & patching
│   ├── places/             # Saved locations
│   └── setting/            # App settings
├── navigation/             # Navigation setup
├── components/             # Shared UI components
├── theme/                  # Material theme
└── di/                     # Koin modules
```

## Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device/emulator

```bash
./gradlew assembleDebug
```

## Requirements

- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK 34
- Min SDK: 26 (Android 8.0)

## License

*TBD*
