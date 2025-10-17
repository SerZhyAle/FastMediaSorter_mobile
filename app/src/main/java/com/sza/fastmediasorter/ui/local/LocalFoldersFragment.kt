package com.sza.fastmediasorter.ui.local

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.network.LocalStorageClient
import kotlinx.coroutines.launch

data class LocalFolder(
val name: String,
val icon: String,
val count: Int,
val isCustom: Boolean = false
)

class LocalFoldersFragment : Fragment() {
private lateinit var rvLocalFolders: RecyclerView
private lateinit var btnAddCustomFolder: Button
private lateinit var tvNoPermission: TextView
private lateinit var localClient: LocalStorageClient
private lateinit var adapter: LocalFolderAdapter
private val folders = mutableListOf<LocalFolder>()

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
btnAddCustomFolder = view.findViewById(R.id.btnAddCustomFolder)
tvNoPermission = view.findViewById(R.id.tvNoPermission)
localClient = LocalStorageClient(requireContext())
adapter = LocalFolderAdapter(folders) { folder, isDoubleClick ->
if (isDoubleClick) {
// TODO: launch slideshow
} else {
// TODO: select folder
}
}
rvLocalFolders.layoutManager = LinearLayoutManager(requireContext())
rvLocalFolders.adapter = adapter
btnAddCustomFolder.setOnClickListener {
pickFolderLauncher.launch(null)
}
checkPermissionAndLoadFolders()
}

private fun checkPermissionAndLoadFolders() {
if (LocalStorageClient.hasMediaPermission(requireContext())) {
tvNoPermission.visibility = View.GONE
rvLocalFolders.visibility = View.VISIBLE
btnAddCustomFolder.visibility = View.VISIBLE
loadStandardFolders()
} else {
tvNoPermission.visibility = View.VISIBLE
rvLocalFolders.visibility = View.GONE
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
if (count > 0) {
folders.add(
LocalFolder(
name = name,
icon = iconMap[name] ?: "📁",
count = count,
isCustom = false
)
)
}
}
adapter.notifyDataSetChanged()
}
}
}
