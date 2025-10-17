package com.sza.fastmediasorter.network

import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SmbClient {
    private var context: CIFSContext? = null
    
    suspend fun connect(serverAddress: String, username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    setProperty("jcifs.smb.client.minVersion", "SMB202")
                    setProperty("jcifs.smb.client.maxVersion", "SMB311")
                    setProperty("jcifs.resolveOrder", "DNS")
                    setProperty("jcifs.smb.client.responseTimeout", "30000")
                }
                
                val config = PropertyConfiguration(props)
                val baseContext = BaseContext(config)
                
                context = if (username.isNotEmpty() && password.isNotEmpty()) {
                    val auth = NtlmPasswordAuthenticator(null, username, password)
                    baseContext.withCredentials(auth)
                } else {
                    baseContext.withAnonymousCredentials()
                }
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    suspend fun getImageFiles(serverAddress: String, folderPath: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanServer = serverAddress.trim()
                    .removePrefix("smb://")
                    .removePrefix("\\\\")
                    .replace("\\", "/")
                
                val cleanFolder = folderPath.trim()
                    .removePrefix("/")
                    .removePrefix("\\")
                    .replace("\\", "/")
                
                val smbUrl = "smb://$cleanServer/$cleanFolder/"
                val smbFile = SmbFile(smbUrl, context)
                
                if (!smbFile.exists() || !smbFile.isDirectory()) {
                    return@withContext emptyList()
                }
                
                val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
                val imageFiles = mutableListOf<String>()
                
                smbFile.listFiles()?.forEach { file ->
                    if (file.isFile()) {
                        val extension = file.name.substringAfterLast('.', "").lowercase()
                        if (extension in imageExtensions) {
                            imageFiles.add(file.url.toString())
                        }
                    }
                }
                
                imageFiles.sorted()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    suspend fun downloadImage(imageUrl: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val smbFile = SmbFile(imageUrl, context)
                
                if (!smbFile.exists() || !smbFile.isFile()) {
                    return@withContext null
                }
                
                smbFile.inputStream.use { it.readBytes() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    fun disconnect() {
        context = null
    }
}