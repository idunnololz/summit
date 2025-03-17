package com.idunnololz.summit.settings.backupAndRestore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.db.raw.DbHelper
import com.idunnololz.summit.db.raw.TableInfo
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ViewCurrentSettingsViewModel @Inject constructor(
    private val preferences: Preferences,
    private val mainDatabase: MainDatabase,
) : ViewModel() {

    val model = StatefulLiveData<Model>()

    data class Model(
        val settingsDataPreview: SettingsDataPreview,
    )

    fun generatePreviewFromSettingsJson(databaseFile: File) {
        model.setIsLoading()

        viewModelScope.launch {
            val dbHelper = DbHelper(
                mainDatabase.openHelper.readableDatabase,
                shouldClose = false,
            )

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

            val tableNames = dbHelper.getTableNames()
//            val systemTableNames = setOf(
//                "android_metadata",
//                "sqlite_sequence",
//            )
            val roomMasterTableName = "room_master_table"
            val databaseTablePreview = mutableMapOf<String, TableInfo>()

            tableNames.forEach { tableName ->
                databaseTablePreview[tableName] = dbHelper.getTableInfo(tableName)
            }

            model.postValue(
                Model(
                    SettingsDataPreview(
                        keys = currentSettingsJson.keys().asSequence().toList(),
                        diffs = diffs,
                        settingsPreview = settingsPreview,
                        keyToType = keyToType,
                        rawData = currentSettingsJson.toString(),
                        tablePath = null,
                        databaseTablePreview = databaseTablePreview,
                    ),
                ),
            )

            dbHelper.close()
        }
    }

    fun resetSetting(settingKey: String?, databaseFile: File) {
        settingKey ?: return

        preferences.reset(settingKey)

        generatePreviewFromSettingsJson(databaseFile)
    }
}
