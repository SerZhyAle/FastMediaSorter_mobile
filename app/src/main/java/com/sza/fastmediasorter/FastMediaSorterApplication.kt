package com.sza.fastmediasorter

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class FastMediaSorterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Register BouncyCastle provider for jCIFS-ng MD4 algorithm support
        try {
            Security.removeProvider("BC")
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            android.util.Log.d("Application", "BouncyCastle provider registered successfully")
        } catch (e: Exception) {
            android.util.Log.e("Application", "Failed to register BouncyCastle provider", e)
        }
    }
}
