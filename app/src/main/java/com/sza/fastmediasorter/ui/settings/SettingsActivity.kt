package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sza.fastmediasorter.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide action bar
        supportActionBar?.hide()
        
        setupToolbar()
    }
    
    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
