package com.idunnololz.summit.lemmy.inbox

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.account.info.AccountInfoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InboxTabbedViewModel @Inject constructor(
    private val accountInfoManager: AccountInfoManager
) : ViewModel() {

    var pagePosition: Int = 0

    fun updateUnreadCount() {
        accountInfoManager.updateUnreadCount()
    }
}