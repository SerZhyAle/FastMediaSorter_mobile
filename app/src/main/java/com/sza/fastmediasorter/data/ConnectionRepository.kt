package com.sza.fastmediasorter.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ConnectionRepository(private val dao: ConnectionConfigDao) {
    
    val allConfigs: Flow<List<ConnectionConfig>> = dao.getAllConfigs()
    val sortDestinations: Flow<List<ConnectionConfig>> = dao.getSortDestinations()
    
    suspend fun getConfigById(id: Long): ConnectionConfig? {
        return dao.getConfigById(id)
    }
    
    suspend fun getConfigByName(name: String): ConnectionConfig? {
        return dao.getConfigByName(name)
    }
    
    suspend fun getConfigByFolderAddress(server: String, folder: String): ConnectionConfig? {
        return dao.getConfigByFolderAddress(server, folder)
    }
    
    suspend fun getLastUsedConfig(): ConnectionConfig? {
        return dao.getLastUsedConfig()
    }
    
    suspend fun insertConfig(config: ConnectionConfig): Long {
        return dao.insertConfig(config)
    }
    
    suspend fun updateConfig(config: ConnectionConfig) {
        dao.updateConfig(config)
    }
    
    suspend fun deleteConfig(config: ConnectionConfig) {
        dao.deleteConfig(config)
    }
    
    suspend fun deleteConfigById(id: Long) {
        dao.deleteConfigById(id)
    }
    
    suspend fun updateLastUsed(id: Long) {
        dao.updateLastUsed(id, System.currentTimeMillis())
    }
    
    // Sort destinations methods
    suspend fun addSortDestination(configId: Long, sortName: String) {
        val maxOrder = dao.getMaxSortOrder() ?: -1
        val newOrder = maxOrder + 1
        if (newOrder <= 9) {
            dao.updateSortDestination(configId, newOrder, sortName)
        }
    }
    
    suspend fun removeSortDestination(configId: Long) {
        val config = dao.getConfigById(configId)
        config?.sortOrder?.let { removedOrder ->
            dao.removeSortDestination(configId)
            
            // Reorder remaining destinations
            val destinations = dao.getSortDestinations().first()
            destinations.filter { it.sortOrder!! > removedOrder }.forEach { dest ->
                dao.updateSortDestination(dest.id, dest.sortOrder!! - 1, dest.sortName)
            }
        }
    }
    
    suspend fun moveSortDestination(config: ConnectionConfig, direction: Int) {
        val currentOrder = config.sortOrder ?: return
        val newOrder = currentOrder + direction
        
        if (newOrder < 0) return
        
        val destinations = dao.getSortDestinations().first()
        if (newOrder >= destinations.size) return
        
        // Swap with target position
        val targetConfig = destinations.find { it.sortOrder == newOrder }
        if (targetConfig != null) {
            dao.updateSortDestination(config.id, newOrder, config.sortName)
            dao.updateSortDestination(targetConfig.id, currentOrder, targetConfig.sortName)
        }
    }
    
    suspend fun getSortDestinationsCount(): Int {
        return dao.getSortDestinations().first().size
    }
}
