package com.sza.fastmediasorter.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.sza.fastmediasorter.MainActivity
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityWelcomeBinding
import com.sza.fastmediasorter.utils.PreferenceManager

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: WelcomePagerAdapter
    private val indicators = mutableListOf<ImageView>()
    
    private val pages = listOf(
        WelcomePage(
            icon = "👋",
            title = "Welcome to FastMediaSorter!",
            description = "Quick tour of what you can do with this app",
            features = listOf(
                "View photos from local folders or network shares",
                "Organize images with quick sort functionality",
                "Enjoy slideshow with customizable controls"
            )
        ),
        WelcomePage(
            icon = "📁",
            title = "1. Local Folder Access",
            description = "Access photos from your device",
            features = listOf(
                "✓ Grant media permission to access device photos",
                "✓ Standard folders: Camera, Screenshots, Pictures, Download",
                "✓ Add custom folders with Storage Access Framework",
                "⚠️ Optional - you can skip if using only network folders"
            )
        ),
        WelcomePage(
            icon = "🌐",
            title = "2. Network Connections",
            description = "Connect to SMB/CIFS shares on your local network",
            features = listOf(
                "✓ Add network folders: \\\\server\\folder",
                "✓ Save multiple connections with credentials",
                "✓ Test connection before saving",
                "✓ Works with NAS, Windows shares, Samba servers"
            )
        ),
        WelcomePage(
            icon = "🎯",
            title = "3. Sort Destinations",
            description = "Set up folders where you want to copy/move photos",
            features = listOf(
                "✓ Create up to 10 colored destinations",
                "✓ Use saved network connections",
                "✓ Give each destination a short name",
                "✓ Quick access during sorting"
            )
        ),
        WelcomePage(
            icon = "🖼️",
            title = "4. Slideshow Mode",
            description = "View your photos with automatic transitions",
            features = listOf(
                "✓ Set interval: 1-300 seconds",
                "✓ Touch zones: Navigate, pause, shuffle",
                "✓ Button controls: Optional overlay panel",
                "✓ Fullscreen experience"
            )
        ),
        WelcomePage(
            icon = "📤",
            title = "5. Sort Activity",
            description = "Organize your photos efficiently",
            features = listOf(
                "✓ Copy media to destinations (keeps original)",
                "✓ Move media (removes from source)",
                "✓ Delete media with confirmation",
                "✓ Navigate with touch zones or swipe"
            )
        ),
        WelcomePage(
            icon = "🚀",
            title = "Ready to Start!",
            description = "You're all set to use FastMediaSorter",
            features = listOf(
                "💡 Tip: Start by adding a network connection or granting media access",
                "💡 Tip: Configure sort destinations in Settings",
                "💡 Tip: Use Test button to verify network connections",
                "📖 You can always return to this guide from Settings"
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        
        preferenceManager = PreferenceManager(this)
        
        // Show close button if opened from Settings (not first launch)
        val isFirstLaunch = intent.getBooleanExtra("isFirstLaunch", false)
        binding.closeButton.visibility = if (isFirstLaunch) View.GONE else View.VISIBLE
        
        setupViewPager()
        setupIndicators()
        setupButtons()
        
        binding.closeButton.setOnClickListener {
            finish()
        }
    }
    
    private fun setupViewPager() {
        adapter = WelcomePagerAdapter(pages)
        binding.viewPager.adapter = adapter
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                updateButtons(position)
            }
        })
    }
    
    private fun setupIndicators() {
        indicators.clear()
        binding.indicatorLayout.removeAllViews()
        
        val size = (8 * resources.displayMetrics.density).toInt()
        val margin = (4 * resources.displayMetrics.density).toInt()
        
        for (i in pages.indices) {
            val indicator = ImageView(this)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, 0, margin, 0)
            indicator.layoutParams = params
            indicator.setImageResource(R.drawable.ic_settings)
            indicator.setColorFilter(
                ContextCompat.getColor(this, 
                    if (i == 0) R.color.primary else android.R.color.darker_gray
                )
            )
            binding.indicatorLayout.addView(indicator)
            indicators.add(indicator)
        }
    }
    
    private fun updateIndicators(position: Int) {
        indicators.forEachIndexed { index, indicator ->
            indicator.setColorFilter(
                ContextCompat.getColor(this,
                    if (index == position) R.color.primary else android.R.color.darker_gray
                )
            )
        }
    }
    
    private fun updateButtons(position: Int) {
        when {
            position == pages.size - 1 -> {
                binding.skipButton.visibility = View.GONE
                binding.nextButton.text = "Get Started"
            }
            else -> {
                binding.skipButton.visibility = View.VISIBLE
                binding.nextButton.text = "Next"
            }
        }
    }
    
    private fun setupButtons() {
        binding.nextButton.setOnClickListener {
            if (binding.viewPager.currentItem < pages.size - 1) {
                binding.viewPager.currentItem = binding.viewPager.currentItem + 1
            } else {
                finishWelcome()
            }
        }
        
        binding.skipButton.setOnClickListener {
            finishWelcome()
        }
    }
    
    private fun finishWelcome() {
        val isFirstLaunch = intent.getBooleanExtra("isFirstLaunch", false)
        
        // Mark welcome as shown
        preferenceManager.setWelcomeShown()
        
        if (isFirstLaunch) {
            // First launch: start MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        
        finish()
    }
}

data class WelcomePage(
    val icon: String,
    val title: String,
    val description: String,
    val features: List<String>
)
