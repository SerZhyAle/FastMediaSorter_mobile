package com.sza.fastmediasorter.ui.slideshow

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySlideshowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferenceManager = PreferenceManager(this)
        imageRepository = ImageRepository(SmbClient(), preferenceManager)
        
        setupFullscreen()
        loadImages()
    }
    
    private fun setupFullscreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        supportActionBar?.hide()
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
                    currentIndex = (currentIndex + 1) % images.size
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