# FastMediaSorter Mobile

Android app for slideshow of images from SMB network shares.

## Features

### Connection Management
- Connect to SMB servers over local network
- Save multiple SMB connection configurations
- SQLite database for persistent storage
- Auto-load last used connection on startup
- Anonymous and authenticated access support

### Slideshow
- Automatic image slideshow with configurable interval (default: 10 seconds)
- Fullscreen mode with original image proportions (fitCenter)
- Supports: JPG, JPEG, PNG, GIF, BMP, WebP
- Orientation support (portrait and landscape)
- State preservation on screen rotation

### Touch Controls (Invisible Zones)
- **Top 12.5% of screen** - Back to connection selection
- **Middle 75% of screen** (divided into 3 equal horizontal zones):
  - **Left third** - Previous image
  - **Center third** - Pause/Resume slideshow
  - **Right third** - Next image
- **Bottom 12.5% of screen** (divided into 2 equal horizontal zones):
  - **Left half** - Rotate image 90° counter-clockwise
  - **Right half** - Rotate image 90° clockwise
  - Rotation persists for all subsequent images until changed

### UI Features
- Material Design 3
- Vertical scroll support for landscape orientation
- RecyclerView with saved connections list
- Delete saved connections
- Fullscreen slideshow without visible controls

## Requirements

- Android 9.0 (API 28) or higher
- Local network access
- SMB server with shared folder containing images

## Build APK

```bash
./gradlew assembleDebug
```

APK file: `app/build/outputs/apk/debug/fastmediasorter.apk`

For release build:
```bash
./gradlew assembleRelease
```

APK file: `app/build/outputs/apk/release/fastmediasorter.apk`

## Setup

1. Enter connection name (optional, for saving)
2. Enter SMB server address (e.g., `192.168.1.100` or `//server/share`)
3. Provide username and password (leave empty for anonymous access)
4. Enter path to images folder (e.g., `Photos/Vacation`)
5. Set slide interval in seconds (default: 10)
6. Click **Save** to store connection
7. Click **Connect** to start slideshow

## Usage

### Main Screen
- View list of saved connections
- Tap connection to load settings
- Delete connection by clicking delete icon
- Create new connection or modify existing

### Slideshow Screen
- Tap anywhere to skip to next image (also resets timer)
- Use invisible zones for navigation:
  - Top area - return to main screen
  - Left side - previous image
  - Center - pause/resume
  - Right side - next image
- Rotate device - slideshow continues with current image

## Technical Details

- **Language**: Kotlin 1.9.10
- **Target SDK**: Android 14 (API 34)
- **Minimum SDK**: Android 9 (API 28)
- **Build System**: Gradle 8.13 with KSP
- **Database**: Room 2.6.1 (SQLite)
- **SMB Library**: jCIFS-ng 2.1.10
- **Image Loading**: Glide 4.16.0
- **UI**: Material Design 3, ConstraintLayout, RecyclerView
- **Async**: Kotlin Coroutines 1.7.3
- **Architecture**: MVVM with Repository pattern

## Project Structure

```
app/src/main/java/com/sza/fastmediasorter/
├── MainActivity.kt - main screen with connection management
├── data/
│   ├── ConnectionConfig.kt - Room entity
│   ├── ConnectionConfigDao.kt - database DAO
│   ├── AppDatabase.kt - Room database
│   └── ConnectionRepository.kt - repository pattern
├── network/
│   ├── SmbClient.kt - SMB client with jCIFS-ng
│   └── ImageRepository.kt - image loading
├── ui/
│   ├── ConnectionViewModel.kt - ViewModel for saved connections
│   ├── ConnectionAdapter.kt - RecyclerView adapter
│   └── slideshow/
│       └── SlideshowActivity.kt - fullscreen slideshow
└── utils/
    └── PreferenceManager.kt - legacy preferences (migration to Room)
```

## License

MIT License