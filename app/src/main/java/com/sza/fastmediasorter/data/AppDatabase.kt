package com.sza.fastmediasorter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ConnectionConfig::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionConfigDao(): ConnectionConfigDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE connection_configs ADD COLUMN type TEXT NOT NULL DEFAULT 'SMB'")
                database.execSQL("ALTER TABLE connection_configs ADD COLUMN localUri TEXT")
                database.execSQL("ALTER TABLE connection_configs ADD COLUMN localDisplayName TEXT")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fastmediasorter_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
