package com.sza.fastmediasorter.ui.network

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android:view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.data.ConnectionConfig
import com.sza.fastmediasorter.ui.ConnectionAdapter
import com.sza.fastmediasorter.ui.ConnectionViewModel
import kotlinx.coroutines.launch

class NetworkFragment : Fragment() {
private val viewModel: ConnectionViewModel by activityViewModels()
private lateinit var adapter: ConnectionAdapter
private lateinit var rvConnections: RecyclerView
private lateinit var etFolderAddress: TextInputEditText
private lateinit var etUsername: TextInputEditText
private lateinit var etPassword: TextInputEditText
private lateinit var etName: TextInputEditText
private lateinit var btnSave: Button
var onConfigSelected: ((ConnectionConfig) -> Unit)? = null
var onConfigDoubleClick: ((ConnectionConfig) -> Unit)? = null

override fun onCreateView(
inflater: LayoutInflater,
container: ViewGroup?,
savedInstanceState: Bundle?
): View? {
return inflater.inflate(R.layout.fragment_network, container, false)
}

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
super.onViewCreated(view, savedInstanceState)
rvConnections = view.findViewById(R.id.connectionsRecyclerView)
etFolderAddress = view.findViewById(R.id.folderAddressInput)
etUsername = view.findViewById(R.id.usernameInput)
etPassword = view.findViewById(R.id.passwordInput)
etName = view.findViewById(R.id.nameInput)
btnSave = view.findViewById(R.id.saveButton)
setupRecyclerView()
observeConnections()
setupSaveButton()
}

private fun setupRecyclerView() {
adapter = ConnectionAdapter(
onItemClick = { config ->
loadConfig(config)
onConfigSelected?.invoke(config)
},
onItemDoubleClick = { config ->
onConfigDoubleClick?.invoke(config)
},
onDeleteClick = { config ->
viewModel.deleteConfig(config)
Toast.makeText(requireContext(), R.string.connection_deleted, Toast.LENGTH_SHORT).show()
}
)
rvConnections.apply {
layoutManager = LinearLayoutManager(requireContext())
adapter = this@NetworkFragment.adapter
}
}

private fun observeConnections() {
viewModel.allConfigs.observe(viewLifecycleOwner) { configs ->
val smbConfigs = configs.filter { it.type == "SMB" }
adapter.submitList(smbConfigs)
}
}

private fun setupSaveButton() {
btnSave.setOnClickListener {
saveConnection()
}
}

private fun loadConfig(config: ConnectionConfig) {
val folderAddress = "${config.serverAddress}\\${config.folderPath}"
etFolderAddress.setText(folderAddress)
etUsername.setText(config.username)
etPassword.setText(config.password)
etName.setText(config.name)
}

fun clearInputs() {
etFolderAddress.setText("")
etUsername.setText("")
etPassword.setText("")
etName.setText("")
}

private fun saveConnection() {
val folderAddress = etFolderAddress.text.toString().trim()
val username = etUsername.text.toString().trim()
val password = etPassword.text.toString().trim()
var name = etName.text.toString().trim()
if (folderAddress.isEmpty()) {
Toast.makeText(requireContext(), "Please enter folder address", Toast.LENGTH_SHORT).show()
return
}
if (name.isEmpty()) {
name = folderAddress
}
val (server, folder) = parseFolderAddress(folderAddress)
lifecycleScope.launch {
val existingConfig = viewModel.getConfigByFolderAddress(server, folder)
if (existingConfig != null) {
val config = existingConfig.copy(
name = name,
username = username,
password = password,
lastUsed = System.currentTimeMillis()
)
viewModel.updateConfig(config)
Toast.makeText(requireContext(), "Connection updated", Toast.LENGTH_SHORT).show()
} else {
val config = ConnectionConfig(
id = 0,
name = name,
serverAddress = server,
username = username,
password = password,
folderPath = folder,
interval = 10,
lastUsed = System.currentTimeMillis(),
type = "SMB"
)
viewModel.insertConfig(config)
Toast.makeText(requireContext(), R.string.connection_saved, Toast.LENGTH_SHORT).show()
}
clearInputs()
}
}

private fun parseFolderAddress(folderAddress: String): Pair<String, String> {
val normalized = folderAddress.replace("/", "\\").trimStart('\\')
val parts = normalized.split("\\", limit = 2)
val server = parts[0]
val folder = if (parts.size > 1) parts[1] else ""
return Pair(server, folder)
}
}
