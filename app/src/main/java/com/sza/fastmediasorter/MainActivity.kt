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
}
binding.intervalInput.setOnFocusChangeListener { _, hasFocus ->
if (!hasFocus) {
saveIntervalIfConfigSelected()
}
}
}

private fun saveIntervalIfConfigSelected() {
currentConfigId?.let { configId ->
val interval = binding.intervalInput.text.toString().toIntOrNull()
if (interval != null && interval in 1..300) {
lifecycleScope.launch {
val config = viewModel.getConfigById(configId)
if (config != null && config.interval != interval) {
val updatedConfig = config.copy(interval = interval)
viewModel.updateConfig(updatedConfig)
}
}
}
}
}

private fun setupNetworkFragmentCallbacks(fragment: NetworkFragment) {
fragment.onConfigSelected = { config ->
currentConfigId = config.id
val interval = binding.intervalInput.text.toString().toIntOrNull() ?: config.interval
binding.intervalInput.setText(interval.toString())
}
fragment.onConfigDoubleClick = { config ->
loadConfigAndStartSlideshow(config)
}
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
preferenceManager.saveConnectionSettings(
config.serverAddress,
config.username,
config.password,
config.folderPath
)
preferenceManager.setInterval(config.interval)
viewModel.updateLastUsed(config.id)
val intent = Intent(this, SlideshowActivity::class.java)
startActivity(intent)
}

private fun setupClickListeners() {
binding.slideshowButton.setOnClickListener {
currentConfigId?.let { configId ->
lifecycleScope.launch {
val config = viewModel.getConfigById(configId)
if (config != null) {
val interval = binding.intervalInput.text.toString().toIntOrNull() ?: config.interval
val updatedConfig = config.copy(interval = interval)
viewModel.updateConfig(updatedConfig)
loadConfigAndStartSlideshow(updatedConfig)
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

override fun onPause() {
super.onPause()
saveIntervalIfConfigSelected()
}
}