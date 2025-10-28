package com.sza.fastmediasorter.network

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.sza.fastmediasorter.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class LocalImageInfo(
val uri: Uri,
val name: String,
val size: Long,
val dateModified: Long
)

class LocalStorageClient(private val context: Context) {
    /**
     * Get image files from local storage using dual-access pattern:
     * 1. MediaStore access (by bucketName) - for folders found via SCAN
     * 2. SAF/DocumentFile access (by folderUri) - for user-selected folders
     * 
     * @param folderUri TreeUri from SAF (Storage Access Framework), or null for MediaStore
     * @param bucketName BUCKET_DISPLAY_NAME from MediaStore, or null for SAF
     * @param isVideoEnabled Whether to include video files
     * @param maxVideoSizeMb Maximum video file size in MB
     * @return List of media files (images and optionally videos)
     */
    suspend fun getImageFiles(
        folderUri: Uri?, 
        bucketName: String? = null,
        isVideoEnabled: Boolean = false,
        maxVideoSizeMb: Int = 100
    ): List<LocalImageInfo> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Logger.d("LocalStorageClient", "⏱️ START: getImageFiles - bucketName: $bucketName, folderUri: $folderUri")
        
        // Priority 1: MediaStore access (for SCAN-discovered folders)
        if (bucketName != null) {
            val result = getImageFilesByBucketName(bucketName, isVideoEnabled, maxVideoSizeMb)
            val duration = System.currentTimeMillis() - startTime
            Logger.d("LocalStorageClient", "⏱️ DONE: MediaStore query took ${duration}ms, found ${result.size} files")
            return@withContext result
        }
        
        // Priority 2: SAF/DocumentFile access (for user-selected folders)
        if (folderUri == null) {
            Logger.d("LocalStorageClient", "⏱️ DONE: No folder URI provided")
            return@withContext emptyList()
        }
        
        val maxVideoSizeBytes = maxVideoSizeMb * 1024L * 1024L
        
        try {
            val listStartTime = System.currentTimeMillis()
            Logger.d("LocalStorageClient", "⏱️ START: Listing DocumentFile contents...")
            val folder = DocumentFile.fromTreeUri(context, folderUri)
            val files = folder?.listFiles()
            val listDuration = System.currentTimeMillis() - listStartTime
            Logger.d("LocalStorageClient", "⏱️ DONE: DocumentFile listing took ${listDuration}ms, found ${files?.size ?: 0} files")
            
            val processStartTime = System.currentTimeMillis()
            var imageCount = 0
            var videoCount = 0
            var skippedLargeVideo = 0
            var skippedOther = 0
            
            val result = files?.filterIndexed { index, file ->
                // Log progress every 100 files
                if (index % 100 == 0 && index > 0) {
                    val elapsed = System.currentTimeMillis() - processStartTime
                    val rate = index.toFloat() / elapsed * 1000
                    Logger.d("LocalStorageClient", "⏱️ Progress: $index/${files.size} files (${rate.toInt()} files/sec)")
                }
                
                if (!file.isFile) {
                    skippedOther++
                    return@filterIndexed false
                }
                
                val mimeType = file.type ?: run {
                    skippedOther++
                    return@filterIndexed false
                }
                val isImage = mimeType.startsWith("image/")
                val isVideo = mimeType.startsWith("video/")
                
                when {
                    isImage -> {
                        imageCount++
                        true
                    }
                    isVideo && isVideoEnabled -> {
                        val fileSize = file.length()
                        if (fileSize <= maxVideoSizeBytes) {
                            videoCount++
                            true
                        } else {
                            skippedLargeVideo++
                            false
                        }
                    }
                    else -> {
                        skippedOther++
                        false
                    }
                }
            }?.map {
                LocalImageInfo(
                    uri = it.uri,
                    name = it.name ?: "unknown",
                    size = it.length(),
                    dateModified = it.lastModified()
                )
            }?.sortedBy { it.name } ?: emptyList()
            
            val processDuration = System.currentTimeMillis() - processStartTime
            val totalDuration = System.currentTimeMillis() - startTime
            
            Logger.d("LocalStorageClient", "⏱️ SUMMARY:")
            Logger.d("LocalStorageClient", "  - Total time: ${totalDuration}ms (${totalDuration/1000.0}s)")
            Logger.d("LocalStorageClient", "  - Total files scanned: ${files?.size ?: 0}")
            Logger.d("LocalStorageClient", "  - Images found: $imageCount")
            Logger.d("LocalStorageClient", "  - Videos found: $videoCount")
            Logger.d("LocalStorageClient", "  - Skipped (too large): $skippedLargeVideo")
            Logger.d("LocalStorageClient", "  - Skipped (other): $skippedOther")
            Logger.d("LocalStorageClient", "  - Processing rate: ${if (processDuration > 0) (files?.size ?: 0).toFloat() / processDuration * 1000 else 0} files/sec")
            
            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Logger.e("LocalStorageClient", "⏱️ ERROR: Failed after ${duration}ms", e)
            emptyList()
        }
    }

suspend fun scanStandardFolders(): Map<String, Pair<Uri?, Int>> = withContext(Dispatchers.IO) {
val folders = mutableMapOf<String, Pair<Uri?, Int>>()
val projection = arrayOf(
MediaStore.Images.Media._ID,
MediaStore.Images.Media.RELATIVE_PATH,
MediaStore.Images.Media.BUCKET_DISPLAY_NAME
)
val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
try {
context.contentResolver.query(
MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
projection,
null,
null,
sortOrder
)?.use { cursor ->
val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
val counts = mutableMapOf<String, Int>()
while (cursor.moveToNext()) {
val bucket = cursor.getString(bucketColumn) ?: continue
counts[bucket] = (counts[bucket] ?: 0) + 1
}
val standardFolders = mapOf(
"Camera" to "📷",
"Screenshots" to "📸",
"Pictures" to "🖼️",
"Download" to "📥"
)
standardFolders.forEach { (folderName, _) ->
val count = counts[folderName] ?: 0
folders[folderName] = Pair(null, count)
}
}
} catch (e: Exception) {
e.printStackTrace()
}
return@withContext folders
}

suspend fun scanAllImageFolders(): Map<String, Int> = withContext(Dispatchers.IO) {
val folders = mutableMapOf<String, Int>()
val projection = arrayOf(
MediaStore.Images.Media._ID,
MediaStore.Images.Media.BUCKET_DISPLAY_NAME
)
val sortOrder = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC"
try {
context.contentResolver.query(
MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
projection,
null,
null,
sortOrder
)?.use { cursor ->
val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
while (cursor.moveToNext()) {
val bucket = cursor.getString(bucketColumn) ?: continue
folders[bucket] = (folders[bucket] ?: 0) + 1
}
}
} catch (e: Exception) {
e.printStackTrace()
}
return@withContext folders
}

suspend fun getImageFilesByBucketName(
    bucketName: String,
    isVideoEnabled: Boolean = false,
    maxVideoSizeMb: Int = 100
): List<LocalImageInfo> = withContext(Dispatchers.IO) {
val startTime = System.currentTimeMillis()
Logger.d("LocalStorageClient", "⏱️ START: MediaStore query for bucket '$bucketName'")

val mediaFiles = mutableListOf<LocalImageInfo>()
val maxVideoSizeBytes = maxVideoSizeMb * 1024L * 1024L

// Query images
val imageProjection = arrayOf(
MediaStore.Images.Media._ID,
MediaStore.Images.Media.DISPLAY_NAME,
MediaStore.Images.Media.SIZE,
MediaStore.Images.Media.DATE_MODIFIED,
MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
MediaStore.Images.Media.DATA
)
val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
val selectionArgs = arrayOf("%/$bucketName/%")
val sortOrder = "${MediaStore.Images.Media.DISPLAY_NAME} ASC"

var imageCount = 0
var videoCount = 0
var skippedLargeVideo = 0

try {
val imageQueryStart = System.currentTimeMillis()
Logger.d("LocalStorageClient", "⏱️ Querying images...")
context.contentResolver.query(
MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
imageProjection,
selection,
selectionArgs,
sortOrder
)?.use { cursor ->
val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
imageCount = cursor.count
Logger.d("LocalStorageClient", "⏱️ Image cursor returned $imageCount rows")
while (cursor.moveToNext()) {
val id = cursor.getLong(idColumn)
val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
mediaFiles.add(
LocalImageInfo(
uri = uri,
name = cursor.getString(nameColumn),
size = cursor.getLong(sizeColumn),
dateModified = cursor.getLong(dateColumn) * 1000
)
)
}
}
val imageQueryDuration = System.currentTimeMillis() - imageQueryStart
Logger.d("LocalStorageClient", "⏱️ Image query took ${imageQueryDuration}ms, found $imageCount images")
} catch (e: Exception) {
Logger.e("LocalStorageClient", "⏱️ Image query failed", e)
e.printStackTrace()
}

// Query videos if enabled
if (isVideoEnabled) {
val videoQueryStart = System.currentTimeMillis()
Logger.d("LocalStorageClient", "⏱️ Querying videos...")
val videoProjection = arrayOf(
MediaStore.Video.Media._ID,
MediaStore.Video.Media.DISPLAY_NAME,
MediaStore.Video.Media.SIZE,
MediaStore.Video.Media.DATE_MODIFIED,
MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
MediaStore.Video.Media.DATA
)
try {
context.contentResolver.query(
MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
videoProjection,
selection,
selectionArgs,
sortOrder
)?.use { cursor ->
val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
val totalVideos = cursor.count
Logger.d("LocalStorageClient", "⏱️ Video cursor returned $totalVideos rows")
while (cursor.moveToNext()) {
val size = cursor.getLong(sizeColumn)
if (size <= maxVideoSizeBytes) {
val id = cursor.getLong(idColumn)
val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
mediaFiles.add(
LocalImageInfo(
uri = uri,
name = cursor.getString(nameColumn),
size = size,
dateModified = cursor.getLong(dateColumn) * 1000
)
)
videoCount++
} else {
skippedLargeVideo++
}
}
}
val videoQueryDuration = System.currentTimeMillis() - videoQueryStart
Logger.d("LocalStorageClient", "⏱️ Video query took ${videoQueryDuration}ms, found $videoCount videos, skipped $skippedLargeVideo large")
} catch (e: Exception) {
Logger.e("LocalStorageClient", "⏱️ Video query failed", e)
e.printStackTrace()
}
}

val totalDuration = System.currentTimeMillis() - startTime
Logger.d("LocalStorageClient", "⏱️ SUMMARY:")
Logger.d("LocalStorageClient", "  - Total time: ${totalDuration}ms (${totalDuration/1000.0}s)")
Logger.d("LocalStorageClient", "  - Images: $imageCount")
Logger.d("LocalStorageClient", "  - Videos: $videoCount")
Logger.d("LocalStorageClient", "  - Skipped (too large): $skippedLargeVideo")
Logger.d("LocalStorageClient", "  - Total media: ${mediaFiles.size}")

return@withContext mediaFiles.sortedBy { it.name }
}

suspend fun downloadImage(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
try {
Logger.d("LocalStorageClient", "Reading image from URI: $uri")
val result = context.contentResolver.openInputStream(uri)?.use { input ->
val buffer = ByteArrayOutputStream()
val data = ByteArray(16384)
var count: Int
while (input.read(data, 0, data.size).also { count = it } != -1) {
buffer.write(data, 0, count)
}
buffer.toByteArray()
}
if (result != null) {
Logger.d("LocalStorageClient", "Successfully read ${result.size} bytes")
} else {
Logger.e("LocalStorageClient", "Failed to open input stream for URI: $uri")
}
result
} catch (e: Exception) {
Logger.e("LocalStorageClient", "Error reading image from URI: $uri", e)
e.printStackTrace()
null
}
}

fun getFileInfo(uri: Uri): LocalImageInfo? {
return try {
Logger.d("LocalStorageClient", "Getting file info for URI: $uri")
Logger.d("LocalStorageClient", "URI scheme: ${uri.scheme}, authority: ${uri.authority}")
Logger.d("LocalStorageClient", "URI path: ${uri.path}")

val file = DocumentFile.fromSingleUri(context, uri)
Logger.d("LocalStorageClient", "DocumentFile created: ${file != null}")

if (file != null) {
Logger.d("LocalStorageClient", "File exists: ${file.exists()}, canRead: ${file.canRead()}")
Logger.d("LocalStorageClient", "File name: ${file.name}, type: ${file.type}")
Logger.d("LocalStorageClient", "File length: ${file.length()}, lastModified: ${file.lastModified()}")

if (file.exists()) {
val info = LocalImageInfo(
uri = uri,
name = file.name ?: "unknown",
size = file.length(),
dateModified = file.lastModified()
)
Logger.d("LocalStorageClient", "File info SUCCESS: name=${info.name}, size=${info.size}")
info
} else {
Logger.e("LocalStorageClient", "File exists() returned false for: $uri")
null
}
} else {
Logger.e("LocalStorageClient", "DocumentFile.fromSingleUri returned null for: $uri")
null
}
} catch (e: Exception) {
Logger.e("LocalStorageClient", "EXCEPTION getting file info for URI: $uri", e)
e.printStackTrace()
null
}
}

suspend fun deleteImage(uri: Uri): Boolean = withContext(Dispatchers.IO) {
try {
Logger.d("LocalStorageClient", "Attempting to delete media: $uri")
Logger.d("LocalStorageClient", "Android version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")

// Determine URI type and use appropriate deletion method
val uriString = uri.toString()

if (uriString.startsWith("content://com.android.externalstorage.documents/")) {
    // SAF/DocumentFile URI - use DocumentFile API
    Logger.d("LocalStorageClient", "Using DocumentFile delete for SAF URI")
    val documentFile = DocumentFile.fromSingleUri(context, uri)
    if (documentFile != null && documentFile.exists()) {
        val deleted = documentFile.delete()
        Logger.d("LocalStorageClient", "DocumentFile delete result: $deleted")
        return@withContext deleted
    } else {
        Logger.e("LocalStorageClient", "DocumentFile is null or doesn't exist for SAF URI")
        return@withContext false
    }
} else {
    // MediaStore URI - use ContentResolver
    Logger.d("LocalStorageClient", "Using ContentResolver delete for MediaStore URI")
    val deleted = context.contentResolver.delete(uri, null, null)
    Logger.d("LocalStorageClient", "ContentResolver delete result: $deleted row(s)")
    return@withContext deleted > 0
}

} catch (e: Exception) {
Logger.e("LocalStorageClient", "Exception during delete: ${e.javaClass.simpleName}: ${e.message}", e)
e.printStackTrace()
return@withContext false
}
}

companion object {
fun hasMediaPermission(context: Context): Boolean {
return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
android.content.pm.PackageManager.PERMISSION_GRANTED
} else {
context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
android.content.pm.PackageManager.PERMISSION_GRANTED
}
}
}

suspend fun renameFile(uri: Uri, newFileName: String): Pair<Boolean, String>? = withContext(Dispatchers.IO) {
try {
Logger.d("LocalStorageClient", "Attempting to rename file: $uri to $newFileName")

// Determine URI type and use appropriate rename method
val uriString = uri.toString()

if (uriString.startsWith("content://com.android.externalstorage.documents/")) {
    // SAF/DocumentFile URI - use DocumentFile API
    Logger.d("LocalStorageClient", "Using DocumentFile rename for SAF URI")
    val documentFile = DocumentFile.fromSingleUri(context, uri)
    if (documentFile != null && documentFile.exists()) {
        val renamed = documentFile.renameTo(newFileName)
        Logger.d("LocalStorageClient", "DocumentFile rename result: $renamed")
        if (renamed) {
            // Return the same URI since DocumentFile.renameTo() modifies the file in place
            return@withContext Pair(true, uri.toString())
        } else {
            Logger.d("LocalStorageClient", "DocumentFile rename failed")
            return@withContext Pair(false, uri.toString())
        }
    } else {
        Logger.e("LocalStorageClient", "DocumentFile is null or doesn't exist for SAF URI")
        return@withContext Pair(false, uri.toString())
    }
} else {
    // MediaStore URI - use ContentResolver approach
    Logger.d("LocalStorageClient", "Using ContentResolver rename for MediaStore URI")

    // Get current file info
    val projection = arrayOf(
        android.provider.MediaStore.MediaColumns._ID,
        android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
        android.provider.MediaStore.MediaColumns.MIME_TYPE
    )

    val cursor = context.contentResolver.query(uri, projection, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val idColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID)
            val nameColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
            val pathColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.RELATIVE_PATH)
            val mimeColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.MIME_TYPE)

            val id = it.getLong(idColumn)
            val oldName = it.getString(nameColumn)
            val relativePath = it.getString(pathColumn)
            val mimeType = it.getString(mimeColumn)

            Logger.d("LocalStorageClient", "Old name: $oldName, New name: $newFileName")
            Logger.d("LocalStorageClient", "Path: $relativePath, MIME: $mimeType")

            // Check if file with new name already exists
            val collection = when {
                mimeType.startsWith("image/") -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                mimeType.startsWith("video/") -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> return@withContext Pair(false, uri.toString())
            }

            val checkSelection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${android.provider.MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val checkArgs = arrayOf(newFileName, relativePath)
            val checkCursor = context.contentResolver.query(collection, arrayOf(android.provider.MediaStore.MediaColumns._ID), checkSelection, checkArgs, null)
            val exists = checkCursor?.use { it.count > 0 } ?: false
            checkCursor?.close()

            if (exists) {
                Logger.d("LocalStorageClient", "File with new name already exists")
                return@withContext Pair(false, uri.toString())
            }

            // Perform rename using ContentValues
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
            }

            val updated = context.contentResolver.update(uri, values, null, null)
            if (updated > 0) {
                Logger.d("LocalStorageClient", "File renamed successfully")
                // Create new URI for renamed file
                val newUri = android.content.ContentUris.withAppendedId(collection, id)
                return@withContext Pair(true, newUri.toString())
            } else {
                Logger.d("LocalStorageClient", "Failed to rename file")
                return@withContext Pair(false, uri.toString())
            }
        }
    }

    Logger.d("LocalStorageClient", "Failed to query file info")
    return@withContext Pair(false, uri.toString())
}
} catch (securityException: android.app.RecoverableSecurityException) {
Logger.e("LocalStorageClient", "RecoverableSecurityException - need user permission", securityException)
// Re-throw to be handled by caller
throw securityException
} catch (e: Exception) {
Logger.e("LocalStorageClient", "Error renaming file", e)
return@withContext Pair(false, uri.toString())
}
}

suspend fun writeFile(destinationFolderUri: Uri, fileName: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
    try {
        Logger.d("LocalStorageClient", "Writing file: $fileName (${data.size} bytes) to $destinationFolderUri")
        
        // Check if this is a DocumentFile TreeUri or MediaStore URI
        val uriString = destinationFolderUri.toString()
        
        if (uriString.startsWith("content://com.android.externalstorage.documents/tree/")) {
            // SAF/DocumentFile approach for user-selected folders
            val folder = DocumentFile.fromTreeUri(context, destinationFolderUri)
            if (folder == null || !folder.isDirectory) {
                Logger.e("LocalStorageClient", "Invalid destination folder URI")
                return@withContext false
            }
            
            // Determine MIME type from file extension
            val mimeType = when {
                fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
                fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
                fileName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
                fileName.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
                else -> "application/octet-stream"
            }
            
            // Check if file already exists
            val existingFile = folder.listFiles().find { it.name == fileName }
            if (existingFile != null) {
                Logger.w("LocalStorageClient", "File already exists: $fileName")
                return@withContext false
            }
            
            // Create new file
            val newFile = folder.createFile(mimeType, fileName)
            if (newFile == null) {
                Logger.e("LocalStorageClient", "Failed to create file")
                return@withContext false
            }
            
            // Write data
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                output.write(data)
                output.flush()
            }
            
            Logger.d("LocalStorageClient", "File written successfully via SAF")
            return@withContext true
            
        } else {
            Logger.e("LocalStorageClient", "Unsupported URI format for write: $destinationFolderUri")
            return@withContext false
        }
        
    } catch (e: Exception) {
        Logger.e("LocalStorageClient", "Error writing file", e)
        return@withContext false
    }
}

suspend fun writeFileToStandardFolder(fileName: String, data: ByteArray, folderName: String): Boolean = withContext(Dispatchers.IO) {
    try {
        Logger.d("LocalStorageClient", "Writing file: $fileName (${data.size} bytes) to standard folder: $folderName")
        
        // Determine MIME type and collection
        val mimeType = when {
            fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
            fileName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            fileName.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            else -> "application/octet-stream"
        }
        
        val collection = when {
            mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> {
                Logger.e("LocalStorageClient", "Unsupported MIME type: $mimeType")
                return@withContext false
            }
        }
        
        // Map folder names to relative paths
        val relativePath = when (folderName) {
            "Camera" -> "DCIM/Camera/"
            "Screenshots" -> "Pictures/Screenshots/"
            "Pictures" -> "Pictures/"
            "Download" -> "Download/"
            else -> "Pictures/"
        }
        
        // Prepare ContentValues
        val values = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        // Insert new file into MediaStore
        val newUri = context.contentResolver.insert(collection, values)
        if (newUri == null) {
            Logger.e("LocalStorageClient", "Failed to create file in MediaStore")
            return@withContext false
        }
        
        Logger.d("LocalStorageClient", "Created file URI: $newUri")
        
        // Write data to the file
        try {
            context.contentResolver.openOutputStream(newUri)?.use { output ->
                output.write(data)
                output.flush()
            }
            
            // Mark file as ready (not pending)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val readyValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(newUri, readyValues, null, null)
            }
            
            Logger.d("LocalStorageClient", "File written successfully to standard folder")
            return@withContext true
            
        } catch (e: Exception) {
            // If write failed, delete the created file
            context.contentResolver.delete(newUri, null, null)
            Logger.e("LocalStorageClient", "Failed to write data, file deleted", e)
            return@withContext false
        }
        
    } catch (e: Exception) {
        Logger.e("LocalStorageClient", "Error writing file to standard folder", e)
        return@withContext false
    }
}
}


