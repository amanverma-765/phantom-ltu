# PhantomGPS

A modern Android app for GPS location spoofing. Patch installed apps to use custom GPS coordinates without requiring root access.

## How It Works

PhantomGPS uses LSPatch/LSPosed technology to inject location hooks into target applications:

1. **Discover** - Scans for installed apps with location permissions
2. **Patch** - Modifies the target APK by injecting a custom loader
3. **Install** - Reinstalls the patched APK with location interception enabled
4. **Spoof** - The patched app now receives your custom GPS coordinates

The patching process injects a metaloader that intercepts Android's Location APIs at runtime, allowing precise control over reported GPS coordinates.

## Features

- **App Discovery** - Automatically detects installed apps that use location permissions
- **APK Patching** - Injects location hooks using LSPatch-based technology
- **Timeline UI** - Visual progress tracking during the bootstrap/patching process
- **Custom Locations** - Set any GPS coordinates for spoofed location
- **Material You** - Modern UI with dynamic theming and Material 3 design

## Tech Stack

| Category | Technology | Version |
|----------|------------|---------|
| Language | Kotlin | 2.3.0 |
| UI | Jetpack Compose | BOM 2025.12.01 |
| Design | Material 3 | Dynamic theming |
| Navigation | Navigation3 | 1.1.0-alpha01 |
| DI | Koin | 4.2.0 |
| Networking | Ktor | 3.3.3 |
| Database | Room | 2.8.4 |
| Async | Kotlin Coroutines + Flow | - |
| Image Loading | Coil | 3.3.0 |
| Logging | Kermit | 2.0.8 |
| Native | C++ (CMake) | NDK 29 |

## Architecture

Clean Architecture with MVVM pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Screens   │  │  ViewModels │  │    State    │         │
│  │  (Compose)  │  │   (Logic)   │  │   (UiState) │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Models    │  │ Repositories│  │   Errors    │         │
│  │  (Domain)   │  │ (Interfaces)│  │  (Sealed)   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
├─────────────────────────────────────────────────────────────┤
│                       Data Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ DataSources │  │    DTOs     │  │   Mappers   │         │
│  │   (Local)   │  │  (Transfer) │  │ (DTO→Model) │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

| Module | Purpose |
|--------|---------|
| `app` | Main Android application (UI, ViewModels, DI) |
| `patcher` | APK patching logic (pure JVM library) |
| `meta-loader` | Bootstrap loader injected into patched APKs |
| `patch-loader` | Runtime loader with native code (libphantom.so) |
| `shared/java` | Shared constants, configs, DTOs |
| `shared/android` | Android-specific utilities |
| `core` | LSPosed core library (Xposed hook framework) |
| `apkzlib` | Google's APK manipulation library |
| `axml` | Android binary XML parser/editor |

## Project Structure

```
PhantomGPS/
├── app/src/main/java/com/navi/phantom/
│   ├── data/                   # Data layer
│   │   └── local/
│   │       ├── datasource/     # Data sources
│   │       ├── dto/            # Data transfer objects
│   │       ├── mapper/         # DTO to domain mappers
│   │       └── repository/     # Repository implementations
│   ├── domain/                 # Domain layer
│   │   ├── models/             # Domain models
│   │   ├── repository/         # Repository interfaces
│   │   └── errors/             # Domain errors
│   ├── features/               # Feature modules
│   │   ├── apps/               # App listing & selection
│   │   ├── bootstrap/          # Patching workflow UI
│   │   ├── places/             # Saved locations
│   │   └── setting/            # App settings
│   ├── navigation/             # Navigation setup
│   ├── components/             # Shared UI components
│   ├── theme/                  # Material theme
│   └── di/                     # Koin modules
├── patcher/                    # APK patching engine
├── meta-loader/                # Injected bootstrap loader
├── patch-loader/               # Runtime hooks + native code
├── shared/                     # Shared utilities
│   ├── java/                   # Pure JVM shared code
│   └── android/                # Android-specific shared code
├── core/                       # LSPosed core framework
├── apkzlib/                    # APK manipulation library
└── axml/                       # Android XML parser
```

## Bootstrap Process

The patching workflow (called "bootstrap") transforms a regular APK into one that spoofs GPS:

| Step | Description |
|------|-------------|
| Parse APK | Read manifest and extract metadata |
| Setup Signing | Configure APK signature |
| Extract Signature | For signature bypass (optional) |
| Modify Manifest | Inject custom component factory |
| Add Config | Embed bootstrap configuration |
| Add Metaloader | Inject metaloader.dex |
| Create Links | Link original APK entries |
| Write APK | Finalize and sign patched APK |

## Building

### Requirements

- Android Studio Ladybug or newer
- JDK 21+
- Android SDK 36
- NDK 29.0.13113456

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

### Min/Target SDK

| Property | Value |
|----------|-------|
| Min SDK | 28 (Android 9.0) |
| Target SDK | 36 |
| Compile SDK | 36 |

## Supported ABIs

- arm64-v8a
- armeabi-v7a
- x86
- x86_64

## License

*TBD*
