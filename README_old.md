# FastMediaSorter Mobile

Android app for viewing images from SMB network shares with automatic slideshow and intuitive touch controls.

## Overview

FastMediaSorter is a lightweight Android application designed for browsing and displaying photos stored on network-attached storage (NAS), Windows shares, or Samba servers. Perfect for creating digital photo frames, presentations, or quick access to network photo collections.

**Key Features:**
- 📡 Direct SMB/CIFS network access (no file copying required)
- 💾 Multiple connection profiles with auto-resume
- 🎬 Configurable slideshow with countdown timer (1-300 seconds)
- 🎮 Invisible touch zones for distraction-free viewing
- 🔄 Image rotation with ABC/Random ordering
- 📦 File sorting: Copy/Move/Delete images to 10 configurable destinations
- ⚡ Image preloading for instant transitions (slideshow and sort)
- ⚙️ Flexible settings: default credentials, operation permissions
- � Optimized for both phones and tablets

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
- SQLite database for persistent storage (Room v2)
- Auto-load last used connection on startup
- Anonymous and authenticated access support
- Dual purpose: Slideshow source or Sort destination

### Sort Destinations (0-9)
- Configure up to 10 sort destinations from saved connections
- Each destination has:
  - **Sort order** (0-9) - position in button list
  - **Sort name** - short display name (max 20 chars)
  - **Unique color** - 10 distinct colors for visual identification
- Automatic reordering when destinations added/removed
- Used by both Copy and Move operations in Sort screen

### Slideshow
- Automatic image slideshow with configurable interval: **1-300 seconds** (default: 10)
- **Image preloading optimization**: Background loading of next image for instant transitions
  - Preload starts when interval > 2× load time
  - Manual next: instant switch using preloaded image
  - Auto-advance: seamless transition with preloaded data
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
- Configurable interval: **1-300 seconds** (default: 10)
- Countdown timer: shows "in 3/2/1" for last 3 seconds
- Shows interval number on resume for 1 second
- Auto-pause on: rotation, previous image, manual back
- Timer reset on: next image click
- ABC ordering: alphabetically by filename
- Random ordering: shuffled on toggle, remembers mode between sessions
- Session auto-resume: starts from last viewed image on app relaunch
- **Performance**: Preloads next image in background for smooth transitions

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
- **Sort** button - Open file sorting mode for selected connection
- **⚙️ Settings** - Configure slideshow help and sort destinations
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
- **Slideshow tab** - Visual guide showing all touch control zones with color-coded areas
- **Sort to.. tab** - Configure up to 10 sort destinations (0-9)
  - Add destination: Select connection + enter short name (max 20 chars)
  - Reorder destinations: Move Up/Down buttons
  - Delete destination: Remove from list
  - Each destination gets unique color for easy identification
- **Settings tab** - Application-wide settings:
  - **Default Username/Password** - Auto-filled for connections without credentials
  - **Sorting Settings**:
    - **Allow to Copy** - Enable/disable Copy buttons (default: ON)
    - **Allow to Move** - Enable/disable Move buttons (default: OFF)
    - **Allow to Delete** - Enable/disable Delete button (default: OFF)
    - **Request user for deletion** - Confirmation dialog before delete (default: ON)
- Contact information: sza@ukr.net 2025

### Sort Screen
- **Navigation**: Left/right touch zones (50/50 split) with infinite cycling
- **Image preloading**: Background loading of next image for instant navigation
- **File info**: Displays filename, size (KB), and modification date
- **Connection name**: Shows current connection at top of screen
- **Copy to..**: Up to 10 colored buttons for copying current image to destinations
  - Visible only if "Allow to Copy" enabled in Settings
  - File remains in source folder
  - Auto-advance to next image on success
  - Error handling: Already exists, same folder, network/security errors
- **Move to..**: Up to 10 colored buttons for moving current image to destinations
  - Visible only if "Allow to Move" enabled in Settings
  - File deleted from source after successful copy
  - Removed from current list, auto-load next image
  - Error handling: Copy errors + delete permission errors
- **Delete**: Red button for deleting current image
  - Visible only if "Allow to Delete" enabled in Settings
  - Optional confirmation dialog ("Request user for deletion" setting)
  - File permanently deleted from source
  - Removed from list, auto-load next image
- **Progress indicator**: "Copying..." / "Moving..." / "Deleting..." overlay during operations
- **Counter**: Shows current position (e.g., "5 / 23")
- **Back button**: Return to main screen

## Technical Details

- **Language**: Kotlin 1.9.10
- **Target SDK**: Android 14 (API 34)
- **Minimum SDK**: Android 9 (API 28)
- **Build System**: Gradle 8.13 with KSP
- **Database**: Room 2.6.1 (SQLite, version 2 with sort fields)
- **SMB Library**: jCIFS-ng 2.1.10 (SMB 2/3 support)
- **Image Loading**: Glide 4.16.0
- **UI**: Material Design 3, ConstraintLayout, RecyclerView, TabLayout, ViewPager2
- **Async**: Kotlin Coroutines 1.7.3 with lifecycle-aware scopes
- **Architecture**: MVVM with Repository pattern, LiveData/Flow reactivity

## Project Structure

```
app/src/main/java/com/sza/fastmediasorter/
├── MainActivity.kt - connection management, compact UI
├── data/
│   ├── ConnectionConfig.kt - Room entity v2 (sortOrder, sortName fields)
│   ├── ConnectionConfigDao.kt - database DAO with Flow, sort queries
│   ├── AppDatabase.kt - Room database v2 with migration
│   └── ConnectionRepository.kt - CRUD + sort destination management
├── network/
│   ├── SmbClient.kt - jCIFS-ng wrapper with copy/move/delete operations
│   └── ImageRepository.kt - image loading with default credentials fallback
├── ui/
│   ├── ConnectionViewModel.kt - MVVM for connections & destinations
│   ├── ConnectionAdapter.kt - RecyclerView adapter for connections
│   ├── slideshow/
│   │   └── SlideshowActivity.kt - fullscreen with touch zones & preloading
│   ├── settings/
│   │   ├── SettingsActivity.kt - TabLayout with 3 tabs
│   │   ├── SlideshowHelpFragment.kt - touch zones diagram
│   │   ├── SortHelpFragment.kt - sort destinations management
│   │   ├── SettingsFragment.kt - app settings (credentials, permissions)
│   │   ├── SortDestinationAdapter.kt - RecyclerView for destinations
│   │   └── AddSortDestinationDialog.kt - add destination dialog
│   └── sort/
│       └── SortActivity.kt - image sorting with copy/move/delete & preloading
└── utils/
    └── PreferenceManager.kt - SharedPreferences wrapper (credentials, permissions)

res/
├── drawable/
│   ├── ic_settings.xml - gear icon
│   ├── ic_save.xml - save icon
│   ├── ic_back.xml - back arrow
│   └── touch_zones_scheme.xml - control zones diagram with labels
└── layout/
    ├── activity_main.xml - compact 4-button layout
    ├── activity_slideshow.xml - invisible touch zones
    ├── activity_settings.xml - TabLayout + ViewPager2 (3 tabs)
    ├── activity_sort.xml - fullscreen with Copy/Move/Delete buttons
    ├── fragment_slideshow_help.xml - touch zones visualization
    ├── fragment_sort_help.xml - destinations list with add button
    ├── fragment_settings.xml - credentials + sorting permissions
    ├── item_connection.xml - connection list item
    ├── item_sort_destination.xml - destination with up/down/delete
    └── dialog_add_sort_destination.xml - add destination dialog
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