package com.sza.fastmediasorter.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.sza.fastmediasorter.MainActivity
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.databinding.ActivityWelcomeBinding
import com.sza.fastmediasorter.ui.base.LocaleActivity
import com.sza.fastmediasorter.utils.PreferenceManager

class WelcomeActivity : LocaleActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: WelcomePagerAdapter
    private val indicators = mutableListOf<ImageView>()
    
    private val pages = listOf(
        WelcomePage(
            icon = "👋",
            title = "Welcome to FastMediaSorter!",
            description = "Before you start, let's configure the app properly",
            features = listOf(
                "⚠️ IMPORTANT: The app needs initial setup",
                "⚠️ Without setup, main screens will be empty",
                "✓ Follow these steps to configure everything",
                "✓ Takes only 2-3 minutes"
            )
        ),
        WelcomePage(
            icon = "⚙️",
            title = "STEP 1: Open Settings",
            description = "All configuration happens in Settings tab",
            features = listOf(
                "📍 Tap Settings icon (gear ⚙️) at the bottom",
                "📍 This is where you configure everything",
                "📍 You MUST do this before using other tabs",
                "⚠️ Skip this = empty main screen"
            )
        ),
        WelcomePage(
            icon = "🌐",
            title = "STEP 2: Add Network Connection",
            description = "Connect to your NAS or network share",
            features = listOf(
                "📍 In Settings → tap 'Network' section",
                "📍 Enter server: \\\\192.168.1.100\\Photos",
                "📍 Add username/password if needed",
                "📍 Tap 'Test Connection' to verify",
                "📍 Tap Save (💾) to store connection"
            )
        ),
        WelcomePage(
            icon = "🎯",
            title = "STEP 3: Configure Destinations",
            description = "Set up folders for sorting photos",
            features = listOf(
                "📍 In Settings → tap 'Sort to..' section",
                "📍 Create 2-10 colored destinations",
                "📍 Each destination = folder for sorted files",
                "📍 Example: 'Family', 'Work', 'Trash'",
                "⚠️ Without destinations, sorting won't work"
            )
        ),
        WelcomePage(
            icon = "�",
            title = "STEP 4 (Optional): Local Folders",
            description = "Access device photos - optional if using network",
            features = listOf(
                "📍 In Settings → tap 'Grant Media Access'",
                "📍 Allow permission to see Camera, Screenshots",
                "📍 Or add custom folders via '+' button",
                "✓ Skip if only using network shares"
            )
        ),
        WelcomePage(
            icon = "✅",
            title = "Setup Complete!",
            description = "Now you can use the app",
            features = listOf(
                "🎬 Slideshow tab: View photos automatically",
                "📤 Sort tab: Organize photos to destinations",
                "⚠️ If tabs are empty → check Settings again",
                "💡 Tip: Use 'Test' button to verify connections"
            )
        ),
        WelcomePage(
            icon = "🚀",
            title = "Quick Start Checklist",
            description = "Did you complete these steps?",
            features = listOf(
                "☐ Opened Settings tab (⚙️)",
                "☐ Added at least 1 network connection OR granted media access",
                "☐ Created at least 2 sort destinations",
                "☐ Tested connection with 'Test' button",
                "✅ If YES to all → tap 'Start Using App' below"
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
