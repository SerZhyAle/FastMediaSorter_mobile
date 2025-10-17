package com.sza.fastmediasorter.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R
import com.sza.fastmediasorter.data.ConnectionConfig

class ConnectionAdapter(
    private val onItemClick: (ConnectionConfig) -> Unit,
    private val onDeleteClick: (ConnectionConfig) -> Unit
) : ListAdapter<ConnectionConfig, ConnectionAdapter.ConnectionViewHolder>(ConnectionDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_connection, parent, false)
        return ConnectionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.connectionName)
        private val detailsText: TextView = itemView.findViewById(R.id.connectionDetails)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        
        fun bind(config: ConnectionConfig) {
            nameText.text = config.name
            detailsText.text = "${config.serverAddress}/${config.folderPath}"
            
            itemView.setOnClickListener {
                onItemClick(config)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(config)
            }
        }
    }
    
    class ConnectionDiffCallback : DiffUtil.ItemCallback<ConnectionConfig>() {
        override fun areItemsTheSame(oldItem: ConnectionConfig, newItem: ConnectionConfig): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ConnectionConfig, newItem: ConnectionConfig): Boolean {
            return oldItem == newItem
        }
    }
}
