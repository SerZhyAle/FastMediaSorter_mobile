# FastMediaSorter Mobile

Android app for viewing images from SMB network shares with automatic slideshow and intuitive touch controls.

## Overview

FastMediaSorter is a lightweight Android application designed for browsing and displaying photos stored on network-attached storage (NAS), Windows shares, or Samba servers. Perfect for creating digital photo frames, presentations, or quick access to network photo collections.

**Key Features:**
- 📡 Direct SMB/CIFS network access (no file copying required)
- 💾 Multiple connection profiles with auto-resume
- 🎬 Configurable slideshow with countdown timer
- 🎮 Invisible touch zones for distraction-free viewing
- 🔄 Image rotation with ABC/Random ordering
- 📱 Optimized for both phones and tablets

## Quick Start

1. **Connect to Network Share:**
   - Enter folder address: `192.168.1.100\Photos`
   - Provide credentials (or leave empty for guest access)
   - Set slideshow interval (5-60 seconds)

2. **Start Slideshow:**
   - Tap **Slideshow** button
   - View images in fullscreen mode

3. **Touch Controls During Slideshow:**

```
┌─────────────────────────────┐
│  BACK   │   ABC/RND toggle  │ ← Top 12.5%
├─────────────────────────────┤
│         │        │          │
│  PREV   │ PAUSE  │   NEXT   │ ← Middle 75%
│         │        │          │
├─────────────────────────────┤
│  ROTATE │      ROTATE       │ ← Bottom 12.5%
│   LEFT  │       RIGHT       │
└─────────────────────────────┘
```

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
- **Top 12.5% of screen** (divided into 2 equal horizontal zones):
  - **Left half** - Back to connection selection
  - **Right half** - Toggle ABC ⇄ Random order
- **Middle 75% of screen** (divided into 3 equal horizontal zones):
  - **Left third** - Previous image (auto-pause enabled)
  - **Center third** - Pause/Resume slideshow
  - **Right third** - Next image (timer reset)
- **Bottom 12.5% of screen** (divided into 2 equal horizontal zones):
  - **Left half** - Rotate image 90° counter-clockwise
  - **Right half** - Rotate image 90° clockwise
  - Rotation persists for all subsequent images until changed

### Slideshow Features
- Configurable interval: 5-60 seconds (default: 10)
- Countdown timer: shows "in 3/2/1" for last 3 seconds
- Shows interval number on resume for 1 second
- Auto-pause on: rotation, previous image, manual back
- Timer reset on: next image click
- ABC ordering: alphabetically by filename
- Random ordering: shuffled on toggle, remembers mode between sessions
- Session auto-resume: starts from last viewed image on app relaunch

### UI Features
- Material Design 3 with adaptive icons
- Compact layout without ActionBar (maximized space)
- Icon-based buttons (Settings ⚙️, Save 💾)
- RecyclerView with up to 5 visible connections (scrollable)
- Touch zone visualization in Settings screen
- Fullscreen slideshow without visible controls
- Vertical scroll support for landscape orientation

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

1. Enter folder address (combined format): `192.168.1.100\Photos\Vacation`
2. Provide username and password (leave empty for anonymous/guest access)
3. Enter connection name (optional - auto-generated from address if empty)
4. Set slide interval in seconds (5-60, default: 10)
5. Click **💾 Save** to store connection for future use
6. Click **Slideshow** to start viewing

## Usage

### Main Screen
- **Sort** button - (Coming soon) Sort options
- **⚙️ Settings** - View touch control zones diagram and help
- **Slideshow** - Start slideshow with current settings
- **Interval** - Set seconds between images
- Tap saved connection to load its settings
- Delete connection by clicking trash icon (🗑️)

### Slideshow Screen
- Touch zones control everything (see diagram in Settings)
- Tap **right zone** - next image + timer reset (doesn't resume if paused)
- Tap **center zone** - pause/resume (shows interval on resume)
- Tap **left zone** - previous image + auto-pause
- Top right zone - toggle between ABC (alphabetic) and RND (random) order
- Bottom zones - rotate current image ±90°
- Device rotation preserved - slideshow continues

### Settings Screen
- Visual guide showing all touch control zones
- Color-coded areas with descriptions
- Contact information: sza@ukr.net 2025

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
├── MainActivity.kt - connection management, compact UI
├── data/
│   ├── ConnectionConfig.kt - Room entity (unique by folderAddress)
│   ├── ConnectionConfigDao.kt - database DAO with Flow
│   ├── AppDatabase.kt - Room database singleton
│   └── ConnectionRepository.kt - repository pattern
├── network/
│   ├── SmbClient.kt - jCIFS-ng wrapper (SMB 2/3 support)
│   └── ImageRepository.kt - image loading and caching
├── ui/
│   ├── ConnectionViewModel.kt - MVVM for connections
│   ├── ConnectionAdapter.kt - RecyclerView adapter
│   ├── slideshow/
│   │   └── SlideshowActivity.kt - fullscreen with touch zones
│   └── settings/
│       └── SettingsActivity.kt - help screen with touch diagram
└── utils/
    └── PreferenceManager.kt - SharedPreferences wrapper

res/
├── drawable/
│   ├── ic_settings.xml - gear icon
│   ├── ic_save.xml - save icon
│   ├── ic_back.xml - back arrow
│   └── touch_zones_scheme.xml - control zones diagram
└── layout/
    ├── activity_main.xml - compact 4-button layout
    ├── activity_slideshow.xml - invisible touch zones
    ├── activity_settings.xml - help screen
    └── item_connection.xml - connection list item
```

## Screenshots & Assets

- `store_assets/screenshots/` - Google Play screenshots
- `store_assets/feature_graphic/fms.png` - 1024x500 banner
- `store_assets/touch_zones_guide.svg` - full color touch zones diagram
- `PRIVACY_POLICY.md` - privacy policy for Google Play

## Release Build

Configured for Google Play Store publication:
- Keystore signing (fastmediasorter.keystore)
- ProGuard/R8 code shrinking and obfuscation
- Release APK/AAB output name: `fastmediasorter.apk`

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Privacy

- **No data collection**: All settings stored locally on device
- **No internet access**: Only local network (SMB) access
- **No analytics or tracking**
- Full privacy policy: [PRIVACY_POLICY.md](PRIVACY_POLICY.md)

## License

MIT License

## Contact

sza@ukr.net - 2025