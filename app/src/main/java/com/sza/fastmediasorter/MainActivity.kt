package com.sza.fastmediasorter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.sza.fastmediasorter.data.ConnectionConfig
import com.sza.fastmediasorter.databinding.ActivityMainBinding
import com.sza.fastmediasorter.ui.ConnectionViewModel
import com.sza.fastmediasorter.ui.MainPagerAdapter
import com.sza.fastmediasorter.ui.network.NetworkFragment
import com.sza.fastmediasorter.ui.slideshow.SlideshowActivity
import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
private lateinit var binding: ActivityMainBinding
private lateinit var preferenceManager: PreferenceManager
private val viewModel: ConnectionViewModel by viewModels()
private var currentConfigId: Long? = null

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
binding = ActivityMainBinding.inflate(layoutInflater)
setContentView(binding.root)
supportActionBar?.hide()
preferenceManager = PreferenceManager(this)
setupViewPager()
setupClickListeners()
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
        binding.viewPager.post {
            val networkFragment = supportFragmentManager.findFragmentByTag("f1") as? NetworkFragment
            networkFragment?.let { setupNetworkFragmentCallbacks(it) }
            val localFragment = supportFragmentManager.findFragmentByTag("f0") as? com.sza.fastmediasorter.ui.local.LocalFoldersFragment
            localFragment?.let { setupLocalFragmentCallbacks(it) }
        }
    }
    
    private fun getIntervalFromInput(): Int? {
        return binding.intervalInput.text.toString().toIntOrNull()?.takeIf { it in 1..300 }
    }
    
    private suspend fun updateConfigInterval(configId: Long): ConnectionConfig? {
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
val interval = binding.intervalInput.text.toString().toIntOrNull() ?: config.interval
binding.intervalInput.setText(interval.toString())
}
fragment.onConfigDoubleClick = { config ->
loadConfigAndStartSlideshow(config)
}
}

private fun setupLocalFragmentCallbacks(fragment: com.sza.fastmediasorter.ui.local.LocalFoldersFragment) {
fragment.onFolderSelected = { folder ->
lifecycleScope.launch {
val config = if (folder.isCustom) {
viewModel.localCustomFolders.value?.find { it.localDisplayName == folder.name }
} else {
viewModel.localCustomFolders.value?.find { it.localDisplayName == folder.name }
?: createTempLocalConfig(folder.name, "", folder.name)
}
config?.let {
currentConfigId = it.id
binding.intervalInput.setText(it.interval.toString())
}
}
}
fragment.onFolderDoubleClick = { folder ->
lifecycleScope.launch {
val config = if (folder.isCustom) {
viewModel.localCustomFolders.value?.find { it.localDisplayName == folder.name }
} else {
viewModel.localCustomFolders.value?.find { it.localDisplayName == folder.name }
?: createTempLocalConfig(folder.name, "", folder.name)
}
config?.let {
val interval = binding.intervalInput.text.toString().toIntOrNull() ?: it.interval
val updatedConfig = it.copy(interval = interval)
if (it.id > 0) viewModel.updateConfig(updatedConfig)
loadConfigAndStartSlideshow(updatedConfig)
}
}
}
}

private fun createTempLocalConfig(name: String, uri: String, bucketName: String): com.sza.fastmediasorter.data.ConnectionConfig {
return com.sza.fastmediasorter.data.ConnectionConfig(
id = 0,
name = name,
serverAddress = "",
username = "",
password = "",
folderPath = "",
interval = binding.intervalInput.text.toString().toIntOrNull() ?: 10,
lastUsed = System.currentTimeMillis(),
type = "LOCAL_CUSTOM",
localUri = uri,
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

    private fun loadConfigAndStartSlideshow(config: ConnectionConfig) {
        if (config.type == "LOCAL_CUSTOM") {
            preferenceManager.saveLocalFolderSettings(
                config.localUri ?: "",
                config.localDisplayName ?: "",
                config.interval
            )
        } else {
            preferenceManager.saveConnectionSettings(
                config.serverAddress,
                config.username,
                config.password,
                config.folderPath
            )
            preferenceManager.setInterval(config.interval)
        }
        viewModel.updateLastUsed(config.id)
        val intent = Intent(this, SlideshowActivity::class.java)
        startActivity(intent)
    }private fun setupClickListeners() {
binding.slideshowButton.setOnClickListener {
currentConfigId?.let { configId ->
lifecycleScope.launch {
val config = updateConfigInterval(configId)
if (config != null) {
loadConfigAndStartSlideshow(config)
} else {
Toast.makeText(this@MainActivity, "Please select a connection first", Toast.LENGTH_SHORT).show()
}
}
} ?: run {
Toast.makeText(this, "Please select a connection first", Toast.LENGTH_SHORT).show()
}
}
binding.sortButton.setOnClickListener {
currentConfigId?.let { configId ->
lifecycleScope.launch {
val destinations = viewModel.getSortDestinationsCount()
if (destinations == 0) {
Toast.makeText(this@MainActivity, "Set destinations first", Toast.LENGTH_LONG).show()
} else {
updateConfigInterval(configId)
val intent = Intent(this@MainActivity, com.sza.fastmediasorter.ui.sort.SortActivity::class.java)
intent.putExtra("configId", configId)
startActivity(intent)
}
}
} ?: run {
Toast.makeText(this, "Please select a connection first", Toast.LENGTH_SHORT).show()
}
}
binding.settingsButton.setOnClickListener {
val intent = Intent(this, com.sza.fastmediasorter.ui.settings.SettingsActivity::class.java)
startActivity(intent)
}
}

private fun showLoading(show: Boolean) {
binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
binding.slideshowButton.isEnabled = !show
}
}