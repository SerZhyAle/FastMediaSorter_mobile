package com.sza.fastmediasorter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sza.fastmediasorter.databinding.ActivityMainBinding
import com.sza.fastmediasorter.ui.slideshow.SlideshowActivity
import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferenceManager = PreferenceManager(this)
        
        loadSavedSettings()
        setupClickListeners()
    }
    
    private fun loadSavedSettings() {
        binding.serverInput.setText(preferenceManager.getServerAddress())
        binding.usernameInput.setText(preferenceManager.getUsername())
        binding.passwordInput.setText(preferenceManager.getPassword())
        binding.folderInput.setText(preferenceManager.getFolderPath())
        binding.intervalInput.setText(preferenceManager.getInterval().toString())
    }
    
    private fun setupClickListeners() {
        binding.connectButton.setOnClickListener {
            val server = binding.serverInput.text.toString().trim()
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val folder = binding.folderInput.text.toString().trim()
            val interval = binding.intervalInput.text.toString().toIntOrNull() ?: 5
            
            if (server.isEmpty() || folder.isEmpty()) {
                Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            saveSettings(server, username, password, folder, interval)
            startSlideshow()
        }
    }
    
    private fun saveSettings(server: String, username: String, password: String, folder: String, interval: Int) {
        preferenceManager.saveConnectionSettings(server, username, password, folder)
        preferenceManager.setInterval(interval)
    }
    
    private fun startSlideshow() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val intent = Intent(this@MainActivity, SlideshowActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.connectButton.isEnabled = !show
    }
}