# FastMediaSorter Mobile# FastMediaSorter Mobile



Android app for viewing images from local device storage and SMB network shares with automatic slideshow and intuitive touch controls.Android app for viewing images from SMB network shares with automatic slideshow and intuitive touch controls.



## Overview## Overview



FastMediaSorter is a lightweight Android application for browsing and displaying photos from your Android device (Camera, Screenshots, Pictures, Downloads) or network storage (NAS, Windows shares, Samba servers). Perfect for digital photo frames, presentations, or quick photo access.FastMediaSorter is a lightweight Android application designed for browsing and displaying photos stored on network-attached storage (NAS), Windows shares, or Samba servers. Perfect for creating digital photo frames, presentations, or quick access to network photo collections.



**Key Features:****Key Features:**

- 📱 **Local folders access**: Camera, Screenshots, Pictures, Downloads + custom folders- 📡 Direct SMB/CIFS network access (no file copying required)

- 📡 SMB/CIFS network access (no file copying required)- 💾 Multiple connection profiles with auto-resume

- 💾 Multiple connection profiles with auto-resume- 🎬 Configurable slideshow with countdown timer (1-300 seconds)

- 🎬 Configurable slideshow (1-300 seconds)- 🎮 Invisible touch zones for distraction-free viewing

- 🎮 Invisible touch zones for distraction-free viewing- 🔄 Image rotation with ABC/Random ordering

- 🔄 Image rotation with ABC/Random ordering- 📦 File sorting: Copy/Move/Delete images to 10 configurable destinations

- 📦 File sorting: Copy/Move/Delete to 10 destinations- ⚡ Image preloading for instant transitions (slideshow and sort)

- ⚡ Image preloading for instant transitions- ⚙️ Flexible settings: default credentials, operation permissions

- ⚙️ Flexible settings: permissions, default credentials- � Optimized for both phones and tablets



## Quick Start## Quick Start



### Local Folders (Device Storage)1. **Connect to Network Share:**

   - Enter folder address: `192.168.1.100\Photos`

1. **Grant Media Access:**   - Provide credentials (or leave empty for guest access)

   - Go to **Settings** tab   - Set slideshow interval (5-60 seconds)

   - Tap **Grant Media Access**

   - Allow READ_MEDIA_IMAGES permission2. **Start Slideshow:**

   - Tap **Slideshow** button

2. **Select Folder:**   - View images in fullscreen mode

   - Switch to **Local Folders** tab

   - Tap folder: Camera 📷, Screenshots 📸, Pictures 🖼️, Downloads 📥3. **Touch Controls During Slideshow:**

   - Or **+ Add Custom Folder**

```

3. **Start Slideshow:**┌─────────────────────────────┐

   - Set interval (1-300 sec)│  BACK   │   ABC/RND toggle  │ ← Top 12.5%

   - Tap **Slideshow**├─────────────────────────────┤

│         │        │          │

### Network Share (SMB)│  PREV   │ PAUSE  │   NEXT   │ ← Middle 75%

│         │        │          │

1. **Connect:**├─────────────────────────────┤

   - Switch to **Network** tab│  ROTATE │      ROTATE       │ ← Bottom 12.5%

   - Enter: `192.168.1.100\Photos`│   LEFT  │       RIGHT       │

   - Credentials (or empty for guest)└─────────────────────────────┘

```

2. **Start Slideshow:**

   - Tap **Slideshow**## Features



## Touch Controls (Slideshow)### Connection Management

- Connect to SMB servers over local network

```- Save multiple SMB connection configurations

┌─────────────────────────────┐- SQLite database for persistent storage (Room v2)

│  BACK   │   ABC/RND toggle  │ ← Top 12.5%- Auto-load last used connection on startup

├─────────────────────────────┤- Anonymous and authenticated access support

│         │        │          │- Dual purpose: Slideshow source or Sort destination

│  PREV   │ PAUSE  │   NEXT   │ ← Middle 75%

│         │        │          │### Sort Destinations (0-9)

├─────────────────────────────┤- Configure up to 10 sort destinations from saved connections

│  ROTATE │      ROTATE       │ ← Bottom 12.5%- Each destination has:

│   LEFT  │       RIGHT       │  - **Sort order** (0-9) - position in button list

└─────────────────────────────┘  - **Sort name** - short display name (max 20 chars)

```  - **Unique color** - 10 distinct colors for visual identification

- Automatic reordering when destinations added/removed

## Features- Used by both Copy and Move operations in Sort screen



### Two-Tab Interface### Slideshow

- **Local Folders** - Android device storage access- Automatic image slideshow with configurable interval: **1-300 seconds** (default: 10)

  - Standard folders: Camera, Screenshots, Pictures, Downloads- **Image preloading optimization**: Background loading of next image for instant transitions

  - Shows only folders with images (count in parentheses)  - Preload starts when interval > 2× load time

  - Add custom folders via Storage Access Framework (SAF)  - Manual next: instant switch using preloaded image

  - Single tap: select, Double tap: start slideshow  - Auto-advance: seamless transition with preloaded data

  - **Note**: LOCAL folders can be slideshow/sort SOURCE only (not destination)- Fullscreen mode with original image proportions (fitCenter)

  - Supports: JPG, JPEG, PNG, GIF, BMP, WebP

- **Network** - SMB network connections- Orientation support (portrait and landscape)

  - Save multiple SMB configurations- State preservation on screen rotation

  - Auto-load last used connection

  - Can be both source AND destination### Touch Controls (Invisible Zones)

- **Top 12.5% of screen** (divided into 2 equal horizontal zones):

### Permissions  - **Left half** - Back to connection selection

- **Android 13+**: READ_MEDIA_IMAGES  - **Right half** - Toggle ABC ⇄ Random order

- **Android 10-12**: READ_EXTERNAL_STORAGE- **Middle 75% of screen** (divided into 3 equal horizontal zones):

- Request via Settings tab  - **Left third** - Previous image (auto-pause enabled)

- On-demand (not required at startup)  - **Center third** - Pause/Resume slideshow

  - **Right third** - Next image (timer reset)

### Local Storage Access- **Bottom 12.5% of screen** (divided into 2 equal horizontal zones):

- **MediaStore API** for standard folders  - **Left half** - Rotate image 90° counter-clockwise

- **Storage Access Framework (SAF)** for custom folders  - **Right half** - Rotate image 90° clockwise

- Persistable URI permissions  - Rotation persists for all subsequent images until changed

- No file copying required

### Slideshow Features

### Slideshow- Configurable interval: **1-300 seconds** (default: 10)

- **1-300 seconds** interval (default: 10)- Countdown timer: shows "in 3/2/1" for last 3 seconds

- **Image preloading**: background loading for instant transitions- Shows interval number on resume for 1 second

- Fullscreen, original proportions (fitCenter)- Auto-pause on: rotation, previous image, manual back

- Formats: JPG, PNG, GIF, BMP, WebP- Timer reset on: next image click

- Touch zones (invisible controls)- ABC ordering: alphabetically by filename

- State preservation on rotation- Random ordering: shuffled on toggle, remembers mode between sessions

- Session auto-resume: starts from last viewed image on app relaunch

### Sort Screen- **Performance**: Preloads next image in background for smooth transitions

- **Copy/Move/Delete** to 10 destinations

- Image preloading for instant navigation### UI Features

- File info: name, size, date- Material Design 3 with adaptive icons

- Colored buttons per destination- Compact layout without ActionBar (maximized space)

- Progress overlay- Icon-based buttons (Settings ⚙️, Save 💾)

- Counter (e.g., "5 / 23")- RecyclerView with up to 5 visible connections (scrollable)

- Touch zone visualization in Settings screen

### Settings- Fullscreen slideshow without visible controls

- **Slideshow** tab - touch zones guide- Vertical scroll support for landscape orientation

- **Sort to..** tab - 10 destinations (0-9)

- **Settings** tab:## Requirements

  - Default username/password

  - Allow Copy/Move/Delete (ON/OFF)- Android 9.0 (API 28) or higher

  - Confirm delete dialog (ON/OFF)- Local network access

  - **Grant Media Access** button- SMB server with shared folder containing images



## Requirements## Build APK



- Android 9.0+ (API 28)```bash

- Local network (for SMB)./gradlew assembleDebug

- SMB server with images (for network access)```



## Technical DetailsAPK file: `app/build/outputs/apk/debug/fastmediasorter.apk`



- **Language**: Kotlin 1.9.10For release build:

- **Target SDK**: Android 14 (API 34)```bash

- **Min SDK**: Android 9 (API 28)./gradlew assembleRelease

- **Database**: Room 2.6.1 (SQLite v3)```

- **SMB**: jCIFS-ng 2.1.10

- **Image**: Glide 4.16.0APK file: `app/build/outputs/apk/release/fastmediasorter.apk`

- **UI**: Material 3, ViewPager2, TabLayout

- **Async**: Coroutines 1.7.3## Setup



## Project Structure1. Enter folder address (combined format): `192.168.1.100\Photos\Vacation`

2. Provide username and password (leave empty for anonymous/guest access)

```3. Enter connection name (optional - auto-generated from address if empty)

app/src/main/java/com/sza/fastmediasorter/4. Set slide interval in seconds (5-60, default: 10)

├── MainActivity.kt - TabLayout with Local/Network5. Click **💾 Save** to store connection for future use

├── data/6. Click **Slideshow** to start viewing

│   ├── ConnectionConfig.kt - v3 (type, localUri, localDisplayName)

│   ├── AppDatabase.kt - migration 2→3## Usage

│   └── ConnectionRepository.kt

├── network/### Main Screen

│   ├── SmbClient.kt - SMB operations- **Sort** button - Open file sorting mode for selected connection

│   ├── LocalStorageClient.kt - local storage operations- **⚙️ Settings** - Configure slideshow help and sort destinations

│   └── ImageRepository.kt- **Slideshow** - Start slideshow with current settings

├── ui/- **Interval** - Set seconds between images

│   ├── MainPagerAdapter.kt - ViewPager2 adapter- Tap saved connection to load its settings

│   ├── local/- Delete connection by clicking trash icon (🗑️)

│   │   ├── LocalFoldersFragment.kt

│   │   └── LocalFolderAdapter.kt### Slideshow Screen

│   ├── network/- Touch zones control everything (see diagram in Settings)

│   │   └── NetworkFragment.kt- Tap **right zone** - next image + timer reset (doesn't resume if paused)

│   ├── slideshow/- Tap **center zone** - pause/resume (shows interval on resume)

│   │   └── SlideshowActivity.kt- Tap **left zone** - previous image + auto-pause

│   ├── sort/- Top right zone - toggle between ABC (alphabetic) and RND (random) order

│   │   └── SortActivity.kt- Bottom zones - rotate current image ±90°

│   └── settings/- Device rotation preserved - slideshow continues

│       └── SettingsActivity.kt

└── utils/### Settings Screen

    └── PreferenceManager.kt- **Slideshow tab** - Visual guide showing all touch control zones with color-coded areas

```- **Sort to.. tab** - Configure up to 10 sort destinations (0-9)

  - Add destination: Select connection + enter short name (max 20 chars)

## Build  - Reorder destinations: Move Up/Down buttons

  - Delete destination: Remove from list

```bash  - Each destination gets unique color for easy identification

./gradlew assembleDebug- **Settings tab** - Application-wide settings:

```  - **Default Username/Password** - Auto-filled for connections without credentials

Output: `app/build/outputs/apk/debug/fastmediasorter.apk`  - **Sorting Settings**:

    - **Allow to Copy** - Enable/disable Copy buttons (default: ON)

Release:    - **Allow to Move** - Enable/disable Move buttons (default: OFF)

```bash    - **Allow to Delete** - Enable/disable Delete button (default: OFF)

./gradlew assembleRelease    - **Request user for deletion** - Confirmation dialog before delete (default: ON)

```- Contact information: sza@ukr.net 2025

Output: `app/build/outputs/apk/release/fastmediasorter.apk`

### Sort Screen

## Privacy- **Navigation**: Left/right touch zones (50/50 split) with infinite cycling

- **Image preloading**: Background loading of next image for instant navigation

- No data collection- **File info**: Displays filename, size (KB), and modification date

- No internet access (only local network)- **Connection name**: Shows current connection at top of screen

- No analytics/tracking- **Copy to..**: Up to 10 colored buttons for copying current image to destinations

- All settings stored locally  - Visible only if "Allow to Copy" enabled in Settings

  - File remains in source folder

See: [PRIVACY_POLICY.md](PRIVACY_POLICY.md)  - Auto-advance to next image on success

  - Error handling: Already exists, same folder, network/security errors

## License- **Move to..**: Up to 10 colored buttons for moving current image to destinations

  - Visible only if "Allow to Move" enabled in Settings

MIT License  - File deleted from source after successful copy

  - Removed from current list, auto-load next image

## Contact  - Error handling: Copy errors + delete permission errors

- **Delete**: Red button for deleting current image

sza@ukr.net - 2025  - Visible only if "Allow to Delete" enabled in Settings

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