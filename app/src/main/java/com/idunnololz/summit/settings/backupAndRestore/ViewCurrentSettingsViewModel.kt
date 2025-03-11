package com.idunnololz.summit.settings.backupAndRestore

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.AllSettings
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.File
import javax.inject.Inject

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

//            val tableNames = mutableListOf<String>()
//            runInterruptible(Dispatchers.Default) {
//                val cursor = mainDatabase.query(
//                    "SELECT name FROM sqlite_master WHERE type='table'", arrayOf())
//                while(cursor.moveToNext()) {
//                    val tableName = cursor.getString(0)
//                    tableNames.add(tableName)
//                }
//            }
//            val systemTableNames = setOf(
//                "android_metadata",
//                "sqlite_sequence",
//            )
//            val roomMasterTableName = "room_master_table"
//
//            Log.d("HAHA", tableNames.joinToString(separator = ","))

            model.postValue(
                Model(
                    SettingsDataPreview(
                        keys = currentSettingsJson.keys().asSequence().toList(),
                        diffs = diffs,
                        settingsPreview = settingsPreview,
                        keyToType = keyToType,
                        rawData = currentSettingsJson.toString(),
                    ),
                ),
            )
        }
    }

    fun resetSetting(settingKey: String?, databaseFile: File) {
        settingKey ?: return

        preferences.reset(settingKey)

        generatePreviewFromSettingsJson(databaseFile)
    }
}
