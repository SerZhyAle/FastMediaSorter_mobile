package com.sza.fastmediasorter.ui.network

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
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
private lateinit var cbAddToDestinations: CheckBox
private lateinit var btnSave: Button
private lateinit var btnTest: Button
private var selectedConfig: ConnectionConfig? = null
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
cbAddToDestinations = view.findViewById(R.id.addToDestinationsCheckbox)
btnSave = view.findViewById(R.id.saveButton)
btnTest = view.findViewById(R.id.testButton)
setupRecyclerView()
observeConnections()
setupSaveButton()
setupTestButton()
}

private fun setupRecyclerView() {
adapter = ConnectionAdapter(
onItemClick = { config ->
selectedConfig = config
loadConfig(config)
onConfigSelected?.invoke(config)
},
onItemDoubleClick = { config ->
onConfigDoubleClick?.invoke(config)
},
onDeleteClick = { config ->
if (selectedConfig?.id == config.id) {
selectedConfig = null
clearInputs()
}
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
adapter.submitList(smbConfigs) {
// Restore selection after list update
selectedConfig?.let { selected ->
val position = smbConfigs.indexOfFirst { it.id == selected.id }
if (position >= 0) {
adapter.setSelectedPosition(position)
} else {
// Selected config no longer exists
selectedConfig = null
adapter.clearSelection()
clearInputs()
}
}
}
}
}

private fun setupSaveButton() {
btnSave.setOnClickListener {
saveConnection()
}
}

private fun setupTestButton() {
btnTest.setOnClickListener {
testConnection()
}
}

private fun testConnection() {
val folderAddress = etFolderAddress.text.toString().trim()
var username = etUsername.text.toString().trim()
var password = etPassword.text.toString().trim()

if (folderAddress.isEmpty()) {
Toast.makeText(requireContext(), "Please enter folder address", Toast.LENGTH_SHORT).show()
return
}

val preferenceManager = com.sza.fastmediasorter.utils.PreferenceManager(requireContext())
if (username.isEmpty()) {
username = preferenceManager.getDefaultUsername()
}
if (password.isEmpty()) {
password = preferenceManager.getDefaultPassword()
}

val (server, folder) = parseFolderAddress(folderAddress)

lifecycleScope.launch {
btnTest.isEnabled = false
btnTest.text = "Testing..."

try {
val smbClient = com.sza.fastmediasorter.network.SmbClient()
val connected = smbClient.connect(server, username, password)

if (!connected) {
com.sza.fastmediasorter.ui.dialogs.DiagnosticDialog.show(
requireContext(),
"Test FAILED",
"Connection failed: Unable to connect to server\n\nCheck:\n• Server IP correct?\n• Server running?\n• Firewall settings?",
false
)
btnTest.isEnabled = true
btnTest.text = "Test"
return@launch
}

val result = smbClient.getImageFiles(server, folder)

if (result.errorMessage != null) {
com.sza.fastmediasorter.ui.dialogs.DiagnosticDialog.show(
requireContext(),
"Test FAILED",
result.errorMessage,
false
)
} else {
val successMsg = "=== SMB CONNECTION TEST SUCCESS ===\n" +
"Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n" +
"Server: $server\n" +
"Folder: $folder\n\n" +
"=== RESULTS ===\n" +
"✓ Connection: SUCCESS\n" +
"✓ Authentication: SUCCESS\n" +
"✓ Folder Access: SUCCESS\n" +
"✓ Images Found: ${result.files.size}\n\n" +
"=== SYSTEM INFO ===\n" +
"Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n" +
"Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n" +
"Connection test completed successfully!"

com.sza.fastmediasorter.ui.dialogs.DiagnosticDialog.show(
requireContext(),
"Test PASSED ✓",
successMsg,
true
)
}
} catch (e: Exception) {
val diagnostic = "=== UNEXPECTED ERROR IN TEST ===\n" +
"Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n\n" +
"=== ERROR DETAILS ===\n" +
"Exception: ${e.javaClass.simpleName}\n" +
"Message: ${e.message ?: "No message"}\n" +
"Cause: ${e.cause?.javaClass?.simpleName ?: "None"}\n\n" +
"=== STACK TRACE ===\n" +
android.util.Log.getStackTraceString(e)

com.sza.fastmediasorter.ui.dialogs.DiagnosticDialog.show(
requireContext(),
"Test FAILED - Unexpected Error",
diagnostic,
false
)
}

btnTest.isEnabled = true
btnTest.text = "Test"
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
selectedConfig = null
adapter.clearSelection()
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
val savedConfig: ConnectionConfig
if (existingConfig != null) {
val config = existingConfig.copy(
name = name,
username = username,
password = password,
lastUsed = System.currentTimeMillis()
)
viewModel.updateConfig(config)
selectedConfig = config
savedConfig = config
onConfigSelected?.invoke(config)
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
val newId = viewModel.insertConfigAndGetId(config)
savedConfig = config.copy(id = newId)
selectedConfig = savedConfig
onConfigSelected?.invoke(savedConfig)
Toast.makeText(requireContext(), R.string.connection_saved, Toast.LENGTH_SHORT).show()
}
if (cbAddToDestinations.isChecked) {
val destinations = viewModel.sortDestinations.value ?: emptyList()
if (destinations.size < 10) {
val alreadyExists = destinations.any {
it.serverAddress == savedConfig.serverAddress && it.folderPath == savedConfig.folderPath
}
if (!alreadyExists) {
viewModel.addSortDestination(savedConfig.id, savedConfig.name)
Toast.makeText(requireContext(), "Added to destinations", Toast.LENGTH_SHORT).show()
}
} else {
Toast.makeText(requireContext(), "Destinations list is full (max 10)", Toast.LENGTH_SHORT).show()
}
}
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
