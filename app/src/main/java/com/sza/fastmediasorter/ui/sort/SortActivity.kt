package com.sza.fastmediasorter.ui.sort

import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.sza.fastmediasorter.data.ConnectionConfig
import com.sza.fastmediasorter.databinding.ActivitySortBinding
import com.sza.fastmediasorter.network.LocalStorageClient
import com.sza.fastmediasorter.network.SmbClient
import com.sza.fastmediasorter.ui.ConnectionViewModel
import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SortActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySortBinding
    private lateinit var viewModel: ConnectionViewModel
    private lateinit var smbClient: SmbClient
    private lateinit var preferenceManager: PreferenceManager
    private var localStorageClient: LocalStorageClient? = null
    
    private var currentConfig: ConnectionConfig? = null
    private var imageFiles = listOf<String>()
    private var currentIndex = 0
    private var sortDestinations = listOf<ConnectionConfig>()
    private var isLocalMode = false
    
    // Preloading optimization
    private var nextImageData: ByteArray? = null
    private var nextImageIndex: Int = -1
    private var preloadJob: Job? = null
    
    // For handling delete permission requests on Android 11+
    private var pendingDeleteUri: Uri? = null
    private var isDeleteFromMove: Boolean = false  // Track if delete is from Move operation
    private lateinit var deletePermissionLauncher: ActivityResultLauncher<IntentSenderRequest>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySortBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.hide()
        
        // Register delete permission launcher for Android 11+
        deletePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                android.util.Log.d("SortActivity", "User granted delete permission")
                pendingDeleteUri?.let { uri ->
                    lifecycleScope.launch {
                        handleDeleteSuccess(uri)
                    }
                }
            } else {
                android.util.Log.w("SortActivity", "User denied delete permission")
                android.widget.Toast.makeText(
                    this,
                    "Delete permission denied",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            pendingDeleteUri = null
        }
        
        viewModel = ViewModelProvider(this)[ConnectionViewModel::class.java]
        preferenceManager = PreferenceManager(this)
        
        val configId = intent.getLongExtra("configId", -1)
        if (configId == -1L) {
            finish()
            return
        }
        
        setupTouchAreas()
        setupBackButton()
        setupObservers()
        
        lifecycleScope.launch {
            loadConnection(configId)
        }
    }
    
    private fun setupObservers() {
        // Observe sort destinations
        viewModel.sortDestinations.observe(this) { destinations ->
            sortDestinations = destinations
            setupSortButtons()
        }
    }
    
    private fun setupTouchAreas() {
        binding.previousArea.setOnClickListener {
            if (imageFiles.isEmpty()) return@setOnClickListener
            
            currentIndex = if (currentIndex > 0) {
                currentIndex - 1
            } else {
                imageFiles.size - 1  // Jump to last
            }
            loadImage()
        }
        
        binding.nextArea.setOnClickListener {
            if (imageFiles.isEmpty()) return@setOnClickListener
            
            currentIndex = if (currentIndex < imageFiles.size - 1) {
                currentIndex + 1
            } else {
                0  // Jump to first
            }
            loadImage()
        }
    }
    
    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    

    
    private fun setupSortButtons() {
        val copyButtons = listOf(
            binding.sortButton0, binding.sortButton1, binding.sortButton2,
            binding.sortButton3, binding.sortButton4, binding.sortButton5,
            binding.sortButton6, binding.sortButton7, binding.sortButton8,
            binding.sortButton9
        )
        
        val moveButtons = listOf(
            binding.moveButton0, binding.moveButton1, binding.moveButton2,
            binding.moveButton3, binding.moveButton4, binding.moveButton5,
            binding.moveButton6, binding.moveButton7, binding.moveButton8,
            binding.moveButton9
        )
        
        // Check if move is allowed
        val allowMove = preferenceManager.isAllowMove()
        
        // Check if copy is allowed
        val allowCopy = preferenceManager.isAllowCopy()
        
        // Check if delete is allowed
        val allowDelete = preferenceManager.isAllowDelete()
        
        // Check if we have destinations
        val hasDestinations = sortDestinations.isNotEmpty()
        
        // Show warning if no destinations
        binding.noDestinationsWarning.visibility = if (!hasDestinations && allowDelete) View.VISIBLE else View.GONE
        
        // Hide/show copy section (only if we have destinations)
        val showCopy = allowCopy && hasDestinations
        binding.copyToLabel.visibility = if (showCopy) View.VISIBLE else View.GONE
        binding.buttonsRow1.visibility = if (showCopy) View.VISIBLE else View.GONE
        binding.buttonsRow2.visibility = if (showCopy) View.VISIBLE else View.GONE
        
        // Hide/show move section (only if we have destinations)
        val showMove = allowMove && hasDestinations
        binding.moveToLabel.visibility = if (showMove) View.VISIBLE else View.GONE
        binding.moveButtonsRow1.visibility = if (showMove) View.VISIBLE else View.GONE
        binding.moveButtonsRow2.visibility = if (showMove) View.VISIBLE else View.GONE
        
        // Hide/show delete button
        binding.deleteButton.visibility = if (allowDelete) View.VISIBLE else View.GONE
        
        // Setup delete button click listener
        binding.deleteButton.setOnClickListener {
            deleteCurrentImage()
        }
        
        // Color shades for buttons
        val colors = listOf(
            android.graphics.Color.parseColor("#5C6BC0"), // Indigo
            android.graphics.Color.parseColor("#42A5F5"), // Blue
            android.graphics.Color.parseColor("#26C6DA"), // Cyan
            android.graphics.Color.parseColor("#66BB6A"), // Green
            android.graphics.Color.parseColor("#9CCC65"), // Light Green
            android.graphics.Color.parseColor("#FFCA28"), // Amber
            android.graphics.Color.parseColor("#FFA726"), // Orange
            android.graphics.Color.parseColor("#EF5350"), // Red
            android.graphics.Color.parseColor("#AB47BC"), // Purple
            android.graphics.Color.parseColor("#EC407A")  // Pink
        )
        
        // Hide all buttons first
        copyButtons.forEach { it.visibility = View.GONE }
        moveButtons.forEach { it.visibility = View.GONE }
        
        // Show and configure buttons for active destinations
        sortDestinations.forEachIndexed { index, config ->
            if (index < copyButtons.size) {
                // Skip local folders (defensive check)
                if (config.type == "LOCAL_CUSTOM") return@forEachIndexed
                
                val copyButton = copyButtons[index]
                copyButton.text = config.sortName
                copyButton.setBackgroundColor(colors[index])
                copyButton.setTextColor(android.graphics.Color.BLACK)
                copyButton.visibility = View.VISIBLE
                copyButton.setOnClickListener {
                    copyToDestination(config)
                }
                
                val moveButton = moveButtons[index]
                moveButton.text = config.sortName
                moveButton.setBackgroundColor(colors[index])
                moveButton.setTextColor(android.graphics.Color.BLACK)
                moveButton.visibility = View.VISIBLE
                moveButton.setOnClickListener {
                    moveToDestination(config)
                }
            }
        }
    }
    
    private fun copyToDestination(destination: ConnectionConfig) {
        if (imageFiles.isEmpty()) return
        
        // Validate destination type
        if (destination.type == "LOCAL_CUSTOM") {
            android.widget.Toast.makeText(
                this,
                "Cannot copy to local folders. Use SMB destinations only.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        
        val currentImageUrl = imageFiles[currentIndex]
        
        lifecycleScope.launch(Dispatchers.IO) {
            // Show progress
            withContext(Dispatchers.Main) {
                binding.progressText.text = "Copying..."
                binding.copyProgressLayout.visibility = View.VISIBLE
            }
            
            try {
                val result = if (isLocalMode && destination.type == "SMB") {
                    copyFromLocalToSmb(currentImageUrl, destination)
                } else if (!isLocalMode && destination.type == "SMB") {
                    smbClient.copyFile(
                        currentImageUrl,
                        destination.serverAddress,
                        destination.folderPath
                    )
                } else {
                    SmbClient.CopyResult.UnknownError("Unsupported operation")
                }
                
                withContext(Dispatchers.Main) {
                    // Hide progress
                    binding.copyProgressLayout.visibility = View.GONE
                    
                    val message = when (result) {
                        is SmbClient.CopyResult.Success -> "File copied"
                        is SmbClient.CopyResult.AlreadyExists -> "File already exists"
                        is SmbClient.CopyResult.SameFolder -> "Same folder"
                        is SmbClient.CopyResult.NetworkError -> "Network error: ${result.message}"
                        is SmbClient.CopyResult.SecurityError -> "Security error: ${result.message}"
                        is SmbClient.CopyResult.UnknownError -> "Error: ${result.message}"
                    }
                    
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        message,
                        if (result is SmbClient.CopyResult.Success) 
                            android.widget.Toast.LENGTH_SHORT 
                        else 
                            android.widget.Toast.LENGTH_LONG
                    ).show()
                    
                    // Jump to next image on success
                    if (result is SmbClient.CopyResult.Success) {
                        if (imageFiles.isNotEmpty()) {
                            currentIndex = if (currentIndex < imageFiles.size - 1) {
                                currentIndex + 1
                            } else {
                                0  // Jump to first
                            }
                            loadImage()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Hide progress
                    binding.copyProgressLayout.visibility = View.GONE
                    
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Unexpected error: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private suspend fun copyFromLocalToSmb(localUri: String, destination: ConnectionConfig): SmbClient.CopyResult {
        return try {
            val uri = Uri.parse(localUri)
            val imageBytes = localStorageClient?.downloadImage(uri) 
            if (imageBytes == null) {
                android.util.Log.e("SortActivity", "Failed to read local file: $localUri")
                return SmbClient.CopyResult.UnknownError("Failed to read local file")
            }
            
            val fileInfo = localStorageClient?.getFileInfo(uri)
            val fileName = fileInfo?.name ?: "unknown.jpg"
            android.util.Log.d("SortActivity", "Copying file: $fileName (${imageBytes.size} bytes)")
            
            // Connect to destination SMB if needed
            val destClient = SmbClient()
            val connected = destClient.connect(destination.serverAddress, destination.username, destination.password)
            if (!connected) {
                android.util.Log.e("SortActivity", "Failed to connect to ${destination.serverAddress}")
                return SmbClient.CopyResult.NetworkError("Failed to connect to destination")
            }
            
            android.util.Log.d("SortActivity", "Connected to ${destination.serverAddress}, writing to ${destination.folderPath}")
            
            // Write file to SMB and return result
            val result = destClient.writeFile(destination.serverAddress, destination.folderPath, fileName, imageBytes)
            
            when (result) {
                is SmbClient.CopyResult.Success -> 
                    android.util.Log.d("SortActivity", "File copied successfully: $fileName")
                is SmbClient.CopyResult.AlreadyExists -> 
                    android.util.Log.w("SortActivity", "File already exists: $fileName")
                is SmbClient.CopyResult.NetworkError -> 
                    android.util.Log.e("SortActivity", "Network error: ${result.message}")
                is SmbClient.CopyResult.SecurityError -> 
                    android.util.Log.e("SortActivity", "Security error: ${result.message}")
                else -> 
                    android.util.Log.e("SortActivity", "Unknown error during copy")
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("SortActivity", "Exception during copy: ${e.message}", e)
            e.printStackTrace()
            SmbClient.CopyResult.UnknownError(e.message ?: "Unknown error")
        }
    }
    
    private suspend fun moveFromLocalToSmb(localUri: String, destination: ConnectionConfig): SmbClient.MoveResult {
        try {
            android.util.Log.d("SortActivity", "Moving from local to SMB: $localUri")
            val copyResult = copyFromLocalToSmb(localUri, destination)
            
            if (copyResult is SmbClient.CopyResult.Success) {
                val uri = Uri.parse(localUri)
                
                // Try to delete with proper permission handling for Android 11+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    deleteImageWithPermission(uri, fromMove = true)
                    // For Android 11+, return pending status
                    // Actual success will be handled in the callback
                    return SmbClient.MoveResult.PendingUserConfirmation
                } else {
                    val deleted = localStorageClient?.deleteImage(uri) ?: false
                    return if (deleted) {
                        android.util.Log.d("SortActivity", "File deleted successfully after copy")
                        SmbClient.MoveResult.Success
                    } else {
                        android.util.Log.w("SortActivity", "Failed to delete local file after successful copy")
                        SmbClient.MoveResult.DeleteError("Failed to delete local file after copy")
                    }
                }
            } else {
                val errorMsg = when (copyResult) {
                    is SmbClient.CopyResult.AlreadyExists -> "File already exists"
                    is SmbClient.CopyResult.NetworkError -> "Network: ${copyResult.message}"
                    is SmbClient.CopyResult.SecurityError -> "Security: ${copyResult.message}"
                    is SmbClient.CopyResult.UnknownError -> "Copy failed: ${copyResult.message}"
                    else -> "Copy failed"
                }
                android.util.Log.e("SortActivity", "Copy failed: $errorMsg")
                return SmbClient.MoveResult.UnknownError(errorMsg)
            }
        } catch (e: Exception) {
            android.util.Log.e("SortActivity", "Exception during move: ${e.message}", e)
            e.printStackTrace()
            return SmbClient.MoveResult.UnknownError(e.message ?: "Unknown error")
        }
    }
    
    private suspend fun deleteImageWithPermission(uri: Uri, fromMove: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                android.util.Log.d("SortActivity", "Requesting delete permission for Android 11+")
                
                val intentSender = MediaStore.createDeleteRequest(
                    contentResolver,
                    listOf(uri)
                ).intentSender
                
                pendingDeleteUri = uri
                isDeleteFromMove = fromMove
                val request = IntentSenderRequest.Builder(intentSender).build()
                
                withContext(Dispatchers.Main) {
                    deletePermissionLauncher.launch(request)
                }
            } catch (e: Exception) {
                android.util.Log.e("SortActivity", "Error requesting delete permission", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Cannot request delete permission: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private suspend fun handleDeleteSuccess(uri: Uri) {
        android.util.Log.d("SortActivity", "File deleted successfully: $uri")
        
        withContext(Dispatchers.Main) {
            // Show success message based on context
            val message = if (isDeleteFromMove) "File moved" else "File deleted"
            android.widget.Toast.makeText(
                this@SortActivity,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            // Reset flag
            isDeleteFromMove = false
            
            // Remove from list and load next
            if (imageFiles.isNotEmpty()) {
                imageFiles = imageFiles.toMutableList().apply {
                    val index = indexOfFirst { it == uri.toString() }
                    if (index != -1) {
                        removeAt(index)
                        if (currentIndex >= size) {
                            currentIndex = size - 1
                        }
                    }
                }
                
                if (imageFiles.isEmpty()) {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "No more images",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    loadImage()
                }
            }
        }
    }
    
    private fun moveToDestination(destination: ConnectionConfig) {
        if (imageFiles.isEmpty()) return
        
        // Validate destination type
        if (destination.type == "LOCAL_CUSTOM") {
            android.widget.Toast.makeText(
                this,
                "Cannot move to local folders. Use SMB destinations only.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        
        val currentImageUrl = imageFiles[currentIndex]
        
        lifecycleScope.launch(Dispatchers.IO) {
            // Show progress
            withContext(Dispatchers.Main) {
                binding.progressText.text = "Moving..."
                binding.copyProgressLayout.visibility = View.VISIBLE
            }
            
            try {
                val result = if (isLocalMode && destination.type == "SMB") {
                    moveFromLocalToSmb(currentImageUrl, destination)
                } else if (!isLocalMode && destination.type == "SMB") {
                    smbClient.moveFile(
                        currentImageUrl,
                        destination.serverAddress,
                        destination.folderPath
                    )
                } else {
                    SmbClient.MoveResult.UnknownError("Unsupported operation")
                }
                
                withContext(Dispatchers.Main) {
                    // Hide progress
                    binding.copyProgressLayout.visibility = View.GONE
                    
                    val message = when (result) {
                        is SmbClient.MoveResult.Success -> "File moved"
                        is SmbClient.MoveResult.PendingUserConfirmation -> "Confirm file deletion..."
                        is SmbClient.MoveResult.AlreadyExists -> "File already exists"
                        is SmbClient.MoveResult.SameFolder -> "Same folder"
                        is SmbClient.MoveResult.NetworkError -> "Network error: ${result.message}"
                        is SmbClient.MoveResult.SecurityError -> "Security error: ${result.message}"
                        is SmbClient.MoveResult.DeleteError -> "Delete error: ${result.message}"
                        is SmbClient.MoveResult.UnknownError -> "Error: ${result.message}"
                    }
                    
                    // Show toast only for non-pending results
                    if (result !is SmbClient.MoveResult.PendingUserConfirmation) {
                        android.widget.Toast.makeText(
                            this@SortActivity,
                            message,
                            if (result is SmbClient.MoveResult.Success) 
                                android.widget.Toast.LENGTH_SHORT 
                            else 
                                android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    
                    // Remove from list and load next on success
                    if (result is SmbClient.MoveResult.Success) {
                        if (imageFiles.isNotEmpty()) {
                            // Remove current file from list
                            imageFiles = imageFiles.toMutableList().apply {
                                removeAt(currentIndex)
                            }
                            
                            // Check if list is now empty
                            if (imageFiles.isEmpty()) {
                                android.widget.Toast.makeText(
                                    this@SortActivity,
                                    "No more images",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@withContext
                            }
                            
                            // Adjust index if we removed the last item
                            if (currentIndex >= imageFiles.size) {
                                currentIndex = imageFiles.size - 1
                            }
                            
                            // Load image at current index (which is now the next file)
                            loadImage()
                        }
                    }
                    // For PendingUserConfirmation, the dialog will be shown automatically
                    // Actual deletion and list update happens in handleDeleteSuccess callback
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Hide progress
                    binding.copyProgressLayout.visibility = View.GONE
                    
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Unexpected error: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private suspend fun loadConnection(configId: Long) {
        // Handle standard local folders (negative IDs)
        if (configId < 0) {
            val bucketName = when (configId) {
                -1L -> "Camera"
                -2L -> "Screenshots"
                -3L -> "Pictures"
                -4L -> "Download"
                else -> {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            this@SortActivity,
                            "Invalid folder ID",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return
                }
            }
            
            currentConfig = com.sza.fastmediasorter.data.ConnectionConfig(
                id = configId,
                name = bucketName,
                serverAddress = "",
                username = "",
                password = "",
                folderPath = "",
                interval = 10,
                lastUsed = System.currentTimeMillis(),
                type = "LOCAL_STANDARD",
                localUri = "",
                localDisplayName = bucketName
            )
            
            isLocalMode = true
            localStorageClient = LocalStorageClient(this@SortActivity)
            smbClient = SmbClient() // Still need for destinations
            
            withContext(Dispatchers.Main) {
                binding.connectionNameText.text = "Local: $bucketName"
                android.widget.Toast.makeText(
                    this@SortActivity,
                    "Loading local images...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            loadImages()
            return
        }
        
        // Get config from database directly (for custom folders and SMB)
        val database = com.sza.fastmediasorter.data.AppDatabase.getDatabase(applicationContext)
        val config = withContext(Dispatchers.IO) {
            database.connectionConfigDao().getConfigById(configId)
        }
        
        if (config == null) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    this@SortActivity,
                    "Connection not found",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                finish()
            }
            return
        }
        
        currentConfig = config
        isLocalMode = config.type == "LOCAL_CUSTOM" || config.type == "LOCAL_STANDARD"
        
        if (isLocalMode) {
            localStorageClient = LocalStorageClient(this@SortActivity)
            smbClient = SmbClient() // Still need for destinations
            
            withContext(Dispatchers.Main) {
                binding.connectionNameText.text = "Local: ${config.localDisplayName}"
                android.widget.Toast.makeText(
                    this@SortActivity,
                    "Loading local images...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            loadImages()
        } else {
            smbClient = SmbClient()
            
            withContext(Dispatchers.Main) {
                binding.connectionNameText.text = "${config.name} - ${config.serverAddress}\\${config.folderPath}"
            }
            
            // Use default credentials if not set
            var username = config.username
            var password = config.password
            if (username.isEmpty()) {
                username = preferenceManager.getDefaultUsername()
            }
            if (password.isEmpty()) {
                password = preferenceManager.getDefaultPassword()
            }
            
            // Connect to SMB
            val connected = withContext(Dispatchers.IO) {
                smbClient.connect(config.serverAddress, username, password)
            }
            
            if (!connected) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Failed to connect to ${config.serverAddress}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                return
            }
            
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    this@SortActivity,
                    "Connected. Loading images...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            loadImages()
        }
    }
    
    private fun loadImages() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                currentConfig?.let { config ->
                    val files = if (isLocalMode) {
                        val localUri = if (!config.localUri.isNullOrEmpty()) Uri.parse(config.localUri) else null
                        val bucketName = config.localDisplayName?.ifEmpty { null }
                        val imageInfoList = localStorageClient?.getImageFiles(localUri, bucketName) ?: emptyList()
                        imageInfoList.map { it.uri.toString() }
                    } else {
                        val result = smbClient.getImageFiles(config.serverAddress, config.folderPath)
                        if (result.errorMessage != null) {
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(this@SortActivity, result.errorMessage, android.widget.Toast.LENGTH_LONG).show()
                                finish()
                            }
                            return@launch
                        }
                        result.files
                    }
                    imageFiles = files.sorted() // ABC order
                    
                    withContext(Dispatchers.Main) {
                        if (imageFiles.isNotEmpty()) {
                            currentIndex = 0
                            loadImage()
                        } else {
                            android.widget.Toast.makeText(
                                this@SortActivity,
                                "No images found in folder",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Error loading images: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun loadImage() {
        if (imageFiles.isEmpty()) return
        
        // Cancel any ongoing preload
        preloadJob?.cancel()
        
        val imageUrl = imageFiles[currentIndex]
        updateImageCounter()
        
        // Check if we have preloaded this image
        if (nextImageIndex == currentIndex && nextImageData != null) {
            // Use preloaded data
            val imageBytes = nextImageData
            nextImageData = null
            nextImageIndex = -1
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val fileInfo = smbClient.getFileInfo(imageUrl)
                    
                    withContext(Dispatchers.Main) {
                        Glide.with(this@SortActivity)
                            .load(imageBytes)
                            .into(binding.imageView)
                        
                        updateFileInfo(fileInfo)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Start preloading next image
                preloadNextImage()
            }
        } else {
            // Load normally
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val imageBytes = if (isLocalMode) {
                        localStorageClient?.downloadImage(Uri.parse(imageUrl))
                    } else {
                        smbClient.downloadImage(imageUrl)
                    }
                    
                    val fileInfo = if (isLocalMode) {
                        localStorageClient?.getFileInfo(Uri.parse(imageUrl))?.let {
                            SmbClient.FileInfo(
                                name = it.name,
                                sizeKB = (it.size / 1024),
                                modifiedDate = it.dateModified
                            )
                        }
                    } else {
                        smbClient.getFileInfo(imageUrl)
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (imageBytes != null) {
                            Glide.with(this@SortActivity)
                                .load(imageBytes)
                                .into(binding.imageView)
                            
                            updateFileInfo(fileInfo)
                        } else {
                            android.widget.Toast.makeText(
                                this@SortActivity,
                                "Failed to load image: $imageUrl",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            this@SortActivity,
                            "Error: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                
                // Start preloading next image
                preloadNextImage()
            }
        }
    }
    
    private fun updateFileInfo(fileInfo: SmbClient.FileInfo?) {
        if (fileInfo == null) {
            binding.fileInfoText.text = "File info unavailable"
            return
        }
        
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val dateStr = dateFormat.format(java.util.Date(fileInfo.modifiedDate))
        
        binding.fileInfoText.text = "${fileInfo.name}  ${fileInfo.sizeKB} KB  $dateStr"
    }
    
    private fun updateImageCounter() {
        val total = imageFiles.size
        val current = currentIndex + 1
        binding.imageCounterText.text = "$current / $total"
    }
    
    private fun preloadNextImage() {
        if (imageFiles.isEmpty()) return
        
        val nextIndex = if (currentIndex < imageFiles.size - 1) {
            currentIndex + 1
        } else {
            0 // Loop to first
        }
        
        preloadJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nextImageUrl = imageFiles[nextIndex]
                val imageBytes = if (isLocalMode) {
                    localStorageClient?.downloadImage(Uri.parse(nextImageUrl))
                } else {
                    smbClient.downloadImage(nextImageUrl)
                }
                
                if (imageBytes != null) {
                    nextImageData = imageBytes
                    nextImageIndex = nextIndex
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Silently fail preload
            }
        }
    }
    
    private fun deleteCurrentImage() {
        if (imageFiles.isEmpty()) return
        
        val currentImageUrl = imageFiles[currentIndex]
        
        // Check if confirmation is required
        if (preferenceManager.isConfirmDelete()) {
            // Show confirmation dialog
            android.app.AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Delete") { _, _ ->
                    performDelete(currentImageUrl)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Delete without confirmation
            performDelete(currentImageUrl)
        }
    }
    
    private fun performDelete(imageUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                binding.progressText.text = "Deleting..."
                binding.copyProgressLayout.visibility = View.VISIBLE
            }
            
            try {
                if (isLocalMode) {
                    val uri = Uri.parse(imageUrl)
                    
                    // Use permission dialog for Android 11+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        withContext(Dispatchers.Main) {
                            binding.copyProgressLayout.visibility = View.GONE
                        }
                        deleteImageWithPermission(uri)
                        // Dialog will be shown, actual deletion in callback
                    } else {
                        // Direct delete for Android 10 and below
                        val deleted = localStorageClient?.deleteImage(uri) ?: false
                        handleDeleteResult(deleted, imageUrl)
                    }
                } else {
                    // SMB delete
                    val deleted = smbClient.deleteFile(imageUrl)
                    handleDeleteResult(deleted, imageUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.copyProgressLayout.visibility = View.GONE
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Error: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private suspend fun handleDeleteResult(deleted: Boolean, imageUrl: String) {
        withContext(Dispatchers.Main) {
            binding.copyProgressLayout.visibility = View.GONE
            
            if (deleted) {
                android.widget.Toast.makeText(
                    this@SortActivity,
                    "File deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // Remove from list and load next
                imageFiles = imageFiles.toMutableList().apply {
                    remove(imageUrl)
                }
                
                if (imageFiles.isEmpty()) {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "No more images",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@withContext
                }
                
                if (currentIndex >= imageFiles.size) {
                    currentIndex = imageFiles.size - 1
                }
                
                loadImage()
            } else {
                android.widget.Toast.makeText(
                    this@SortActivity,
                    "Failed to delete file",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    
    override fun onDestroy() {
        super.onDestroy()
        preloadJob?.cancel()
        try {
            smbClient.disconnect()
        } catch (e: Exception) {
            // Ignore disconnect errors on destroy
        }
    }
}
