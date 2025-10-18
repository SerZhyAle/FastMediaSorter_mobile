# FastMediaSorter Mobile

A lightweight and private Android application for viewing, sorting, and managing images and videos from your device's local storage and SMB (Samba/Windows) network shares. Perfect for creating a digital photo frame, organizing media, or simply browsing your collection with intuitive controls.

## Key Features

-   **Dual Media Access**: Seamlessly browse media from both local device folders (Camera, Screenshots, etc.) and remote SMB network shares.
-   **Powerful Slideshow**:
    -   Supports both images and videos.
    -   Configurable interval (1-300 seconds).
    -   Image and video preloading for smooth, instant transitions.
    -   Two control modes: invisible touch zones or a visible button panel.
    -   ABC (alphabetical) and Random (shuffle) order modes.
-   **Advanced File Sorting**:
    -   Organize media by copying, moving, or deleting files.
    -   Configure up to 10 custom sort destinations.
    -   Safe by design: uses system permissions for delete/move operations on Android 11+.
-   **Video Settings**:
    -   Enable or disable video playback in slideshows.
    -   Set a maximum file size for videos to prevent playback of large files.
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
2.  Enter the server address and folder path (e.g., `192.168.1.100\Photos`).
3.  Provide a username and password, or leave them blank for guest access.
4.  Optionally, enter a name for the connection and tap the **Save** icon (💾).
5.  Tap **Slideshow** or **Sort** to begin.

## Usage Guide

### Slideshow Controls

The slideshow offers two control modes, which can be toggled in **Settings**.

**Mode 1: Invisible Touch Zones (Default)**

The screen is divided into invisible zones for a distraction-free experience.

'''
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
'''

**Mode 2: Visible Button Panel**

Enable this mode in **Settings → Slideshow Settings → Show Controls**. A minimalist panel with buttons for Back, Previous, Play/Pause, Next, and Rotate will appear.

### Sorting Screen

-   **Navigate**: Swipe left or right to browse through media.
-   **Copy/Move/Delete**: Use the colored buttons at the bottom to sort the current file. These operations can be enabled or disabled in **Settings**.
-   **Destinations**: Configure up to 10 sort destinations in the **Settings → Sort to..** tab.

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
-   **Image Loading**: Glide for efficient image loading and caching.
-   **Minimum SDK**: API 28 (Android 9.0)
-   **Target SDK**: API 34 (Android 14)

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact

sza@ukr.net - 2025
