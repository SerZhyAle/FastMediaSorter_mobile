# Language System Implementation

## Overview
The app now supports persistent language settings that apply globally across all activities and survive app restarts.

## Architecture

### 1. Base Activity: `LocaleActivity`
**Location:** `app/src/main/java/com/sza/fastmediasorter/ui/base/LocaleActivity.kt`

All activities inherit from this base class which:
- Overrides `attachBaseContext()` to apply language before activity creation
- Reads saved language from `PreferenceManager`
- Creates a new context with the selected locale
- Sets the locale as system default

```kotlin
open class LocaleActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateBaseContextLocale(newBase))
    }
    
    private fun updateBaseContextLocale(context: Context): Context {
        val preferenceManager = PreferenceManager(context)
        val languageCode = preferenceManager.getLanguage()
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}
```

### 2. Application Class: `FastMediaSorterApplication`
**Location:** `app/src/main/java/com/sza/fastmediasorter/FastMediaSorterApplication.kt`

The application class applies the saved language on app startup:
- Called when app process starts
- Sets default locale for the entire app
- Updates base configuration

### 3. Activities Updated

All activities now inherit from `LocaleActivity`:
- `MainActivity`
- `SettingsActivity`
- `SlideshowActivity`
- `WelcomeActivity`
- `SortActivity`

### 4. Settings Fragment
**Location:** `app/src/main/java/com/sza/fastmediasorter/ui/settings/SettingsActivity.kt`

Language selection:
- Uses `AutoCompleteTextView` spinner
- Saves language to `PreferenceManager` on change
- Calls `activity.recreate()` to apply immediately
- No need for app restart

## Data Flow

```
App Launch
    ↓
FastMediaSorterApplication.onCreate()
    ↓
applySavedLanguage() → reads PreferenceManager
    ↓
Locale.setDefault(savedLocale)
    ↓
Activity Launch
    ↓
LocaleActivity.attachBaseContext()
    ↓
updateBaseContextLocale() → reads PreferenceManager
    ↓
createConfigurationContext(newConfig)
    ↓
Activity displays with correct language
```

## User Flow

1. User opens Settings
2. User selects language from spinner
3. Language saved to SharedPreferences
4. Current activity recreates with new language
5. All activities now use new language
6. Language persists after app restart

## Supported Languages

- English (en)
- Russian (ru)
- Ukrainian (uk)

Defined in:
- `res/values/arrays.xml` (entries)
- `res/values-ru/strings.xml` (Russian translations)
- `res/values-uk/strings.xml` (Ukrainian translations)

## Storage

Language preference stored in SharedPreferences:
- Key: `"language"`
- Default value: `"en"`
- Managed by `PreferenceManager.getLanguage()` and `PreferenceManager.setLanguage()`

## Testing

To test language switching:
1. Open app
2. Navigate to Settings
3. Change language from spinner
4. Verify all UI elements update immediately
5. Close app completely
6. Reopen app
7. Verify language persisted

## Implementation Notes

- Uses `createConfigurationContext()` instead of deprecated `updateConfiguration()`
- Locale applied in `attachBaseContext()` before activity inflation
- Each activity gets fresh locale context on creation
- System default locale set for consistency
- Fragment recreation handled automatically by parent activity
