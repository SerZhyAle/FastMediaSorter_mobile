package com.sza.fastmediasorter.ui.slideshow

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.sza.fastmediasorter.databinding.ActivitySlideshowBinding
import com.sza.fastmediasorter.network.ImageRepository
import com.sza.fastmediasorter.network.SmbClient
import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SlideshowActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySlideshowBinding
    private lateinit var imageRepository: ImageRepository
    private lateinit var preferenceManager: PreferenceManager
    
    private var images: List<String> = emptyList()
    private var sortedImages: List<String> = emptyList()
    private var currentIndex = 0
    private var slideshowJob: Job? = null
    private var isPaused = false
    private var imageRotation = 0f
    private var currentBitmap: Bitmap? = null
    private var elapsedTime = 0
    private var isShuffleMode = false
    
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
        imageRepository = ImageRepository(SmbClient(), preferenceManager)
        
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
    }
    
    private fun rotateImage(degrees: Float) {
        imageRotation = (imageRotation + degrees) % 360
        currentBitmap?.let { bitmap ->
            val rotatedBitmap = rotateBitmap(bitmap, imageRotation)
            binding.imageView.setImageBitmap(rotatedBitmap)
            binding.imageView.rotation = 0f
        }
        // Auto-pause on rotation
        isPaused = true
    }
    
    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
    
    private fun togglePause() {
        isPaused = !isPaused
        
        // Show interval when resuming
        if (!isPaused) {
            val interval = preferenceManager.getInterval()
            showTimerText("$interval", 1000)
        }
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
                loadCurrentImage()
            }
            // Auto-pause when going to previous
            isPaused = true
        }
    }
    
    private fun skipToNextImage() {
        if (images.isNotEmpty()) {
            val targetIndex = (currentIndex + 1) % images.size
            
            // Check if target image is already preloaded
            if (nextImageIndex == targetIndex && nextImageData != null) {
                // Use preloaded data - instant switch
                currentIndex = targetIndex
                lifecycleScope.launch {
                    loadCurrentImage()
                    
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
                    loadCurrentImage()
                    
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
            
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun startSlideshow() {
        slideshowJob = lifecycleScope.launch {
            while (true) {
                if (images.isNotEmpty()) {
                    val loadStartTime = System.currentTimeMillis()
                    loadCurrentImage()
                    val loadDuration = (System.currentTimeMillis() - loadStartTime) / 1000
                    lastLoadTime = loadDuration
                    
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
    
    private suspend fun loadCurrentImage() {
        try {
            val imageUrl = images[currentIndex]
            
            // Use preloaded data if available for current index
            val imageData = if (nextImageIndex == currentIndex && nextImageData != null) {
                val preloaded = nextImageData
                nextImageData = null // Clear after use
                nextImageIndex = -1
                preloaded
            } else {
                imageRepository.downloadImage(imageUrl)
            }
            
            imageData?.let { data ->
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                currentBitmap = bitmap
                
                if (imageRotation != 0f) {
                    val rotatedBitmap = rotateBitmap(bitmap, imageRotation)
                    binding.imageView.setImageBitmap(rotatedBitmap)
                } else {
                    binding.imageView.setImageBitmap(bitmap)
                }
                binding.imageView.rotation = 0f
                
                // Save last session state
                saveSessonState()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun preloadNextImage(nextIndex: Int) {
        try {
            if (nextIndex < images.size) {
                val imageUrl = images[nextIndex]
                val imageData = imageRepository.downloadImage(imageUrl)
                
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
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_INDEX, currentIndex)
        outState.putStringArrayList(KEY_IMAGES, ArrayList(images))
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupFullscreen()
        if (images.isNotEmpty()) {
            lifecycleScope.launch {
                loadCurrentImage()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        slideshowJob?.cancel()
    }
    
    override fun onBackPressed() {
        slideshowJob?.cancel()
        // Clear session when user manually exits
        preferenceManager.clearLastSession()
        super.onBackPressed()
    }
}