package com.idunnololz.summit.main

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import coil.load
import com.google.android.material.chip.Chip
import com.google.android.material.imageview.ShapeableImageView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.databinding.CustomAppBarBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder

class LemmyAppBarController(
    private val mainActivity: MainActivity,
    private val binding: CustomAppBarBinding,
) {

    private val TAG = "RedditAppBarController"

    private val context = mainActivity

    private val customActionBar: ViewGroup = binding.customActionBar
    private val accountChip: Chip = binding.accountChip
    private val accountImageView: ShapeableImageView = binding.accountImageView
    private val communityTextView: Chip = binding.communityTextView
    private val pageTextView: TextView = binding.pageTextView

    private var currentCommunity: CommunityRef? = null
    private var defaultCommunity: CommunityRef? = null

    fun setup(
        communitySelectedListener: CommunitySelectedListener,
        onAccountClick: (currentAccount: Account?) -> Unit,
        onSortOrderClick: () -> Unit,
    ) {
        accountChip.setOnClickListener {
            onAccountClick(null)
        }
        accountImageView.setOnClickListener {
            val account = it.tag as? Account
            onAccountClick(account)
        }
        communityTextView.setOnClickListener {
            val controller = mainActivity.showCommunitySelector(currentCommunity)
            controller.onCommunitySelectedListener = communitySelectedListener
        }
        customActionBar.setOnClickListener {
            val controller = mainActivity.showCommunitySelector(currentCommunity)
            controller.onCommunitySelectedListener = communitySelectedListener
        }
        binding.communitySortOrder.setOnClickListener {
            onSortOrderClick()
        }
    }

    fun showCommunitySelector() {
        communityTextView.performClick()
    }

    fun setCommunity(communityRef: CommunityRef?) {
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
        onPageSelectedListener: (pageIndex: Int) -> Unit,
    ) {
        binding.communitySortOrder.visibility = View.GONE

        pageTextView.text = context.getString(R.string.page_format, (pageIndex + 1).toString())

        pageTextView.setOnClickListener {
            PopupMenu(context, it).apply {
                menu.apply {
                    for (i in 0..pageIndex) {
                        add(0, i, 0, context.getString(R.string.page_format, (i + 1).toString()))
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

    fun setIsInfinity(isInfinity: Boolean) {
        if (isInfinity) {
            binding.communitySortOrder.visibility = View.VISIBLE
            pageTextView.visibility = View.GONE
        } else {
            binding.communitySortOrder.visibility = View.GONE
            pageTextView.visibility = View.VISIBLE
        }
    }

    fun setSortOrder(communitySortOrder: CommunitySortOrder) {
        binding.communitySortOrder.text =
            when (communitySortOrder) {
                CommunitySortOrder.Active -> context.getString(R.string.sort_order_active)
                CommunitySortOrder.Hot -> context.getString(R.string.sort_order_hot)
                CommunitySortOrder.MostComments -> context.getString(R.string.sort_order_most_comments)
                CommunitySortOrder.New -> context.getString(R.string.sort_order_new)
                CommunitySortOrder.NewComments -> context.getString(R.string.sort_order_new_comments)
                CommunitySortOrder.Old -> context.getString(R.string.sort_order_old)
                is CommunitySortOrder.TopOrder ->
                    context.getString(
                        R.string.sort_order_top_format,
                        when (communitySortOrder.timeFrame) {
                            CommunitySortOrder.TimeFrame.LastHour ->
                                context.getString(R.string.time_frame_last_hour)
                            CommunitySortOrder.TimeFrame.LastSixHour ->
                                context.getString(R.string.time_frame_last_hours_format, "6")
                            CommunitySortOrder.TimeFrame.LastTwelveHour ->
                                context.getString(R.string.time_frame_last_hours_format, "12")
                            CommunitySortOrder.TimeFrame.Today ->
                                context.getString(R.string.time_frame_today)
                            CommunitySortOrder.TimeFrame.ThisWeek ->
                                context.getString(R.string.time_frame_this_week)
                            CommunitySortOrder.TimeFrame.ThisMonth ->
                                context.getString(R.string.time_frame_this_month)
                            CommunitySortOrder.TimeFrame.LastThreeMonth ->
                                context.getString(R.string.time_frame_last_months_format, "3")
                            CommunitySortOrder.TimeFrame.LastSixMonth ->
                                context.getString(R.string.time_frame_last_months_format, "6")
                            CommunitySortOrder.TimeFrame.LastNineMonth ->
                                context.getString(R.string.time_frame_last_months_format, "9")
                            CommunitySortOrder.TimeFrame.ThisYear ->
                                context.getString(R.string.time_frame_this_year)
                            CommunitySortOrder.TimeFrame.AllTime ->
                                context.getString(R.string.time_frame_all_time)
                        },
                    )
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
                allowHardware(false)
                placeholder(R.drawable.baseline_person_24)
            }
        }
    }
}
