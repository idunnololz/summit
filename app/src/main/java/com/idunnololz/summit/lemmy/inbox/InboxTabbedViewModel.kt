package com.idunnololz.summit.lemmy.inbox

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.SlidingPaneController
import com.idunnololz.summit.notifications.NotificationsManager
import com.idunnololz.summit.util.PageItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxTabbedViewModel @Inject constructor(
    private val context: Application,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
    private val notificationsManager: NotificationsManager,
) : ViewModel(), SlidingPaneController.PostViewPagerViewModel {

    class InboxItemWithInstance(
        val inboxItem: InboxItem,
        val instance: String,
    )

    var pageItems = MutableLiveData<List<PageItem>>()

    val notificationInboxItem = MutableLiveData<InboxItemWithInstance>()

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

    fun findInboxItemFromNotificationId(notificationId: Int) {
        viewModelScope.launch {
            val inboxEntry = notificationsManager.findInboxItem(notificationId)
            val inboxItem = inboxEntry?.inboxItem

            if (inboxEntry != null && inboxItem != null) {
                val inboxItemAccount = accountManager.getAccounts().firstOrNull {
                    it.fullName == inboxEntry.accountFullName
                }

                if (inboxItemAccount != null) {
                    accountManager.setCurrentAccount(inboxItemAccount)

                    notificationInboxItem.postValue(
                        InboxItemWithInstance(
                            inboxItem,
                            inboxItemAccount.instance,
                        ),
                    )
                }
            }
        }
    }
}
