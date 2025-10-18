package com.sza.fastmediasorter.ui.local

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.network.LocalStorageClient
import com.sza.fastmediasorter.ui.ConnectionViewModel
import kotlinx.coroutines.launch

data class LocalFolder(
    val name: String,
    val icon: String,
    val count: Int,
    val isCustom: Boolean = false,
    val configId: Long = 0
)

class LocalFoldersFragment : Fragment() {
    private lateinit var rvLocalFolders: RecyclerView
    private lateinit var btnScanFolders: Button
    private lateinit var btnAddCustomFolder: Button
    private lateinit var tvNoPermission: TextView
    private lateinit var localClient: LocalStorageClient
    private lateinit var adapter: LocalFolderAdapter
    private val folders = mutableListOf<LocalFolder>()
    private val viewModel: ConnectionViewModel by activityViewModels()
    private val standardFolderNames = setOf("Camera", "Screenshots", "Pictures", "Download")
    
    var onFolderSelected: ((LocalFolder) -> Unit)? = null
    var onFolderDoubleClick: ((LocalFolder) -> Unit)? = null
    
    private val pickFolderLauncher = registerForActivityResult(
ActivityResultContracts.OpenDocumentTree()
) { uri ->
uri?.let {
requireContext().contentResolver.takePersistableUriPermission(
it,
Intent.FLAG_GRANT_READ_URI_PERMISSION
)
}
}

override fun onCreateView(
inflater: LayoutInflater,
container: ViewGroup?,
savedInstanceState: Bundle?
): View? {
return inflater.inflate(R.layout.fragment_local_folders, container, false)
}

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
super.onViewCreated(view, savedInstanceState)
rvLocalFolders = view.findViewById(R.id.rvLocalFolders)
btnScanFolders = view.findViewById(R.id.btnScanFolders)
btnAddCustomFolder = view.findViewById(R.id.btnAddCustomFolder)
tvNoPermission = view.findViewById(R.id.tvNoPermission)
localClient = LocalStorageClient(requireContext())
adapter = LocalFolderAdapter(folders) { folder, isDoubleClick ->
if (isDoubleClick) {
onFolderDoubleClick?.invoke(folder)
} else {
onFolderSelected?.invoke(folder)
}
}
rvLocalFolders.layoutManager = LinearLayoutManager(requireContext())
rvLocalFolders.adapter = adapter
btnScanFolders.setOnClickListener {
scanAllFolders()
}
btnAddCustomFolder.setOnClickListener {
pickFolderLauncher.launch(null)
}
checkPermissionAndLoadFolders()
}

private fun checkPermissionAndLoadFolders() {
if (LocalStorageClient.hasMediaPermission(requireContext())) {
tvNoPermission.visibility = View.GONE
rvLocalFolders.visibility = View.VISIBLE
btnScanFolders.visibility = View.VISIBLE
btnAddCustomFolder.visibility = View.VISIBLE
loadStandardFolders()
loadCustomFolders()
} else {
tvNoPermission.visibility = View.VISIBLE
rvLocalFolders.visibility = View.GONE
btnScanFolders.visibility = View.GONE
btnAddCustomFolder.visibility = View.GONE
}
}

private fun loadStandardFolders() {
lifecycleScope.launch {
val standardFolders = localClient.scanStandardFolders()
folders.clear()
val iconMap = mapOf(
"Camera" to "📷",
"Screenshots" to "📸",
"Pictures" to "🖼️",
"Download" to "📥"
)
standardFolders.forEach { (name, pair) ->
val count = pair.second
folders.add(
LocalFolder(
name = name,
icon = iconMap[name] ?: "📁",
count = count,
isCustom = false,
configId = 0
)
)
}
adapter.notifyDataSetChanged()
}
}

private fun loadCustomFolders() {
viewModel.localCustomFolders.observe(viewLifecycleOwner) { customConfigs ->
// Get current counts from scanAllImageFolders
lifecycleScope.launch {
val allFolders = localClient.scanAllImageFolders()
val customFolders = customConfigs.map { config ->
val folderName = config.localDisplayName ?: "Unknown"
val count = allFolders[folderName] ?: 0
LocalFolder(
name = folderName,
icon = "📁",
count = count,
isCustom = true,
configId = config.id
)
}
folders.addAll(customFolders)
adapter.notifyDataSetChanged()
}
}
}

private fun scanAllFolders() {
lifecycleScope.launch {
Toast.makeText(requireContext(), R.string.scanning_folders, Toast.LENGTH_SHORT).show()
try {
val allFolders = localClient.scanAllImageFolders()
var newFoldersCount = 0
var updatedFoldersCount = 0

// Get existing custom folders
val existingFolders = viewModel.localCustomFolders.value?.map { it.localDisplayName } ?: emptyList()
allFolders.forEach { (folderName, count) ->
if (folderName !in standardFolderNames) {
if (folderName !in existingFolders) {
viewModel.addLocalCustomFolder(folderName, "")
newFoldersCount++
} else {
updatedFoldersCount++
}
}
}

// Reload folders to update counts
loadStandardFolders()
loadCustomFolders()

val message = when {
newFoldersCount > 0 && updatedFoldersCount > 0 ->
"Added $newFoldersCount, updated $updatedFoldersCount folders"
newFoldersCount > 0 ->
getString(R.string.scan_complete, newFoldersCount)
updatedFoldersCount > 0 ->
"Updated $updatedFoldersCount folders"
else ->
"No new folders found"
}

Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
} catch (e: Exception) {
Toast.makeText(requireContext(), "Scan failed: ${e.message}", Toast.LENGTH_LONG).show()
}
}
}
}