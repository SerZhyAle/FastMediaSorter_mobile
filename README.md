# FastMediaSorter Mobile

A lightweight and private Android application for viewing, sorting, and managing images and videos from your device's local storage and SMB (Samba/Windows) network shares. Perfect for creating a digital photo frame, organizing media, or simply browsing your collection with intuitive controls.

## Key Features

-   **Dual Media Access**: Seamlessly browse media from both local device folders (Camera, Screenshots, etc.) and remote SMB network shares.
-   **Smart Network Setup**:
    -   Adaptive subnet detection - automatically suggests IP templates based on your current network.
    -   DNS resolution with NetBIOS fallback for server names.
    -   Auto-discovery - scan and add all accessible SMB shares from a server with one tap.
    -   Connection testing with comprehensive diagnostics.
-   **Powerful Slideshow**:
    -   Supports both images and videos.
    -   Configurable interval (1-300 seconds).
    -   Image and video preloading for smooth, instant transitions.
    -   Two control modes: invisible touch zones or a visible button panel.
    -   ABC (alphabetical) and Random (shuffle) order modes.
    -   Media validation to skip corrupted files before playback attempts.
-   **Advanced File Sorting**:
    -   Organize media by copying, moving, or deleting files.
    -   Rename files with filename validation.
    -   Configure up to 10 custom sort destinations.
    -   Safe by design: uses system permissions for delete/move operations on Android 11+.
-   **Video Settings**:
    -   Enable or disable video playback in slideshows.
    -   Set a maximum file size for videos to prevent playback of large files.
    -   Detailed error diagnostics for troubleshooting video playback issues.
-   **Customizable Settings**:
    -   Control which operations are allowed (copy, move, delete, rename).
    -   Prevent device from sleeping during slideshow and sorting.
    -   Optional confirmation dialogs for destructive operations.
-   **Privacy-Focused**:
    -   No internet access required (only local network for SMB).
    -   No data collection, analytics, or tracking. All settings are stored locally on your device.
-   **Modern UI**:
    -   Clean, intuitive interface built with Material Design 3.
    -   Optimized for both phones and tablets in portrait and landscape orientations.

## Getting Started

### 1. Accessing Local Folders

1.  Navigate to the **Settings** tab.
2.  Tap **Grant Media Access** and allow the required permission.
3.  On the main screen, switch to the **Local Folders** tab.
4.  Select a folder (e.g., Camera, Screenshots) or tap **+ Add Custom Folder** to add your own.

### 2. Accessing a Network (SMB) Share

1.  On the main screen, switch to the **Network** tab.
2.  Enter the server address and folder path (e.g., `192.168.1.100\Photos` or `MYSERVER\Photos`).
    -   The app automatically detects your current subnet and suggests the appropriate IP template.
    -   You can use either IP addresses or server names (DNS resolution with NetBIOS fallback).
3.  Provide a username and password, or leave them blank for guest access.
4.  Optionally, enter a name for the connection and tap the **Save** icon (💾).
5.  **Auto-discovery**: Tap the **🔍** button to automatically scan and add all accessible shares from the server.
6.  Tap **Test** to verify the connection before saving.
7.  Tap **Slideshow** or **Sort** to begin.

## Usage Guide

### Slideshow Controls

The slideshow offers two control modes, which can be toggled in **Settings**.

**Mode 1: Invisible Touch Zones (Default)**

The screen is divided into invisible zones for a distraction-free experience.

![Touch Control Zones](assets/touch_zones.png)

**Mode 2: Visible Button Panel**

Enable this mode in **Settings → Slideshow Settings → Show Controls**. A minimalist panel with buttons for Back, Previous, Play/Pause, Next, and Rotate will appear.

### Sorting Screen

-   **Navigate**: Swipe left or right to browse through media.
-   **Copy/Move/Delete/Rename**: Use the buttons at the bottom to manage files. These operations can be enabled or disabled in **Settings**.
-   **Refresh**: Tap the refresh button at the top to re-read the folder contents.
-   **Destinations**: Configure up to 10 sort destinations in the **Settings → Sort to..** tab.

## Settings

### Sort to.. Settings

-   **Allow to Copy**: Enable copying files to destination folders.
-   **Allow to Move**: Enable moving files to destination folders.
-   **Allow to Delete**: Enable deleting files with optional confirmation.
-   **Allow renaming**: Enable renaming files (validates filename and prevents conflicts).

### Slideshow Settings

-   **Show Controls**: Toggle between invisible touch zones and visible button panel.

### Video Settings

-   **Enable Video**: Allow video playback in slideshows.
-   **Max Video Size**: Set maximum file size for videos (in MB).
-   **Show detailed video error information**: Display comprehensive error diagnostics when video playback fails.
-   **Supported Formats**: MP4, MKV, MOV, WEBM, 3GP (AVI format not supported for SMB streaming due to library compatibility issues).

### General Settings

-   **Not allow device to sleep**: Keep screen on during slideshow and sorting (enabled by default).

## Build from Source

To build the application from the source code, follow these steps:

1.  Clone the repository.
2.  To sign a release build, create a `keystore.properties` file in the root directory with your signing key details.
3.  Run the appropriate Gradle task:

    -   **Debug APK**:
        '''bash
        ./gradlew assembleDebug
        '''
    -   **Release App Bundle (for Google Play)**:
        '''bash
        ./gradlew bundleRelease
        '''

## Technical Details

-   **Language**: 100% Kotlin
-   **Architecture**: MVVM with a Repository pattern, using LiveData and Kotlin Coroutines.
-   **UI**: Material Design 3, ViewPager2, RecyclerView, and ConstraintLayout.
-   **Database**: Room for storing SMB connection profiles.
-   **Networking**: jCIFS-ng for SMBv2/SMBv3 client support.
-   **Media**: ExoPlayer for video playback with custom SMB data source.
-   **Image Loading**: Glide for efficient image loading and caching.
-   **Validation**: Fast media header validation (<100ms) to detect corrupted files before loading.
-   **Minimum SDK**: API 28 (Android 9.0)
-   **Target SDK**: API 34 (Android 14)

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact

sza@ukr.net - 2025
