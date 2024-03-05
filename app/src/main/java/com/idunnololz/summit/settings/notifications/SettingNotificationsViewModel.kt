package com.idunnololz.summit.settings.notifications

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.notifications.NotificationsManager
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingNotificationsViewModel @Inject constructor(
    private val notificationsManager: NotificationsManager,
) : ViewModel() {

    fun onNotificationSettingsChanged() {
        notificationsManager.onPreferencesChanged()
    }
}