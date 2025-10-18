package com.sza.fastmediasorter.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.sza.fastmediasorter.utils.PreferenceManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.hide()
        
        setupToolbar()
        setupTabs()
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
                0 -> "Sort to.."
                1 -> "Slideshow"
                2 -> "Settings"
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
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SortHelpFragment()
            1 -> SlideshowHelpFragment()
            2 -> SettingsFragment()
            else -> SortHelpFragment()
        }
    }
}

class SlideshowHelpFragment : Fragment() {
    private var _binding: FragmentSlideshowHelpBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSlideshowHelpBinding.inflate(inflater, container, false)
        return binding.root
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
        
        // Load show controls setting
        binding.showControlsCheckbox.isChecked = preferenceManager.isShowControls()
        
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
        
        // Save show controls on change
        binding.showControlsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.setShowControls(isChecked)
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
