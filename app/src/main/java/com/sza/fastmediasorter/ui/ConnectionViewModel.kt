package com.sza.fastmediasorter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.sza.fastmediasorter.data.AppDatabase
import com.sza.fastmediasorter.data.ConnectionConfig
import com.sza.fastmediasorter.data.ConnectionRepository
import kotlinx.coroutines.launch

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ConnectionRepository
    val allConfigs: LiveData<List<ConnectionConfig>>
    
    init {
        val dao = AppDatabase.getDatabase(application).connectionConfigDao()
        repository = ConnectionRepository(dao)
        allConfigs = repository.allConfigs.asLiveData()
    }
    
    fun insertConfig(config: ConnectionConfig) = viewModelScope.launch {
        repository.insertConfig(config)
    }
    
    fun updateConfig(config: ConnectionConfig) = viewModelScope.launch {
        repository.updateConfig(config)
    }
    
    fun deleteConfig(config: ConnectionConfig) = viewModelScope.launch {
        repository.deleteConfig(config)
    }
    
    fun updateLastUsed(id: Long) = viewModelScope.launch {
        repository.updateLastUsed(id)
    }
    
    suspend fun getLastUsedConfig(): ConnectionConfig? {
        return repository.getLastUsedConfig()
    }
    
    suspend fun getConfigByName(name: String): ConnectionConfig? {
        return repository.getConfigByName(name)
    }
    
    suspend fun getConfigByFolderAddress(server: String, folder: String): ConnectionConfig? {
        return repository.getConfigByFolderAddress(server, folder)
    }
    
    suspend fun getConfigByFolderAddress(folderAddress: String): ConnectionConfig? {
        val parts = folderAddress.split("\\")
        if (parts.size < 2) return null
        val server = parts[0]
        val folder = parts.drop(1).joinToString("\\")
        return repository.getConfigByFolderAddress(server, folder)
    }
}
