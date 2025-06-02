package com.idunnololz.summit.presets

import android.net.Uri
import android.util.Base64
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.SummitServerClient
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.SharedPreferencesManager
import com.idunnololz.summit.settings.importAndExport.Diff
import com.idunnololz.summit.settings.importAndExport.DiffType
import com.idunnololz.summit.settings.importAndExport.SettingsDataPreview
import com.idunnololz.summit.util.PiiDetector
import com.idunnololz.summit.preferences.PreferenceKeys.PREFERENCE_VERSION_CODE
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.asJson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CreatePresetViewModel @Inject constructor(
  private val preferences: Preferences,
  private val sharedPreferencesManager: SharedPreferencesManager,
  private val summitServerClient: SummitServerClient,
) : ViewModel() {

  data class SettingIssue(
    val key: String,
    val value: String,
    val keyError: PiiDetector.ColumnNamePiiIssue?,
    val valueError: PiiDetector.ValuePiiIssue?,
  )

  data class PresetForm(
    val name: String = "",
    val description: String = "",
    val data: Map<String, Any> = mapOf(),
    val phonePreviewSs: Uri? = null,
    val tabletPreviewSs: Uri? = null,
  )

  sealed interface State {
    data class Initial(
      val allSettings: Map<String, Any?>,
    ) : State

    data class TakePreviewScreenshot(
      val phonePreviewSs: Uri? = null,
      val tabletPreviewSs: Uri? = null,
    ) : State

    data class ResolveSettingIssues(
      val settingIssues: List<SettingIssue>,
    ) : State

    data class ConfirmSubmit(
      val isSubmitting: Boolean = false,
      val submitError: Throwable? = null,
    ) : State

    data object Complete : State
  }

  val model = StatefulLiveData<Model>()
  val excludedKeys = mutableSetOf<String>()
  val presetForm = MutableStateFlow<PresetForm>(PresetForm())
  val state = MutableStateFlow<State>(newInitialState())
  private var isDraftPreferencesPublished = false

  data class Model(
    val settingsDataPreview: SettingsDataPreview,
  )

  fun updateState() {
    viewModelScope.launch {
      val curState = state.value
      val newState = when (curState) {
        State.Complete -> curState
        is State.ConfirmSubmit -> curState
        is State.Initial -> newInitialState()
        is State.ResolveSettingIssues -> curState
        is State.TakePreviewScreenshot -> newTakePreviewScreenshotState()
      }

      state.emit(newState)
    }
  }

  fun refreshDraftPreferences() {
    viewModelScope.launch {
      val allPrefs = sharedPreferencesManager.defaultSharedPreferences.all

      val oldAllPrefs = sharedPreferencesManager.tempPreferences.all

      sharedPreferencesManager.tempPreferences.edit {
        clear()

        for ((key, value) in allPrefs) {
          if (excludedKeys.contains(key)) {
            continue
          }

          when (value) {
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is String -> putString(key, value)
            is Set<Any?> -> putStringSet(key, value as Set<String>)
          }
        }
      }

      if (oldAllPrefs != sharedPreferencesManager.tempPreferences.all ||
        !isDraftPreferencesPublished
      ) {
        isDraftPreferencesPublished = true
        updateState()
      }
    }
  }

  fun generatePreviewFromSettingsJson() {
    model.setIsLoading()

    viewModelScope.launch {
      val currentSettingsJson = sharedPreferencesManager.defaultSharedPreferences.asJson()

      val keysToIgnore = setOf(
        "pref_version",
        PREFERENCE_VERSION_CODE,
      )

      for (keyToIgnore in keysToIgnore) {
        currentSettingsJson.remove(keyToIgnore)
      }

      val allKeys = currentSettingsJson.keys().asSequence()

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
            diffs = diffs,
            settingsPreview = settingsPreview,
            keyToType = keyToType,
            rawData = currentSettingsJson.toString(),
            tablePath = null,
            databaseTablePreview = null,
          ),
        ),
      )
    }
  }

  fun prepareSubmitPreset(name: String?, description: String?) {
    viewModelScope.launch {
      presetForm.emit(
        presetForm.value.copy(
          name = name ?: presetForm.value.name,
          description = description ?: presetForm.value.description,
        ),
      )

      val allSettings = sharedPreferencesManager.tempPreferences.all
      val piiDetector = PiiDetector()

      val settingIssues = mutableListOf<SettingIssue>()

      for ((key, value) in allSettings) {
        val keyIssue = piiDetector.isColumnNameSafe(key)

        val valueIssue = if (value is String) {
          piiDetector.isValueSafe(value)
        } else {
          null
        }

        if (keyIssue != null || valueIssue != null) {
          settingIssues.add(
            SettingIssue(
              key,
              value.toString(),
              keyIssue,
              valueIssue,
            ),
          )
        }
      }

      if (settingIssues.isNotEmpty()) {
        state.emit(State.ResolveSettingIssues(settingIssues))
      } else {
        state.emit(newTakePreviewScreenshotState())
      }
    }
  }

  fun resolveSettingIssues() {
    viewModelScope.launch {
      state.emit(newTakePreviewScreenshotState())
    }
  }

  fun onConfirmSubmitPreset() {
//        summitServerClient.communitySuggestions()
  }

  fun updatePresetForm(name: String, description: String) {
    viewModelScope.launch {
      presetForm.emit(
        presetForm.value.copy(
          name = name,
          description = description,
        ),
      )
    }
  }

  fun goBack() {
    viewModelScope.launch {
      val previousState = when (state.value) {
        is State.Initial -> state.value
        is State.ResolveSettingIssues -> newInitialState()
        is State.TakePreviewScreenshot -> newInitialState()
        is State.ConfirmSubmit -> newTakePreviewScreenshotState()
        State.Complete -> State.ConfirmSubmit()
      }

      state.emit(previousState)
    }
  }

  fun submitPreset() {
    viewModelScope.launch {
      state.emit(State.ConfirmSubmit(isSubmitting = true))

      val presetForm = presetForm.value
      val submitResult = summitServerClient.submitPreset(
        presetName = presetForm.name,
        presetDescription = presetForm.description,
        presetData = Utils.compress(
          sharedPreferencesManager.tempPreferences.asJson().toString(),
          Base64.NO_WRAP,
        ),
        phoneScreenshot = presetForm.phonePreviewSs,
        tabletScreenshot = presetForm.tabletPreviewSs,
      )
      submitResult
        .onSuccess { state.emit(State.Complete) }
        .onFailure { state.emit(State.ConfirmSubmit(submitError = it)) }
    }
  }

  fun submitPreviewScreenshots() {
    viewModelScope.launch {
      state.emit(State.ConfirmSubmit())
    }
  }

  fun updateScreenshots(phonePreviewSs: Uri, tabletPreviewSs: Uri) {
    viewModelScope.launch {
      presetForm.emit(
        presetForm.value.copy(
          phonePreviewSs = phonePreviewSs,
          tabletPreviewSs = tabletPreviewSs,
        ),
      )
      updateState()
    }
  }

  private fun newInitialState() = State.Initial(
    allSettings = sharedPreferencesManager.tempPreferences.all,
  )

  private fun newTakePreviewScreenshotState() = State.TakePreviewScreenshot(
    phonePreviewSs = presetForm.value.phonePreviewSs,
    tabletPreviewSs = presetForm.value.tabletPreviewSs,
  )
}
