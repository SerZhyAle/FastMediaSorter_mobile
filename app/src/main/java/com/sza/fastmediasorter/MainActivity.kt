package com.sza.fastmediasorter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sza.fastmediasorter.data.ConnectionConfig
import com.sza.fastmediasorter.databinding.ActivityMainBinding
import com.sza.fastmediasorter.ui.ConnectionAdapter
import com.sza.fastmediasorter.ui.ConnectionViewModel
import com.sza.fastmediasorter.ui.slideshow.SlideshowActivity
import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private val viewModel: ConnectionViewModel by viewModels()
    private lateinit var adapter: ConnectionAdapter
    private var currentConfigId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide action bar
        supportActionBar?.hide()
        
        preferenceManager = PreferenceManager(this)
        
        // Always setup UI first
        setupRecyclerView()
        setupClickListeners()
        observeConnections()
        
        // Try to auto-resume last session
        tryAutoResumeSession()
        
        // Load last used config if not auto-resuming
        loadLastUsedConfig()
    }
    
    private fun tryAutoResumeSession() {
        val lastFolderAddress = preferenceManager.getLastFolderAddress()
        if (lastFolderAddress.isEmpty()) {
            return
        }
        
        lifecycleScope.launch {
            // Try to load config by folder address
            val config = viewModel.getConfigByFolderAddress(lastFolderAddress)
            if (config != null) {
                // Config exists, try to restore session
                loadConfigAndStartSlideshow(config)
            } else {
                // Config not found, clear last session
                preferenceManager.clearLastSession()
            }
        }
    }
    
    private fun loadConfigAndStartSlideshow(config: ConnectionConfig) {
        // Set connection settings
        preferenceManager.saveConnectionSettings(
            config.serverAddress,
            config.username,
            config.password,
            config.folderPath
        )
        preferenceManager.setInterval(config.interval)
        
        // Update last used timestamp
        viewModel.updateLastUsed(config.id)
        
        // Start slideshow activity
        val intent = Intent(this, SlideshowActivity::class.java)
        startActivity(intent)
    }
    
    private fun setupRecyclerView() {
        adapter = ConnectionAdapter(
            onItemClick = { config ->
                loadConfig(config)
            },
            onItemDoubleClick = { config ->
                loadConfigAndStartSlideshow(config)
            },
            onDeleteClick = { config ->
                viewModel.deleteConfig(config)
                Toast.makeText(this, R.string.connection_deleted, Toast.LENGTH_SHORT).show()
            }
        )
        
        binding.connectionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            isNestedScrollingEnabled = true
        }
    }
    
    private fun observeConnections() {
        viewModel.allConfigs.observe(this) { configs ->
            adapter.submitList(configs)
        }
    }
    
    private fun loadLastUsedConfig() {
        lifecycleScope.launch {
            val config = viewModel.getLastUsedConfig()
            config?.let { loadConfig(it) }
        }
    }
    
    private fun loadConfig(config: ConnectionConfig) {
        currentConfigId = config.id
        val folderAddress = "${config.serverAddress}\\${config.folderPath}"
        binding.folderAddressInput.setText(folderAddress)
        binding.usernameInput.setText(config.username)
        binding.passwordInput.setText(config.password)
        binding.nameInput.setText(config.name)
        binding.intervalInput.setText(config.interval.toString())
    }
    
    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveConnection()
        }
        
        binding.slideshowButton.setOnClickListener {
            val folderAddress = binding.folderAddressInput.text.toString().trim()
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val interval = binding.intervalInput.text.toString().toIntOrNull() ?: 10
            
            if (folderAddress.isEmpty()) {
                Toast.makeText(this, "Please enter folder address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val (server, folder) = parseFolderAddress(folderAddress)
            
            saveSettings(server, username, password, folder, interval)
            currentConfigId?.let { viewModel.updateLastUsed(it) }
            startSlideshow()
        }
        
        binding.sortButton.setOnClickListener {
            currentConfigId?.let { configId ->
                // Check if sort destinations are configured
                lifecycleScope.launch {
                    val destinations = viewModel.getSortDestinationsCount()
                    if (destinations == 0) {
                        Toast.makeText(
                            this@MainActivity,
                            "Set destinations first",
                            Toast.LENGTH_LONG
                        ).show()
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
    
    private fun parseFolderAddress(folderAddress: String): Pair<String, String> {
        // Parse "192.168.1.100\photos" or "//server/share/folder"
        val normalized = folderAddress.replace("/", "\\").trimStart('\\')
        val parts = normalized.split("\\", limit = 2)
        val server = parts[0]
        val folder = if (parts.size > 1) parts[1] else ""
        return Pair(server, folder)
    }
    
    private fun saveConnection() {
        val folderAddress = binding.folderAddressInput.text.toString().trim()
        val username = binding.usernameInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        var name = binding.nameInput.text.toString().trim()
        val interval = binding.intervalInput.text.toString().toIntOrNull() ?: 10
        
        if (folderAddress.isEmpty()) {
            Toast.makeText(this, "Please enter folder address", Toast.LENGTH_SHORT).show()
            return
        }
        
        // If name is empty, use folder address as name
        if (name.isEmpty()) {
            name = folderAddress
        }
        
        val (server, folder) = parseFolderAddress(folderAddress)
        
        lifecycleScope.launch {
            val existingConfig = viewModel.getConfigByFolderAddress(server, folder)
            
            if (existingConfig != null) {
                // Update existing connection with same folder address
                val config = existingConfig.copy(
                    name = name,
                    username = username,
                    password = password,
                    interval = interval,
                    lastUsed = System.currentTimeMillis()
                )
                viewModel.updateConfig(config)
                Toast.makeText(this@MainActivity, "Connection updated", Toast.LENGTH_SHORT).show()
            } else {
                // Create new connection
                val config = ConnectionConfig(
                    id = 0,
                    name = name,
                    serverAddress = server,
                    username = username,
                    password = password,
                    folderPath = folder,
                    interval = interval,
                    lastUsed = System.currentTimeMillis()
                )
                viewModel.insertConfig(config)
                Toast.makeText(this@MainActivity, R.string.connection_saved, Toast.LENGTH_SHORT).show()
            }
            
            currentConfigId = null
            clearInputs()
        }
    }
    
    private fun clearInputs() {
        binding.folderAddressInput.setText("")
        binding.usernameInput.setText("")
        binding.passwordInput.setText("")
        binding.nameInput.setText("")
        binding.intervalInput.setText("10")
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
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.slideshowButton.isEnabled = !show
    }
}