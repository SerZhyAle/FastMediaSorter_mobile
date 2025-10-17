package com.sza.fastmediasorter.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sza.fastmediasorter.ui.local.LocalFoldersFragment
import com.sza.fastmediasorter.ui.network.NetworkFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
override fun getItemCount(): Int = 2

override fun createFragment(position: Int): Fragment {
return when (position) {
0 -> LocalFoldersFragment()
1 -> NetworkFragment()
else -> throw IllegalArgumentException("Invalid position $position")
}
}
}
