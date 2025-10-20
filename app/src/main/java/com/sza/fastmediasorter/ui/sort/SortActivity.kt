package com.sza.fastmediasorter.ui.sort

import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.data.ConnectionConfig
import com.sza.fastmediasorter.databinding.ActivitySortBinding
import com.sza.fastmediasorter.network.LocalStorageClient
import com.sza.fastmediasorter.network.SmbClient
import com.sza.fastmediasorter.utils.Logger
import com.sza.fastmediasorter.network.SmbDataSource
import com.sza.fastmediasorter.network.SmbDataSourceFactory
import com.sza.fastmediasorter.ui.ConnectionViewModel
import com.sza.fastmediasorter.ui.base.LocaleActivity
import com.sza.fastmediasorter.utils.MediaUtils
import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SortActivity : LocaleActivity() {
    private lateinit var binding: ActivitySortBinding
    private lateinit var viewModel: ConnectionViewModel
    private lateinit var smbClient: SmbClient
    private lateinit var preferenceManager: PreferenceManager
    private var localStorageClient: LocalStorageClient? = null
    
    private var currentConfig: ConnectionConfig? = null
    private var imageFiles = mutableListOf<String>()
    private var currentIndex = 0
    private var sortDestinations = listOf<ConnectionConfig>()
    private var isLocalMode = false
    
    // Video support
    private var exoPlayer: ExoPlayer? = null
    private var isCurrentMediaVideo = false
    
    // Preloading optimization
    private var nextImageData: ByteArray? = null
    private var nextImageIndex: Int = -1
    private var preloadJob: Job? = null
    
    // Error tracking
    private data class MediaError(
        val fileName: String,
        val errorType: String,
        val errorMessage: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    private val errorLog = mutableListOf<MediaError>()
    private var consecutiveErrors = 0
    private val MAX_CONSECUTIVE_ERRORS = 5
    
    // For handling delete permission requests on Android 11+
    private var pendingDeleteUri: Uri? = null
    private var isDeleteFromMove: Boolean = false  // Track if delete is from Move operation
    private lateinit var deletePermissionLauncher: ActivityResultLauncher<IntentSenderRequest>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySortBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.hide()
        
        // Initialize preference manager first
        preferenceManager = PreferenceManager(this)
        
        // Keep screen on if enabled
        if (preferenceManager.isKeepScreenOn()) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        // Register delete permission launcher for Android 11+
        deletePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Logger.d("SortActivity", "User granted delete permission")
                pendingDeleteUri?.let { uri ->
                    lifecycleScope.launch {
                        handleDeleteSuccess(uri)
                    }
                }
            } else {
                Logger.w("SortActivity", "User denied delete permission")
                android.widget.Toast.makeText(
                    this,
                    "Delete permission denied",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            pendingDeleteUri = null
        }
        
        viewModel = ViewModelProvider(this)[ConnectionViewModel::class.java]
        
        val configId = intent.getLongExtra("configId", -1)
        if (configId == -1L) {
            finish()
            return
        }
        
        setupExoPlayer()
        setupTouchAreas()
        setupBackButton()
        setupImageCounter()
        setupObservers()
        
        lifecycleScope.launch {
            loadConnection(configId)
        }
    }
    
    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        
        // Configure player view for better control positioning
        binding.playerView.useController = true
        binding.playerView.controllerAutoShow = false
        binding.playerView.controllerHideOnTouch = false
        
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        binding.videoLoadingLayout.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        binding.videoLoadingLayout.visibility = View.GONE
                    }
                    Player.STATE_ENDED -> {
                        binding.videoLoadingLayout.visibility = View.GONE
                    }
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                binding.videoLoadingLayout.visibility = View.GONE
                
                // Log error consistently
                val currentMediaUrl = imageFiles.getOrNull(currentIndex) ?: "unknown"
                handleMediaError(currentMediaUrl, "Video Playback", error.message ?: "Playback error")
                
                if (preferenceManager.isShowVideoErrorDetails()) {
                    showVideoErrorDialog(error)
                } else {
                    Toast.makeText(
                        this@SortActivity,
                        "Video playback error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
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
            Logger.d("SortActivity", "Previous area clicked")
            if (imageFiles.isEmpty()) return@setOnClickListener
            
            currentIndex = if (currentIndex > 0) {
                currentIndex - 1
            } else {
                imageFiles.size - 1  // Jump to last
            }
            loadMedia()
        }
        
        binding.nextArea.setOnClickListener {
            Logger.d("SortActivity", "Next area clicked")
            if (imageFiles.isEmpty()) return@setOnClickListener
            
            currentIndex = if (currentIndex < imageFiles.size - 1) {
                currentIndex + 1
            } else {
                0  // Jump to first
            }
            loadMedia()
        }


    }
    
    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        binding.refreshButton.setOnClickListener {
            refreshFileList()
        }
    }
    
    private fun setupImageCounter() {
        binding.imageCounterText.setOnClickListener {
            showJumpToDialog()
        }
    }
    
    private fun showJumpToDialog() {
        if (imageFiles.isEmpty()) return
        
        val totalFiles = imageFiles.size
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "1 - $totalFiles"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Jump to file")
            .setMessage("Enter file number (1 - $totalFiles):")
            .setView(input)
            .setPositiveButton("Jump") { _, _ ->
                val inputText = input.text.toString()
                if (inputText.isNotEmpty()) {
                    try {
                        val targetNumber = inputText.toInt()
                        if (targetNumber in 1..totalFiles) {
                            currentIndex = targetNumber - 1
                            loadMedia()
                            Toast.makeText(
                                this,
                                "Jumped to file $targetNumber",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Number must be between 1 and $totalFiles",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(
                            this,
                            "Invalid number",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        
        // Check if current source folder has write permissions
        val sourceHasWritePermission = when (currentConfig?.type) {
            "LOCAL_CUSTOM", "LOCAL_STANDARD" -> true  // Local folders always have write permission
            else -> currentConfig?.writePermission ?: false
        }
        
        // Check if move is allowed
        val allowMove = preferenceManager.isAllowMove() && sourceHasWritePermission
        
        // Check if copy is allowed
        val allowCopy = preferenceManager.isAllowCopy()
        
        // Check if delete is allowed
        val allowDelete = preferenceManager.isAllowDelete() && sourceHasWritePermission
        
        // Filter out destinations that are the same as source (pre-calculation for UI logic)
        val filteredDestinations = sortDestinations.filter { config ->
            // Skip if no write permission (except for local folders which always have write permission)
            val hasWritePermission = when (config.type) {
                "LOCAL_CUSTOM", "LOCAL_STANDARD" -> true  // Local folders always have write permission
                else -> config.writePermission
            }
            if (!hasWritePermission) return@filter false
            
            // Skip if it's the same as current source connection
            currentConfig?.let { currentSource ->
                // Skip if same config ID (works for both local and network)
                if (currentSource.id > 0 && config.id == currentSource.id) {
                    return@filter false
                }
                
                // For network connections, compare server+folder
                if (currentSource.type == "SMB" && config.type == "SMB") {
                    val sameServer = currentSource.serverAddress == config.serverAddress
                    val sameFolder = currentSource.folderPath == config.folderPath
                    if (sameServer && sameFolder) return@filter false
                }
                
                // For local folders, compare by name (localDisplayName or name)
                if (currentSource.type in listOf("LOCAL_CUSTOM", "LOCAL_STANDARD") && 
                    config.type in listOf("LOCAL_CUSTOM", "LOCAL_STANDARD")) {
                    val sourceName = currentSource.localDisplayName ?: currentSource.name
                    val destName = config.localDisplayName ?: config.name
                    if (sourceName.equals(destName, ignoreCase = true)) return@filter false
                }
            }
            true
        }
        
        // Check if we have destinations after filtering
        val hasDestinations = filteredDestinations.isNotEmpty()
        
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
        
        // Hide/show rename button
        val allowRename = preferenceManager.isAllowRename() && sourceHasWritePermission
        binding.renameButton.visibility = if (allowRename) View.VISIBLE else View.GONE
        
        // Setup rename button click listener
        binding.renameButton.setOnClickListener {
            renameCurrentMedia()
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
        
        // Show and configure buttons for filtered destinations
        filteredDestinations.forEachIndexed { index, config ->
            if (index < copyButtons.size) {
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
                            loadMedia()
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
        val destClient = SmbClient()
        return try {
            val uri = Uri.parse(localUri)
            val imageBytes = localStorageClient?.downloadImage(uri) 
            if (imageBytes == null) {
                Logger.e("SortActivity", "Failed to read local file: $localUri")
                return SmbClient.CopyResult.UnknownError("Failed to read local file")
            }
            
            val fileInfo = localStorageClient?.getFileInfo(uri)
            val fileName = fileInfo?.name ?: "unknown.jpg"
            Logger.d("SortActivity", "Copying file: $fileName (${imageBytes.size} bytes)")
            
            // Connect to destination SMB if needed
            val connected = destClient.connect(destination.serverAddress, destination.username, destination.password)
            if (!connected) {
                Logger.e("SortActivity", "Failed to connect to ${destination.serverAddress}")
                return SmbClient.CopyResult.NetworkError("Failed to connect to destination")
            }
            
            Logger.d("SortActivity", "Connected to ${destination.serverAddress}, writing to ${destination.folderPath}")
            
            // Write file to SMB and return result
            val result = destClient.writeFile(destination.serverAddress, destination.folderPath, fileName, imageBytes)
            
            when (result) {
                is SmbClient.CopyResult.Success -> 
                    Logger.d("SortActivity", "File copied successfully: $fileName")
                is SmbClient.CopyResult.AlreadyExists -> 
                    Logger.w("SortActivity", "File already exists: $fileName")
                is SmbClient.CopyResult.NetworkError -> 
                    Logger.e("SortActivity", "Network error: ${result.message}")
                is SmbClient.CopyResult.SecurityError -> 
                    Logger.e("SortActivity", "Security error: ${result.message}")
                else -> 
                    Logger.e("SortActivity", "Unknown error during copy")
            }
            
            result
        } catch (e: Exception) {
            Logger.e("SortActivity", "Exception during copy: ${e.message}", e)
            e.printStackTrace()
            SmbClient.CopyResult.UnknownError(e.message ?: "Unknown error")
        } finally {
            // Clear credentials from memory immediately after operation
            destClient.disconnect()
        }
    }
    
    private suspend fun moveFromLocalToSmb(localUri: String, destination: ConnectionConfig): SmbClient.MoveResult {
        try {
            Logger.d("SortActivity", "Moving from local to SMB: $localUri")
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
                        Logger.d("SortActivity", "File deleted successfully after copy")
                        SmbClient.MoveResult.Success
                    } else {
                        Logger.w("SortActivity", "Failed to delete local file after successful copy")
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
                Logger.e("SortActivity", "Copy failed: $errorMsg")
                return SmbClient.MoveResult.UnknownError(errorMsg)
            }
        } catch (e: Exception) {
            Logger.e("SortActivity", "Exception during move: ${e.message}", e)
            e.printStackTrace()
            return SmbClient.MoveResult.UnknownError(e.message ?: "Unknown error")
        }
    }
    
    private suspend fun deleteImageWithPermission(uri: Uri, fromMove: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Logger.d("SortActivity", "Requesting delete permission for Android 11+")
                
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
                Logger.e("SortActivity", "Error requesting delete permission", e)
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
        Logger.d("SortActivity", "File deleted successfully: $uri")
        
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
                val index = imageFiles.indexOfFirst { it == uri.toString() }
                if (index != -1) {
                    imageFiles.removeAt(index)
                    if (currentIndex >= imageFiles.size) {
                        currentIndex = imageFiles.size - 1
                    }
                }
                
                if (imageFiles.isEmpty()) {
                    showNoFilesState()
                } else {
                    loadMedia()
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
                        imageFiles.removeAt(currentIndex)
                        
                        // Check if list is now empty
                        if (imageFiles.isEmpty()) {
                            showNoFilesState()
                            return@withContext
                        }
                        
                        // Adjust index if we removed the last item
                        if (currentIndex >= imageFiles.size) {
                            currentIndex = imageFiles.size - 1
                        }                            // Load media at current index (which is now the next file)
                            loadMedia()
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
    
    private fun refreshFileList() {
        Toast.makeText(this, "Refreshing file list...", Toast.LENGTH_SHORT).show()
        
        // Save current file name to restore position after refresh
        val currentFileName = if (imageFiles.isNotEmpty() && currentIndex < imageFiles.size) {
            imageFiles[currentIndex].substringAfterLast('/')
        } else null
        
        loadImages()
        
        // Try to restore position to same file
        if (currentFileName != null) {
            lifecycleScope.launch {
                kotlinx.coroutines.delay(500) // Wait for loadImages to complete
                val newIndex = imageFiles.indexOfFirst { it.substringAfterLast('/') == currentFileName }
                if (newIndex >= 0) {
                    currentIndex = newIndex
                    loadMedia()
                }
            }
        }
    }
    
    private fun loadImages() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                currentConfig?.let { config ->
                    val isVideoEnabled = preferenceManager.isVideoEnabled()
                    val maxVideoSizeMb = preferenceManager.getMaxVideoSizeMb()
                    
                    val files = if (isLocalMode) {
                        val localUri = if (!config.localUri.isNullOrEmpty()) Uri.parse(config.localUri) else null
                        val bucketName = config.localDisplayName?.ifEmpty { null }
                        val imageInfoList = localStorageClient?.getImageFiles(localUri, bucketName, isVideoEnabled, maxVideoSizeMb) ?: emptyList()
                        imageInfoList.map { it.uri.toString() }
                    } else {
                        val result = smbClient.getImageFiles(config.serverAddress, config.folderPath, isVideoEnabled, maxVideoSizeMb)
                        if (result.errorMessage != null) {
                            withContext(Dispatchers.Main) {
                                androidx.appcompat.app.AlertDialog.Builder(this@SortActivity)
                                    .setTitle("Connection Error")
                                    .setMessage(result.errorMessage)
                                    .setPositiveButton("OK") { _, _ -> finish() }
                                    .setCancelable(false)
                                    .show()
                            }
                            return@launch
                        }
                        result.files
                    }
                    imageFiles.clear()
                imageFiles.addAll(files.sorted()) // ABC order
                    
                    withContext(Dispatchers.Main) {
                        if (imageFiles.isNotEmpty()) {
                            currentIndex = 0
                            loadMedia()
                        } else {
                            android.widget.Toast.makeText(
                                this@SortActivity,
                                "No media found in folder",
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
    
    private fun showVideoErrorDialog(error: androidx.media3.common.PlaybackException) {
        val currentFile = if (imageFiles.isNotEmpty() && currentIndex < imageFiles.size) {
            imageFiles[currentIndex]
        } else "Unknown"
        
        val errorDetails = StringBuilder()
        errorDetails.append("=== VIDEO PLAYBACK ERROR ===\n")
        errorDetails.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n\n")
        
        errorDetails.append("=== FILE INFO ===\n")
        errorDetails.append("File: ${currentFile.substringAfterLast('/')}\n")
        errorDetails.append("Path: $currentFile\n")
        errorDetails.append("Source: ${if (isLocalMode) "Local Storage" else "SMB Network"}\n\n")
        
        errorDetails.append("=== ERROR DETAILS ===\n")
        errorDetails.append("Exception: ${error.javaClass.simpleName}\n")
        errorDetails.append("Error Code: ${error.errorCode}\n")
        errorDetails.append("Message: ${error.message ?: "No message"}\n")
        errorDetails.append("Cause: ${error.cause?.javaClass?.simpleName ?: "None"}\n")
        if (error.cause != null) {
            errorDetails.append("Cause Message: ${error.cause?.message ?: "No message"}\n")
        }
        errorDetails.append("\n")
        
        errorDetails.append("=== PLAYBACK STATE ===\n")
        errorDetails.append("Video Enabled: ${preferenceManager.isVideoEnabled()}\n")
        errorDetails.append("Max Video Size: ${preferenceManager.getMaxVideoSizeMb()} MB\n")
        errorDetails.append("Play Video Till End: ${preferenceManager.isPlayVideoTillEnd()}\n")
        errorDetails.append("\n")
        
        errorDetails.append("=== SYSTEM INFO ===\n")
        errorDetails.append("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
        errorDetails.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
        errorDetails.append("\n")
        
        errorDetails.append("=== POSSIBLE CAUSES ===\n")
        when {
            error.message?.contains("codec", ignoreCase = true) == true -> {
                errorDetails.append("• Unsupported video codec\n")
                errorDetails.append("• Try converting to MP4 with H.264 codec\n")
            }
            error.message?.contains("source", ignoreCase = true) == true -> {
                errorDetails.append("• File not accessible\n")
                errorDetails.append("• Network connection issue (for SMB)\n")
                errorDetails.append("• File may be corrupted\n")
            }
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                errorDetails.append("• Network timeout (for SMB)\n")
                errorDetails.append("• File too large or slow connection\n")
            }
            else -> {
                errorDetails.append("• Unsupported video format\n")
                errorDetails.append("• Corrupted video file\n")
                errorDetails.append("• Insufficient device resources\n")
            }
        }
        errorDetails.append("\n")
        
        errorDetails.append("=== RECOMMENDATIONS ===\n")
        errorDetails.append("1. Try playing file with another app\n")
        errorDetails.append("2. Convert to MP4 (H.264 codec)\n")
        errorDetails.append("3. Reduce video resolution\n")
        errorDetails.append("4. Check file is not corrupted\n")
        errorDetails.append("5. For SMB: ensure stable network\n")
        errorDetails.append("6. For SMB connection errors: reconnect to share\n")
        errorDetails.append("\n=== DEBUG INFO ===\n")
        errorDetails.append("SMB Context Available: ${if (isLocalMode) "N/A (Local)" else smbClient.getContext() != null}\n")
        errorDetails.append("Full Path: $currentFile\n")
        
        com.sza.fastmediasorter.ui.dialogs.DiagnosticDialog.show(
            this,
            "Video Playback Error",
            errorDetails.toString(),
            false
        )
    }
    
    private fun showVideoValidationError(fileName: String, validationResult: com.sza.fastmediasorter.utils.MediaValidator.ValidationResult) {
        val errorDetails = StringBuilder()
        errorDetails.append("=== VIDEO FILE VALIDATION ERROR ===\n")
        errorDetails.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n\n")
        
        errorDetails.append("=== FILE INFO ===\n")
        errorDetails.append("File: $fileName\n")
        errorDetails.append("Extension: .${fileName.substringAfterLast('.', "unknown")}\n")
        errorDetails.append("Source: ${if (isLocalMode) "Local Storage" else "SMB Network"}\n\n")
        
        errorDetails.append("=== PRE-VALIDATION RESULT ===\n")
        errorDetails.append("Status: FAILED (file rejected before playback attempt)\n")
        errorDetails.append("Error Type: ${validationResult.errorType}\n")
        errorDetails.append("Details: ${validationResult.errorDetails}\n\n")
        
        errorDetails.append("=== WHY THIS HAPPENED ===\n")
        errorDetails.append("This file was rejected INSTANTLY (< 100ms) by header validation.\n")
        errorDetails.append("No playback was attempted, avoiding errors and delays.\n\n")
        
        errorDetails.append("=== DIAGNOSIS ===\n")
        when {
            validationResult.errorType?.contains("Size mismatch", ignoreCase = true) == true -> {
                errorDetails.append("▶ FILE TRUNCATED OR CORRUPTED\n")
                errorDetails.append("  The file header declares one size, but actual file is different.\n")
                errorDetails.append("  This causes ArrayIndexOutOfBoundsException during playback.\n")
                errorDetails.append("  File was likely damaged during transfer or incomplete download.\n\n")
            }
            validationResult.errorType?.contains("Invalid", ignoreCase = true) == true -> {
                errorDetails.append("▶ INVALID FILE FORMAT\n")
                errorDetails.append("  File header doesn't match expected format signature.\n")
                errorDetails.append("  File may be renamed, corrupted, or not a valid video.\n\n")
            }
            validationResult.errorType?.contains("Truncated", ignoreCase = true) == true -> {
                errorDetails.append("▶ FILE TOO SMALL\n")
                errorDetails.append("  File doesn't have enough bytes for a valid header.\n")
                errorDetails.append("  File is incomplete or severely damaged.\n\n")
            }
        }
        
        errorDetails.append("=== RECOMMENDATION ===\n")
        errorDetails.append("${validationResult.recommendation}\n\n")
        
        errorDetails.append("=== ACTIONS TAKEN ===\n")
        errorDetails.append("✓ File validation failed\n")
        errorDetails.append("✓ No playback attempted\n")
        errorDetails.append("✓ Error logged for review\n")
        
        com.sza.fastmediasorter.ui.dialogs.DiagnosticDialog.show(
            this,
            "Video Validation Failed",
            errorDetails.toString(),
            false
        )
    }
    
    private fun showImageValidationError(fileName: String, validationResult: com.sza.fastmediasorter.utils.MediaValidator.ValidationResult) {
        val errorDetails = StringBuilder()
        errorDetails.append("=== IMAGE FILE VALIDATION ERROR ===\n")
        errorDetails.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n\n")
        
        errorDetails.append("=== FILE INFO ===\n")
        errorDetails.append("File: $fileName\n")
        errorDetails.append("Extension: .${fileName.substringAfterLast('.', "unknown")}\n")
        errorDetails.append("Source: ${if (isLocalMode) "Local Storage" else "SMB Network"}\n\n")
        
        errorDetails.append("=== PRE-VALIDATION RESULT ===\n")
        errorDetails.append("Status: FAILED (file rejected before loading)\n")
        errorDetails.append("Error Type: ${validationResult.errorType}\n")
        errorDetails.append("Details: ${validationResult.errorDetails}\n\n")
        
        errorDetails.append("=== WHY THIS HAPPENED ===\n")
        errorDetails.append("This file was rejected INSTANTLY (< 100ms) by header validation.\n")
        errorDetails.append("No loading attempt was made.\n\n")
        
        errorDetails.append("=== DIAGNOSIS ===\n")
        when {
            validationResult.errorType?.contains("Size mismatch", ignoreCase = true) == true -> {
                errorDetails.append("▶ FILE TRUNCATED OR CORRUPTED\n")
                errorDetails.append("  The file header declares one size, but actual file is different.\n")
                errorDetails.append("  File was likely damaged during transfer or incomplete download.\n\n")
            }
            validationResult.errorType?.contains("Invalid", ignoreCase = true) == true -> {
                errorDetails.append("▶ INVALID FILE FORMAT\n")
                errorDetails.append("  File header doesn't match expected format signature.\n")
                errorDetails.append("  File may be renamed, corrupted, or not a valid image.\n\n")
            }
            validationResult.errorType?.contains("Truncated", ignoreCase = true) == true -> {
                errorDetails.append("▶ FILE TOO SMALL\n")
                errorDetails.append("  File doesn't have enough bytes for a valid header.\n")
                errorDetails.append("  File is incomplete or severely damaged.\n\n")
            }
            validationResult.errorType?.contains("SOI marker", ignoreCase = true) == true -> {
                errorDetails.append("▶ INVALID JPEG SIGNATURE\n")
                errorDetails.append("  JPEG files must start with 0xFFD8 marker.\n")
                errorDetails.append("  File is not a valid JPEG or is corrupted.\n\n")
            }
            validationResult.errorType?.contains("PNG", ignoreCase = true) == true -> {
                errorDetails.append("▶ INVALID PNG SIGNATURE\n")
                errorDetails.append("  PNG files must start with specific 8-byte signature.\n")
                errorDetails.append("  File is not a valid PNG or is corrupted.\n\n")
            }
        }
        
        errorDetails.append("=== RECOMMENDATION ===\n")
        errorDetails.append("${validationResult.recommendation}\n\n")
        
        errorDetails.append("=== ACTIONS TAKEN ===\n")
        errorDetails.append("✓ File validation failed\n")
        errorDetails.append("✓ No loading attempted\n")
        errorDetails.append("✓ Fast rejection prevents decode errors\n")
        
        com.sza.fastmediasorter.ui.dialogs.DiagnosticDialog.show(
            this,
            "Image Validation Failed",
            errorDetails.toString(),
            false
        )
    }
    
    private fun showNoFilesState() {
        // Hide all media views
        binding.imageView.visibility = View.GONE
        binding.playerView.visibility = View.GONE
        binding.videoLoadingLayout.visibility = View.GONE
        binding.copyProgressLayout.visibility = View.GONE
        
        // Show "No files" message
        binding.fileInfoText.text = "No files in this folder"
        binding.imageCounterText.text = "0 / 0"
        
        // Stop video player if running
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        
        Toast.makeText(
            this,
            "No more files in folder",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun loadMedia() {
        if (imageFiles.isEmpty()) {
            showNoFilesState()
            return
        }
        
        // Cancel any ongoing preload
        preloadJob?.cancel()
        
        val mediaUrl = imageFiles[currentIndex]
        
        // Determine if current media is video
        isCurrentMediaVideo = MediaUtils.isVideo(mediaUrl)
        
        // Show/hide appropriate view
        if (isCurrentMediaVideo) {
            binding.imageView.visibility = View.GONE
            binding.playerView.visibility = View.VISIBLE
            loadVideo(mediaUrl)
        } else {
            binding.playerView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            loadImage(mediaUrl)
        }
        
        updateImageCounter()
        updateVideoControlZone()
    }
    
    private fun updateVideoControlZone() {
        // Post to ensure layout is complete
        binding.root.post {
            // Get the actual media display area
            val mediaAreaHeight = binding.fileInfoText.top
            
            // Set guideline at 2/3 from top (bottom 1/3 for video controls)  
            val controlZoneStart = (mediaAreaHeight * 2) / 3
            
            val params = binding.videoControlGuideline.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.guideBegin = controlZoneStart
            binding.videoControlGuideline.layoutParams = params
            
            // Debug log
            Logger.d("VideoControl", "Media height: $mediaAreaHeight, Control zone starts at: $controlZoneStart")
        }
    }
    
    private fun loadImage(imageUrl: String) {
        if (imageFiles.isEmpty()) return
        
        // FAST PRE-VALIDATION for SMB images
        if (!isLocalMode) {
            lifecycleScope.launch(Dispatchers.IO) {
                val fileName = imageUrl.substringAfterLast('/')
                val extension = imageUrl.substringAfterLast('.', "").lowercase()
                Logger.d("SortActivity", "▶ Pre-validating SMB image file: $fileName")
                val validationStartTime = System.currentTimeMillis()
                val validationResult = com.sza.fastmediasorter.utils.MediaValidator.validateSmbMedia(
                    imageUrl,
                    smbClient.getContext()?.let { smbClient },
                    isVideo = false
                )
                val validationTime = System.currentTimeMillis() - validationStartTime
                Logger.d("SortActivity", "  Validation completed in ${validationTime}ms")
                
                if (!validationResult.isValid) {
                    Logger.e("SortActivity", "═══════════════════════════════════════════")
                    Logger.e("SortActivity", "▶▶▶ IMAGE FILE VALIDATION FAILED ▶▶▶")
                    Logger.e("SortActivity", "  File: $fileName")
                    Logger.e("SortActivity", "  Extension: .$extension")
                    Logger.e("SortActivity", "  Error Type: ${validationResult.errorType}")
                    Logger.e("SortActivity", "  Details: ${validationResult.errorDetails}")
                    Logger.e("SortActivity", "  SKIPPING WITHOUT ATTEMPTING LOAD")
                    Logger.e("SortActivity", "═══════════════════════════════════════════")
                    
                    withContext(Dispatchers.Main) {
                        if (preferenceManager.isShowVideoErrorDetails()) {
                            showImageValidationError(fileName, validationResult)
                        } else {
                            Toast.makeText(
                                this@SortActivity,
                                "Image file corrupted: $fileName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        
                        handleMediaError(imageUrl, "Image Validation", validationResult.errorDetails ?: "File corrupted")
                    }
                    return@launch
                }
                
                // Continue with normal loading if validation passed
                loadImageAfterValidation(imageUrl)
            }
        } else {
            // Local mode - load directly
            loadImageAfterValidation(imageUrl)
        }
    }
    
    private fun loadImageAfterValidation(imageUrl: String) {
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
                            .error(R.drawable.error_placeholder)
                            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<android.graphics.drawable.Drawable>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Logger.e("SortActivity", "Failed to load image: ${e?.message}", e)
                                    handleMediaError(imageUrl, "Image Load", e?.message ?: "Glide load failed")
                                    return false
                                }
                                
                                override fun onResourceReady(
                                    resource: android.graphics.drawable.Drawable,
                                    model: Any,
                                    target: Target<android.graphics.drawable.Drawable>?,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    consecutiveErrors = 0
                                    return false
                                }
                            })
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
                var toastShown = false
                var loadingToast: Toast? = null
                
                // Schedule toast if loading takes >1 second
                val toastJob = launch(Dispatchers.IO) {
                    delay(1000)
                    val fileInfo = try {
                        if (isLocalMode) {
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
                    } catch (e: Exception) {
                        null
                    }
                    
                    withContext(Dispatchers.Main) {
                        val sizeText = fileInfo?.let { "${it.sizeKB}KB" } ?: "..."
                        loadingToast = Toast.makeText(
                            this@SortActivity,
                            "Loading... $sizeText",
                            Toast.LENGTH_SHORT
                        )
                        loadingToast?.show()
                        toastShown = true
                    }
                }
                
                try {
                    val imageBytes = if (isLocalMode) {
                        localStorageClient?.downloadImage(Uri.parse(imageUrl))
                    } else {
                        smbClient.downloadImage(imageUrl)
                    }
                    
                    val fileInfo = if (isLocalMode) {
                        Logger.d("SortActivity", "Getting file info for local file: $imageUrl")
                        val uri = Uri.parse(imageUrl)
                        Logger.d("SortActivity", "Parsed URI: $uri")
                        
                        val localInfo = localStorageClient?.getFileInfo(uri)
                        Logger.d("SortActivity", "LocalStorageClient returned: ${if (localInfo == null) "NULL" else "name=${localInfo.name}, size=${localInfo.size}"}")
                        
                        localInfo?.let {
                            SmbClient.FileInfo(
                                name = it.name,
                                sizeKB = (it.size / 1024),
                                modifiedDate = it.dateModified
                            )
                        }
                    } else {
                        smbClient.getFileInfo(imageUrl)
                    }
                    
                    // Cancel toast timer if still waiting
                    toastJob.cancel()
                    
                    withContext(Dispatchers.Main) {
                        // Hide toast if it was shown
                        if (toastShown) {
                            loadingToast?.cancel()
                        }
                        
                        if (imageBytes != null) {
                            Glide.with(this@SortActivity)
                                .load(imageBytes)
                                .error(R.drawable.error_placeholder)
                                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<android.graphics.drawable.Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Logger.e("SortActivity", "Failed to load image: ${e?.message}", e)
                                        handleMediaError(imageUrl, "Image Load", e?.message ?: "Glide load failed")
                                        return false
                                    }
                                    
                                    override fun onResourceReady(
                                        resource: android.graphics.drawable.Drawable,
                                        model: Any,
                                        target: Target<android.graphics.drawable.Drawable>?,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        consecutiveErrors = 0
                                        return false
                                    }
                                })
                                .into(binding.imageView)
                            
                            updateFileInfo(fileInfo)
                        } else {
                            handleMediaError(imageUrl, "Image Download", "No data received")
                        }
                    }
                } catch (e: Exception) {
                    toastJob.cancel()
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        if (toastShown) {
                            loadingToast?.cancel()
                        }
                        handleMediaError(imageUrl, "Image Load", e.message ?: "Unknown error")
                    }
                }
                
                // Start preloading next image
                preloadNextImage()
            }
        }
    }
    
    private fun loadVideo(videoUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = videoUrl.substringAfterLast('/')
                val extension = videoUrl.substringAfterLast('.', "").lowercase()
                
                // FAST PRE-VALIDATION: Check video file integrity BEFORE ExoPlayer
                if (!isLocalMode) {
                    Logger.d("SortActivity", "▶ Pre-validating SMB video file: $fileName")
                    val validationStartTime = System.currentTimeMillis()
                    val validationResult = com.sza.fastmediasorter.utils.MediaValidator.validateSmbMedia(
                        videoUrl,
                        smbClient.getContext()?.let { smbClient },
                        isVideo = true
                    )
                    val validationTime = System.currentTimeMillis() - validationStartTime
                    Logger.d("SortActivity", "  Validation completed in ${validationTime}ms")
                    Logger.d("SortActivity", "  Validation result: isValid=${validationResult.isValid}, errorType=${validationResult.errorType}, errorDetails=${validationResult.errorDetails}")
                    
                    if (!validationResult.isValid) {
                        Logger.e("SortActivity", "═══════════════════════════════════════════")
                        Logger.e("SortActivity", "▶▶▶ VIDEO FILE VALIDATION FAILED ▶▶▶")
                        Logger.e("SortActivity", "  File: $fileName")
                        Logger.e("SortActivity", "  Extension: .$extension")
                        Logger.e("SortActivity", "  Error Type: ${validationResult.errorType}")
                        Logger.e("SortActivity", "  Details: ${validationResult.errorDetails}")
                        Logger.e("SortActivity", "  Recommendation: ${validationResult.recommendation}")
                        Logger.e("SortActivity", "  SKIPPING WITHOUT ATTEMPTING PLAYBACK")
                        Logger.e("SortActivity", "═══════════════════════════════════════════")
                        
                        withContext(Dispatchers.Main) {
                            binding.videoLoadingLayout.visibility = View.GONE
                            
                            if (preferenceManager.isShowVideoErrorDetails()) {
                                showVideoValidationError(fileName, validationResult)
                            } else {
                                Toast.makeText(
                                    this@SortActivity,
                                    "Video file corrupted: $fileName",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            
                            handleMediaError(videoUrl, "Video Validation", validationResult.errorDetails ?: "File corrupted")
                        }
                        return@launch
                    } else if (validationResult.errorDetails != null) {
                        Logger.w("SortActivity", "⚠ Validation warning: ${validationResult.errorDetails}")
                    }
                }
                
                // Get file info for display
                val fileInfo = if (isLocalMode) {
                    localStorageClient?.getFileInfo(Uri.parse(videoUrl))?.let {
                        SmbClient.FileInfo(
                            name = it.name,
                            sizeKB = (it.size / 1024),
                            modifiedDate = it.dateModified
                        )
                    }
                } else {
                    smbClient.getFileInfo(videoUrl)
                }
                
                withContext(Dispatchers.Main) {
                    try {
                        // Stop current playback
                        exoPlayer?.stop()
                        exoPlayer?.clearMediaItems()
                        
                        if (isLocalMode) {
                            // Local video - use URI directly
                            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                            exoPlayer?.setMediaItem(mediaItem)
                            exoPlayer?.prepare()
                            exoPlayer?.play()
                        } else {
                            // SMB video - need to recreate player with custom data source
                            val smbContext = smbClient.getContext()
                            if (smbContext != null) {
                                // Release old player
                                exoPlayer?.release()
                                
                                // Create new player with SmbDataSource
                                val dataSourceFactory = SmbDataSourceFactory(smbClient)
                                exoPlayer = ExoPlayer.Builder(this@SortActivity)
                                    .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory as androidx.media3.datasource.DataSource.Factory))
                                    .build()
                                binding.playerView.player = exoPlayer
                                
                                // Re-attach listener after recreating player
                                exoPlayer?.addListener(object : Player.Listener {
                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                        when (playbackState) {
                                            Player.STATE_BUFFERING -> {
                                                binding.videoLoadingLayout.visibility = View.VISIBLE
                                            }
                                            Player.STATE_READY -> {
                                                binding.videoLoadingLayout.visibility = View.GONE
                                            }
                                            Player.STATE_ENDED -> {
                                                binding.videoLoadingLayout.visibility = View.GONE
                                            }
                                        }
                                    }
                                    
                                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                        binding.videoLoadingLayout.visibility = View.GONE
                                        handleMediaError(videoUrl, "Video Playback", error.message ?: "Playback error")
                                        
                                        if (preferenceManager.isShowVideoErrorDetails()) {
                                            showVideoErrorDialog(error)
                                        } else {
                                            Toast.makeText(
                                                this@SortActivity,
                                                "Video playback error: ${error.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                })
                                
                                val mediaItem = MediaItem.fromUri(videoUrl)
                                exoPlayer?.setMediaItem(mediaItem)
                                exoPlayer?.prepare()
                                exoPlayer?.play()
                                consecutiveErrors = 0
                            } else {
                                handleMediaError(videoUrl, "Video Load", "SMB context not available")
                                return@withContext
                            }
                        }
                        
                        updateFileInfo(fileInfo)
                        
                    } catch (e: Exception) {
                        Logger.e("SortActivity", "Failed to load video: ${e.message}", e)
                        handleMediaError(videoUrl, "Video Load", e.message ?: "Unknown error")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    handleMediaError(videoUrl, "Video Load", e.message ?: "Unknown error")
                }
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
                .setTitle("Delete Media")
                .setMessage("Are you sure you want to delete this media file?")
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
                        handleDeleteResultLocal(deleted, imageUrl)
                    }
                } else {
                    // SMB delete
                    val deleteResult = smbClient.deleteFile(imageUrl)
                    handleDeleteResult(deleteResult, imageUrl)
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
    
    private suspend fun handleDeleteResultLocal(deleted: Boolean, imageUrl: String) {
        withContext(Dispatchers.Main) {
            binding.copyProgressLayout.visibility = View.GONE
            
            if (deleted) {
                android.widget.Toast.makeText(
                    this@SortActivity,
                    "File deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // Remove from list and load next
                imageFiles.remove(imageUrl)
                
                if (imageFiles.isEmpty()) {
                    showNoFilesState()
                    return@withContext
                }
                
                if (currentIndex >= imageFiles.size) {
                    currentIndex = imageFiles.size - 1
                }
                loadMedia()
            } else {
                android.widget.Toast.makeText(
                    this@SortActivity,
                    "Failed to delete file",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private suspend fun handleDeleteResult(deleteResult: SmbClient.DeleteResult, imageUrl: String) {
        withContext(Dispatchers.Main) {
            binding.copyProgressLayout.visibility = View.GONE
            
            when (deleteResult) {
                is SmbClient.DeleteResult.Success -> {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "File deleted",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    
                    // Remove from list and load next
                    imageFiles.remove(imageUrl)
                    
                    if (imageFiles.isEmpty()) {
                        showNoFilesState()
                        return@withContext
                    }
                    
                    if (currentIndex >= imageFiles.size) {
                        currentIndex = imageFiles.size - 1
                    }
                    loadMedia()
                }
                is SmbClient.DeleteResult.FileNotFound -> {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "File not found",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                is SmbClient.DeleteResult.SecurityError -> {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Access denied: ${deleteResult.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                is SmbClient.DeleteResult.NetworkError -> {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Network error: ${deleteResult.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                is SmbClient.DeleteResult.UnknownError -> {
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Error: ${deleteResult.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }    private fun renameCurrentMedia() {
        if (imageFiles.isEmpty()) return
        
        val currentUrl = imageFiles[currentIndex]
        val currentFileName = currentUrl.substringAfterLast('/')
        
        // Create input dialog
        val input = EditText(this)
        input.setText(currentFileName)
        input.selectAll()
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Rename Media")
            .setMessage("Enter new filename:")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newFileName = input.text.toString().trim()
                if (newFileName.isNotEmpty() && newFileName != currentFileName) {
                    performRename(currentUrl, newFileName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performRename(oldUrl: String, newFileName: String) {
        // Validate filename
        val invalidChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        if (invalidChars.any { newFileName.contains(it) }) {
            android.widget.Toast.makeText(
                this,
                "Filename contains invalid characters: / \\ : * ? \" < > |",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                binding.progressText.text = "Renaming..."
                binding.copyProgressLayout.visibility = View.VISIBLE
            }
            
            try {
                val newUrl: String
                val success: Boolean
                
                if (isLocalMode) {
                    // Local storage rename
                    val oldUri = Uri.parse(oldUrl)
                    val result = localStorageClient?.renameFile(oldUri, newFileName)
                    success = result?.first ?: false
                    newUrl = result?.second ?: oldUrl
                } else {
                    // SMB rename
                    val folderPath = oldUrl.substringBeforeLast('/')
                    newUrl = "$folderPath/$newFileName"
                    
                    val renameResult = smbClient.renameFile(oldUrl, newUrl)
                    
                    withContext(Dispatchers.Main) {
                        binding.copyProgressLayout.visibility = View.GONE
                        
                        when (renameResult) {
                            is SmbClient.RenameResult.Success -> {
                                // Update the list with new URL
                                imageFiles[currentIndex] = newUrl
                                
                                android.widget.Toast.makeText(
                                    this@SortActivity,
                                    "File renamed successfully",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                
                                // Reload current media with new name
                                loadMedia()
                            }
                            is SmbClient.RenameResult.SourceNotFound -> {
                                android.widget.Toast.makeText(
                                    this@SortActivity,
                                    "Source file not found",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            is SmbClient.RenameResult.TargetExists -> {
                                android.widget.Toast.makeText(
                                    this@SortActivity,
                                    "File with this name already exists",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            is SmbClient.RenameResult.SecurityError -> {
                                android.widget.Toast.makeText(
                                    this@SortActivity,
                                    "Access denied: ${renameResult.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            is SmbClient.RenameResult.NetworkError -> {
                                android.widget.Toast.makeText(
                                    this@SortActivity,
                                    "Network error: ${renameResult.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            is SmbClient.RenameResult.UnknownError -> {
                                android.widget.Toast.makeText(
                                    this@SortActivity,
                                    "Error: ${renameResult.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    return@launch
                }
                
                withContext(Dispatchers.Main) {
                    binding.copyProgressLayout.visibility = View.GONE
                    
                    if (success) {
                        // Update the list with new URL (for local mode)
                        imageFiles[currentIndex] = newUrl
                        
                        android.widget.Toast.makeText(
                            this@SortActivity,
                            "File renamed successfully",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        
                        // Reload current media with new name
                        loadMedia()
                    } else {
                        android.widget.Toast.makeText(
                            this@SortActivity,
                            "Failed to rename file",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.copyProgressLayout.visibility = View.GONE
                    android.widget.Toast.makeText(
                        this@SortActivity,
                        "Error renaming file: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        
        // Stop media playback immediately
        exoPlayer?.playWhenReady = false
        
        // Cancel preload job
        preloadJob?.cancel()
        preloadJob = null
        
        Logger.d("SortActivity", "onStop: Media playback stopped, preload cancelled")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        preloadJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        try {
            smbClient.disconnect()
        } catch (e: Exception) {
            // Ignore disconnect errors on destroy
        }
    }
    
    private fun handleMediaError(mediaUrl: String, errorType: String, errorMessage: String) {
        val fileName = mediaUrl.substringAfterLast('/')
        errorLog.add(MediaError(fileName, errorType, errorMessage))
        consecutiveErrors++
        
        lifecycleScope.launch(Dispatchers.Main) {
            if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                showErrorReportDialog()
            } else {
                Toast.makeText(
                    this@SortActivity,
                    "⚠ $errorType error (${consecutiveErrors}/${MAX_CONSECUTIVE_ERRORS})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun showErrorReportDialog() {
        val recentErrors = errorLog.takeLast(10)
        val errorSummary = buildString {
            append("⚠️ MULTIPLE ERRORS DETECTED\n\n")
            append("Consecutive errors: $consecutiveErrors\n")
            append("Total errors: ${errorLog.size}\n\n")
            append("Recent errors:\n")
            append("━".repeat(40))
            append("\n\n")
            
            recentErrors.forEach { error ->
                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date(error.timestamp))
                append("[$time] ${error.errorType}\n")
                append("File: ${error.fileName}\n")
                append("Error: ${error.errorMessage}\n\n")
            }
            
            append("\nRecommendations:\n")
            append("• Check network connection\n")
            append("• Verify SMB server is accessible\n")
            append("• Check file permissions\n")
            append("• Some files may be corrupted\n")
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Media Loading Issues")
            .setMessage(errorSummary)
            .setPositiveButton("Continue") { dialog, _ ->
                consecutiveErrors = 0
                dialog.dismiss()
            }
            .setNegativeButton("View Full Log") { _, _ ->
                showFullErrorLog()
            }
            .setNeutralButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showFullErrorLog() {
        val fullLog = buildString {
            append("FULL ERROR LOG\n")
            append("Total errors: ${errorLog.size}\n")
            append("━".repeat(50))
            append("\n\n")
            
            errorLog.reversed().forEach { error ->
                val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date(error.timestamp))
                append("[$time]\n")
                append("Type: ${error.errorType}\n")
                append("File: ${error.fileName}\n")
                append("Error: ${error.errorMessage}\n")
                append("\n")
            }
        }
        
        val scrollView = android.widget.ScrollView(this)
        val textView = android.widget.TextView(this).apply {
            text = fullLog
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
            setPadding(32, 32, 32, 32)
        }
        scrollView.addView(textView)
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Error Log (${errorLog.size} entries)")
            .setView(scrollView)
            .setPositiveButton("Close") { dialog, _ ->
                consecutiveErrors = 0
                dialog.dismiss()
            }
            .setNegativeButton("Clear Log") { dialog, _ ->
                errorLog.clear()
                consecutiveErrors = 0
                Toast.makeText(this, "Error log cleared", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }
}

