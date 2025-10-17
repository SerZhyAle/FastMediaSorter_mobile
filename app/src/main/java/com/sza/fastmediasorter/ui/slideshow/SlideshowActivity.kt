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
    private var currentIndex = 0
    private var slideshowJob: Job? = null
    private var isPaused = false
    private var imageRotation = 0f
    private var currentBitmap: Bitmap? = null
    
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
        
        binding.backButton.setOnClickListener {
            onBackPressed()
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
            
            val backLayoutParams = binding.backButton.layoutParams as FrameLayout.LayoutParams
            backLayoutParams.height = topMargin
            binding.backButton.layoutParams = backLayoutParams
            
            val rotationLayoutParams = binding.rotationLayout.layoutParams as FrameLayout.LayoutParams
            rotationLayoutParams.height = bottomMargin
            binding.rotationLayout.layoutParams = rotationLayoutParams
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
            showTimerText("${preferenceManager.getInterval()}", 1000)
        }
    }
    
    private fun skipToPreviousImage() {
        if (images.isNotEmpty()) {
            currentIndex = if (currentIndex > 0) currentIndex - 1 else images.size - 1
            lifecycleScope.launch {
                loadCurrentImage()
            }
            // Auto-pause when going to previous
            isPaused = true
        }
    }
    
    private fun skipToNextImage() {
        if (images.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % images.size
            lifecycleScope.launch {
                loadCurrentImage()
            }
        }
    }
    
    private fun loadImages() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            imageRepository.loadImages()
                .onSuccess { imageList ->
                    images = imageList
                    if (images.isNotEmpty()) {
                        startSlideshow()
                    } else {
                        showError("No images found")
                    }
                }
                .onFailure { error ->
                    showError("Loading error: ${error.message}")
                }
            
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun startSlideshow() {
        slideshowJob = lifecycleScope.launch {
            while (true) {
                if (images.isNotEmpty()) {
                    loadCurrentImage()
                    
                    if (!isPaused) {
                        // Show interval after image load (1 second)
                        showTimerText("${preferenceManager.getInterval()}", 1000)
                    }
                    
                    val interval = preferenceManager.getInterval()
                    var elapsed = 0
                    
                    while (elapsed < interval) {
                        delay(1000L)
                        elapsed++
                        
                        if (isPaused) {
                            // Wait while paused
                            continue
                        }
                        
                        val remaining = interval - elapsed
                        
                        // Show countdown for last 3 seconds
                        if (remaining in 1..3) {
                            showTimerText("in $remaining", 0)
                        }
                    }
                    
                    if (!isPaused) {
                        currentIndex = (currentIndex + 1) % images.size
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
            val imageData = imageRepository.downloadImage(imageUrl)
            
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        super.onBackPressed()
        slideshowJob?.cancel()
        finish()
    }
}