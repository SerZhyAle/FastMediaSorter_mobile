package com.sza.fastmediasorter.data

import kotlinx.coroutines.flow.Flow

class ConnectionRepository(private val dao: ConnectionConfigDao) {
    
    val allConfigs: Flow<List<ConnectionConfig>> = dao.getAllConfigs()
    
    suspend fun getConfigById(id: Long): ConnectionConfig? {
        return dao.getConfigById(id)
    }
    
    suspend fun getConfigByName(name: String): ConnectionConfig? {
        return dao.getConfigByName(name)
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
}
