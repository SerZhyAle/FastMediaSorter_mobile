package com.sza.fastmediasorter.utils

object MediaUtils {
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp")
    
    fun isImage(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in IMAGE_EXTENSIONS
    }
    
    fun isVideo(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in VIDEO_EXTENSIONS
    }
    
    // Alias methods for backward compatibility and testing
    fun isImageFile(filename: String): Boolean = isImage(filename)
    
    fun isVideoFile(filename: String): Boolean = isVideo(filename)
    
    fun getFileExtension(filename: String): String {
        return filename.substringAfterLast('.', "")
    }
    
    fun getImageExtensions(): Set<String> = IMAGE_EXTENSIONS
    
    fun getVideoExtensions(): Set<String> = VIDEO_EXTENSIONS
    
    fun getAllMediaExtensions(): Set<String> = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS
    
    fun formatFileSize(bytes: Long): String {
        return when {
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
}
