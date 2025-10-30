# Technical Description of FastMediaSorter Mobile - Preparation for Version 2

## Project Overview
FastMediaSorter Mobile is an Android application for viewing and sorting media files (images and videos) from network SMB folders and local storage.

## Plan for Creating Technical Description

### 0. Preparation
- [ ] Check and format all code files with ktlint

### 1. Code Documentation (English comments in Kotlin files)

#### Activities
- [x] `SlideshowActivity.kt` - slideshow screen (completed)
- [x] `SortActivity.kt` - file sorting screen (completed)
- [ ] `MainActivity.kt` - main application screen (completed)
- [ ] `WelcomeActivity.kt` - welcome screen and settings

#### Adapters
- [ ] `MainPagerAdapter.kt` - adapter for main screen ViewPager2
- [ ] `SortAdapter.kt` - adapter for file list in sorting
- [ ] `SlideshowAdapter.kt` - adapter for slideshow

#### Repositories
- [ ] `ImageRepository.kt` - repository for image operations
- [ ] `SortRepository.kt` - repository for sorting operations

#### Services and Utilities
- [ ] `LocalStorageClient.kt` - client for local storage operations
- [ ] `MediaUtils.kt` - utilities for media file operations
- [ ] `MediaValidator.kt` - media file validator
- [ ] `SmbClient.kt` - client for SMB connections
- [ ] `SmbDataSourceFactory.kt` - data source factory for SMB

#### Data Models
- [ ] `ConnectionConfig.kt` - connection configuration model
- [ ] `MediaError.kt` - media error model
- [ ] `FileInfo.kt` - file information model

#### Dialogs
- [ ] `DiagnosticDialog.kt` - error diagnostics dialog
- [ ] `SortDialog.kt` - file operation dialogs

### 2. Program Behavior Description (English description files)

#### Main Usage Scenarios
- [ ] `user_scenarios.md` - description of typical usage scenarios
- [ ] `navigation_flow.md` - description of navigation flow between screens
- [ ] `error_handling.md` - description of error handling and recovery

#### Architectural Solutions
- [ ] `architecture_overview.md` - application architecture overview
- [ ] `data_flow.md` - description of data flows
- [ ] `state_management.md` - application state management

#### External Integrations
- [ ] `smb_integration.md` - SMB protocol integration
- [ ] `local_storage_integration.md` - local storage operations
- [ ] `media_processing.md` - media file processing

### 3. Functional Breakdown by Sections

#### Core Functionality
- [ ] `core_features.md` - basic application capabilities
  - Image and video viewing
  - Slideshow navigation
  - Playback controls

#### Network Capabilities
- [ ] `network_features.md` - network resource operations
  - SMB connections
  - Authentication
  - Network error handling

#### Local Storage
- [ ] `local_features.md` - local file operations
  - MediaStore access
  - Folder filtering
  - Caching

#### Sorting and Organization
- [ ] `sorting_features.md` - sorting capabilities
  - File moving
  - Folder creation
  - File operations

#### User Interface
- [ ] `ui_features.md` - interface elements
  - Fullscreen mode
  - Gesture controls
  - Display settings

#### Settings and Configuration
- [ ] `settings_features.md` - settings system
  - Connection parameters
  - Playback settings
  - Interface preferences

### 4. Additional Materials

#### Technical Documentation
- [ ] `api_reference.md` - API reference
- [ ] `build_configuration.md` - build configuration
- [ ] `dependencies.md` - project dependencies

#### Testing and QA
- [ ] `testing_strategy.md` - testing strategy
- [ ] `known_issues.md` - known issues
- [ ] `performance_metrics.md` - performance metrics

#### Deployment and Support
- [ ] `deployment_guide.md` - deployment guide
- [ ] `troubleshooting.md` - troubleshooting
- [ ] `maintenance.md` - maintenance and support

## Next Steps

1. **Continue code documentation** - start with `WelcomeActivity.kt` (welcome screen and settings)
2. **Create structure for description files** - create `docs/` folder for description files
3. **Start behavior description** - begin with main usage scenarios

## Notes
- All comments in code must be in English in KDoc format
- Behavior description files must be in English
- Final technical description will be in Russian based on collected materials
- At this stage, only documentation changes, code is not modified