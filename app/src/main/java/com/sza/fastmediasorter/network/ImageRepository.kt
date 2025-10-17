package com.sza.fastmediasorter.network

import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageRepository(private val smbClient: SmbClient, private val preferenceManager: PreferenceManager) {
    
    suspend fun loadImages(): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val serverAddress = preferenceManager.getServerAddress()
                val username = preferenceManager.getUsername()
                val password = preferenceManager.getPassword()
                val folderPath = preferenceManager.getFolderPath()
                
                if (serverAddress.isEmpty() || folderPath.isEmpty()) {
                    return@withContext Result.failure(Exception("Connection settings not configured"))
                }
                
                val connected = smbClient.connect(serverAddress, username, password)
                if (!connected) {
                    return@withContext Result.failure(Exception("Failed to connect to server"))
                }
                
                val images = smbClient.getImageFiles(serverAddress, folderPath)
                if (images.isEmpty()) {
                    return@withContext Result.failure(Exception("No images found"))
                }
                
                Result.success(images)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun downloadImage(imageUrl: String): ByteArray? {
        return smbClient.downloadImage(imageUrl)
    }
}