package com.idunnololz.summit.settings.navigation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.preferences.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsNavigationViewModel @Inject constructor(
    private val preferences: Preferences,
) : ViewModel() {
    val navBarOptions = MutableLiveData<List<NavBarDestId>?>()

    fun loadNavBarOptions() {
        val options = MutableList(5) { NavBarDestinations.None }
        preferences.navBarConfig.navBarDestinations.take(5).withIndex().forEach { (index, value) ->
            options[index] = value
        }

        navBarOptions.value = options
    }

    fun applyChanges() {
        val navBarOptions = navBarOptions.value ?: return
        preferences.navBarConfig = preferences.navBarConfig.copy(
            navBarDestinations = navBarOptions.distinct(),
        )
    }

    fun updateDestination(position: Int, destId: NavBarDestId) {
        val navBarOptions = navBarOptions.value ?: return
        this.navBarOptions.value = navBarOptions.toMutableList().apply {
            this[position] = destId
        }
    }
}
