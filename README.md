# FastMediaSorter Mobile

Android app for slideshow of images from SMB server.

## Features

- Connect to SMB servers over network
- Automatic image slideshow
- Configurable slide interval
- Supports: JPG, PNG, GIF, BMP, WebP
- Fullscreen slideshow mode

## Requirements

- Android 9.0 (API 28) or higher
- Local network access
- SMB server with images

## Build APK

```bash
./gradlew assembleRelease
```

APK file will be created in `app/build/outputs/apk/release/`

## Setup

1. Enter SMB server address (e.g., `192.168.1.100`)
2. Provide username and password (if required)
3. Enter path to images folder
4. Set slide interval in seconds
5. Click "Connect"

## Technical Details

- **Language**: Kotlin
- **Minimum Android Version**: API 28 (Android 9)
- **SMB Library**: jCIFS-ng 2.1.10
- **UI**: Material Design 3
- **Async**: Kotlin Coroutines

## Development

Project uses standard Android app structure:

- `MainActivity` - main screen with settings
- `SlideshowActivity` - slideshow screen
- `SmbClient` - SMB client
- `ImageRepository` - repository for image loading
- `PreferenceManager` - settings management

## License

MIT License