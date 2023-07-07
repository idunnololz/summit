package com.idunnololz.summit.main

import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import coil.load
import com.google.android.material.chip.Chip
import com.google.android.material.imageview.ShapeableImageView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.view.TabsImageButton

class LemmyAppBarController(
    private val mainActivity: MainActivity,
    v: View
) {

    private val TAG = "RedditAppBarController"

    private val context = mainActivity

    private val customActionBar: ViewGroup = v.findViewById(R.id.customActionBar)
    private val accountChip: Chip = v.findViewById(R.id.account_chip)
    private val accountImageView: ShapeableImageView = v.findViewById(R.id.account_image_view)
    private val communityTextView: Chip = v.findViewById(R.id.communityTextView)
    private val pageTextView: TextView = v.findViewById(R.id.pageTextView)

    private var currentCommunity: CommunityRef? = null
    private var defaultCommunity: CommunityRef? = null

    fun setup(
        communitySelectedListener: CommunitySelectedListener,
        onAccountClick: (currentAccount: Account?) -> Unit,
    ) {
        accountChip.setOnClickListener {
            onAccountClick(null)
        }
        accountImageView.setOnClickListener {
            val account = it.tag as? Account
            onAccountClick(account)
        }
        communityTextView.setOnClickListener {
            val controller = mainActivity.showCommunitySelector()
            controller.onCommunitySelectedListener = communitySelectedListener
        }
        customActionBar.setOnClickListener {
            val controller = mainActivity.showCommunitySelector()
            controller.onCommunitySelectedListener = communitySelectedListener
        }
    }

    fun showCommunitySelector() {
        communityTextView.performClick()
    }

    fun setCommunity(communityRef: CommunityRef?, isHome: Boolean) {
        currentCommunity = communityRef

        updateCommunityButton()
    }

    fun setDefaultCommunity(defaultCommunity: CommunityRef?) {
        this.defaultCommunity = defaultCommunity

        updateCommunityButton()
    }

    private fun updateCommunityButton() {
        communityTextView.text = currentCommunity?.getName(context) ?: ""
        val isHome = currentCommunity == defaultCommunity

        if (isHome) {
            communityTextView.isChipIconVisible = true
            communityTextView.setChipIconResource(R.drawable.baseline_home_18)
        } else {
            communityTextView.isChipIconVisible = false
        }
    }

    fun setPageIndex(
        pageIndex: Int,
        onPageSelectedListener: (pageIndex: Int) -> Unit
    ) {
        pageTextView.text = context.getString(R.string.page_format, pageIndex + 1)

        pageTextView.setOnClickListener {
            PopupMenu(context, it).apply {
                menu.apply {
                    for (i in 0..pageIndex) {
                        add(0, i, 0, context.getString(R.string.page_format, i + 1))
                    }
                }
                setOnMenuItemClickListener {
                    Log.d(TAG, "Page selected: ${it.itemId}")
                    onPageSelectedListener(it.itemId)
                    true
                }
            }.show()
        }
    }

    fun clearPageIndex() {
        pageTextView.text = ""
        pageTextView.setOnClickListener(null)
    }

    fun onAccountChanged(it: AccountView?) {
        if (it == null) {
            accountChip.visibility = View.VISIBLE
            accountImageView.visibility = View.INVISIBLE
//            accountButton.setImageResource(R.drawable.baseline_add_24)
//            val padding = Utils.convertDpToPixel(10f).toInt()
//            accountButton.setPadding(padding, padding, padding, padding)
//            accountButton.imageTintList = ColorStateList.valueOf(context.getColorFromAttribute(
//                androidx.appcompat.R.attr.colorControlNormal))
        } else {
            accountChip.visibility = View.INVISIBLE
            accountImageView.visibility = View.VISIBLE

            accountImageView.tag = it.account
            accountImageView.load(it.profileImage) {
                placeholder(R.drawable.baseline_person_24)
            }
        }
    }
}