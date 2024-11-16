package com.idunnololz.summit.settings.backupAndRestore

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.AllSettings
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewCurrentSettingsViewModel @Inject constructor(
    private val preferences: Preferences,
    private val allSettings: AllSettings,
) : ViewModel() {

    val model = StatefulLiveData<Model>()

    data class Model(
        val settingsDataPreview: SettingsDataPreview,
    )

    fun generatePreviewFromSettingsJson() {
        val keyToSettingItems = allSettings.generateMapFromKeysToRelatedSettingItems()

        val currentSettingsJson = preferences.asJson()

        val allKeys = currentSettingsJson.keys().asSequence()

        // diff current vs the json we are importing
        val diffs = mutableListOf<Diff>()
        for (key in allKeys) {
            val currentValue = currentSettingsJson.opt(key) ?: continue
            diffs.add(Diff(DiffType.Added, "null", currentValue.toString()))
        }

        val settingsPreview = currentSettingsJson.keys().asSequence()
            .associateWith { (currentSettingsJson.opt(it)?.toString() ?: "null") }
        val keyToType = currentSettingsJson.keys().asSequence()
            .associateWith { (currentSettingsJson.opt(it)?.javaClass?.simpleName ?: "?") }

        model.postValue(
            Model(
                SettingsDataPreview(
                    keys = currentSettingsJson.keys().asSequence().toList(),
                    keyToSettingItems = keyToSettingItems,
                    diffs = diffs,
                    settingsPreview = settingsPreview,
                    keyToType = keyToType,
                    rawData = currentSettingsJson.toString(),
                ),
            ),
        )
    }

    fun resetSetting(settingKey: String?) {
        settingKey ?: return

        preferences.reset(settingKey)

        generatePreviewFromSettingsJson()
    }
}
