package com.sza.fastmediasorter.network

import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageRepository(val smbClient: SmbClient, private val preferenceManager: PreferenceManager) {
    
    suspend fun loadImages(): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val serverAddress = preferenceManager.getServerAddress()
                var username = preferenceManager.getUsername()
                var password = preferenceManager.getPassword()
                val folderPath = preferenceManager.getFolderPath()
                
                // Use default credentials if not set
                if (username.isEmpty()) {
                    username = preferenceManager.getDefaultUsername()
                }
                if (password.isEmpty()) {
                    password = preferenceManager.getDefaultPassword()
                }
                
                if (serverAddress.isEmpty() || folderPath.isEmpty()) {
                    return@withContext Result.failure(Exception("Connection settings not configured"))
                }
                
                val connected = smbClient.connect(serverAddress, username, password)
                if (!connected) {
                    return@withContext Result.failure(Exception("Failed to connect to server: $serverAddress\n\nCheck:\n• Server is running?\n• Firewall settings?"))
                }
                
                val isVideoEnabled = preferenceManager.isVideoEnabled()
                val maxVideoSizeMb = preferenceManager.getMaxVideoSizeMb()
                val result = smbClient.getImageFiles(serverAddress, folderPath, isVideoEnabled, maxVideoSizeMb)
                if (result.errorMessage != null) {
                    return@withContext Result.failure(Exception(result.errorMessage))
                }
                
                Result.success(result.files)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun downloadImage(imageUrl: String): ByteArray? {
        return smbClient.downloadImage(imageUrl)
    }
    
    fun getSmbContext(): jcifs.CIFSContext? {
        return smbClient.getContext()
    }
    
    fun clearCredentials() {
        smbClient.disconnect()
    }
}