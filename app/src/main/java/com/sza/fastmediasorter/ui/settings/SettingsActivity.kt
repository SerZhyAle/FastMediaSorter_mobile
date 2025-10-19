package com.sza.fastmediasorter.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.data.ConnectionConfig
import com.sza.fastmediasorter.databinding.ActivitySettingsBinding
import com.sza.fastmediasorter.databinding.FragmentSlideshowHelpBinding
import com.sza.fastmediasorter.databinding.FragmentSortHelpBinding
import com.sza.fastmediasorter.databinding.FragmentSettingsBinding
import com.sza.fastmediasorter.ui.ConnectionViewModel
import com.sza.fastmediasorter.ui.welcome.WelcomeActivity
import com.sza.fastmediasorter.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.hide()
        
        setupToolbar()
        setupTabs()
        
        // Check for initial tab from intent
        val initialTab = intent.getIntExtra("initialTab", 0)
        if (initialTab in 0..2) {
            binding.viewPager.setCurrentItem(initialTab, false)
        }
    }
    
    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupTabs() {
        val adapter = SettingsPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_sort_to)
                1 -> getString(R.string.tab_slideshow)
                2 -> getString(R.string.tab_video)
                3 -> getString(R.string.tab_settings)
                else -> ""
            }
        }.attach()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

class SettingsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 4
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SortHelpFragment()
            1 -> SlideshowHelpFragment()
            2 -> VideoSettingsFragment()
            3 -> SettingsFragment()
            else -> SortHelpFragment()
        }
    }
}

class SlideshowHelpFragment : Fragment() {
    private var _binding: FragmentSlideshowHelpBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSlideshowHelpBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager = PreferenceManager(requireContext())
        updateVisibility()
    }
    
    override fun onResume() {
        super.onResume()
        updateVisibility()
    }
    
    private fun updateVisibility() {
        val showControls = preferenceManager.isShowControls()
        binding.touchZonesContainer.visibility = if (showControls) View.GONE else View.VISIBLE
        binding.buttonControlsInfo.visibility = if (showControls) View.VISIBLE else View.GONE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SortHelpFragment : Fragment() {
    private var _binding: FragmentSortHelpBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ConnectionViewModel
    private lateinit var adapter: SortDestinationAdapter
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSortHelpBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[ConnectionViewModel::class.java]
        
        setupRecyclerView()
        setupObservers()
        
        binding.addDestinationButton.setOnClickListener {
            showAddDestinationDialog()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = SortDestinationAdapter(
            onMoveUp = { config -> moveDestination(config, -1) },
            onMoveDown = { config -> moveDestination(config, 1) },
            onDelete = { config -> deleteDestination(config) }
        )
        
        binding.destinationsRecyclerView.adapter = adapter
        binding.destinationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun setupObservers() {
        viewModel.sortDestinations.observe(viewLifecycleOwner) { destinations ->
            adapter.submitList(destinations)
            binding.emptyText.visibility = if (destinations.isEmpty()) View.VISIBLE else View.GONE
            binding.destinationsRecyclerView.visibility = if (destinations.isEmpty()) View.GONE else View.VISIBLE
        }
    }
    
    private fun showAddDestinationDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_sort_destination, null)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        val connectionsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.connectionsRecyclerView)
        val sortNameInput = dialogView.findViewById<EditText>(R.id.sortNameInput)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val addButton = dialogView.findViewById<Button>(R.id.addButton)
        
        var selectedConfig: ConnectionConfig? = null
        
        val connectionAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            private var connections = listOf<ConnectionConfig>()
            
            fun setConnections(list: List<ConnectionConfig>) {
                connections = list
                notifyDataSetChanged()
            }
            
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val textView = TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(16, 16, 16, 16)
                    textSize = 14f
                }
                return object : RecyclerView.ViewHolder(textView) {}
            }
            
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val config = connections[position]
                val textView = holder.itemView as TextView
                textView.text = "${config.name}\n${config.serverAddress}\\${config.folderPath}"
                textView.setBackgroundColor(if (selectedConfig?.id == config.id) 0xFFE0E0E0.toInt() else 0xFFFFFFFF.toInt())
                
                textView.setOnClickListener {
                    selectedConfig = config
                    notifyDataSetChanged()
                    addButton.isEnabled = sortNameInput.text.isNotEmpty()
                }
            }
            
            override fun getItemCount() = connections.size
        }
        
        connectionsRecyclerView.adapter = connectionAdapter
        connectionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        viewModel.allConfigs.observe(viewLifecycleOwner) { configs ->
            viewModel.sortDestinations.value?.let { destinations ->
                val usedIds = destinations.map { it.id }.toSet()
                val available = configs.filter { 
                    it.id !in usedIds && it.type != "LOCAL_CUSTOM"
                }
                connectionAdapter.setConnections(available)
            }
        }
        
        sortNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                addButton.isEnabled = s?.isNotEmpty() == true && selectedConfig != null
            }
        })
        
        cancelButton.setOnClickListener { dialog.dismiss() }
        
        addButton.setOnClickListener {
            selectedConfig?.let { config ->
                val sortName = sortNameInput.text.toString().trim()
                if (sortName.isNotEmpty()) {
                    viewModel.addSortDestination(config.id, sortName)
                    dialog.dismiss()
                }
            }
        }
        
        dialog.show()
    }
    
    private fun moveDestination(config: ConnectionConfig, direction: Int) {
        viewModel.moveSortDestination(config, direction)
    }
    
    private fun deleteDestination(config: ConnectionConfig) {
        viewModel.removeSortDestination(config.id)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class VideoSettingsFragment : Fragment() {
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var videoEnabledCheckbox: android.widget.CheckBox
    private lateinit var maxVideoSizeEdit: EditText
    private lateinit var showVideoErrorDetailsCheckbox: android.widget.CheckBox
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_video_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferenceManager = PreferenceManager(requireContext())
        
        videoEnabledCheckbox = view.findViewById(R.id.videoEnabledCheckbox)
        maxVideoSizeEdit = view.findViewById(R.id.maxVideoSizeEdit)
        showVideoErrorDetailsCheckbox = view.findViewById(R.id.showVideoErrorDetailsCheckbox)
        
        // Load current settings
        videoEnabledCheckbox.isChecked = preferenceManager.isVideoEnabled()
        maxVideoSizeEdit.setText(preferenceManager.getMaxVideoSizeMb().toString())
        maxVideoSizeEdit.isEnabled = videoEnabledCheckbox.isChecked
        showVideoErrorDetailsCheckbox.isChecked = preferenceManager.isShowVideoErrorDetails()
        
        // Enable/disable size field based on checkbox
        videoEnabledCheckbox.setOnCheckedChangeListener { _, isChecked ->
            maxVideoSizeEdit.isEnabled = isChecked
            preferenceManager.setVideoEnabled(isChecked)
        }
        
        // Save error details checkbox
        showVideoErrorDetailsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setShowVideoErrorDetails(isChecked)
        }
        
        // Validate and save video size
        maxVideoSizeEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty()) {
                    try {
                        val value = text.toInt()
                        if (value in 1..999999) {
                            preferenceManager.setMaxVideoSizeMb(value)
                        } else {
                            // Show error hint
                            maxVideoSizeEdit.error = "Range: 1-999999 MB"
                        }
                    } catch (e: NumberFormatException) {
                        maxVideoSizeEdit.error = "Invalid number"
                    }
                }
            }
        })
    }
}

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updateMediaAccessButton()
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferenceManager = PreferenceManager(requireContext())
        
        // Load default credentials
        binding.defaultUsernameInput.setText(preferenceManager.getDefaultUsername())
        binding.defaultPasswordInput.setText(preferenceManager.getDefaultPassword())
        
        // Load allow move setting
        binding.allowMoveCheckbox.isChecked = preferenceManager.isAllowMove()
        
        // Load allow copy setting
        binding.allowCopyCheckbox.isChecked = preferenceManager.isAllowCopy()
        
        // Load allow delete setting
        binding.allowDeleteCheckbox.isChecked = preferenceManager.isAllowDelete()
        
        // Load confirm delete setting
        binding.confirmDeleteCheckbox.isChecked = preferenceManager.isConfirmDelete()
        
        // Load allow rename setting
        binding.allowRenameCheckbox.isChecked = preferenceManager.isAllowRename()
        
        // Load show controls setting
        binding.showControlsCheckbox.isChecked = preferenceManager.isShowControls()
        
        // Load keep screen on setting
        binding.keepScreenOnCheckbox.isChecked = preferenceManager.isKeepScreenOn()
        
        // Setup User Guide button
        binding.userGuideButton.setOnClickListener {
            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            intent.putExtra("isFirstLaunch", false)
            startActivity(intent)
        }
        
        // Setup View Logs button
        binding.viewLogsButton.setOnClickListener {
            showLogsDialog()
        }
        
        // Setup media access button
        updateMediaAccessButton()
        binding.requestMediaAccessButton.setOnClickListener {
            requestMediaPermission()
        }
        
        // Save on focus loss
        binding.defaultUsernameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveDefaultUsername()
            }
        }
        
        binding.defaultPasswordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveDefaultPassword()
            }
        }
        
        // Save allow move on change
        binding.allowMoveCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setAllowMove(isChecked)
        }
        
        // Save allow copy on change
        binding.allowCopyCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setAllowCopy(isChecked)
        }
        
        // Save allow delete on change
        binding.allowDeleteCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setAllowDelete(isChecked)
        }
        
        // Save confirm delete on change
        binding.confirmDeleteCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setConfirmDelete(isChecked)
        }
        
        // Save allow rename on change
        binding.allowRenameCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setAllowRename(isChecked)
        }
        
        // Save show controls on change
        binding.showControlsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setShowControls(isChecked)
        }
        
        // Save keep screen on on change
        binding.keepScreenOnCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setKeepScreenOn(isChecked)
        }
    }
    
    private fun updateMediaAccessButton() {
        val hasPermission = com.sza.fastmediasorter.network.LocalStorageClient.hasMediaPermission(requireContext())
        binding.requestMediaAccessButton.isEnabled = !hasPermission
        binding.requestMediaAccessButton.text = if (hasPermission) "Media Access Granted ✓" else "Grant Media Access"
    }
    
    private fun requestMediaPermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissionLauncher.launch(permission)
    }
    
    private fun saveDefaultUsername() {
        val username = binding.defaultUsernameInput.text.toString().trim()
        preferenceManager.setDefaultUsername(username)
    }
    
    private fun saveDefaultPassword() {
        val password = binding.defaultPasswordInput.text.toString().trim()
        preferenceManager.setDefaultPassword(password)
    }
    
    private fun showLogsDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val logs = getLast1000LogLines()
            
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.logs_title))
                    .setMessage(logs.ifEmpty { getString(R.string.logs_empty) })
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .setNeutralButton(getString(R.string.copy_logs)) { _, _ ->
                        copyLogsToClipboard(logs)
                    }
                    .show()
            }
        }
    }
    
    private fun getLast1000LogLines(): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            
            val allLines = bufferedReader.readLines()
            val appLines = allLines.filter { line ->
                line.contains("fastmediasorter", ignoreCase = true) ||
                line.contains("FastMediaSorter", ignoreCase = true) ||
                line.contains("SortActivity", ignoreCase = true) ||
                line.contains("SlideshowActivity", ignoreCase = true) ||
                line.contains("MainActivity", ignoreCase = true) ||
                line.contains("SmbClient", ignoreCase = true) ||
                line.contains("com.sza.fastmediasorter", ignoreCase = true)
            }
            
            val last1000 = if (appLines.size > 1000) {
                appLines.takeLast(1000)
            } else {
                appLines
            }
            
            last1000.joinToString("\n")
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }
    
    private fun copyLogsToClipboard(logs: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Application Logs", logs)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.logs_copied), Toast.LENGTH_SHORT).show()
    }
    
    override fun onPause() {
        super.onPause()
        // Save when leaving fragment (tab change, back)
        saveDefaultUsername()
        saveDefaultPassword()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
