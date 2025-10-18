# FastMediaSorter Mobile# FastMediaSorter Mobile# FastMediaSorter Mobile



Android app for viewing images from local device storage and SMB network shares with automatic slideshow and intuitive touch controls.



## OverviewAndroid app for viewing images from local device storage and SMB network shares with automatic slideshow and intuitive touch controls.Android app for viewing images from SMB network shares with automatic slideshow and intuitive touch controls.



FastMediaSorter is a lightweight Android application for browsing and displaying photos from your Android device (Camera, Screenshots, Pictures, Downloads) or network storage (NAS, Windows shares, Samba servers). Perfect for digital photo frames, presentations, or quick photo access.



**Key Features:**## Overview## Overview

- 📱 **Local folders access**: Camera, Screenshots, Pictures, Downloads + custom folders

- 📡 **SMB/CIFS network access** (no file copying required)

- 💾 Multiple connection profiles with auto-resume

- 🎬 Configurable slideshow with countdown timer (1-300 seconds)FastMediaSorter is a lightweight Android application for browsing and displaying photos from your Android device (Camera, Screenshots, Pictures, Downloads) or network storage (NAS, Windows shares, Samba servers). Perfect for digital photo frames, presentations, or quick photo access.FastMediaSorter is a lightweight Android application designed for browsing and displaying photos stored on network-attached storage (NAS), Windows shares, or Samba servers. Perfect for creating digital photo frames, presentations, or quick access to network photo collections.

- 🎮 **Two control modes**: Invisible touch zones OR visible button panel

- 🔄 Image rotation with ABC/Random ordering

- 📦 File sorting: Copy/Move/Delete images to 10 configurable destinations

- ⚡ Image preloading for instant transitions (slideshow and sort)**Key Features:****Key Features:**

- 🔐 **Android 11+ delete permissions**: System dialog for safe file deletion

- ⚙️ Flexible settings: default credentials, operation permissions- 📱 **Local folders access**: Camera, Screenshots, Pictures, Downloads + custom folders- 📡 Direct SMB/CIFS network access (no file copying required)

- 📱 Optimized for both phones and tablets

- 📡 SMB/CIFS network access (no file copying required)- 💾 Multiple connection profiles with auto-resume

## Quick Start

- 💾 Multiple connection profiles with auto-resume- 🎬 Configurable slideshow with countdown timer (1-300 seconds)

### Local Folders (Device Storage)

- 🎬 Configurable slideshow (1-300 seconds)- 🎮 Invisible touch zones for distraction-free viewing

1. **Grant Media Access:**

   - Go to **Settings** tab- 🎮 Invisible touch zones for distraction-free viewing- 🔄 Image rotation with ABC/Random ordering

   - Tap **Grant Media Access**

   - Allow READ_MEDIA_IMAGES permission- 🔄 Image rotation with ABC/Random ordering- 📦 File sorting: Copy/Move/Delete images to 10 configurable destinations



2. **Select Folder:**- 📦 File sorting: Copy/Move/Delete to 10 destinations- ⚡ Image preloading for instant transitions (slideshow and sort)

   - Switch to **Local Folders** tab

   - Tap folder: Camera 📷, Screenshots 📸, Pictures 🖼️, Downloads 📥- ⚡ Image preloading for instant transitions- ⚙️ Flexible settings: default credentials, operation permissions

   - Or **+ Add Custom Folder**

- ⚙️ Flexible settings: permissions, default credentials- � Optimized for both phones and tablets

3. **Start Slideshow:**

   - Set interval (1-300 sec)

   - Tap **Slideshow**

## Quick Start## Quick Start

### Network Share (SMB)



1. **Connect:**

   - Switch to **Network** tab### Local Folders (Device Storage)1. **Connect to Network Share:**

   - Enter: `192.168.1.100\Photos`

   - Credentials (or empty for guest)   - Enter folder address: `192.168.1.100\Photos`



2. **Start Slideshow:**1. **Grant Media Access:**   - Provide credentials (or leave empty for guest access)

   - Tap **Slideshow**

   - Go to **Settings** tab   - Set slideshow interval (5-60 seconds)

## Slideshow Controls

   - Tap **Grant Media Access**

### Mode 1: Invisible Touch Zones (Default)

   - Allow READ_MEDIA_IMAGES permission2. **Start Slideshow:**

```

┌─────────────────────────────┐   - Tap **Slideshow** button

│  BACK   │   ABC/RND toggle  │ ← Top 12.5%

├─────────────────────────────┤2. **Select Folder:**   - View images in fullscreen mode

│         │        │          │

│  PREV   │ PAUSE  │   NEXT   │ ← Middle 75%   - Switch to **Local Folders** tab

│         │        │          │

├─────────────────────────────┤   - Tap folder: Camera 📷, Screenshots 📸, Pictures 🖼️, Downloads 📥3. **Touch Controls During Slideshow:**

│  ROTATE │      ROTATE       │ ← Bottom 12.5%

│   LEFT  │       RIGHT       │   - Or **+ Add Custom Folder**

└─────────────────────────────┘

``````



### Mode 2: Visible Button Panel (Optional)3. **Start Slideshow:**┌─────────────────────────────┐



Enable in Settings → Slideshow Settings → **Show Controls**   - Set interval (1-300 sec)│  BACK   │   ABC/RND toggle  │ ← Top 12.5%



Minimalist button panel at bottom:   - Tap **Slideshow**├─────────────────────────────┤

- **←** Back to menu

- **◄** Previous image│         │        │          │

- **⏸/▶** Play/Pause

- **►** Next image### Network Share (SMB)│  PREV   │ PAUSE  │   NEXT   │ ← Middle 75%

- **↶** Rotate left

- **↷** Rotate right│         │        │          │

- **🔀** Shuffle mode

1. **Connect:**├─────────────────────────────┤

## Features

   - Switch to **Network** tab│  ROTATE │      ROTATE       │ ← Bottom 12.5%

### Connection Management

   - Enter: `192.168.1.100\Photos`│   LEFT  │       RIGHT       │

**Local Folders:**

- Standard folders: Camera, Screenshots, Pictures, Downloads   - Credentials (or empty for guest)└─────────────────────────────┘

- Shows only folders with images (count in parentheses)

- Add custom folders via Storage Access Framework (SAF)```

- Single tap: select, Double tap: start slideshow

- **Important**: Local folders can only be SOURCE (not sort destination)2. **Start Slideshow:**



**Network (SMB):**   - Tap **Slideshow**## Features

- Connect to SMB servers over local network

- Save multiple SMB connection configurations

- SQLite database for persistent storage (Room v2)

- Auto-load last used connection on startup## Touch Controls (Slideshow)### Connection Management

- Anonymous and authenticated access support

- Can be both slideshow source AND sort destination- Connect to SMB servers over local network



### Sort Destinations (0-9)```- Save multiple SMB connection configurations



- Configure up to 10 sort destinations from saved connections┌─────────────────────────────┐- SQLite database for persistent storage (Room v2)

- Each destination has:

  - **Sort order** (0-9) - position in button list│  BACK   │   ABC/RND toggle  │ ← Top 12.5%- Auto-load last used connection on startup

  - **Sort name** - short display name (max 20 chars)

  - **Unique color** - 10 distinct colors for visual identification├─────────────────────────────┤- Anonymous and authenticated access support

- Automatic reordering when destinations added/removed

- Used by both Copy and Move operations in Sort screen│         │        │          │- Dual purpose: Slideshow source or Sort destination



### Slideshow│  PREV   │ PAUSE  │   NEXT   │ ← Middle 75%



- Configurable interval: **1-300 seconds** (default: 10)│         │        │          │### Sort Destinations (0-9)

- **Image preloading optimization**: Background loading of next image for instant transitions

  - Preload starts when interval > 2× load time├─────────────────────────────┤- Configure up to 10 sort destinations from saved connections

  - Manual next: instant switch using preloaded image

  - Auto-advance: seamless transition with preloaded data│  ROTATE │      ROTATE       │ ← Bottom 12.5%- Each destination has:

- Fullscreen mode with original image proportions (fitCenter)

- Supports: JPG, JPEG, PNG, GIF, BMP, WebP│   LEFT  │       RIGHT       │  - **Sort order** (0-9) - position in button list

- Orientation support (portrait and landscape)

- State preservation on screen rotation└─────────────────────────────┘  - **Sort name** - short display name (max 20 chars)

- Countdown timer: shows "in 3/2/1" for last 3 seconds

- Shows interval number on resume for 1 second```  - **Unique color** - 10 distinct colors for visual identification

- Auto-pause on: rotation, previous image, manual back

- Timer reset on: next image click- Automatic reordering when destinations added/removed

- ABC ordering: alphabetically by filename

- Random ordering: shuffled on toggle, remembers mode between sessions## Features- Used by both Copy and Move operations in Sort screen

- Session auto-resume: starts from last viewed image on app relaunch



### Sort Screen

### Two-Tab Interface### Slideshow

- **Copy/Move/Delete** operations to 10 destinations

- Image preloading for instant navigation (left/right zones)- **Local Folders** - Android device storage access- Automatic image slideshow with configurable interval: **1-300 seconds** (default: 10)

- File info: name, size, date

- Colored buttons per destination  - Standard folders: Camera, Screenshots, Pictures, Downloads- **Image preloading optimization**: Background loading of next image for instant transitions

- Progress overlay during operations

- Counter (e.g., "5 / 23")  - Shows only folders with images (count in parentheses)  - Preload starts when interval > 2× load time

- **Android 11+ safe deletion**:

  - System permission dialog for Move/Delete  - Add custom folders via Storage Access Framework (SAF)  - Manual next: instant switch using preloaded image

  - User must confirm each file deletion

  - Works for files created by other apps (Camera, Downloads)  - Single tap: select, Double tap: start slideshow  - Auto-advance: seamless transition with preloaded data

- Error handling: Network errors, security errors, already exists, same folder

  - **Note**: LOCAL folders can be slideshow/sort SOURCE only (not destination)- Fullscreen mode with original image proportions (fitCenter)

**Operations:**

- **Copy**: File remains in source, copy to destination  - Supports: JPG, JPEG, PNG, GIF, BMP, WebP

- **Move**: File copied then deleted from source (with permission on Android 11+)

- **Delete**: File permanently deleted (with permission on Android 11+)- **Network** - SMB network connections- Orientation support (portrait and landscape)



### Settings  - Save multiple SMB configurations- State preservation on screen rotation



**Slideshow Settings:**  - Auto-load last used connection

- **Show Controls** - Toggle between invisible zones and visible button panel

  - Can be both source AND destination### Touch Controls (Invisible Zones)

**Sort Destinations (0-9):**

- Add destination: Select connection + enter short name (max 20 chars)- **Top 12.5% of screen** (divided into 2 equal horizontal zones):

- Reorder destinations: Move Up/Down buttons

- Delete destination: Remove from list### Permissions  - **Left half** - Back to connection selection

- Each destination gets unique color for easy identification

- **Android 13+**: READ_MEDIA_IMAGES  - **Right half** - Toggle ABC ⇄ Random order

**Application Settings:**

- **Default Username/Password** - Auto-filled for connections without credentials- **Android 10-12**: READ_EXTERNAL_STORAGE- **Middle 75% of screen** (divided into 3 equal horizontal zones):

- **Sorting Permissions**:

  - **Allow to Copy** - Enable/disable Copy buttons (default: ON)- Request via Settings tab  - **Left third** - Previous image (auto-pause enabled)

  - **Allow to Move** - Enable/disable Move buttons (default: OFF)

  - **Allow to Delete** - Enable/disable Delete button (default: OFF)- On-demand (not required at startup)  - **Center third** - Pause/Resume slideshow

  - **Request user for deletion** - Confirmation dialog before delete (default: ON)

- **Grant Media Access** - Request READ_MEDIA_IMAGES permission  - **Right third** - Next image (timer reset)



## Android Permissions### Local Storage Access- **Bottom 12.5% of screen** (divided into 2 equal horizontal zones):



### Required Permissions- **MediaStore API** for standard folders  - **Left half** - Rotate image 90° counter-clockwise

- `READ_MEDIA_IMAGES` (Android 13+) - Access device photos

- `READ_EXTERNAL_STORAGE` (Android 10-12) - Legacy storage access- **Storage Access Framework (SAF)** for custom folders  - **Right half** - Rotate image 90° clockwise

- `WRITE_EXTERNAL_STORAGE` (Android 9) - Legacy storage write

- Persistable URI permissions  - Rotation persists for all subsequent images until changed

### Android 11+ Scoped Storage

- **Copy**: Works directly (read-only operation)- No file copying required

- **Move/Delete**: Requires system permission dialog

  - Uses `MediaStore.createDeleteRequest()`### Slideshow Features

  - User sees: "Allow FastMediaSorter to delete 1 item?"

  - Required for files created by other apps (Camera, Downloads, etc.)### Slideshow- Configurable interval: **1-300 seconds** (default: 10)

- **Fallback methods**:

  1. `ContentResolver.delete()` - for MediaStore URIs- **1-300 seconds** interval (default: 10)- Countdown timer: shows "in 3/2/1" for last 3 seconds

  2. `DocumentFile.delete()` - for SAF URIs

- **Image preloading**: background loading for instant transitions- Shows interval number on resume for 1 second

### Network Permissions

- `INTERNET` - SMB network access- Fullscreen, original proportions (fitCenter)- Auto-pause on: rotation, previous image, manual back

- `ACCESS_NETWORK_STATE` - Network status checks

- `ACCESS_WIFI_STATE` - WiFi status checks- Formats: JPG, PNG, GIF, BMP, WebP- Timer reset on: next image click



## Requirements- Touch zones (invisible controls)- ABC ordering: alphabetically by filename



- Android 9.0 (API 28) or higher- State preservation on rotation- Random ordering: shuffled on toggle, remembers mode between sessions

- Local network access (for SMB features)

- SMB server with shared folder containing images (optional)- Session auto-resume: starts from last viewed image on app relaunch



## Build APK### Sort Screen- **Performance**: Preloads next image in background for smooth transitions



Debug build:- **Copy/Move/Delete** to 10 destinations

```bash

./gradlew assembleDebug- Image preloading for instant navigation### UI Features

```

APK: `app/build/outputs/apk/debug/fastmediasorter.apk`- File info: name, size, date- Material Design 3 with adaptive icons



Release build:- Colored buttons per destination- Compact layout without ActionBar (maximized space)

```bash

./gradlew assembleRelease- Progress overlay- Icon-based buttons (Settings ⚙️, Save 💾)

```

APK: `app/build/outputs/apk/release/fastmediasorter.apk`- Counter (e.g., "5 / 23")- RecyclerView with up to 5 visible connections (scrollable)



## Technical Details- Touch zone visualization in Settings screen



- **Language**: Kotlin 1.9.10### Settings- Fullscreen slideshow without visible controls

- **Target SDK**: Android 14 (API 34)

- **Minimum SDK**: Android 9 (API 28)- **Slideshow** tab - touch zones guide- Vertical scroll support for landscape orientation

- **Build System**: Gradle 8.13 with KSP

- **Database**: Room 2.6.1 (SQLite, version 3 with local folder types)- **Sort to..** tab - 10 destinations (0-9)

- **SMB Library**: jCIFS-ng 2.1.10 (SMB 2/3 support)

- **Image Loading**: Glide 4.16.0- **Settings** tab:## Requirements

- **UI**: Material Design 3, ConstraintLayout, RecyclerView, TabLayout, ViewPager2

- **Async**: Kotlin Coroutines 1.7.3 with lifecycle-aware scopes  - Default username/password

- **Architecture**: MVVM with Repository pattern, LiveData reactivity

  - Allow Copy/Move/Delete (ON/OFF)- Android 9.0 (API 28) or higher

## Project Structure

  - Confirm delete dialog (ON/OFF)- Local network access

```

app/src/main/java/com/sza/fastmediasorter/  - **Grant Media Access** button- SMB server with shared folder containing images

├── MainActivity.kt - dual-tab UI (Local/Network), button state management

├── data/

│   ├── ConnectionConfig.kt - Room entity v3 (type, localUri, localDisplayName)

│   ├── ConnectionConfigDao.kt - database DAO with Flow, sort queries## Requirements## Build APK

│   ├── AppDatabase.kt - Room database v3 with migrations

│   └── ConnectionRepository.kt - CRUD + sort destination management

├── network/

│   ├── SmbClient.kt - jCIFS-ng wrapper with copy/move/delete + result types- Android 9.0+ (API 28)```bash

│   ├── LocalStorageClient.kt - MediaStore + SAF dual-access, Android 11+ delete

│   └── ImageRepository.kt - image loading with credentials fallback- Local network (for SMB)./gradlew assembleDebug

├── ui/

│   ├── MainPagerAdapter.kt - ViewPager2 for Local/Network tabs- SMB server with images (for network access)```

│   ├── ConnectionViewModel.kt - MVVM for connections & destinations

│   ├── ConnectionAdapter.kt - RecyclerView adapter with GestureDetector

│   ├── local/

│   │   ├── LocalFoldersFragment.kt - standard + custom folders## Technical DetailsAPK file: `app/build/outputs/apk/debug/fastmediasorter.apk`

│   │   └── LocalFolderAdapter.kt - folder list with selection highlight

│   ├── network/

│   │   └── NetworkFragment.kt - SMB connections management

│   ├── slideshow/- **Language**: Kotlin 1.9.10For release build:

│   │   └── SlideshowActivity.kt - dual control modes, preloading, state preservation

│   ├── settings/- **Target SDK**: Android 14 (API 34)```bash

│   │   ├── SettingsActivity.kt - TabLayout with 3 tabs

│   │   ├── SlideshowHelpFragment.kt - touch zones diagram- **Min SDK**: Android 9 (API 28)./gradlew assembleRelease

│   │   ├── SortHelpFragment.kt - destinations management

│   │   ├── SettingsFragment.kt - app settings (credentials, permissions)- **Database**: Room 2.6.1 (SQLite v3)```

│   │   ├── SortDestinationAdapter.kt - RecyclerView for destinations

│   │   └── AddSortDestinationDialog.kt - add destination dialog- **SMB**: jCIFS-ng 2.1.10

│   └── sort/

│       └── SortActivity.kt - sorting with preloading, Android 11+ delete permissions- **Image**: Glide 4.16.0APK file: `app/build/outputs/apk/release/fastmediasorter.apk`

└── utils/

    └── PreferenceManager.kt - SharedPreferences wrapper (credentials, permissions, showControls)- **UI**: Material 3, ViewPager2, TabLayout



res/- **Async**: Coroutines 1.7.3## Setup

├── drawable/

│   ├── ic_settings.xml - gear icon

│   ├── ic_save.xml - save icon

│   └── touch_zones_scheme.xml - control zones diagram## Project Structure1. Enter folder address (combined format): `192.168.1.100\Photos\Vacation`

└── layout/

    ├── activity_main.xml - 4-button compact layout2. Provide username and password (leave empty for anonymous/guest access)

    ├── activity_slideshow.xml - dual control system (zones + button panel)

    ├── activity_settings.xml - TabLayout + ViewPager2```3. Enter connection name (optional - auto-generated from address if empty)

    ├── activity_sort.xml - fullscreen with colored buttons

    ├── fragment_settings.xml - slideshow + sort settingsapp/src/main/java/com/sza/fastmediasorter/4. Set slide interval in seconds (5-60, default: 10)

    ├── item_local_folder.xml - folder list item with count

    ├── item_connection.xml - connection list item├── MainActivity.kt - TabLayout with Local/Network5. Click **💾 Save** to store connection for future use

    └── item_sort_destination.xml - destination with controls

```├── data/6. Click **Slideshow** to start viewing



## Key Improvements (Latest Updates)│   ├── ConnectionConfig.kt - v3 (type, localUri, localDisplayName)



1. **Local Folders Support**│   ├── AppDatabase.kt - migration 2→3## Usage

   - Standard folders with negative IDs (-1 to -4) to avoid DB pollution

   - LOCAL_STANDARD vs LOCAL_CUSTOM types│   └── ConnectionRepository.kt

   - Custom folders via SAF with persistable permissions

├── network/### Main Screen

2. **Button State Management**

   - Slideshow/Sort buttons enabled only when folder selected│   ├── SmbClient.kt - SMB operations- **Sort** button - Open file sorting mode for selected connection

   - `currentConfig` caching for negative ID handling

│   ├── LocalStorageClient.kt - local storage operations- **⚙️ Settings** - Configure slideshow help and sort destinations

3. **Slideshow Control Modes**

   - Invisible touch zones (default)│   └── ImageRepository.kt- **Slideshow** - Start slideshow with current settings

   - Visible button panel (Settings → Show Controls)

   - Material Design ImageButtons with system icons├── ui/- **Interval** - Set seconds between images



4. **Android 11+ Delete Permissions**│   ├── MainPagerAdapter.kt - ViewPager2 adapter- Tap saved connection to load its settings

   - `MediaStore.createDeleteRequest()` for safe deletion

   - System permission dialog for user confirmation│   ├── local/- Delete connection by clicking trash icon (🗑️)

   - Separate handling for Move vs Delete operations

   - Fallback to ContentResolver and DocumentFile│   │   ├── LocalFoldersFragment.kt



5. **Detailed Error Logging**│   │   └── LocalFolderAdapter.kt### Slideshow Screen

   - Copy/Move/Delete operations with full stack traces

   - Android version detection and method selection│   ├── network/- Touch zones control everything (see diagram in Settings)

   - File size, URI, and operation result logging

│   │   └── NetworkFragment.kt- Tap **right zone** - next image + timer reset (doesn't resume if paused)

6. **UI Improvements**

   - Selection highlight with white text on gray background│   ├── slideshow/- Tap **center zone** - pause/resume (shows interval on resume)

   - Icon emoji for standard folders (📷📸🖼️📥)

   - Count display for all folders including (0)│   │   └── SlideshowActivity.kt- Tap **left zone** - previous image + auto-pause



## Privacy│   ├── sort/- Top right zone - toggle between ABC (alphabetic) and RND (random) order



- **No data collection**: All settings stored locally on device│   │   └── SortActivity.kt- Bottom zones - rotate current image ±90°

- **No internet access**: Only local network (SMB) access required

- **No analytics or tracking**│   └── settings/- Device rotation preserved - slideshow continues

- Full privacy policy: [PRIVACY_POLICY.md](PRIVACY_POLICY.md)

│       └── SettingsActivity.kt

## License

└── utils/### Settings Screen

MIT License

    └── PreferenceManager.kt- **Slideshow tab** - Visual guide showing all touch control zones with color-coded areas

## Contact

```- **Sort to.. tab** - Configure up to 10 sort destinations (0-9)

sza@ukr.net - 2025

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