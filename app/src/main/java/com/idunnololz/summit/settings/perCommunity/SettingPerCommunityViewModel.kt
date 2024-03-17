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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingPerCommunityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val perCommunityPreferences: PerCommunityPreferences,
    private val settings: PerCommunitySettings,
) : ViewModel() {

    private val communityConfigs = MutableStateFlow<List<PerCommunityPreferences.CommunityConfig>>(
        listOf(),
    )

    val baseSettings = makeBaseSettings()

    val data = StatefulLiveData<SettingData>()

    init {
        data.setValue(
            SettingData(
                baseSettings,
                getSettingValues(),
            ),
        )

        viewModelScope.launch {
            communityConfigs.collect {
                updateSettingItems()
            }
        }

        loadData()
    }

    private fun loadData() {
        data.setIsLoading()

        viewModelScope.launch {
            val allCommunityConfigs = perCommunityPreferences.getAllCommunityConfigs()

            if (communityConfigs.value == allCommunityConfigs) {
                updateSettingItems()
            } else {
                communityConfigs.emit(allCommunityConfigs)
            }
        }
    }

    private fun updateSettingItems() {
        val communityConfigs: List<PerCommunityPreferences.CommunityConfig> = communityConfigs.value
        data.setIsLoading()

        val allSettings = mutableListOf<SettingItem>()
        allSettings.addAll(baseSettings)

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
                getSettingValues(),
            ),
        )
    }

    private fun makeBaseSettings() =
        listOf(
            settings.usePerCommunitySettings,
            settings.clearPerCommunitySettings,
        )

    fun onSettingClick(setting: SettingItem) {
        when (setting.id) {
            settings.usePerCommunitySettings.id -> {
                preferences.usePerCommunitySettings = !preferences.usePerCommunitySettings
                updateSettingItems()
            }
            settings.clearPerCommunitySettings.id -> {
                perCommunityPreferences.clear()
                loadData()
            }
        }
    }

    private fun getSettingValues() = mapOf(
        settings.usePerCommunitySettings.id to preferences.usePerCommunitySettings,
    )

    data class SettingData(
        val settingItems: List<SettingItem>,
        val settingValues: Map<Int, Any?>,
    )
}
