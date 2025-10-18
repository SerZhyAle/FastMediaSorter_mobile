package com.sza.fastmediasorter.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val KEY_SERVER_ADDRESS = "server_address"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_FOLDER_PATH = "folder_path"
        private const val KEY_INTERVAL = "interval"
        private const val KEY_LAST_FOLDER_ADDRESS = "last_folder_address"
        private const val KEY_LAST_IMAGE_INDEX = "last_image_index"
        private const val KEY_SHUFFLE_MODE = "shuffle_mode"
        private const val KEY_DEFAULT_USERNAME = "default_username"
        private const val KEY_DEFAULT_PASSWORD = "default_password"
        private const val KEY_ALLOW_MOVE = "allow_move"
        private const val KEY_ALLOW_COPY = "allow_copy"
        private const val KEY_ALLOW_DELETE = "allow_delete"
        private const val KEY_CONFIRM_DELETE = "confirm_delete"
        private const val KEY_LOCAL_URI = "local_uri"
        private const val KEY_LOCAL_BUCKET_NAME = "local_bucket_name"
        private const val KEY_CONNECTION_TYPE = "connection_type"
        private const val KEY_SHOW_CONTROLS = "show_controls"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_WELCOME_SHOWN = "welcome_shown"
        private const val KEY_VIDEO_ENABLED = "video_enabled"
        private const val KEY_MAX_VIDEO_SIZE_MB = "max_video_size_mb"
    }
    
    init {
        migratePasswordsToEncrypted()
    }
    
    private fun migratePasswordsToEncrypted() {
        val oldPassword = prefs.getString(KEY_PASSWORD, null)
        val oldDefaultPassword = prefs.getString(KEY_DEFAULT_PASSWORD, null)
        
        if (oldPassword != null && !encryptedPrefs.contains(KEY_PASSWORD)) {
            encryptedPrefs.edit().putString(KEY_PASSWORD, oldPassword).apply()
            prefs.edit().remove(KEY_PASSWORD).apply()
        }
        
        if (oldDefaultPassword != null && !encryptedPrefs.contains(KEY_DEFAULT_PASSWORD)) {
            encryptedPrefs.edit().putString(KEY_DEFAULT_PASSWORD, oldDefaultPassword).apply()
            prefs.edit().remove(KEY_DEFAULT_PASSWORD).apply()
        }
    }
    
    fun saveConnectionSettings(server: String, username: String, password: String, folder: String) {
        prefs.edit().apply {
            putString(KEY_SERVER_ADDRESS, server)
            putString(KEY_USERNAME, username)
            putString(KEY_FOLDER_PATH, folder)
            putString(KEY_CONNECTION_TYPE, "SMB")
            apply()
        }
        encryptedPrefs.edit().putString(KEY_PASSWORD, password).apply()
    }
    
    fun saveLocalFolderSettings(localUri: String, bucketName: String, interval: Int) {
        prefs.edit().apply {
            putString(KEY_LOCAL_URI, localUri)
            putString(KEY_LOCAL_BUCKET_NAME, bucketName)
            putInt(KEY_INTERVAL, interval)
            putString(KEY_CONNECTION_TYPE, "LOCAL")
            apply()
        }
    }
    
    fun getConnectionType(): String = prefs.getString(KEY_CONNECTION_TYPE, "SMB") ?: "SMB"
    fun getLocalUri(): String = prefs.getString(KEY_LOCAL_URI, "") ?: ""
    fun getLocalBucketName(): String = prefs.getString(KEY_LOCAL_BUCKET_NAME, "") ?: ""
    fun getServerAddress(): String = prefs.getString(KEY_SERVER_ADDRESS, "") ?: ""
    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getPassword(): String = encryptedPrefs.getString(KEY_PASSWORD, "") ?: ""
    fun getFolderPath(): String = prefs.getString(KEY_FOLDER_PATH, "") ?: ""
    fun getInterval(): Int = prefs.getInt(KEY_INTERVAL, 10)
    
    fun setInterval(interval: Int) {
        prefs.edit().putInt(KEY_INTERVAL, interval).apply()
    }
    
    fun saveLastSession(folderAddress: String, imageIndex: Int) {
        prefs.edit().apply {
            putString(KEY_LAST_FOLDER_ADDRESS, folderAddress)
            putInt(KEY_LAST_IMAGE_INDEX, imageIndex)
            apply()
        }
    }
    
    fun getLastFolderAddress(): String = prefs.getString(KEY_LAST_FOLDER_ADDRESS, "") ?: ""
    fun getLastImageIndex(): Int = prefs.getInt(KEY_LAST_IMAGE_INDEX, 0)
    
    fun clearLastSession() {
        prefs.edit().apply {
            remove(KEY_LAST_FOLDER_ADDRESS)
            remove(KEY_LAST_IMAGE_INDEX)
            apply()
        }
    }
    
    fun setShuffleMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE_MODE, enabled).apply()
    }
    
    fun isShuffleMode(): Boolean = prefs.getBoolean(KEY_SHUFFLE_MODE, false)
    
    // Default credentials
    fun setDefaultUsername(username: String) {
        prefs.edit().putString(KEY_DEFAULT_USERNAME, username).apply()
    }
    
    fun getDefaultUsername(): String = prefs.getString(KEY_DEFAULT_USERNAME, "") ?: ""
    
    fun setDefaultPassword(password: String) {
        encryptedPrefs.edit().putString(KEY_DEFAULT_PASSWORD, password).apply()
    }
    
    fun getDefaultPassword(): String = encryptedPrefs.getString(KEY_DEFAULT_PASSWORD, "") ?: ""
    
    // Allow Move operations
    fun setAllowMove(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_MOVE, allow).apply()
    }
    
    fun isAllowMove(): Boolean = prefs.getBoolean(KEY_ALLOW_MOVE, false)
    
    // Allow Copy operations
    fun setAllowCopy(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_COPY, allow).apply()
    }
    
    fun isAllowCopy(): Boolean = prefs.getBoolean(KEY_ALLOW_COPY, true)
    
    // Allow Delete operations
    fun setAllowDelete(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_DELETE, allow).apply()
    }
    
    fun isAllowDelete(): Boolean = prefs.getBoolean(KEY_ALLOW_DELETE, false)
    
    // Confirm deletion
    fun setConfirmDelete(confirm: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_DELETE, confirm).apply()
    }
    
    fun isConfirmDelete(): Boolean = prefs.getBoolean(KEY_CONFIRM_DELETE, true)
    
    // Show controls in slideshow
    fun setShowControls(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_CONTROLS, show).apply()
    }
    
    fun isShowControls(): Boolean = prefs.getBoolean(KEY_SHOW_CONTROLS, true)
    
    // First launch flag
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
    
    // Welcome screen shown
    fun isWelcomeShown(): Boolean = prefs.getBoolean(KEY_WELCOME_SHOWN, false)
    
    fun setWelcomeShown() {
        prefs.edit().putBoolean(KEY_WELCOME_SHOWN, true).apply()
    }
    
    // Video playback settings
    fun isVideoEnabled(): Boolean = prefs.getBoolean(KEY_VIDEO_ENABLED, false)
    
    fun setVideoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_ENABLED, enabled).apply()
    }
    
    fun getMaxVideoSizeMb(): Int = prefs.getInt(KEY_MAX_VIDEO_SIZE_MB, 100)
    
    fun setMaxVideoSizeMb(sizeMb: Int) {
        prefs.edit().putInt(KEY_MAX_VIDEO_SIZE_MB, sizeMb).apply()
    }
}