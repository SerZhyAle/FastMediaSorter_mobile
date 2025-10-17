package com.sza.fastmediasorter.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionConfigDao {
    @Query("SELECT * FROM connection_configs ORDER BY lastUsed DESC")
    fun getAllConfigs(): Flow<List<ConnectionConfig>>
    
    @Query("SELECT * FROM connection_configs WHERE id = :id")
    suspend fun getConfigById(id: Long): ConnectionConfig?
    
    @Query("SELECT * FROM connection_configs WHERE name = :name LIMIT 1")
    suspend fun getConfigByName(name: String): ConnectionConfig?
    
    @Query("SELECT * FROM connection_configs WHERE serverAddress = :server AND folderPath = :folder LIMIT 1")
    suspend fun getConfigByFolderAddress(server: String, folder: String): ConnectionConfig?
    
    @Query("SELECT * FROM connection_configs ORDER BY lastUsed DESC LIMIT 1")
    suspend fun getLastUsedConfig(): ConnectionConfig?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConnectionConfig): Long
    
    @Update
    suspend fun updateConfig(config: ConnectionConfig)
    
    @Delete
    suspend fun deleteConfig(config: ConnectionConfig)
    
    @Query("DELETE FROM connection_configs WHERE id = :id")
    suspend fun deleteConfigById(id: Long)
    
    @Query("UPDATE connection_configs SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)
}
