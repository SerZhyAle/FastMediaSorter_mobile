package com.sza.fastmediasorter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_configs")
data class ConnectionConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val serverAddress: String,
    val username: String,
    val password: String,
    val folderPath: String,
    val interval: Int = 10,
    val lastUsed: Long = 0
)
