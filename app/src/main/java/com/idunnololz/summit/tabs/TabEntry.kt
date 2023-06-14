package com.idunnololz.summit.tabs

import android.net.Uri
import androidx.navigation.NavController
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.idunnololz.summit.R
import com.idunnololz.summit.main.MainFragmentArgs
import com.idunnololz.summit.post.PostFragmentArgs
import com.idunnololz.summit.reddit_objects.ListingItem
import com.idunnololz.summit.tabs.TabEntryType.TYPE_PAGE
import com.idunnololz.summit.util.Utils

@Entity(tableName = "tabs")
data class TabEntry(
    @PrimaryKey
    val tabId: String,
    val sortId: Long,
    val type: Int,
    val previewPath: String?,
    val previewSignature: String?,
    val extras: String
) {
    companion object {
        fun makePageTabEntry(
            tabId: String,
            sortId: Long,
            previewPath: String?,
            previewSignature: String?,
            pageDetails: PageDetails
        ): TabEntry =
            TabEntry(
                tabId,
                sortId,
                TYPE_PAGE,
                previewPath,
                previewSignature,
                Utils.gson.toJson(pageDetails)
            )
    }

    fun toTabItem(): TabItem? = when (type) {
        TYPE_PAGE -> TabItem.PageTabItem(
            tabId,
            sortId,
            previewPath,
            previewSignature,
            Utils.gson.fromJson(extras, PageDetails::class.java)
        )
        else -> null
    }
}

sealed class TabItem(
    open val tabId: String,
    open val sortId: Long,
    open var previewPath: String?,
    open var previewSignature: String?
) {
    abstract fun toTabEntry(): TabEntry

    data class PageTabItem internal constructor(
        override val tabId: String,
        override val sortId: Long,
        override var previewPath: String?,
        override var previewSignature: String?,
        val pageDetails: PageDetails
    ) : TabItem(tabId, sortId, previewPath, previewSignature) {

        override fun toTabEntry(): TabEntry =
            TabEntry.makePageTabEntry(tabId, sortId, previewPath, previewSignature, pageDetails)

        companion object {
            fun newTabItem(tabId: String, url: String): PageTabItem =
                PageTabItem(tabId, 0, null, null, PageDetails(url, false, null))
        }
    }
}

object TabEntryType {
    const val TYPE_PAGE = 1
}

data class PageDetails(
    val url: String,
    val jumpToComments: Boolean,
    val originalPost: ListingItem?
)

fun TabEntry.navigate(navController: NavController) {
    when (type) {
        TYPE_PAGE -> {
            val pageDetails = Utils.gson.fromJson(extras, PageDetails::class.java)
            val uri = Uri.parse(pageDetails.url)
            if (uri.pathSegments.size == 2 && uri.pathSegments[0] == "r") {
                navController.navigate(
                    R.id.mainFragment,
                    MainFragmentArgs(pageDetails.url).toBundle()
                )
            } else {
                navController.navigate(
                    R.id.postFragment, PostFragmentArgs(
                        url = pageDetails.url,
                        jumpToComments = pageDetails.jumpToComments,
                        originalPost = pageDetails.originalPost
                    ).toBundle()
                )
            }
        }
    }
}