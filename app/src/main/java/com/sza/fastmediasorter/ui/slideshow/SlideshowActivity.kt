package com.sza.fastmediasorter.ui.slideshow

import android.content.res.Configuration
import android.graphics.BitmapFactory
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
            
            val controlLayoutParams = binding.controlsLayout.layoutParams as FrameLayout.LayoutParams
            controlLayoutParams.height = controlHeight
            controlLayoutParams.topMargin = topMargin
            binding.controlsLayout.layoutParams = controlLayoutParams
            
            val backLayoutParams = binding.backButton.layoutParams as FrameLayout.LayoutParams
            backLayoutParams.height = topMargin
            binding.backButton.layoutParams = backLayoutParams
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
    }
    
    private fun togglePause() {
        isPaused = !isPaused
    }
    
    private fun skipToPreviousImage() {
        if (images.isNotEmpty()) {
            currentIndex = if (currentIndex > 0) currentIndex - 1 else images.size - 1
            lifecycleScope.launch {
                loadCurrentImage()
            }
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
                    delay(preferenceManager.getInterval() * 1000L)
                    if (!isPaused) {
                        currentIndex = (currentIndex + 1) % images.size
                    }
                }
            }
        }
    }
    
    private suspend fun loadCurrentImage() {
        try {
            val imageUrl = images[currentIndex]
            val imageData = imageRepository.downloadImage(imageUrl)
            
            imageData?.let { data ->
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                binding.imageView.setImageBitmap(bitmap)
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