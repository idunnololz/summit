package com.idunnololz.summit.util

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDrawableCompat

data class PageItem(
    val id: Long,
    val clazz: Class<*>,
    val args: Bundle?,
    val title: String,
    @DrawableRes val drawable: Int? = null,
)

class ViewPagerAdapter(
    private val context: Context,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle), TabLayoutMediator.TabConfigurationStrategy {

    private val fragmentFactory: FragmentFactory = fragmentManager.fragmentFactory
    private val items = ArrayList<PageItem>()

    override fun createFragment(position: Int): Fragment {
        val fragment = fragmentFactory.instantiate(
            context::class.java.classLoader!!,
            items[position].clazz.name,
        )
        fragment.arguments = items[position].args

        // DO NOT DO THIS. New ViewPager2 made this method terribly inconsistent
        // createdFragments.put(position, fragment)

        return fragment
    }

    override fun getItemCount(): Int = items.size

    fun addFrag(
        clazz: Class<*>,
        title: String,
        args: Bundle? = null,
        @DrawableRes drawableRes: Int? = null,
    ) {
        items.add(PageItem(View.generateViewId().toLong(), clazz, args, title, drawableRes))
    }

    fun getTitleForPosition(position: Int): CharSequence = items[position].title

    override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
        val item = items[position]
        tab.text = item.title
        if (item.drawable != null) {
            tab.icon = context.getDrawableCompat(item.drawable)?.apply {
                setTint(context.getColorCompat(R.color.colorTextTitle))
            }
        }
    }
}
