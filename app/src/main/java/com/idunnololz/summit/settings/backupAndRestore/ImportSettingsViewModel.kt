package com.idunnololz.summit.settings.backupAndRestore

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.db.raw.DbHelper
import com.idunnololz.summit.db.raw.TableInfo
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.backupAndRestore.export.ExportableTable
import com.idunnololz.summit.settings.backupAndRestore.export.SYSTEM_TABLES
import com.idunnololz.summit.settings.backupAndRestore.export.tableNameToExportableTable
import com.idunnololz.summit.util.PreferenceUtils
import com.idunnololz.summit.util.PreferenceUtils.KEY_DATABASE_MAIN
import com.idunnololz.summit.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.File

@HiltViewModel
class ImportSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsBackupManager: SettingsBackupManager,
    private val preferences: Preferences,
    private val stateHandle: SavedStateHandle,
    private val json: Json,
    private val mainDatabase: MainDatabase,
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
        viewModelScope.launch {
            // before we overwrite existing settings, create a backup just in case
            settingsBackupManager.saveBackup(
                backup = preferences.generateCode(),
                backupName = "import_settings_backup_%datetime%",
            )

            val settingsToImport = JSONObject(settingsData.rawData)
            preferences.importSettings(settingsToImport, settingsData.excludeKeys)

            if (settingsData.tablePath != null) {
                importDb(settingsData.tablePath, settingsData.tableResolutions)
            }

            state.postValue(State.ImportSettingsCompleted)
        }
    }

    private suspend fun importDb(
        tablePath: String,
        tableResolutions: Map<String, SettingDataAdapter.ImportTableResolution>,
    ) {
        val f = File(tablePath)
        val tempDatabase = MainDatabase.buildDatabase(context, f.name, json)

        DbHelper(tempDatabase.openHelper.readableDatabase, true).use { dbHelper ->
            val tableNames = dbHelper.getTableNames()

            for (tableName in tableNames) {
                val exportableTable = tableNameToExportableTable(tableName)
                    ?: continue
                val resolution = tableResolutions[tableName]
                    ?: SettingDataAdapter.ImportTableResolution.Ignore

                if (resolution == SettingDataAdapter.ImportTableResolution.Ignore) {
                    continue
                }

                when (exportableTable) {
                    ExportableTable.UserCommunities -> {
                        val userCommunitiesDao = mainDatabase.userCommunitiesDao()
                        val allEntries = tempDatabase.userCommunitiesDao().getAllCommunities()

                        when (resolution) {
                            SettingDataAdapter.ImportTableResolution.Merge -> {
                                for (entry in allEntries) {
                                    userCommunitiesDao.insertCommunity(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Overwrite -> {
                                userCommunitiesDao.deleteAll()
                                for (entry in allEntries) {
                                    userCommunitiesDao.insertCommunity(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Ignore -> continue
                        }
                    }
                    ExportableTable.HiddenPosts -> {
                        val hiddenPostsDao = mainDatabase.hiddenPostsDao()
                        val allEntries = tempDatabase.hiddenPostsDao().getAllHiddenPosts()

                        when (resolution) {
                            SettingDataAdapter.ImportTableResolution.Merge -> {
                                for (entry in allEntries) {
                                    hiddenPostsDao.insertHiddenPost(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Overwrite -> {
                                hiddenPostsDao.clear()
                                for (entry in allEntries) {
                                    hiddenPostsDao.insertHiddenPost(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Ignore -> continue
                        }
                    }
                    ExportableTable.ContentFilters -> {
                        val contentFiltersDao = mainDatabase.contentFiltersDao()
                        val allEntries = tempDatabase.contentFiltersDao().getAllFilters()

                        when (resolution) {
                            SettingDataAdapter.ImportTableResolution.Merge -> {
                                for (entry in allEntries) {
                                    contentFiltersDao.insertFilter(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Overwrite -> {
                                contentFiltersDao.clear()
                                for (entry in allEntries) {
                                    contentFiltersDao.insertFilter(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Ignore -> continue
                        }
                    }
                    ExportableTable.Drafts -> {
                        val draftsDao = mainDatabase.draftsDao()
                        val allEntries = tempDatabase.draftsDao().getAllDrafts()

                        when (resolution) {
                            SettingDataAdapter.ImportTableResolution.Merge -> {
                                for (entry in allEntries) {
                                    draftsDao.insert(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Overwrite -> {
                                draftsDao.deleteAll()
                                for (entry in allEntries) {
                                    draftsDao.insert(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Ignore -> continue
                        }
                    }
                    ExportableTable.TextEmojis -> {
                        val textEmojiDao = mainDatabase.textEmojiDao()
                        val allEntries = tempDatabase.textEmojiDao().getAll()

                        when (resolution) {
                            SettingDataAdapter.ImportTableResolution.Merge -> {
                                for (entry in allEntries) {
                                    textEmojiDao.insert(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Overwrite -> {
                                textEmojiDao.deleteAll()
                                for (entry in allEntries) {
                                    textEmojiDao.insert(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Ignore -> continue
                        }
                    }
                    ExportableTable.UserTags -> {
                        val userTagsDao = mainDatabase.userTagsDao()
                        val allEntries = tempDatabase.userTagsDao().getAllUserTags()

                        when (resolution) {
                            SettingDataAdapter.ImportTableResolution.Merge -> {
                                for (entry in allEntries) {
                                    userTagsDao.insertUserTag(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Overwrite -> {
                                userTagsDao.clear()
                                for (entry in allEntries) {
                                    userTagsDao.insertUserTag(entry)
                                }
                            }
                            SettingDataAdapter.ImportTableResolution.Ignore -> continue
                        }
                    }
                }
            }

        }

        tempDatabase.close()
        f.delete()
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
        } catch (e: Exception) { /* do nothing */ }

        try {
            val unb64 = Base64.decode(settings, Base64.DEFAULT).toString(Charsets.UTF_32)
            if (unb64.isJson()) {
                next(unb64)
                return@withContext
            }
        } catch (e: Exception) { /* do nothing */ }

        try {
            if (settings.isJson()) {
                next(settings)
                return@withContext
            }
        } catch (e: Exception) { /* do nothing */ }

        state.postValue(State.Error(ErrorType.UnableToDecipherInput, RuntimeException()))
    }

    private suspend fun generatePreviewFromSettingsJson(settingsJsonStr: String) {
        val settingsJson = try {
            JSONObject(settingsJsonStr)
        } catch (e: Exception) {
            state.postValue(State.Error(ErrorType.InvalidJson, e))
            return
        }

        val o = settingsJson.opt(PreferenceUtils.PREFERENCE_VERSION_CODE)
        val rawString = settingsJson.optJSONObject(KEY_DATABASE_MAIN)?.optString("db_1")
        settingsJson.remove(KEY_DATABASE_MAIN)

        if (o == null) {
            state.postValue(State.Error(ErrorType.InvalidSettingsJson, RuntimeException()))
            return
        }

        val tablesInfo: Map<String, TableInfo>
        val dbPath: File?
        if (!rawString.isNullOrBlank()) {
            val byteArr = Utils.decompressZlibRaw(rawString)
            val tempDb = context.getDatabasePath("_temp_import_db")

            dbPath = tempDb

            tempDb.delete()
            tempDb.outputStream().use { it.write(byteArr) }

            val mainDatabase = MainDatabase.buildDatabase(context, tempDb.name, json)
            tablesInfo = DbHelper(mainDatabase.openHelper.readableDatabase, true)
                .use { dbHelper ->
                    dbHelper.getTableNames()
                        .map { dbHelper.getTableInfo(it) }
                        .filter { it.rowCount > 0 }
                        .filter { !SYSTEM_TABLES.contains(it.tableName) }
                        .associateBy { it.tableName }
                }
        } else {
            tablesInfo = mapOf()
            dbPath = null
        }

        val currentSettingsJson = preferences.asJson()

        val allKeys = settingsJson.keys().asSequence().toMutableSet()
        allKeys.addAll(currentSettingsJson.keys().asSequence())

        // diff current vs the json we are importing
        val diffs = mutableListOf<Diff>()
        for (key in allKeys) {
            val currentValue = currentSettingsJson.opt(key)
            val importValue = settingsJson.opt(key)

            if (currentValue == null && importValue != null) {
                diffs.add(Diff(DiffType.Added, "null", importValue.toString()))
            } else if (currentValue != null && importValue == null) {
                diffs.add(Diff(DiffType.Removed, currentValue.toString(), "null"))
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

        val settingsPreview = settingsJson.keys().asSequence()
            .associateWith { (settingsJson.opt(it)?.toString() ?: "null") }
        val keyToType = settingsJson.keys().asSequence()
            .associateWith { (settingsJson.opt(it)?.javaClass?.simpleName ?: "?") }

        state.postValue(
            State.ConfirmImportSettings(
                SettingsDataPreview(
                    keys = settingsJson.keys().asSequence().toList(),
                    diffs = diffs,
                    settingsPreview = settingsPreview,
                    keyToType = keyToType,
                    rawData = settingsJsonStr,
                    tablePath = dbPath?.path,
                    databaseTablePreview = tablesInfo,
                ),
            ),
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

    fun confirmImport(
        excludeKeys: MutableSet<String>,
        tableResolutions: Map<String, SettingDataAdapter.ImportTableResolution>
    ) {
        val currentState = state.value as? State.ConfirmImportSettings
            ?: return

        state.postValue(
            State.PerformImportSettings(
                SettingsData(
                    rawData = currentState.preview.rawData,
                    tablePath = currentState.preview.tablePath,
                    excludeKeys = excludeKeys,
                    tableResolutions = tableResolutions,
                ),
            ),
        )
    }

    sealed interface State : Parcelable {

        @Parcelize
        data object NotStarted : State

        @Parcelize
        data class DecodeInputString(
            val input: String,
        ) : State

        @Parcelize
        data class GeneratePreviewFromSettingsJson(
            val settingsJson: String,
        ) : State

        @Parcelize
        data class ConfirmImportSettings(
            val preview: SettingsDataPreview,
        ) : State

        @Parcelize
        data class PerformImportSettings(
            val settingsData: SettingsData,
        ) : State

        @Parcelize
        data class Error(
            val error: ErrorType,
            val e: Throwable,
        ) : State

        @Parcelize
        data object ImportSettingsCompleted : State
    }

    enum class ErrorType {
        UnableToDecipherInput,
        InvalidSettingsJson,
        InvalidJson,
    }
}
