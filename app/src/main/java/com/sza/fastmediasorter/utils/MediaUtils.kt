package com.sza.fastmediasorter.utils

object MediaUtils {
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "webm", "3gp")

    fun isImage(filename: String): Boolean {
        // Handle Android content URIs by extracting filename from URI
        val fileName =
            if (filename.startsWith("content://")) {
                // For content URIs, extract the display name from the URI
                filename.substringAfterLast('/', "").substringAfterLast("%2F", "")
            } else {
                // For regular file paths, use the filename as-is
                filename.substringAfterLast('/')
            }

        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in IMAGE_EXTENSIONS
    }

    fun isVideo(filename: String): Boolean {
        // Handle Android content URIs by extracting filename from URI
        val fileName =
            if (filename.startsWith("content://")) {
                // For content URIs, extract the display name from the URI
                filename.substringAfterLast('/', "").substringAfterLast("%2F", "")
            } else {
                // For regular file paths, use the filename as-is
                filename.substringAfterLast('/')
            }

        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in VIDEO_EXTENSIONS
    }

    // Alias methods for backward compatibility and testing
    fun isImageFile(filename: String): Boolean = isImage(filename)

    fun isVideoFile(filename: String): Boolean = isVideo(filename)

    fun getFileExtension(filename: String): String = filename.substringAfterLast('.', "")

    fun getImageExtensions(): Set<String> = IMAGE_EXTENSIONS

    fun getVideoExtensions(): Set<String> = VIDEO_EXTENSIONS

    fun getAllMediaExtensions(): Set<String> = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS

    fun formatFileSize(bytes: Long): String =
        when {
            bytes == 0L -> "0 B"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> {
                val kb = bytes / 1024.0
                if (kb == kb.toInt().toDouble()) "${kb.toInt()} KB" else "${"%.1f".format(kb)} KB"
            }
            bytes < 1024 * 1024 * 1024 -> {
                val mb = bytes / (1024.0 * 1024.0)
                if (mb == mb.toInt().toDouble()) "${mb.toInt()} MB" else "${"%.1f".format(mb)} MB"
            }
            else -> {
                val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                "${"%.1f".format(gb)} GB"
            }
        }
}
