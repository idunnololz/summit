package com.idunnololz.summit.settings.perAccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.importAndExport.Diff
import com.idunnololz.summit.settings.importAndExport.DiffType
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ManageSettingsViewModel @Inject constructor(
  private val preferences: Preferences,
  private val preferenceManager: PreferenceManager,
) : ViewModel() {

  val manageSettingsData = StatefulLiveData<ManageSettingsData>()

  fun loadManageSettingsData(account: Account?) {
    manageSettingsData.setIsLoading()

    viewModelScope.launch {
      val preferences = if (account == null) {
        preferences
      } else {
        preferenceManager.getOnlyPreferencesForAccount(account)
      }

      val currentSettingsJson = preferences.asJson()

      val allKeys = currentSettingsJson.keys().asSequence().toMutableSet()
      allKeys.addAll(currentSettingsJson.keys().asSequence())

      // diff current vs the json we are importing
      val diffs = mutableListOf<Diff>()
      for (key in allKeys) {
        val currentValue = currentSettingsJson.opt(key)
        val importValue = currentSettingsJson.opt(key)

        if (currentValue == null && importValue != null) {
          diffs.add(
            Diff(
              DiffType.Added,
              "null",
              importValue.toString(),
            ),
          )
        } else if (currentValue != null && importValue == null) {
          diffs.add(
            Diff(
              DiffType.Removed,
              currentValue.toString(),
              "null",
            ),
          )
        } else if (currentValue != importValue) {
          diffs.add(
            Diff(
              type = DiffType.Changed,
              currentValue = currentValue?.toString() ?: "null",
              importValue = importValue?.toString() ?: "null",
            ),
          )
        } else {
          diffs.add(
            Diff(
              type = DiffType.Unchanged,
              currentValue = currentValue?.toString() ?: "null",
              importValue = importValue?.toString() ?: "null",
            ),
          )
        }
      }

      val settingsPreview = currentSettingsJson.keys().asSequence()
        .associateWith { (currentSettingsJson.opt(it)?.toString() ?: "null") }
      val keyToType = currentSettingsJson.keys().asSequence()
        .associateWith { (currentSettingsJson.opt(it)?.javaClass?.simpleName ?: "?") }

      manageSettingsData.postValue(
        ManageSettingsData(
          keys = currentSettingsJson.keys().asSequence().toList(),
          settingsPreview = settingsPreview,
          keyToType = keyToType,
        ),
      )
    }
  }

  fun deleteSetting(account: Account?, settingKey: String?) {
    settingKey ?: return

    val preferences = if (account == null) {
      preferences
    } else {
      preferenceManager.getOnlyPreferencesForAccount(account)
    }

    preferences.reset(settingKey)
    loadManageSettingsData(account)
  }

  class ManageSettingsData(
    val keys: List<String>,
    val settingsPreview: Map<String, String>,
    val keyToType: Map<String, String>,
  )
}
