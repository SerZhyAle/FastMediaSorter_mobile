package com.sza.fastmediasorter.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_SERVER_ADDRESS = "server_address"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_FOLDER_PATH = "folder_path"
        private const val KEY_INTERVAL = "interval"
    }
    
    fun saveConnectionSettings(server: String, username: String, password: String, folder: String) {
        prefs.edit().apply {
            putString(KEY_SERVER_ADDRESS, server)
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            putString(KEY_FOLDER_PATH, folder)
            apply()
        }
    }
    
    fun getServerAddress(): String = prefs.getString(KEY_SERVER_ADDRESS, "") ?: ""
    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""
    fun getFolderPath(): String = prefs.getString(KEY_FOLDER_PATH, "") ?: ""
    fun getInterval(): Int = prefs.getInt(KEY_INTERVAL, 10)
    
    fun setInterval(interval: Int) {
        prefs.edit().putInt(KEY_INTERVAL, interval).apply()
    }
}