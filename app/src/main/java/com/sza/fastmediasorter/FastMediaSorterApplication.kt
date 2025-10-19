package com.sza.fastmediasorter

import android.app.Application
import android.content.res.Configuration
import com.sza.fastmediasorter.utils.Logger
import com.sza.fastmediasorter.utils.PreferenceManager
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.Locale

class FastMediaSorterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved language
        applySavedLanguage()
        
        // Register BouncyCastle provider for jCIFS-ng MD4 algorithm support
        try {
            Security.removeProvider("BC")
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            Logger.d("Application", "BouncyCastle provider registered successfully")
        } catch (e: Exception) {
            Logger.e("Application", "Failed to register BouncyCastle provider", e)
        }
    }
    
    private fun applySavedLanguage() {
        try {
            val preferenceManager = PreferenceManager(this)
            val savedLanguage = preferenceManager.getLanguage()
            
            val locale = Locale(savedLanguage)
            Locale.setDefault(locale)
            
            val config = Configuration()
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
            
            Logger.d("Application", "Applied language: $savedLanguage")
        } catch (e: Exception) {
            Logger.e("Application", "Failed to apply saved language", e)
        }
    }
}

