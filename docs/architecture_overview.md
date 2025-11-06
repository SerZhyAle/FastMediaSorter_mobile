# Architecture Overview of FastMediaSorter Mobile

## General Architecture

FastMediaSorter Mobile is built following **MVVM (Model-View-ViewModel)** architecture principles using modern Android components. The application is divided into layers to ensure separation of concerns, testability, and maintainability.

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Activities & Fragments (Views)                     │    │
│  │ - MainActivity, SlideshowActivity, SortActivity    │    │
│  │ - NetworkFragment, LocalFoldersFragment            │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ ViewModels (Business Logic)                        │    │
│  │ - ConnectionViewModel                              │    │
│  │ - UI state management                              │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Repositories (Data Access)                          │    │
│  │ - ImageRepository, SortRepository                   │    │
│  │ - LocalStorageClient, SmbClient                     │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Data Sources (Raw Data)                             │    │
│  │ - Room Database (ConnectionConfig)                  │    │
│  │ - SMB Protocol Implementation                       │    │
│  │ - MediaStore API                                    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Architecture Components

### 1. Presentation Layer

#### Activities
- **MainActivity:** Main screen with ViewPager2 for tabs
- **SlideshowActivity:** Full-screen media file viewing
- **SortActivity:** Sorting interface with assignment buttons
- **WelcomeActivity:** Onboarding for new users
- **SettingsActivity:** Application settings

#### Fragments
- **NetworkFragment:** SMB connection management
- **LocalFoldersFragment:** Local folder access
- **Settings Fragments:** Settings sections

#### ViewModels
- **ConnectionViewModel:** Connection configuration management
- **UI State Management:** Interface state and user preferences

### 2. Data Layer

#### Repositories
- **ImageRepository:** Image loading and caching
- **SortRepository:** Sorting and file moving operations
- **LocalStorageClient:** Local storage operations
- **SmbClient:** SMB protocol for network resources

#### Data Sources
- **Room Database:** Local DB for settings and configurations
- **SMB Implementation:** jcifs-ng library
- **MediaStore:** Android API for media files
- **SharedPreferences:** Simple settings

### 3. Core Components

#### Base Classes
- **LocaleActivity:** Base activity with localization support
- **Base Fragments:** Common fragment functions

#### Utilities
- **MediaUtils:** Media helper functions
- **PreferenceManager:** Settings management
- **Logger:** Logging with levels
- **ErrorDialogHelper:** Standardized error dialogs

## Architectural Patterns

### MVVM (Model-View-ViewModel)
- **View:** Activities and Fragments handle only UI
- **ViewModel:** Contains business logic and UI state
- **Model:** Repositories and data models

### Repository Pattern
- Data access abstraction
- Unified interface for different data sources
- Easy testing and implementation replacement

### Observer Pattern
- LiveData for reactive UI updates
- Coroutines for asynchronous operations
- Flow for data streams

## Key Technologies

### Android Framework
- **AndroidX:** Modern components
- **View Binding:** Safe View access
- **Room:** SQLite ORM
- **Coroutines:** Asynchronous programming

### Network Protocols
- **SMB/CIFS:** jcifs-ng library
- **HTTP:** For future extensions
- **WebSocket:** Potential for real-time updates

### Media Processing
- **Glide:** Image loading and caching
- **ExoPlayer:** Video playback
- **PhotoView:** Image scaling
- **MediaStore:** Device gallery access

### Security
- **EncryptedSharedPreferences:** Settings encryption
- **BouncyCastle:** Cryptographic functions
- **Scoped Storage:** Modern permission model

## Data Flow

### Unidirectional Flow
```
User Action → View → ViewModel → Repository → Data Source → Repository → ViewModel → LiveData → View
```

### Asynchronous Operations
- **Coroutines:** For non-blocking operations
- **Dispatchers:** IO for network/files, Main for UI
- **Exception Handling:** Try-catch with custom dialogs

### Caching
- **Memory Cache:** Glide for images
- **Disk Cache:** Room for configurations
- **Preloading:** Next file preloading

## Scalability and Extensibility

### Modular Structure
- **Feature Modules:** Logical division by functions
- **Dependency Injection:** Preparation for DI frameworks
- **Plugin Architecture:** Extension possibilities

### API Design
- **Clean Interfaces:** Clear contracts between layers
- **Error Handling:** Standardized error handling
- **Logging:** Detailed logging for debugging

### Testability
- **Unit Tests:** JUnit + Mockito for logic
- **Integration Tests:** Espresso for UI
- **Mock Implementations:** For dependency isolation

## Performance

### Optimizations
- **ViewPager2:** Efficient tab navigation
- **RecyclerView:** List virtualization
- **Background Processing:** Coroutines for heavy operations
- **Memory Management:** Resource cleanup on destroy

### Monitoring
- **Performance Metrics:** Loading time, memory usage
- **Error Tracking:** Exception logging
- **User Analytics:** Usage metrics

## Security

### User Data
- **Encryption:** Sensitive data encryption
- **Permissions:** Minimum required permissions
- **Input Validation:** All input data validation

### Network Security
- **Authentication:** Secure credential storage
- **Certificate Validation:** For future HTTPS connections
- **Timeout Protection:** Hanging prevention

## Future Architecture Improvements

### Dependency Injection
- **Hilt/Dagger:** For dependency management
- **Service Locator:** Alternative approach

### Reactive Programming
- **Flow/StateFlow:** More reactive architecture
- **Compose:** Modern UI framework

### Modularization
- **Dynamic Features:** Loadable modules
- **Multi-module Project:** Division into libraries</content>
<parameter name="filePath">c:\GIT\FastMediaSorter_mobile\docs\architecture_overview.md