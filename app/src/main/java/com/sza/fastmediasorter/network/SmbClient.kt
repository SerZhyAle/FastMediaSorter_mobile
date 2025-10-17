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
    
    data class FileInfo(
        val name: String,
        val sizeKB: Long,
        val modifiedDate: Long
    )
    
    suspend fun getFileInfo(imageUrl: String): FileInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val smbFile = SmbFile(imageUrl, context)
                
                if (!smbFile.exists() || !smbFile.isFile()) {
                    return@withContext null
                }
                
                FileInfo(
                    name = smbFile.name,
                    sizeKB = smbFile.length() / 1024,
                    modifiedDate = smbFile.lastModified()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    sealed class CopyResult {
        object Success : CopyResult()
        object AlreadyExists : CopyResult()
        object SameFolder : CopyResult()
        data class NetworkError(val message: String) : CopyResult()
        data class SecurityError(val message: String) : CopyResult()
        data class UnknownError(val message: String) : CopyResult()
    }
    
    sealed class MoveResult {
        object Success : MoveResult()
        object AlreadyExists : MoveResult()
        object SameFolder : MoveResult()
        data class NetworkError(val message: String) : MoveResult()
        data class SecurityError(val message: String) : MoveResult()
        data class DeleteError(val message: String) : MoveResult()
        data class UnknownError(val message: String) : MoveResult()
    }
    
    suspend fun copyFile(
        sourceUrl: String,
        targetServer: String,
        targetFolder: String
    ): CopyResult {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = SmbFile(sourceUrl, context)
                
                if (!sourceFile.exists() || !sourceFile.isFile()) {
                    return@withContext CopyResult.NetworkError("Source file not found")
                }
                
                // Build target URL
                val cleanTargetServer = targetServer.trim()
                    .removePrefix("smb://")
                    .removePrefix("\\\\")
                    .replace("\\", "/")
                
                val cleanTargetFolder = targetFolder.trim()
                    .removePrefix("/")
                    .removePrefix("\\")
                    .replace("\\", "/")
                
                val targetUrl = "smb://$cleanTargetServer/$cleanTargetFolder/"
                val targetDir = SmbFile(targetUrl, context)
                
                // Check if same folder
                if (sourceFile.parent == targetDir.path) {
                    return@withContext CopyResult.SameFolder
                }
                
                // Create target directory if not exists
                if (!targetDir.exists()) {
                    try {
                        targetDir.mkdirs()
                    } catch (e: jcifs.smb.SmbAuthException) {
                        return@withContext CopyResult.SecurityError("Access denied to target folder")
                    } catch (e: Exception) {
                        return@withContext CopyResult.SecurityError("Cannot create target folder: ${e.message}")
                    }
                }
                
                val targetFile = SmbFile(targetUrl + sourceFile.name, context)
                
                // Check if file already exists
                if (targetFile.exists()) {
                    return@withContext CopyResult.AlreadyExists
                }
                
                // Copy file
                try {
                    sourceFile.inputStream.use { input ->
                        targetFile.outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    CopyResult.Success
                } catch (e: jcifs.smb.SmbAuthException) {
                    CopyResult.SecurityError("Write permission denied")
                } catch (e: java.net.UnknownHostException) {
                    CopyResult.NetworkError("Target server not found: ${e.message}")
                } catch (e: java.net.SocketTimeoutException) {
                    CopyResult.NetworkError("Connection timeout")
                } catch (e: java.io.IOException) {
                    CopyResult.NetworkError("Network error: ${e.message}")
                } catch (e: Exception) {
                    CopyResult.UnknownError(e.message ?: "Unknown error occurred")
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                CopyResult.UnknownError(e.message ?: "Unexpected error")
            }
        }
    }
    
    suspend fun moveFile(
        sourceUrl: String,
        targetServer: String,
        targetFolder: String
    ): MoveResult {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = SmbFile(sourceUrl, context)
                
                if (!sourceFile.exists() || !sourceFile.isFile()) {
                    return@withContext MoveResult.NetworkError("Source file not found")
                }
                
                // Build target URL
                val cleanTargetServer = targetServer.trim()
                    .removePrefix("smb://")
                    .removePrefix("\\\\")
                    .replace("\\", "/")
                
                val cleanTargetFolder = targetFolder.trim()
                    .removePrefix("/")
                    .removePrefix("\\")
                    .replace("\\", "/")
                
                val targetUrl = "smb://$cleanTargetServer/$cleanTargetFolder/"
                val targetDir = SmbFile(targetUrl, context)
                
                // Check if same folder
                if (sourceFile.parent == targetDir.path) {
                    return@withContext MoveResult.SameFolder
                }
                
                // Create target directory if not exists
                if (!targetDir.exists()) {
                    try {
                        targetDir.mkdirs()
                    } catch (e: jcifs.smb.SmbAuthException) {
                        return@withContext MoveResult.SecurityError("Access denied to target folder")
                    } catch (e: Exception) {
                        return@withContext MoveResult.SecurityError("Cannot create target folder: ${e.message}")
                    }
                }
                
                val targetFile = SmbFile(targetUrl + sourceFile.name, context)
                
                // Check if file already exists
                if (targetFile.exists()) {
                    return@withContext MoveResult.AlreadyExists
                }
                
                // Copy file first
                try {
                    sourceFile.inputStream.use { input ->
                        targetFile.outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: jcifs.smb.SmbAuthException) {
                    return@withContext MoveResult.SecurityError("Write permission denied")
                } catch (e: java.net.UnknownHostException) {
                    return@withContext MoveResult.NetworkError("Target server not found: ${e.message}")
                } catch (e: java.net.SocketTimeoutException) {
                    return@withContext MoveResult.NetworkError("Connection timeout")
                } catch (e: java.io.IOException) {
                    return@withContext MoveResult.NetworkError("Network error: ${e.message}")
                } catch (e: Exception) {
                    return@withContext MoveResult.UnknownError(e.message ?: "Copy failed")
                }
                
                // Delete source file after successful copy
                try {
                    sourceFile.delete()
                    MoveResult.Success
                } catch (e: jcifs.smb.SmbAuthException) {
                    MoveResult.DeleteError("Delete permission denied on source file")
                } catch (e: Exception) {
                    MoveResult.DeleteError("Cannot delete source file: ${e.message}")
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                MoveResult.UnknownError(e.message ?: "Unexpected error")
            }
        }
    }
    
    suspend fun deleteFile(fileUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentContext = context ?: return@withContext false
                
                val smbFile = SmbFile(fileUrl, currentContext)
                
                if (!smbFile.exists()) {
                    return@withContext false
                }
                
                smbFile.delete()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    fun disconnect() {
        context = null
    }
}