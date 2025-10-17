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
        binding.nameInput.setText(config.name)
        binding.serverInput.setText(config.serverAddress)
        binding.usernameInput.setText(config.username)
        binding.passwordInput.setText(config.password)
        binding.folderInput.setText(config.folderPath)
        binding.intervalInput.setText(config.interval.toString())
    }
    
    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveConnection()
        }
        
        binding.connectButton.setOnClickListener {
            val server = binding.serverInput.text.toString().trim()
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val folder = binding.folderInput.text.toString().trim()
            val interval = binding.intervalInput.text.toString().toIntOrNull() ?: 10
            
            if (server.isEmpty() || folder.isEmpty()) {
                Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            saveSettings(server, username, password, folder, interval)
            currentConfigId?.let { viewModel.updateLastUsed(it) }
            startSlideshow()
        }
    }
    
    private fun saveConnection() {
        val name = binding.nameInput.text.toString().trim()
        val server = binding.serverInput.text.toString().trim()
        val username = binding.usernameInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val folder = binding.folderInput.text.toString().trim()
        val interval = binding.intervalInput.text.toString().toIntOrNull() ?: 10
        
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.enter_connection_name, Toast.LENGTH_SHORT).show()
            return
        }
        
        if (server.isEmpty() || folder.isEmpty()) {
            Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        val config = ConnectionConfig(
            id = currentConfigId ?: 0,
            name = name,
            serverAddress = server,
            username = username,
            password = password,
            folderPath = folder,
            interval = interval,
            lastUsed = System.currentTimeMillis()
        )
        
        if (currentConfigId != null && currentConfigId != 0L) {
            viewModel.updateConfig(config)
        } else {
            viewModel.insertConfig(config)
        }
        
        Toast.makeText(this, R.string.connection_saved, Toast.LENGTH_SHORT).show()
        currentConfigId = null
        clearInputs()
    }
    
    private fun clearInputs() {
        binding.nameInput.setText("")
        binding.serverInput.setText("")
        binding.usernameInput.setText("")
        binding.passwordInput.setText("")
        binding.folderInput.setText("")
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