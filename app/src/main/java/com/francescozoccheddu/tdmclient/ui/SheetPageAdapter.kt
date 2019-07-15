package com.francescozoccheddu.tdmclient.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter

class SheetPageAdapter(fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager) {

    private val pages = mutableListOf<Fragment>()

    operator fun get(index: Int) = pages[index]

    operator fun set(index: Int, value: Fragment) {
        pages[index] = value
        notifyDataSetChanged()
    }

    operator fun plusAssign(value: Fragment) {
        pages.add(value)
        notifyDataSetChanged()
    }

    operator fun minusAssign(value: Fragment) {
        pages.remove(value)
        notifyDataSetChanged()
    }

    fun remove(index: Int) {
        pages.removeAt(index)
        notifyDataSetChanged()
    }

    val length
        get() = pages.size

    override fun getCount(): Int = length

    override fun getItem(position: Int) = pages[position]

    override fun getItemPosition(item: Any): Int {
        val index = pages.indexOf(item)
        return if (index == -1) PagerAdapter.POSITION_NONE else index
    }

    override fun getPageTitle(position: Int) = "Page N°${position + 1}"

}
