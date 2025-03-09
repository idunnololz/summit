package com.idunnololz.summit.settings.perCommunity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.perCommunity.PerCommunityPreferences
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.PerCommunitySettings
import com.idunnololz.summit.settings.SettingItem
import com.idunnololz.summit.settings.SubgroupItem
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsPerCommunityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val perCommunityPreferences: PerCommunityPreferences,
) : ViewModel() {

    private val communityConfigs = MutableStateFlow<List<PerCommunityPreferences.CommunityConfig>>(
        listOf(),
    )

    val data = StatefulLiveData<SettingData>()

    fun loadData(settings: PerCommunitySettings) {
        data.setIsLoading()

        viewModelScope.launch {
            val allCommunityConfigs = perCommunityPreferences.getAllCommunityConfigs()

            if (communityConfigs.value == allCommunityConfigs) {
                updateSettingItems(settings)
            } else {
                communityConfigs.value = allCommunityConfigs
                updateSettingItems(settings)
            }
        }
    }

    private fun updateSettingItems(settings: PerCommunitySettings) {
        val communityConfigs: List<PerCommunityPreferences.CommunityConfig> = communityConfigs.value
        data.setIsLoading()

        val allSettings = mutableListOf<SettingItem>()
        allSettings.addAll(makeBaseSettings(settings))

        if (communityConfigs.isNotEmpty()) {
            allSettings.add(
                SubgroupItem(
                    context.getString(R.string.per_community_overrides),
                    listOf(),
                ),
            )
            communityConfigs.mapTo(allSettings) {
                BasicSettingItem(
                    null,
                    it.communityRef.getLocalizedFullName(context),
                    context.getString(R.string.view_type_format, it.layout),
                )
            }
        }

        data.postValue(
            SettingData(
                allSettings,
                getSettingValues(settings),
            ),
        )
    }

    private fun makeBaseSettings(settings: PerCommunitySettings) = listOf(
        settings.usePerCommunitySettings,
        settings.clearPerCommunitySettings,
    )

    fun onSettingClick(settings: PerCommunitySettings, settingClicked: SettingItem) {
        when (settingClicked.id) {
            settings.usePerCommunitySettings.id -> {
                preferences.usePerCommunitySettings = !preferences.usePerCommunitySettings
                updateSettingItems(settings)
            }
            settings.clearPerCommunitySettings.id -> {
                perCommunityPreferences.clear()
                loadData(settings)
            }
        }
    }

    private fun getSettingValues(settings: PerCommunitySettings) = mapOf(
        settings.usePerCommunitySettings.id to preferences.usePerCommunitySettings,
    )

    data class SettingData(
        val settingItems: List<SettingItem>,
        val settingValues: Map<Int, Any?>,
    )
}
