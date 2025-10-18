package com.sza.fastmediasorter.network

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
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
        // Priority 1: MediaStore access (for SCAN-discovered folders)
        if (bucketName != null) {
            return@withContext getImageFilesByBucketName(bucketName, isVideoEnabled, maxVideoSizeMb)
        }
        
        // Priority 2: SAF/DocumentFile access (for user-selected folders)
        if (folderUri == null) {
            return@withContext emptyList()
        }
        
        val maxVideoSizeBytes = maxVideoSizeMb * 1024L * 1024L
        
        try {
            val folder = DocumentFile.fromTreeUri(context, folderUri)
            folder?.listFiles()?.filter { file ->
                if (!file.isFile) return@filter false
                
                val mimeType = file.type ?: return@filter false
                val isImage = mimeType.startsWith("image/")
                val isVideo = mimeType.startsWith("video/")
                
                when {
                    isImage -> true
                    isVideo && isVideoEnabled -> {
                        val fileSize = file.length()
                        fileSize <= maxVideoSizeBytes
                    }
                    else -> false
                }
            }?.map {
                LocalImageInfo(
                    uri = it.uri,
                    name = it.name ?: "unknown",
                    size = it.length(),
                    dateModified = it.lastModified()
                )
            }?.sortedBy { it.name } ?: emptyList()
        } catch (e: Exception) {
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
val mediaFiles = mutableListOf<LocalImageInfo>()
val maxVideoSizeBytes = maxVideoSizeMb * 1024L * 1024L

// Query images
val imageProjection = arrayOf(
MediaStore.Images.Media._ID,
MediaStore.Images.Media.DISPLAY_NAME,
MediaStore.Images.Media.SIZE,
MediaStore.Images.Media.DATE_MODIFIED,
MediaStore.Images.Media.BUCKET_DISPLAY_NAME
)
val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
val selectionArgs = arrayOf(bucketName)
val sortOrder = "${MediaStore.Images.Media.DISPLAY_NAME} ASC"

try {
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
} catch (e: Exception) {
e.printStackTrace()
}

// Query videos if enabled
if (isVideoEnabled) {
val videoProjection = arrayOf(
MediaStore.Video.Media._ID,
MediaStore.Video.Media.DISPLAY_NAME,
MediaStore.Video.Media.SIZE,
MediaStore.Video.Media.DATE_MODIFIED,
MediaStore.Video.Media.BUCKET_DISPLAY_NAME
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
}
}
}
} catch (e: Exception) {
e.printStackTrace()
}
}

return@withContext mediaFiles.sortedBy { it.name }
}

suspend fun downloadImage(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
try {
android.util.Log.d("LocalStorageClient", "Reading image from URI: $uri")
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
android.util.Log.d("LocalStorageClient", "Successfully read ${result.size} bytes")
} else {
android.util.Log.e("LocalStorageClient", "Failed to open input stream for URI: $uri")
}
result
} catch (e: Exception) {
android.util.Log.e("LocalStorageClient", "Error reading image from URI: $uri", e)
e.printStackTrace()
null
}
}

fun getFileInfo(uri: Uri): LocalImageInfo? {
return try {
android.util.Log.d("LocalStorageClient", "Getting file info for URI: $uri")
val file = DocumentFile.fromSingleUri(context, uri)
if (file != null && file.exists()) {
val info = LocalImageInfo(
uri = uri,
name = file.name ?: "unknown",
size = file.length(),
dateModified = file.lastModified()
)
android.util.Log.d("LocalStorageClient", "File info: name=${info.name}, size=${info.size}")
info
} else {
android.util.Log.e("LocalStorageClient", "File does not exist or cannot be accessed: $uri")
null
}
} catch (e: Exception) {
android.util.Log.e("LocalStorageClient", "Error getting file info for URI: $uri", e)
e.printStackTrace()
null
}
}

suspend fun deleteImage(uri: Uri): Boolean = withContext(Dispatchers.IO) {
try {
android.util.Log.d("LocalStorageClient", "Attempting to delete media: $uri")
android.util.Log.d("LocalStorageClient", "Android version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        
// First, try using ContentResolver (works for MediaStore URIs)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
// Android 11+ (API 30+)
android.util.Log.d("LocalStorageClient", "Using Android 11+ delete method")
try {
val deleted = context.contentResolver.delete(uri, null, null)
android.util.Log.d("LocalStorageClient", "ContentResolver delete result: $deleted row(s)")
if (deleted > 0) return@withContext true
} catch (e: SecurityException) {
android.util.Log.w("LocalStorageClient", "ContentResolver delete failed with SecurityException, trying DocumentFile", e)
}
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
// Android 10 (API 29)
android.util.Log.d("LocalStorageClient", "Using Android 10 delete method")
try {
val deleted = context.contentResolver.delete(uri, null, null)
android.util.Log.d("LocalStorageClient", "ContentResolver delete result: $deleted row(s)")
if (deleted > 0) return@withContext true
} catch (e: SecurityException) {
android.util.Log.w("LocalStorageClient", "ContentResolver delete failed with SecurityException", e)
if (e is android.app.RecoverableSecurityException) {
android.util.Log.e("LocalStorageClient", "RecoverableSecurityException - would need user permission dialog")
}
}
} else {
// Android 9 and below
android.util.Log.d("LocalStorageClient", "Using Android 9 and below delete method")
val deleted = context.contentResolver.delete(uri, null, null)
android.util.Log.d("LocalStorageClient", "ContentResolver delete result: $deleted row(s)")
if (deleted > 0) return@withContext true
}
        
// Fallback: Try DocumentFile delete (works for some URIs that ContentResolver can't handle)
android.util.Log.d("LocalStorageClient", "Trying DocumentFile fallback delete method")
val documentFile = DocumentFile.fromSingleUri(context, uri)
if (documentFile != null && documentFile.exists()) {
val deleted = documentFile.delete()
android.util.Log.d("LocalStorageClient", "DocumentFile delete result: $deleted")
return@withContext deleted
} else {
android.util.Log.e("LocalStorageClient", "DocumentFile is null or doesn't exist")
return@withContext false
}
        
} catch (e: Exception) {
android.util.Log.e("LocalStorageClient", "Exception during delete: ${e.javaClass.simpleName}: ${e.message}", e)
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
android.util.Log.d("LocalStorageClient", "Attempting to rename file: $uri to $newFileName")

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

android.util.Log.d("LocalStorageClient", "Old name: $oldName, New name: $newFileName")
android.util.Log.d("LocalStorageClient", "Path: $relativePath, MIME: $mimeType")

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
android.util.Log.d("LocalStorageClient", "File with new name already exists")
return@withContext Pair(false, uri.toString())
}

// Perform rename using ContentValues
val values = android.content.ContentValues().apply {
put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
}

val updated = context.contentResolver.update(uri, values, null, null)
if (updated > 0) {
android.util.Log.d("LocalStorageClient", "File renamed successfully")
// Create new URI for renamed file
val newUri = android.content.ContentUris.withAppendedId(collection, id)
return@withContext Pair(true, newUri.toString())
} else {
android.util.Log.d("LocalStorageClient", "Failed to rename file")
return@withContext Pair(false, uri.toString())
}
}
}

android.util.Log.d("LocalStorageClient", "Failed to query file info")
return@withContext Pair(false, uri.toString())
} catch (e: Exception) {
android.util.Log.e("LocalStorageClient", "Error renaming file", e)
return@withContext Pair(false, uri.toString())
}
}
}

