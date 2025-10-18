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
    
    fun getImageExtensions(): Set<String> = IMAGE_EXTENSIONS
    
    fun getVideoExtensions(): Set<String> = VIDEO_EXTENSIONS
    
    fun getAllMediaExtensions(): Set<String> = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS
    
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
