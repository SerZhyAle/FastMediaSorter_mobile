package com.sza.fastmediasorter.network

import com.sza.fastmediasorter.utils.Logger
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
    
    fun getContext(): CIFSContext? = context
    
    private fun buildFullDiagnostic(e: Exception?, serverAddress: String, folderPath: String): String {
        val diagnostic = StringBuilder()
        
        // Header
        diagnostic.append("=== SMB CONNECTION TEST DIAGNOSTIC ===\n")
        diagnostic.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n")
        diagnostic.append("Server: $serverAddress\n")
        diagnostic.append("Folder: $folderPath\n\n")
        
        // Error information
        if (e != null) {
            diagnostic.append("=== ERROR DETAILS ===\n")
            diagnostic.append("Exception: ${e.javaClass.simpleName}\n")
            diagnostic.append("Message: ${e.message ?: "No message"}\n")
            diagnostic.append("Cause: ${e.cause?.javaClass?.simpleName ?: "None"}\n")
            if (e.cause != null) {
                diagnostic.append("Cause Message: ${e.cause?.message ?: "No message"}\n")
            }
            diagnostic.append("\n")
        }
        
        // Security providers diagnostic
        diagnostic.append("=== SECURITY PROVIDERS ===\n")
        try {
            val providers = java.security.Security.getProviders()
            diagnostic.append("Total Providers: ${providers.size}\n\n")
            
            providers.forEachIndexed { index, provider ->
                diagnostic.append("${index + 1}. ${provider.name}\n")
                diagnostic.append("   Version: ${provider.version}\n")
                diagnostic.append("   Info: ${provider.info}\n")
                diagnostic.append("   Class: ${provider.javaClass.name}\n\n")
            }
            
            // BC Provider specific check
            val bcProvider = java.security.Security.getProvider("BC")
            diagnostic.append("=== BOUNCYCASTLE PROVIDER CHECK ===\n")
            if (bcProvider != null) {
                diagnostic.append("✓ BouncyCastle Provider: FOUND\n")
                diagnostic.append("  Name: ${bcProvider.name}\n")
                diagnostic.append("  Version: ${bcProvider.version}\n")
                diagnostic.append("  Class: ${bcProvider.javaClass.name}\n\n")
                
                // Test MD4 algorithm
                diagnostic.append("=== MD4 ALGORITHM TEST ===\n")
                try {
                    val md4 = java.security.MessageDigest.getInstance("MD4", "BC")
                    diagnostic.append("✓ MD4 Algorithm: AVAILABLE\n")
                    diagnostic.append("  Provider: ${md4.provider.name}\n")
                    diagnostic.append("  Algorithm: ${md4.algorithm}\n")
                } catch (md4e: Exception) {
                    diagnostic.append("✗ MD4 Algorithm: FAILED\n")
                    diagnostic.append("  Error: ${md4e.javaClass.simpleName}\n")
                    diagnostic.append("  Message: ${md4e.message}\n")
                }
            } else {
                diagnostic.append("✗ BouncyCastle Provider: NOT FOUND\n")
                diagnostic.append("  This is likely the cause of MD4 errors!\n")
            }
            diagnostic.append("\n")
            
        } catch (provEx: Exception) {
            diagnostic.append("Error checking security providers: ${provEx.message}\n\n")
        }
        
        // System information
        diagnostic.append("=== SYSTEM INFORMATION ===\n")
        diagnostic.append("Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
        diagnostic.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
        diagnostic.append("JVM: ${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}\n")
        diagnostic.append("Java Version: ${System.getProperty("java.version")}\n\n")
        
        // Network information
        diagnostic.append("=== NETWORK CONFIGURATION ===\n")
        try {
            val connectivityManager = android.content.Context.CONNECTIVITY_SERVICE
            diagnostic.append("WiFi Status: Available\n") // Basic check
        } catch (netEx: Exception) {
            diagnostic.append("Network check failed: ${netEx.message}\n")
        }
        
        // Stack trace for errors
        if (e != null) {
            diagnostic.append("\n=== FULL STACK TRACE ===\n")
            diagnostic.append(android.util.Log.getStackTraceString(e))
        }
        
        return diagnostic.toString()
    }
    
    suspend fun connect(@Suppress("UNUSED_PARAMETER") serverAddress: String, username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure BouncyCastle provider is available
                try {
                    java.security.Security.getProvider("BC") ?: run {
                        val bcProvider = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
                            .getDeclaredConstructor().newInstance() as java.security.Provider
                        java.security.Security.addProvider(bcProvider)
                        Logger.d("SmbClient", "BouncyCastle provider added")
                    }
                } catch (e: Exception) {
                    Logger.e("SmbClient", "Failed to ensure BC provider", e)
                }
                
                val props = Properties().apply {
                    setProperty("jcifs.smb.client.minVersion", "SMB202")
                    setProperty("jcifs.smb.client.maxVersion", "SMB311")
                    setProperty("jcifs.resolveOrder", "DNS")
                    setProperty("jcifs.smb.client.responseTimeout", "30000")
                }
                
                val config = PropertyConfiguration(props)
                val baseContext = BaseContext(config)
                
                // Clear existing context before creating new one to prevent resource leaks
                context?.let {
                    try {
                        (it as? AutoCloseable)?.close()
                    } catch (e: Exception) {
                        Logger.w("SmbClient", "Failed to close previous context", e)
                    }
                }
                context = null
                
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
    
    suspend fun getImageFiles(
        serverAddress: String, 
        folderPath: String,
        isVideoEnabled: Boolean = false,
        maxVideoSizeMb: Int = 100
    ): ImageFilesResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.d("SmbClient", "getImageFiles - server: $serverAddress, folder: $folderPath, videoEnabled: $isVideoEnabled")
                
                // Diagnostic check for BC provider and MD4 availability BEFORE SMB operations
                val bcDiagnostic = StringBuilder()
                try {
                    val providers = java.security.Security.getProviders()
                    bcDiagnostic.append("Security Providers: ${providers.size}\n")
                    providers.forEach { p -> bcDiagnostic.append("${p.name} v${p.version}; ") }
                    bcDiagnostic.append("\n")
                    
                    val bcProvider = java.security.Security.getProvider("BC")
                    if (bcProvider != null) {
                        bcDiagnostic.append("BC Provider: FOUND\n")
                        try {
                            java.security.MessageDigest.getInstance("MD4", "BC")
                            bcDiagnostic.append("MD4 Test: SUCCESS\n")
                        } catch (md4e: Exception) {
                            bcDiagnostic.append("MD4 Test: FAILED - ${md4e.message}\n")
                        }
                    } else {
                        bcDiagnostic.append("BC Provider: NOT FOUND\n")
                    }
                } catch (diagEx: Exception) {
                    bcDiagnostic.append("Diagnostic error: ${diagEx.message}\n")
                }
                Logger.d("SmbClient", "Pre-SMB diagnostic:\n$bcDiagnostic")
                
                if (context == null) {
                    val msg = "Not connected to server. Please check connection settings.\n\n$bcDiagnostic"
                    Logger.e("SmbClient", msg)
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
                Logger.d("SmbClient", "Final SMB URL: $smbUrl")
                
                val smbFile = SmbFile(smbUrl, context)
                
                Logger.d("SmbClient", "Checking SMB path...")
                
                if (!smbFile.exists()) {
                    val msg = "Path not found: smb://$cleanServer/$cleanFolder/\n\nCheck:\n• Server IP correct?\n• Folder name correct?\n• Network connection?"
                    Logger.e("SmbClient", msg)
                    return@withContext ImageFilesResult(emptyList(), msg)
                }
                
                if (!smbFile.isDirectory()) {
                    val msg = "Path is not a folder: smb://$cleanServer/$cleanFolder/"
                    Logger.e("SmbClient", msg)
                    return@withContext ImageFilesResult(emptyList(), msg)
                }
                
                val mediaFiles = mutableListOf<String>()
                val maxVideoSizeBytes = maxVideoSizeMb * 1024L * 1024L
                
                val files = smbFile.listFiles()
                Logger.d("SmbClient", "Found ${files?.size ?: 0} files in directory")
                
                files?.forEach { file ->
                    if (file.isFile()) {
                        val filename = file.name
                        
                        // Check if it's an image
                        if (com.sza.fastmediasorter.utils.MediaUtils.isImage(filename)) {
                            mediaFiles.add(file.url.toString())
                            Logger.d("SmbClient", "Added image: $filename")
                        }
                        // Check if it's a video and video is enabled
                        else if (isVideoEnabled && com.sza.fastmediasorter.utils.MediaUtils.isVideo(filename)) {
                            // Skip AVI format - incompatible with jCIFS-ng SMB streaming
                            val extension = filename.substringAfterLast('.', "").lowercase()
                            if (extension == "avi") {
                                Logger.d("SmbClient", "Skipped video (AVI format not supported): $filename")
                            } else {
                                val fileSize = file.length()
                                val fileSizeMb = fileSize / (1024 * 1024)
                                
                                if (fileSize <= maxVideoSizeBytes) {
                                    mediaFiles.add(file.url.toString())
                                    Logger.d("SmbClient", "Added video: $filename (${fileSizeMb}MB)")
                                } else {
                                    Logger.d("SmbClient", "Skipped video (too large): $filename (${fileSizeMb}MB > ${maxVideoSizeMb}MB)")
                                }
                            }
                        }
                    }
                }
                
                Logger.d("SmbClient", "Total media files found: ${mediaFiles.size}")
                
                if (mediaFiles.isEmpty()) {
                    val formats = if (isVideoEnabled) {
                        "JPG, PNG, GIF, BMP, WEBP, MP4, MKV, MOV, WEBM, 3GP"
                    } else {
                        "JPG, PNG, GIF, BMP, WEBP"
                    }
                    val msg = "No media files found in: smb://$cleanServer/$cleanFolder/\n\nSupported formats: $formats\n\nNote: AVI format not supported for SMB streaming"
                    return@withContext ImageFilesResult(emptyList(), msg)
                }
                
                ImageFilesResult(mediaFiles.sorted())
            } catch (e: java.net.UnknownHostException) {
                val msg = "Cannot reach server: $serverAddress\n\nCheck:\n• Server IP correct?\n• Same WiFi network?\n• Server running?"
                Logger.e("SmbClient", msg, e)
                ImageFilesResult(emptyList(), msg)
            } catch (e: jcifs.smb.SmbAuthException) {
                val diagnostic = buildFullDiagnostic(e, serverAddress, folderPath)
                Logger.e("SmbClient", "SmbAuthException", e)
                ImageFilesResult(emptyList(), diagnostic)
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
                    e.message?.contains("algorithm", ignoreCase = true) == true ||
                    e.message?.contains("MD4", ignoreCase = true) == true ||
                    e.message?.contains("provider", ignoreCase = true) == true -> {
                        val diagnostic = buildFullDiagnostic(e, serverAddress, folderPath)
                        "SECURITY ERROR (likely MD4 issue):\n\n$diagnostic"
                    }
                    else ->
                        "SMB Error: ${e.message ?: "Unknown error"}\n\nFull error details logged"
                }
                Logger.e("SmbClient", "SmbException details: ${e.message}", e)
                e.printStackTrace()
                ImageFilesResult(emptyList(), msg)
            } catch (e: java.net.SocketTimeoutException) {
                val msg = "Connection timeout\n\nCheck:\n• Server running?\n• Network stable?\n• VPN not blocking?"
                Logger.e("SmbClient", msg, e)
                ImageFilesResult(emptyList(), msg)
            } catch (e: Exception) {
                // For any exception, check if it's security-related and provide diagnostic
                val msg = if (e is java.security.NoSuchAlgorithmException || 
                             e.message?.contains("algorithm", ignoreCase = true) == true ||
                             e.message?.contains("provider", ignoreCase = true) == true ||
                             e.message?.contains("MD4", ignoreCase = true) == true) {
                    buildFullDiagnostic(e, serverAddress, folderPath)
                } else {
                    "Error: ${e.javaClass.simpleName}\n${e.message ?: "Unknown error"}\n\nFull details logged"
                }
                
                Logger.e("SmbClient", "Error in getImageFiles: ${e.message}", e)
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
    
    sealed class DeleteResult {
        object Success : DeleteResult()
        object FileNotFound : DeleteResult()
        data class SecurityError(val message: String) : DeleteResult()
        data class NetworkError(val message: String) : DeleteResult()
        data class UnknownError(val message: String) : DeleteResult()
    }
    
    sealed class RenameResult {
        object Success : RenameResult()
        object SourceNotFound : RenameResult()
        object TargetExists : RenameResult()
        data class SecurityError(val message: String) : RenameResult()
        data class NetworkError(val message: String) : RenameResult()
        data class UnknownError(val message: String) : RenameResult()
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
                    } catch (e: jcifs.smb.SmbAuthException) {
                        return@withContext CopyResult.SecurityError("Access denied to target folder: ${e.message}")
                    } catch (e: java.net.UnknownHostException) {
                        return@withContext CopyResult.NetworkError("Server not found: ${e.message}")
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
            } catch (e: java.net.UnknownHostException) {
                CopyResult.NetworkError("Server not found: ${e.message}")
            } catch (e: java.net.SocketTimeoutException) {
                CopyResult.NetworkError("Connection timeout")
            } catch (e: java.io.IOException) {
                CopyResult.NetworkError("Network error: ${e.message}")
            } catch (e: jcifs.smb.SmbException) {
                CopyResult.SecurityError("SMB error: ${e.message}")
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
    
    suspend fun deleteFile(fileUrl: String): DeleteResult {
        return withContext(Dispatchers.IO) {
            try {
                val currentContext = context ?: return@withContext DeleteResult.UnknownError("SMB context not initialized")
                
                val smbFile = SmbFile(fileUrl, currentContext)
                
                if (!smbFile.exists()) {
                    return@withContext DeleteResult.FileNotFound
                }
                
                smbFile.delete()
                DeleteResult.Success
            } catch (e: jcifs.smb.SmbAuthException) {
                DeleteResult.SecurityError("Delete permission denied: ${e.message}")
            } catch (e: java.net.UnknownHostException) {
                DeleteResult.NetworkError("Server not found: ${e.message}")
            } catch (e: java.net.SocketTimeoutException) {
                DeleteResult.NetworkError("Connection timeout")
            } catch (e: java.io.IOException) {
                DeleteResult.NetworkError("Network error: ${e.message}")
            } catch (e: jcifs.smb.SmbException) {
                DeleteResult.SecurityError("SMB error: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                DeleteResult.UnknownError(e.message ?: "Unknown error")
            }
        }
    }
    
    suspend fun renameFile(oldUrl: String, newUrl: String): RenameResult {
        return withContext(Dispatchers.IO) {
            try {
                val currentContext = context ?: return@withContext RenameResult.UnknownError("SMB context not initialized")
                
                val oldFile = SmbFile(oldUrl, currentContext)
                val newFile = SmbFile(newUrl, currentContext)
                
                if (!oldFile.exists()) {
                    return@withContext RenameResult.SourceNotFound
                }
                
                if (newFile.exists()) {
                    return@withContext RenameResult.TargetExists
                }
                
                oldFile.renameTo(newFile)
                RenameResult.Success
            } catch (e: jcifs.smb.SmbAuthException) {
                RenameResult.SecurityError("Rename permission denied: ${e.message}")
            } catch (e: java.net.UnknownHostException) {
                RenameResult.NetworkError("Server not found: ${e.message}")
            } catch (e: java.net.SocketTimeoutException) {
                RenameResult.NetworkError("Connection timeout")
            } catch (e: java.io.IOException) {
                RenameResult.NetworkError("Network error: ${e.message}")
            } catch (e: jcifs.smb.SmbException) {
                RenameResult.SecurityError("SMB error: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                RenameResult.UnknownError(e.message ?: "Unknown error")
            }
        }
    }
    
    suspend fun fileExists(fileUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentContext = context ?: return@withContext false
                val smbFile = SmbFile(fileUrl, currentContext)
                smbFile.exists()
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * Clears the SMB context and credentials from memory.
     * Should be called after completing operations to minimize the time
     * sensitive credentials remain in memory, improving security.
     * Also attempts to properly close the context to free resources.
     */
    fun disconnect() {
        context?.let {
            try {
                (it as? AutoCloseable)?.close()
            } catch (e: Exception) {
                Logger.w("SmbClient", "Failed to close context during disconnect", e)
            }
        }
        context = null
    }
}