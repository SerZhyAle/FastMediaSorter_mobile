# Changelog

## [1.1.0] - 2025-10-20

### Added
- **Write Permissions System**: Automatic detection and validation of write permissions for SMB folders
  - Database migration to version 5 with write permission tracking
  - UI restrictions based on folder permissions (disabled sort buttons for read-only folders)
  - Prevention of adding folders without write access to sort destinations
- **Enhanced Empty Folder Handling**: Empty folders now show warning messages instead of error messages
- **Video Playback Control**: New "Play video till end" option in Video Settings
  - Videos can now play completely or follow slideshow interval timing
  - Setting is properly integrated with slideshow controls
- **Improved Slideshow Transitions**: Fixed image flashing during video-to-image transitions
  - Added proper content clearing between different media types
  - Smoother visual experience during slideshow playback

### Fixed
- **Interval Persistence Bug**: Fixed slideshow interval settings not persisting between app sessions
  - Proper interval saving for both database configurations and local folders
  - PreferenceManager fallback for missing database values
  - Comprehensive interval loading/saving logic across all configuration types

### Enhanced
- **Database Schema**: Updated to version 5 with write permission support
- **Error Handling**: Improved error messages and fallback mechanisms
- **UI/UX**: Better visual feedback for folder access status and permissions
- **Performance**: Optimized media loading and transition handling

### Technical
- Added `updateConfigInterval()` methods across DAO, Repository, and ViewModel layers
- Enhanced `SmbClient` with write permission testing capabilities
- Improved `PreferenceManager` with comprehensive interval storage
- Updated database migration system with proper error handling

## [1.0.3] - Previous Release
- Initial stable release with basic slideshow and sorting functionality