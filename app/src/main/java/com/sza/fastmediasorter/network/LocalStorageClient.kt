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
suspend fun getImageFiles(folderUri: Uri?): List<LocalImageInfo> = withContext(Dispatchers.IO) {
if (folderUri == null) {
return@withContext emptyList()
}
try {
val folder = DocumentFile.fromTreeUri(context, folderUri)
folder?.listFiles()?.filter {
it.isFile && (it.type?.startsWith("image/") == true)
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

suspend fun downloadImage(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
try {
context.contentResolver.openInputStream(uri)?.use { input ->
val buffer = ByteArrayOutputStream()
val data = ByteArray(16384)
var count: Int
while (input.read(data, 0, data.size).also { count = it } != -1) {
buffer.write(data, 0, count)
}
buffer.toByteArray()
}
} catch (e: Exception) {
null
}
}

fun getFileInfo(uri: Uri): LocalImageInfo? {
return try {
val file = DocumentFile.fromSingleUri(context, uri)
if (file != null && file.exists()) {
LocalImageInfo(
uri = uri,
name = file.name ?: "unknown",
size = file.length(),
dateModified = file.lastModified()
)
} else {
null
}
} catch (e: Exception) {
null
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
}
