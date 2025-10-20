package com.sza.fastmediasorter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.sza.fastmediasorter.data.ConnectionConfig
import com.sza.fastmediasorter.databinding.ActivityMainBinding
import com.sza.fastmediasorter.ui.ConnectionViewModel
import com.sza.fastmediasorter.ui.MainPagerAdapter
import com.sza.fastmediasorter.ui.base.LocaleActivity
import com.sza.fastmediasorter.ui.network.NetworkFragment
import com.sza.fastmediasorter.ui.slideshow.SlideshowActivity
import com.sza.fastmediasorter.ui.welcome.WelcomeActivity
import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : LocaleActivity() {
private lateinit var binding: ActivityMainBinding
private lateinit var preferenceManager: PreferenceManager
private val viewModel: ConnectionViewModel by viewModels()
private var currentConfigId: Long? = null
private var currentConfig: ConnectionConfig? = null

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
binding = ActivityMainBinding.inflate(layoutInflater)
setContentView(binding.root)
supportActionBar?.hide()
preferenceManager = PreferenceManager(this)

setupViewPager()
setupClickListeners()

// Check for first launch - show Welcome activity
if (!preferenceManager.isWelcomeShown()) {
    val intent = Intent(this, WelcomeActivity::class.java)
    intent.putExtra("isFirstLaunch", true)
    startActivity(intent)
}

tryAutoResumeSession()
}

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.local_folders)
                1 -> getString(R.string.network)
                else -> ""
            }
        }.attach()
        binding.viewPager.offscreenPageLimit = 2
        
        // Switch to Network tab if no media permission
        if (!com.sza.fastmediasorter.network.LocalStorageClient.hasMediaPermission(this)) {
            binding.viewPager.setCurrentItem(1, false)
        }
        
        binding.viewPager.post {
            val networkFragment = supportFragmentManager.findFragmentByTag("f1") as? NetworkFragment
            networkFragment?.let { setupNetworkFragmentCallbacks(it) }
            val localFragment = supportFragmentManager.findFragmentByTag("f0") as? com.sza.fastmediasorter.ui.local.LocalFoldersFragment
            localFragment?.let { setupLocalFragmentCallbacks(it) }
        }
        updateButtonsState()
    }
    
    private fun getIntervalFromInput(): Int? {
        return binding.intervalInput.text.toString().toIntOrNull()?.takeIf { it in 1..300 }
    }
    
    private suspend fun updateConfigInterval(configId: Long): ConnectionConfig? {
        // For local standard folders (negative IDs), use cached config
        if (configId < 0) {
            return currentConfig?.let { config ->
                val interval = getIntervalFromInput() ?: config.interval
                // Save interval to PreferenceManager for local folders
                val preferenceManager = PreferenceManager(this)
                preferenceManager.setInterval(interval)
                config.copy(interval = interval)
            }
        }
        
        // For DB configs (positive IDs), fetch from database
        val config = viewModel.getConfigById(configId) ?: return null
        val interval = getIntervalFromInput() ?: config.interval
        
        return if (config.interval != interval) {
            val updatedConfig = config.copy(interval = interval)
            viewModel.updateConfig(updatedConfig)
            updatedConfig
        } else {
            config
        }
    }private fun setupNetworkFragmentCallbacks(fragment: NetworkFragment) {
fragment.onConfigSelected = { config ->
currentConfigId = config.id
currentConfig = config
// Only update interval field if empty or invalid, don't overwrite user's input
val currentInterval = binding.intervalInput.text.toString().toIntOrNull()
if (currentInterval == null || currentInterval !in 1..300) {
    binding.intervalInput.setText(config.interval.toString())
}
updateButtonsState()
}
fragment.onConfigDoubleClick = { config ->
loadConfigAndStartSlideshow(config)
}
}

private fun setupLocalFragmentCallbacks(fragment: com.sza.fastmediasorter.ui.local.LocalFoldersFragment) {
fragment.onFolderSelected = { folder ->
lifecycleScope.launch {
val config = if (folder.isCustom) {
// Custom folders from SCAN or manual selection - stored in DB
viewModel.localCustomFolders.value?.find { it.localDisplayName == folder.name }
} else {
// Standard folders (Camera, Screenshots, Pictures, Download)
// Use negative ID to indicate temporary/standard folder (not in DB)
createStandardLocalConfig(folder.name)
}
config?.let {
currentConfigId = it.id
currentConfig = it
// Only update interval field if empty or invalid, don't overwrite user's input
val currentInterval = binding.intervalInput.text.toString().toIntOrNull()
if (currentInterval == null || currentInterval !in 1..300) {
    binding.intervalInput.setText(it.interval.toString())
}
updateButtonsState()
}
}
}
fragment.onFolderDoubleClick = { folder ->
lifecycleScope.launch {
val config = if (folder.isCustom) {
// Custom folders - use existing DB record
viewModel.localCustomFolders.value?.find { it.localDisplayName == folder.name }
} else {
// Standard folders - create temporary config with negative ID
createStandardLocalConfig(folder.name)
}
config?.let {
val interval = binding.intervalInput.text.toString().toIntOrNull() ?: it.interval
val updatedConfig = it.copy(interval = interval)
// Only update DB for custom folders (positive ID)
if (it.id > 0) {
viewModel.updateConfig(updatedConfig)
}
loadConfigAndStartSlideshow(updatedConfig)
}
}
}
}

private fun createStandardLocalConfig(bucketName: String): com.sza.fastmediasorter.data.ConnectionConfig {
// Use negative ID to distinguish standard folders from DB records
// Camera=-1, Screenshots=-2, Pictures=-3, Download=-4
val id = when (bucketName) {
    "Camera" -> -1L
    "Screenshots" -> -2L
    "Pictures" -> -3L
    "Download" -> -4L
    else -> 0L
}
return com.sza.fastmediasorter.data.ConnectionConfig(
    id = id,
    name = bucketName,
    serverAddress = "",
    username = "",
    password = "",
    folderPath = "",
    interval = binding.intervalInput.text.toString().toIntOrNull() ?: 10,
    lastUsed = System.currentTimeMillis(),
    type = "LOCAL_STANDARD",
    localUri = "",
    localDisplayName = bucketName
)
}

    private fun tryAutoResumeSession() {
        val lastFolderAddress = preferenceManager.getLastFolderAddress()
        if (lastFolderAddress.isEmpty()) {
            return
        }
lifecycleScope.launch {
val config = viewModel.getConfigByFolderAddress(lastFolderAddress)
if (config != null) {
loadConfigAndStartSlideshow(config)
} else {
preferenceManager.clearLastSession()
}
}
}

    private fun openSettingsForFirstLaunch() {
        val intent = Intent(this, com.sza.fastmediasorter.ui.settings.SettingsActivity::class.java)
        intent.putExtra("initialTab", 2) // Open Settings tab (index 2)
        startActivity(intent)
    }

    private fun loadConfigAndStartSlideshow(config: ConnectionConfig) {
        // Get current interval from input field (use config.interval as fallback)
        val currentInterval = binding.intervalInput.text.toString().toIntOrNull()?.takeIf { it in 1..300 } ?: config.interval
        
        // Test connection before starting slideshow
        if (config.type != "LOCAL_CUSTOM" && config.type != "LOCAL_STANDARD") {
            testConnectionAndStartSlideshow(config, currentInterval)
            return
        }
        
        if (config.type == "LOCAL_CUSTOM" || config.type == "LOCAL_STANDARD") {
            preferenceManager.saveLocalFolderSettings(
                config.localUri ?: "",
                config.localDisplayName ?: "",
                currentInterval
            )
        } else {
            preferenceManager.saveConnectionSettings(
                config.serverAddress,
                config.username,
                config.password,
                config.folderPath
            )
            preferenceManager.setInterval(currentInterval)
        }
        // Only update lastUsed for DB records (positive ID)
        if (config.id > 0) {
            viewModel.updateLastUsed(config.id)
            // Also update interval in database if it differs
            if (currentInterval != config.interval) {
                viewModel.updateConfigInterval(config.id, currentInterval)
            }
        }
        val intent = Intent(this, SlideshowActivity::class.java)
        startActivity(intent)
    }
    
    private fun testConnectionAndStartSlideshow(config: ConnectionConfig, currentInterval: Int) {
        lifecycleScope.launch {
            // Show progress
            val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("Checking Connection")
                .setMessage("Testing folder accessibility...")
                .setCancelable(false)
                .create()
            progressDialog.show()
            
            try {
                val testResult = withTimeoutOrNull(5000) {
                    withContext(Dispatchers.IO) {
                        val smbClient = com.sza.fastmediasorter.network.SmbClient()
                        val connected = smbClient.connect(config.serverAddress, config.username, config.password)
                        if (!connected) {
                            throw Exception("Failed to connect to server")
                        }
                        val result = smbClient.getImageFiles(config.serverAddress, config.folderPath, false, 100)
                        if (result.errorMessage != null) {
                            throw Exception(result.errorMessage)
                        }
                        
                        // For connection validation, warnings are OK - only errors should fail
                        // result.warningMessage is informational only
                        
                        // Check write permissions
                        val hasWritePermission = smbClient.checkWritePermission(config.serverAddress, config.folderPath)
                        
                        // Update write permission in database if config has ID
                        if (config.id > 0) {
                            val database = com.sza.fastmediasorter.data.AppDatabase.getDatabase(this@MainActivity)
                            database.connectionConfigDao().updateWritePermission(config.id, hasWritePermission)
                        }
                        
                        hasWritePermission
                    }
                }
                
                progressDialog.dismiss()
                
                if (testResult == null) {
                    throw Exception("Connection timeout (5 seconds). Server or folder may be unreachable.")
                }
                
                // Success - save settings and start slideshow
                preferenceManager.saveConnectionSettings(
                    config.serverAddress,
                    config.username,
                    config.password,
                    config.folderPath
                )
                preferenceManager.setInterval(currentInterval)
                
                if (config.id > 0) {
                    viewModel.updateLastUsed(config.id)
                }
                
                val intent = Intent(this@MainActivity, SlideshowActivity::class.java)
                startActivity(intent)
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Connection Failed")
                    .setMessage("Cannot connect to folder:\n\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun testConnectionAndStartSort(config: ConnectionConfig, configId: Long) {
        // Skip test for local folders
        if (config.type == "LOCAL_CUSTOM" || config.type == "LOCAL_STANDARD") {
            val intent = Intent(this, com.sza.fastmediasorter.ui.sort.SortActivity::class.java)
            intent.putExtra("configId", configId)
            startActivity(intent)
            return
        }
        
        lifecycleScope.launch {
            // Show progress
            val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("Checking Connection")
                .setMessage("Testing folder accessibility...")
                .setCancelable(false)
                .create()
            progressDialog.show()
            
            try {
                val testResult = withTimeoutOrNull(5000) {
                    withContext(Dispatchers.IO) {
                        val smbClient = com.sza.fastmediasorter.network.SmbClient()
                        val connected = smbClient.connect(config.serverAddress, config.username, config.password)
                        if (!connected) {
                            throw Exception("Failed to connect to server")
                        }
                        val result = smbClient.getImageFiles(config.serverAddress, config.folderPath, false, 100)
                        if (result.errorMessage != null) {
                            throw Exception(result.errorMessage)
                        }
                        
                        // For connection validation, warnings are OK - only errors should fail
                        // result.warningMessage is informational only
                        
                        // Check write permissions
                        val hasWritePermission = smbClient.checkWritePermission(config.serverAddress, config.folderPath)
                        
                        // Update write permission in database
                        val database = com.sza.fastmediasorter.data.AppDatabase.getDatabase(this@MainActivity)
                        database.connectionConfigDao().updateWritePermission(configId, hasWritePermission)
                        
                        hasWritePermission
                    }
                }
                
                progressDialog.dismiss()
                
                if (testResult == null) {
                    throw Exception("Connection timeout (5 seconds). Server or folder may be unreachable.")
                }
                
                // Success - start sort activity
                val intent = Intent(this@MainActivity, com.sza.fastmediasorter.ui.sort.SortActivity::class.java)
                intent.putExtra("configId", configId)
                startActivity(intent)
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Connection Failed")
                    .setMessage("Cannot connect to folder:\n\n${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }private fun setupClickListeners() {
// Save interval on text change
binding.intervalInput.addTextChangedListener(object : android.text.TextWatcher {
override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
override fun afterTextChanged(s: android.text.Editable?) {
val interval = s.toString().toIntOrNull()
if (interval != null && interval in 1..300) {
currentConfigId?.let { configId ->
lifecycleScope.launch {
updateConfigInterval(configId)
}
}
}
}
})

binding.slideshowButton.setOnClickListener {
currentConfigId?.let { configId ->
lifecycleScope.launch {
val config = updateConfigInterval(configId)
if (config != null) {
loadConfigAndStartSlideshow(config)
} else {
Toast.makeText(this@MainActivity, getString(R.string.select_connection_first), Toast.LENGTH_SHORT).show()
}
}
} ?: run {
Toast.makeText(this, getString(R.string.select_connection_first), Toast.LENGTH_SHORT).show()
}
}
binding.sortButton.setOnClickListener {
currentConfigId?.let { configId ->
lifecycleScope.launch {
val config = updateConfigInterval(configId)
if (config != null) {
testConnectionAndStartSort(config, configId)
} else {
Toast.makeText(this@MainActivity, getString(R.string.select_connection_first), Toast.LENGTH_SHORT).show()
}
}
} ?: run {
Toast.makeText(this, getString(R.string.select_connection_first), Toast.LENGTH_SHORT).show()
}
}
binding.settingsButton.setOnClickListener {
val intent = Intent(this, com.sza.fastmediasorter.ui.settings.SettingsActivity::class.java)
startActivity(intent)
}
}

private fun showLoading(show: Boolean) {
binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
if (!show) {
updateButtonsState()
}
}

private fun updateButtonsState() {
val isEnabled = currentConfigId != null
binding.slideshowButton.isEnabled = isEnabled
// Sort button enabled if folder selected AND (has destinations OR deletion allowed)
if (isEnabled) {
lifecycleScope.launch {
val hasDestinations = viewModel.getSortDestinationsCount() > 0
val deletionAllowed = preferenceManager.isAllowDelete()
binding.sortButton.isEnabled = hasDestinations || deletionAllowed
}
} else {
binding.sortButton.isEnabled = false
}
}
}
