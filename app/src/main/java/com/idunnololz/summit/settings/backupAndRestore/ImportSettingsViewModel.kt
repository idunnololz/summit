package com.idunnololz.summit.settings.backupAndRestore

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.AllSettings
import com.idunnololz.summit.settings.SettingItem
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ImportSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsBackupManager: SettingsBackupManager,
    private val preferences: Preferences,
    private val stateHandle: SavedStateHandle,
    private val allSettings: AllSettings,
) : ViewModel() {

    val state =
        stateHandle.getLiveData<State>("state", State.NotStarted)

    init {
        state.observeForever {
            nextState()
        }
    }

    private fun nextState() {
        viewModelScope.launch {
            when (val s = state.value) {
                is State.DecodeInputString -> {
                    loadSettingsPreview(s.input)
                }
                is State.GeneratePreviewFromSettingsJson -> {
                    generatePreviewFromSettingsJson(s.settingsJson)
                }
                is State.ConfirmImportSettings -> {}
                is State.Error -> {}
                is State.PerformImportSettings -> {
                    performImportSettings(s.settingsData)
                }
                State.NotStarted -> {}
                null -> {}
                State.ImportSettingsCompleted -> {}
            }
        }
    }

    private fun performImportSettings(settingsData: SettingsData) {
        // before we overwrite existing settings, create a backup just in case
        settingsBackupManager.saveBackup(
            backup = preferences.generateCode(),
            backupName = "import_settings_backup_%datetime%"
        )

        val settingsToImport = JSONObject(settingsData.rawData)
        preferences.importSettings(settingsToImport, settingsData.excludeKeys)

        state.postValue(State.ImportSettingsCompleted)
    }

    fun importSettings(settingsText: String) {
        state.postValue(State.DecodeInputString(settingsText))
    }

    private suspend fun loadSettingsPreview(settings: String) = withContext(Dispatchers.Default) {
        // We need to figure out the form of this string

        fun next(jsonString: String) {
            state.postValue(State.GeneratePreviewFromSettingsJson(jsonString))
        }

        try {
            val string = Utils.decompressZlib(settings)
            if (string.isJson()) {
                next(string)
                return@withContext
            }
        } catch (e: Exception) {/* do nothing */}

        try {
            val unb64 = Base64.decode(settings, Base64.DEFAULT).toString(Charsets.UTF_32)
            if (unb64.isJson()) {
                next(unb64)
                return@withContext
            }
        } catch (e: Exception) {/* do nothing */}

        try {
            if (settings.isJson()) {
                next(settings)
                return@withContext
            }
        } catch (e: Exception) {/* do nothing */}

        state.postValue(State.Error(ErrorType.UnableToDecipherInput, RuntimeException()))
    }

    private fun generatePreviewFromSettingsJson(settingsJson: String) {
        val json = try {
            JSONObject(settingsJson)
        } catch (e: Exception) {
            state.postValue(State.Error(ErrorType.InvalidJson, e))
            return
        }

        val o = json.opt(PreferenceUtil.PREFERENCE_VERSION_CODE)

        if (o == null) {
            state.postValue(State.Error(ErrorType.InvalidSettingsJson, RuntimeException()))
            return
        }

        val keyToSettingItems = allSettings.generateMapFromKeysToRelatedSettingItems()

        val currentSettingsJson = preferences.asJson()

        val allKeys = json.keys().asSequence().toMutableSet()
        allKeys.addAll(currentSettingsJson.keys().asSequence())

        // diff current vs the json we are importing
        val diffs = mutableListOf<Diff>()
        for (key in allKeys) {
            val currentValue = currentSettingsJson.opt(key)
            val importValue = json.opt(key)

            if (currentValue == null && importValue != null) {
                diffs.add(Diff(DiffType.Added, "null", importValue.toString()))
            } else if (currentValue != null && importValue == null) {
                diffs.add(Diff(DiffType.Removed, currentValue.toString(), "null"))
            } else if (currentValue != importValue) {
                diffs.add(Diff(
                    type = DiffType.Changed,
                    currentValue = currentValue?.toString() ?: "null",
                    importValue = importValue?.toString() ?: "null",
                ))
            } else {
                diffs.add(Diff(
                    type = DiffType.Unchanged,
                    currentValue = currentValue?.toString() ?: "null",
                    importValue = importValue?.toString() ?: "null",
                ))
            }
        }


        val settingsPreview = json.keys().asSequence()
            .associateWith { (json.opt(it)?.toString() ?: "null") }
        val keyToType = json.keys().asSequence()
            .associateWith { (json.opt(it)?.javaClass?.simpleName ?: "?") }

        state.postValue(
            State.ConfirmImportSettings(
                SettingsDataPreview(
                    keys = json.keys().asSequence().toList(),
                    keyToSettingItems = keyToSettingItems,
                    diffs = diffs,
                    settingsPreview = settingsPreview,
                    keyToType = keyToType,
                    rawData = settingsJson,
                )
            )
        )
    }

    private fun String.isJson(): Boolean {
        val trimmedString = this.trim()

        if (trimmedString.startsWith("{") && trimmedString.endsWith("}")) {
            // this is a json...

            return true
        }
        return false
    }

    fun generatePreviewFromFile(uri: Uri) {
        context.contentResolver.openInputStream(uri).use {
            val content = it
                ?.bufferedReader()
                ?.readText()

            if (content != null) {
                importSettings(content)
            }
        }
    }

    fun confirmImport(excludeKeys: MutableSet<String>) {
        val currentState = state.value as? State.ConfirmImportSettings
            ?: return

        state.postValue(State.PerformImportSettings(SettingsData(
            currentState.preview.rawData,
            excludeKeys,
        )))
    }

    sealed interface State : Parcelable {

        @Parcelize
        data object NotStarted : State

        @Parcelize
        data class DecodeInputString(
            val input: String
        ): State

        @Parcelize
        data class GeneratePreviewFromSettingsJson(
            val settingsJson: String
        ): State

        @Parcelize
        data class ConfirmImportSettings(
            val preview: SettingsDataPreview
        ): State

        @Parcelize
        data class PerformImportSettings(
            val settingsData: SettingsData
        ): State

        @Parcelize
        data class Error(
            val error: ErrorType,
            val e: Throwable
        ): State

        @Parcelize
        data object ImportSettingsCompleted : State
    }

    enum class ErrorType {
        UnableToDecipherInput,
        InvalidSettingsJson,
        InvalidJson,
    }

    @Parcelize
    data class SettingsDataPreview (
        val keys: List<String>,
        val keyToSettingItems: Map<String, List<SettingItem>>,
        val diffs: List<Diff>,
        val settingsPreview: Map<String, String>,
        val keyToType: Map<String, String>,
        val rawData: String,
    ): Parcelable

    @Parcelize
    data class Diff(
        val type: DiffType,
        val currentValue: String,
        val importValue: String,
    ): Parcelable

    @Parcelize
    data class SettingsData(
        val rawData: String,
        val excludeKeys: Set<String>
    ): Parcelable

    enum class DiffType {
        Added,
        Removed,
        Changed,
        Unchanged,
    }

}