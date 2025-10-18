package com.sza.fastmediasorter.ui.slideshow

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.bumptech.glide.Glide
import com.sza.fastmediasorter.databinding.ActivitySlideshowBinding
import com.sza.fastmediasorter.network.ImageRepository
import com.sza.fastmediasorter.network.LocalStorageClient
import com.sza.fastmediasorter.network.SmbClient
import com.sza.fastmediasorter.network.SmbDataSourceFactory
import com.sza.fastmediasorter.utils.MediaUtils
import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SlideshowActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySlideshowBinding
    private lateinit var imageRepository: ImageRepository
    private lateinit var preferenceManager: PreferenceManager
    private var localStorageClient: LocalStorageClient? = null
    private var isLocalMode = false
    
    private var images: List<String> = emptyList()
    private var sortedImages: List<String> = emptyList()
    private var currentIndex = 0
    private var slideshowJob: Job? = null
    private var isPaused = false
    private var currentBitmap: Bitmap? = null
    private var elapsedTime = 0
    private var isShuffleMode = false
    
    // Video support
    private var exoPlayer: ExoPlayer? = null
    private var isCurrentMediaVideo = false
    private var waitingForVideoEnd = false
    
    // Preloading optimization
    private var nextImageData: ByteArray? = null
    private var nextImageIndex: Int = -1
    private var lastLoadTime: Long = 0
    
    companion object {
        private const val KEY_CURRENT_INDEX = "current_index"
        private const val KEY_IMAGES = "images"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySlideshowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferenceManager = PreferenceManager(this)
        
        // Keep screen on if enabled
        if (preferenceManager.isKeepScreenOn()) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        imageRepository = ImageRepository(SmbClient(), preferenceManager)
        
        isLocalMode = preferenceManager.getConnectionType() == "LOCAL"
        if (isLocalMode) {
            localStorageClient = LocalStorageClient(this)
        }
        
        setupExoPlayer()
        setupFullscreen()
        
        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX, 0)
            val imagesList = savedInstanceState.getStringArrayList(KEY_IMAGES)
            if (imagesList != null && imagesList.isNotEmpty()) {
                images = imagesList
                startSlideshow()
            } else {
                loadImages()
            }
        } else {
            // Try to restore last session index
            currentIndex = preferenceManager.getLastImageIndex()
            loadImages()
        }
    }
    
    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        binding.videoLoadingLayout.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        binding.videoLoadingLayout.visibility = View.GONE
                    }
                    Player.STATE_ENDED -> {
                        binding.videoLoadingLayout.visibility = View.GONE
                        // Auto-advance to next media when video ends
                        if (waitingForVideoEnd && !isPaused) {
                            waitingForVideoEnd = false
                            skipToNextImage()
                        }
                    }
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                binding.videoLoadingLayout.visibility = View.GONE
                
                if (preferenceManager.isShowVideoErrorDetails()) {
                    showVideoErrorDialog(error)
                } else {
                    android.widget.Toast.makeText(
                        this@SlideshowActivity,
                        "Video playback error: ${error.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Skip to next on error
                if (!isPaused) {
                    skipToNextImage()
                }
            }
        })
    }
    
    private fun setupFullscreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        supportActionBar?.hide()
        
        binding.imageView.setOnClickListener {
            skipToNextImage()
        }
        
        setupControlAreas()
        updateControlsVisibility()
    }
    
    private fun updateControlsVisibility() {
        val showControls = preferenceManager.isShowControls()
        
        if (showControls) {
            // Show visible control panel
            binding.controlPanel.visibility = View.VISIBLE
            // Hide invisible touch areas
            binding.topLayout.visibility = View.GONE
            binding.controlsLayout.visibility = View.GONE
            binding.rotationLayout.visibility = View.GONE
        } else {
            // Hide control panel
            binding.controlPanel.visibility = View.GONE
            // Show invisible touch areas (transparent)
            binding.topLayout.visibility = View.VISIBLE
            binding.controlsLayout.visibility = View.VISIBLE
            binding.rotationLayout.visibility = View.VISIBLE
        }
    }
    
    private fun setupControlAreas() {
        binding.controlsLayout.post {
            val screenHeight = binding.controlsLayout.height
            val controlHeight = (screenHeight * 0.75).toInt()
            val topMargin = (screenHeight * 0.125).toInt()
            val bottomMargin = (screenHeight * 0.125).toInt()
            
            val controlLayoutParams = binding.controlsLayout.layoutParams as FrameLayout.LayoutParams
            controlLayoutParams.height = controlHeight
            controlLayoutParams.topMargin = topMargin
            binding.controlsLayout.layoutParams = controlLayoutParams
            
            val topLayoutParams = binding.topLayout.layoutParams as FrameLayout.LayoutParams
            topLayoutParams.height = topMargin
            binding.topLayout.layoutParams = topLayoutParams
            
            val rotationLayoutParams = binding.rotationLayout.layoutParams as FrameLayout.LayoutParams
            rotationLayoutParams.height = bottomMargin
            binding.rotationLayout.layoutParams = rotationLayoutParams
        }
        
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
        
        binding.shuffleButton.setOnClickListener {
            toggleShuffleMode()
        }
        
        binding.previousButton.setOnClickListener {
            skipToPreviousImage()
        }
        
        binding.pauseButton.setOnClickListener {
            togglePause()
        }
        
        binding.nextButton.setOnClickListener {
            skipToNextImage()
        }
        
        binding.rotateLeftButton.setOnClickListener {
            rotateImage(-90f)
        }
        
        binding.rotateRightButton.setOnClickListener {
            rotateImage(90f)
        }
        
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        
        binding.btnPrevious.setOnClickListener {
            skipToPreviousImage()
        }
        
        binding.btnPlayPause.setOnClickListener {
            togglePause()
        }
        
        binding.btnNext.setOnClickListener {
            skipToNextImage()
        }
        
        binding.btnRotateLeft.setOnClickListener {
            rotateImage(-90f)
        }
        
        binding.btnRotateRight.setOnClickListener {
            rotateImage(90f)
        }
        
        binding.btnShuffle.setOnClickListener {
            toggleShuffleMode()
        }
    }
    
    private fun rotateImage(degrees: Float) {
        val currentRotation = binding.imageView.rotation
        val newRotation = (currentRotation + degrees) % 360
        binding.imageView.rotation = newRotation
        
        // Auto-pause on rotation
        isPaused = true
    }
    
    private fun togglePause() {
        isPaused = !isPaused
        updatePlayPauseIcon()
        
        // Show interval when resuming, OFF when paused
        if (!isPaused) {
            val interval = preferenceManager.getInterval()
            showTimerText("$interval", 1000)
        } else {
            showTimerText("OFF", 1000)
        }
    }
    
    private fun updatePlayPauseIcon() {
        binding.btnPlayPause.setImageResource(
            if (isPaused) android.R.drawable.ic_media_play
            else android.R.drawable.ic_media_pause
        )
    }
    
    private fun toggleShuffleMode() {
        isShuffleMode = !isShuffleMode
        preferenceManager.setShuffleMode(isShuffleMode)
        
        val currentImagePath = if (images.isNotEmpty() && currentIndex < images.size) {
            images[currentIndex]
        } else null
        
        if (isShuffleMode) {
            // Switch to random
            images = sortedImages.shuffled()
            showTimerText("RND", 1000)
        } else {
            // Switch to ABC - continue from current file
            images = sortedImages
            currentImagePath?.let { path ->
                currentIndex = images.indexOf(path).takeIf { it >= 0 } ?: 0
            }
            showTimerText("ABC", 1000)
        }
        
        // Clear preloaded data on shuffle mode change
        nextImageData = null
        nextImageIndex = -1
        
        // Skip to next image in new order
        skipToNextImage()
    }
    
    private fun skipToPreviousImage() {
        if (images.isNotEmpty()) {
            currentIndex = if (currentIndex > 0) currentIndex - 1 else images.size - 1
            // Clear preloaded data on manual navigation
            nextImageData = null
            nextImageIndex = -1
            lifecycleScope.launch {
                loadCurrentMedia()
            }
            // Auto-pause when going to previous
            isPaused = true
        }
    }
    
    private fun skipToNextImage() {
        if (images.isNotEmpty()) {
            val targetIndex = (currentIndex + 1) % images.size
            
            // Re-shuffle when wrapping around to beginning in shuffle mode
            if (targetIndex == 0 && isShuffleMode && images.size > 1) {
                images = sortedImages.shuffled()
                // Clear preload cache since order changed
                nextImageData = null
                nextImageIndex = -1
            }
            
            // Check if target image is already preloaded
            if (nextImageIndex == targetIndex && nextImageData != null) {
                // Use preloaded data - instant switch
                currentIndex = targetIndex
                lifecycleScope.launch {
                    loadCurrentMedia()
                    
                    // Show interval after manual next
                    if (!isPaused) {
                        val interval = preferenceManager.getInterval()
                        showTimerText("$interval", 1000)
                    }
                }
            } else {
                // Not preloaded - clear cache and load normally
                currentIndex = targetIndex
                nextImageData = null
                nextImageIndex = -1
                lifecycleScope.launch {
                    loadCurrentMedia()
                    
                    // Show interval after manual next
                    if (!isPaused) {
                        val interval = preferenceManager.getInterval()
                        showTimerText("$interval", 1000)
                    }
                }
            }
            // Reset timer on manual next
            elapsedTime = 0
        }
    }
    
    private fun loadImages() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            if (isLocalMode) {
                loadLocalImages()
            } else {
                imageRepository.loadImages()
                    .onSuccess { imageList ->
                        // Save sorted list
                        sortedImages = imageList.sorted()
                        
                        // Load shuffle mode preference
                        isShuffleMode = preferenceManager.isShuffleMode()
                        
                        // Apply current mode
                        images = if (isShuffleMode) {
                            sortedImages.shuffled()
                        } else {
                            sortedImages
                        }
                        
                        if (images.isNotEmpty()) {
                            // Validate currentIndex
                            if (currentIndex >= images.size) {
                                currentIndex = 0
                            }
                            startSlideshow()
                        } else {
                            showError("No images found")
                        }
                    }
                    .onFailure { error ->
                        preferenceManager.clearLastSession()
                        showError("Loading error: ${error.message}")
                    }
            }
            
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private suspend fun loadLocalImages() {
        try {
            val localUri = preferenceManager.getLocalUri()
            val bucketName = preferenceManager.getLocalBucketName()
            val isVideoEnabled = preferenceManager.isVideoEnabled()
            val maxVideoSizeMb = preferenceManager.getMaxVideoSizeMb()
            
            val folderUri = if (localUri.isNotEmpty()) Uri.parse(localUri) else null
            val imageInfoList = localStorageClient?.getImageFiles(folderUri, bucketName.ifEmpty { null }, isVideoEnabled, maxVideoSizeMb) ?: emptyList()
            
            val imageUris = imageInfoList.map { it.uri.toString() }
            sortedImages = imageUris.sorted()
            
            isShuffleMode = preferenceManager.isShuffleMode()
            images = if (isShuffleMode) {
                sortedImages.shuffled()
            } else {
                sortedImages
            }
            
            if (images.isNotEmpty()) {
                if (currentIndex >= images.size) {
                    currentIndex = 0
                }
                startSlideshow()
            } else {
                showError("No images found")
            }
        } catch (e: Exception) {
            preferenceManager.clearLastSession()
            showError("Error loading local images: ${e.message}")
        }
    }
    
    private fun startSlideshow() {
        slideshowJob = lifecycleScope.launch {
            while (true) {
                if (images.isNotEmpty()) {
                    val loadStartTime = System.currentTimeMillis()
                    loadCurrentMedia()
                    val loadDuration = (System.currentTimeMillis() - loadStartTime) / 1000
                    lastLoadTime = loadDuration
                    
                    // For videos, skip interval timer - video will auto-advance on completion
                    if (isCurrentMediaVideo && waitingForVideoEnd) {
                        // Wait for video to complete (handled by Player.Listener)
                        while (waitingForVideoEnd && !isPaused) {
                            delay(1000L)
                        }
                        // Video ended, continue to next iteration
                        if (!isPaused) {
                            currentIndex = (currentIndex + 1) % images.size
                        }
                        continue
                    }
                    
                    if (!isPaused) {
                        // Show interval after image load (1 second)
                        val interval = preferenceManager.getInterval()
                        showTimerText("$interval", 1000)
                    }
                    
                    val interval = preferenceManager.getInterval()
                    elapsedTime = 0
                    
                    // Preload next image if interval > 2x load time
                    var preloadStarted = false
                    
                    while (elapsedTime < interval) {
                        delay(1000L)
                        elapsedTime++
                        
                        if (isPaused) {
                            // Wait while paused
                            continue
                        }
                        
                        val remaining = interval - elapsedTime
                        
                        // Start preloading if interval allows and not started yet
                        if (!preloadStarted && interval > loadDuration * 2 && remaining > loadDuration + 1) {
                            preloadStarted = true
                            val nextIndex = (currentIndex + 1) % images.size
                            lifecycleScope.launch {
                                preloadNextImage(nextIndex)
                            }
                        }
                        
                        // Show countdown for last 3 seconds
                        if (remaining in 1..3) {
                            showTimerText("in $remaining", 0)
                        }
                    }
                    
                    if (!isPaused) {
                        val nextIndex = (currentIndex + 1) % images.size
                        
                        // Re-shuffle when wrapping around to beginning in shuffle mode
                        if (nextIndex == 0 && isShuffleMode && images.size > 1) {
                            images = sortedImages.shuffled()
                            // Clear preload cache since order changed
                            nextImageData = null
                            nextImageIndex = -1
                        }
                        
                        // Check if next image is preloaded - if not, start loading now
                        if (nextImageIndex != nextIndex || nextImageData == null) {
                            // Not preloaded, load it now to minimize gap
                            lifecycleScope.launch {
                                preloadNextImage(nextIndex)
                            }.join() // Wait for preload to complete
                        }
                        currentIndex = nextIndex
                    }
                }
            }
        }
    }
    
    private fun showTimerText(text: String, hideAfterMs: Long) {
        lifecycleScope.launch {
            binding.timerText.text = text
            binding.timerText.visibility = View.VISIBLE
            
            if (hideAfterMs > 0) {
                delay(hideAfterMs)
                binding.timerText.visibility = View.GONE
            }
        }
    }
    
    private suspend fun loadCurrentMedia() {
        try {
            val mediaUrl = images[currentIndex]
            
            // Determine if current media is video
            isCurrentMediaVideo = MediaUtils.isVideo(mediaUrl)
            
            if (isCurrentMediaVideo) {
                // Load video
                binding.imageView.visibility = View.GONE
                binding.playerView.visibility = View.VISIBLE
                loadVideo(mediaUrl)
            } else {
                // Load image
                binding.playerView.visibility = View.GONE
                binding.imageView.visibility = View.VISIBLE
                loadImage(mediaUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun loadImage(imageUrl: String) {
        try {
            // Use preloaded data if available for current index
            val imageData = if (nextImageIndex == currentIndex && nextImageData != null) {
                val preloaded = nextImageData
                nextImageData = null // Clear after use
                nextImageIndex = -1
                preloaded
            } else {
                if (isLocalMode) {
                    localStorageClient?.downloadImage(Uri.parse(imageUrl))
                } else {
                    imageRepository.downloadImage(imageUrl)
                }
            }
            
            imageData?.let { data ->
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) {
                    currentBitmap = bitmap
                    binding.imageView.setImageBitmap(bitmap)
                    binding.imageView.rotation = 0f // Reset rotation for new image
                    
                    // Save last session state
                    saveSessonState()
                } else {
                    android.util.Log.e("SlideshowActivity", "Failed to decode image at index $currentIndex: ${images.getOrNull(currentIndex)}")
                    android.widget.Toast.makeText(
                        this@SlideshowActivity,
                        "⚠ Image corrupted, skipping to next",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    
                    // Auto-skip to next image
                    skipToNextImage()
                }
            } ?: run {
                android.util.Log.e("SlideshowActivity", "No image data received for index $currentIndex")
                android.widget.Toast.makeText(
                    this@SlideshowActivity,
                    "⚠ Failed to load image, skipping",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // Auto-skip to next image
                skipToNextImage()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun loadVideo(videoUrl: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            try {
                // Stop current playback
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()
                
                // Mark that we're waiting for video to complete
                waitingForVideoEnd = true
                
                if (isLocalMode) {
                    // Local video - use URI directly
                    val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                    exoPlayer?.setMediaItem(mediaItem)
                    exoPlayer?.prepare()
                    exoPlayer?.play()
                } else {
                    // SMB video - need to recreate player with custom data source
                    val smbContext = imageRepository.getSmbContext()
                    if (smbContext != null) {
                        android.util.Log.d("SlideshowActivity", "SMB Context available, creating player for: $videoUrl")
                        
                        // Release old player
                        exoPlayer?.release()
                        
                        // Create new player with SmbDataSource
                        val dataSourceFactory = SmbDataSourceFactory(imageRepository.smbClient)
                        exoPlayer = ExoPlayer.Builder(this@SlideshowActivity)
                            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory as androidx.media3.datasource.DataSource.Factory))
                            .build()
                        binding.playerView.player = exoPlayer
                        
                        // Re-add listener
                        exoPlayer?.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                when (playbackState) {
                                    Player.STATE_BUFFERING -> {
                                        binding.videoLoadingLayout.visibility = View.VISIBLE
                                        android.util.Log.d("SlideshowActivity", "Video buffering...")
                                    }
                                    Player.STATE_READY -> {
                                        binding.videoLoadingLayout.visibility = View.GONE
                                        android.util.Log.d("SlideshowActivity", "Video ready")
                                    }
                                    Player.STATE_ENDED -> {
                                        binding.videoLoadingLayout.visibility = View.GONE
                                        android.util.Log.d("SlideshowActivity", "Video ended")
                                        // Auto-advance to next media when video ends
                                        if (waitingForVideoEnd && !isPaused) {
                                            waitingForVideoEnd = false
                                            skipToNextImage()
                                        }
                                    }
                                }
                            }
                            
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                binding.videoLoadingLayout.visibility = View.GONE
                                android.util.Log.e("SlideshowActivity", "Video playback error", error)
                                
                                if (preferenceManager.isShowVideoErrorDetails()) {
                                    showVideoErrorDialog(error)
                                } else {
                                    android.widget.Toast.makeText(
                                        this@SlideshowActivity,
                                        "Video playback error: ${error.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                
                                // Skip to next on error
                                if (!isPaused) {
                                    skipToNextImage()
                                }
                            }
                        })
                        
                        val mediaItem = MediaItem.fromUri(videoUrl)
                        android.util.Log.d("SlideshowActivity", "Setting media item: $videoUrl")
                        exoPlayer?.setMediaItem(mediaItem)
                        exoPlayer?.prepare()
                        exoPlayer?.play()
                    } else {
                        android.util.Log.e("SlideshowActivity", "SMB context is null! Cannot play video.")
                        Toast.makeText(
                            this@SlideshowActivity,
                            "SMB connection not available. Reconnecting may help.",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Skip to next
                        if (!isPaused) {
                            skipToNextImage()
                        }
                    }
                }
                
                // Save last session state
                saveSessonState()
                
            } catch (e: Exception) {
                android.util.Log.e("SlideshowActivity", "Failed to load video: ${e.message}", e)
                Toast.makeText(
                    this@SlideshowActivity,
                    "⚠ Failed to load video, skipping",
                    Toast.LENGTH_SHORT
                ).show()
                waitingForVideoEnd = false
                skipToNextImage()
            }
        }
    }
    
    private suspend fun preloadNextImage(nextIndex: Int) {
        try {
            if (nextIndex < images.size) {
                val imageUrl = images[nextIndex]
                val imageData = if (isLocalMode) {
                    localStorageClient?.downloadImage(Uri.parse(imageUrl))
                } else {
                    imageRepository.downloadImage(imageUrl)
                }
                
                // Store preloaded data
                nextImageData = imageData
                nextImageIndex = nextIndex
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Silently fail preloading
        }
    }
    
    private fun saveSessonState() {
        val folderAddress = "${preferenceManager.getServerAddress()}\\${preferenceManager.getFolderPath()}"
        preferenceManager.saveLastSession(folderAddress, currentIndex)
    }
    
    private fun showVideoErrorDialog(error: androidx.media3.common.PlaybackException) {
        val currentFile = if (images.isNotEmpty() && currentIndex < images.size) {
            images[currentIndex]
        } else "Unknown"
        
        val errorDetails = StringBuilder()
        errorDetails.append("=== VIDEO PLAYBACK ERROR ===\n")
        errorDetails.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n\n")
        
        errorDetails.append("=== FILE INFO ===\n")
        errorDetails.append("File: ${currentFile.substringAfterLast('/')}\n")
        errorDetails.append("Path: $currentFile\n")
        errorDetails.append("Source: ${if (isLocalMode) "Local Storage" else "SMB Network"}\n")
        errorDetails.append("Mode: Slideshow\n\n")
        
        errorDetails.append("=== ERROR DETAILS ===\n")
        errorDetails.append("Exception: ${error.javaClass.simpleName}\n")
        errorDetails.append("Error Code: ${error.errorCode}\n")
        errorDetails.append("Message: ${error.message ?: "No message"}\n")
        errorDetails.append("Cause: ${error.cause?.javaClass?.simpleName ?: "None"}\n")
        if (error.cause != null) {
            errorDetails.append("Cause Message: ${error.cause?.message ?: "No message"}\n")
        }
        errorDetails.append("\n")
        
        errorDetails.append("=== PLAYBACK STATE ===\n")
        errorDetails.append("Video Enabled: ${preferenceManager.isVideoEnabled()}\n")
        errorDetails.append("Max Video Size: ${preferenceManager.getMaxVideoSizeMb()} MB\n")
        errorDetails.append("\n")
        
        errorDetails.append("=== SYSTEM INFO ===\n")
        errorDetails.append("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
        errorDetails.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
        errorDetails.append("\n")
        
        errorDetails.append("=== POSSIBLE CAUSES ===\n")
        when {
            error.message?.contains("codec", ignoreCase = true) == true -> {
                errorDetails.append("• Unsupported video codec\n")
                errorDetails.append("• Try converting to MP4 with H.264 codec\n")
            }
            error.message?.contains("source", ignoreCase = true) == true -> {
                errorDetails.append("• File not accessible\n")
                errorDetails.append("• Network connection issue (for SMB)\n")
                errorDetails.append("• File may be corrupted\n")
            }
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                errorDetails.append("• Network timeout (for SMB)\n")
                errorDetails.append("• File too large or slow connection\n")
            }
            else -> {
                errorDetails.append("• Unsupported video format\n")
                errorDetails.append("• Corrupted video file\n")
                errorDetails.append("• Insufficient device resources\n")
            }
        }
        errorDetails.append("\n")
        
        errorDetails.append("=== RECOMMENDATIONS ===\n")
        errorDetails.append("1. Try playing file with another app\n")
        errorDetails.append("2. Convert to MP4 (H.264 codec)\n")
        errorDetails.append("3. Reduce video resolution\n")
        errorDetails.append("4. Check file is not corrupted\n")
        errorDetails.append("5. For SMB: ensure stable network\n")
        errorDetails.append("6. For SMB connection errors: reconnect to share\n")
        errorDetails.append("\n=== DEBUG INFO ===\n")
        errorDetails.append("SMB Context Available: ${imageRepository.getSmbContext() != null}\n")
        errorDetails.append("Full URI: smb://$currentFile\n")
        errorDetails.append("\nSlideshow will skip to next file...\n")
        
        com.sza.fastmediasorter.ui.dialogs.DiagnosticDialog.show(
            this,
            "Video Playback Error",
            errorDetails.toString(),
            false
        )
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_INDEX, currentIndex)
        outState.putStringArrayList(KEY_IMAGES, ArrayList(images))
    }
    
    override fun onResume() {
        super.onResume()
        // Update controls visibility in case setting changed
        updateControlsVisibility()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupFullscreen()
        if (images.isNotEmpty()) {
            lifecycleScope.launch {
                loadCurrentMedia()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        slideshowJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
    
    override fun onBackPressed() {
        slideshowJob?.cancel()
        // Clear session when user manually exits
        preferenceManager.clearLastSession()
        super.onBackPressed()
    }
}