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
    
    data class ImageFilesResult(
        val files: List<String>,
        val errorMessage: String? = null
    )
    
    suspend fun getImageFiles(serverAddress: String, folderPath: String): ImageFilesResult {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("SmbClient", "getImageFiles - server: $serverAddress, folder: $folderPath")
                
                if (context == null) {
                    val msg = "Not connected to server. Please check connection settings."
                    android.util.Log.e("SmbClient", msg)
                    return@withContext ImageFilesResult(emptyList(), msg)
                }
                
                val cleanServer = serverAddress.trim()
                    .removePrefix("smb://")
                    .removePrefix("\\\\")
                    .replace("\\", "/")
                
                val cleanFolder = folderPath.trim()
                    .removePrefix("/")
                    .removePrefix("\\")
                    .replace("\\", "/")
                
                val smbUrl = "smb://$cleanServer/$cleanFolder/"
                android.util.Log.d("SmbClient", "Final SMB URL: $smbUrl")
                
                val smbFile = SmbFile(smbUrl, context)
                
                android.util.Log.d("SmbClient", "Checking SMB path...")
                
                if (!smbFile.exists()) {
                    val msg = "Path not found: smb://$cleanServer/$cleanFolder/\n\nCheck:\n• Server IP correct?\n• Folder name correct?\n• Network connection?"
                    android.util.Log.e("SmbClient", msg)
                    return@withContext ImageFilesResult(emptyList(), msg)
                }
                
                if (!smbFile.isDirectory()) {
                    val msg = "Path is not a folder: smb://$cleanServer/$cleanFolder/"
                    android.util.Log.e("SmbClient", msg)
                    return@withContext ImageFilesResult(emptyList(), msg)
                }
                
                val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
                val imageFiles = mutableListOf<String>()
                
                val files = smbFile.listFiles()
                android.util.Log.d("SmbClient", "Found ${files?.size ?: 0} files in directory")
                
                files?.forEach { file ->
                    if (file.isFile()) {
                        val extension = file.name.substringAfterLast('.', "").lowercase()
                        if (extension in imageExtensions) {
                            imageFiles.add(file.url.toString())
                            android.util.Log.d("SmbClient", "Added image: ${file.name}")
                        }
                    }
                }
                
                android.util.Log.d("SmbClient", "Total images found: ${imageFiles.size}")
                
                if (imageFiles.isEmpty()) {
                    val msg = "No image files found in: smb://$cleanServer/$cleanFolder/\n\nSupported formats: JPG, PNG, GIF, BMP, WEBP"
                    return@withContext ImageFilesResult(emptyList(), msg)
                }
                
                ImageFilesResult(imageFiles.sorted())
            } catch (e: java.net.UnknownHostException) {
                val msg = "Cannot reach server: $serverAddress\n\nCheck:\n• Server IP correct?\n• Same WiFi network?\n• Server running?"
                android.util.Log.e("SmbClient", msg, e)
                ImageFilesResult(emptyList(), msg)
            } catch (e: jcifs.smb.SmbAuthException) {
                val msg = "Authentication failed\n\nCheck username and password"
                android.util.Log.e("SmbClient", msg, e)
                ImageFilesResult(emptyList(), msg)
            } catch (e: jcifs.smb.SmbException) {
                val msg = when {
                    e.message?.contains("Access is denied", ignoreCase = true) == true ->
                        "Access denied to: smb://$serverAddress/$folderPath/\n\nCheck:\n• Username/password correct?\n• Folder permissions?\n• Share enabled?"
                    e.message?.contains("does not exist", ignoreCase = true) == true ->
                        "Folder not found: smb://$serverAddress/$folderPath/\n\nCheck:\n• Folder path correct?\n• Share name correct?"
                    e.message?.contains("timed out", ignoreCase = true) == true ->
                        "Connection timeout\n\nCheck:\n• Server reachable?\n• Firewall blocking SMB?\n• Same network?"
                    e.message?.contains("Connection refused", ignoreCase = true) == true ->
                        "Connection refused\n\nCheck:\n• SMB enabled on server?\n• Firewall settings?\n• Port 445 open?"
                    else ->
                        "SMB Error: ${e.message ?: "Unknown error"}\n\nFull error details logged"
                }
                android.util.Log.e("SmbClient", "SmbException details: ${e.message}", e)
                e.printStackTrace()
                ImageFilesResult(emptyList(), msg)
            } catch (e: java.net.SocketTimeoutException) {
                val msg = "Connection timeout\n\nCheck:\n• Server running?\n• Network stable?\n• VPN not blocking?"
                android.util.Log.e("SmbClient", msg, e)
                ImageFilesResult(emptyList(), msg)
            } catch (e: Exception) {
                val msg = "Error: ${e.javaClass.simpleName}\n${e.message ?: "Unknown error"}\n\nFull details logged"
                android.util.Log.e("SmbClient", "Error in getImageFiles: ${e.message}", e)
                e.printStackTrace()
                ImageFilesResult(emptyList(), msg)
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
        object PendingUserConfirmation : MoveResult()
        object AlreadyExists : MoveResult()
        object SameFolder : MoveResult()
        data class NetworkError(val message: String) : MoveResult()
        data class SecurityError(val message: String) : MoveResult()
        data class DeleteError(val message: String) : MoveResult()
        data class UnknownError(val message: String) : MoveResult()
    }
    
    suspend fun writeFile(
        targetServer: String,
        targetFolder: String,
        fileName: String,
        data: ByteArray
    ): CopyResult {
        return withContext(Dispatchers.IO) {
            try {
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
                
                if (!targetDir.exists()) {
                    try {
                        targetDir.mkdirs()
                    } catch (e: Exception) {
                        return@withContext CopyResult.SecurityError("Cannot create target folder: ${e.message}")
                    }
                }
                
                val targetFile = SmbFile(targetDir, fileName)
                
                if (targetFile.exists()) {
                    return@withContext CopyResult.AlreadyExists
                }
                
                targetFile.openOutputStream().use { output ->
                    output.write(data)
                }
                
                CopyResult.Success
            } catch (e: jcifs.smb.SmbAuthException) {
                CopyResult.SecurityError("Authentication failed: ${e.message}")
            } catch (e: jcifs.smb.SmbException) {
                CopyResult.NetworkError("SMB error: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                CopyResult.UnknownError(e.message ?: "Unknown error")
            }
        }
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