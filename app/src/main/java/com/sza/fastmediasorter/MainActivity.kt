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
        
        preferenceManager = PreferenceManager(this)
        
        setupRecyclerView()
        loadLastUsedConfig()
        setupClickListeners()
        observeConnections()
    }
    
    private fun setupRecyclerView() {
        adapter = ConnectionAdapter(
            onItemClick = { config ->
                loadConfig(config)
            },
            onDeleteClick = { config ->
                viewModel.deleteConfig(config)
                Toast.makeText(this, R.string.connection_deleted, Toast.LENGTH_SHORT).show()
            }
        )
        
        binding.connectionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
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
        
        binding.connectButton.setOnClickListener {
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
        binding.connectButton.isEnabled = !show
    }
}