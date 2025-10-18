package com.sza.fastmediasorter.ui.local

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sza.fastmediasorter.R

class LocalFolderAdapter(
    private val folders: List<LocalFolder>,
    private val onFolderClick: (LocalFolder, Boolean) -> Unit
) : RecyclerView.Adapter<LocalFolderAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFolderIcon: TextView = view.findViewById(R.id.tvFolderIcon)
        val tvFolderName: TextView = view.findViewById(R.id.tvFolderName)
        val tvFolderCount: TextView = view.findViewById(R.id.tvFolderCount)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_local_folder, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        holder.tvFolderIcon.text = folder.icon
        holder.tvFolderName.text = folder.name
        holder.tvFolderCount.text = "(${folder.count})"
        
        val gestureDetector = GestureDetector(holder.itemView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onFolderClick(folder, false)
                return true
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onFolderClick(folder, true)
                return true
            }
        })
        
        holder.itemView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            v.performClick()
            true
        }
    }
    
    override fun getItemCount() = folders.size
}
