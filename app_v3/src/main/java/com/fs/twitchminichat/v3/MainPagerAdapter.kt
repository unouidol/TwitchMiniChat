package com.fs.twitchminichat.v3

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 1 // per ora solo pagina Login

    override fun createFragment(position: Int): Fragment {
        return LoginFragment()
    }
}
