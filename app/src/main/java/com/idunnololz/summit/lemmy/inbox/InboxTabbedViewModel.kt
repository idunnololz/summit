package com.idunnololz.summit.lemmy.inbox

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Either
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.SlidingPaneController
import com.idunnololz.summit.util.PageItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InboxTabbedViewModel @Inject constructor(
    private val context: Application,
    private val accountInfoManager: AccountInfoManager,
) : ViewModel(), SlidingPaneController.PostViewPagerViewModel {

    var pageItems = MutableLiveData<List<PageItem>>()

    override var lastSelectedItem: Either<PostRef, CommentRef>? = null

    init {
        addFrag(
            InboxFragment::class.java,
            InboxViewModel.PageType.All.getName(context),
            InboxFragmentArgs(InboxViewModel.PageType.All).toBundle(),
        )
    }

    fun updateUnreadCount() {
        accountInfoManager.updateUnreadCount()
    }

    fun openMessage(item: InboxItem, instance: String) {
        removeAllButFirst()
        addFrag(
            MessageFragment::class.java,
            "Message",
            MessageFragmentArgs(item, instance).toBundle(),
        )
    }

    fun removeAllButFirst() {
        pageItems.value = pageItems.value?.take(1) ?: listOf()
    }

    private fun addFrag(
        clazz: Class<*>,
        title: String,
        args: Bundle? = null,
        @DrawableRes drawableRes: Int? = null,
    ) {
        val currentItems = pageItems.value ?: listOf()
        pageItems.value =
            currentItems + PageItem(View.generateViewId().toLong(), clazz, args, title, drawableRes)
    }
}
