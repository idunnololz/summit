package com.idunnololz.summit.settings.backupAndRestore

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ExportSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val settingsBackupManager: SettingsBackupManager,
) : ViewModel() {

    companion object {
        private const val TAG = "BackupSettingsViewModel"
    }

    val backupFile = StatefulLiveData<BackupResult>()

    fun createBackupAndSave(backupConfig: BackupConfig) {
        Log.d(TAG, "createBackupAndSave()")

        backupFile.setIsLoading()

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val encodedString = preferences.generateCode()
                Log.d(TAG, encodedString)

                val uri = if (backupConfig.dest == null) {
                    FileProviderHelper(context)
                        .openTempFile(getBackupFileName()) {
                            it.write(encodedString.encodeToByteArray())
                        }
                } else {
                    context.contentResolver.openOutputStream(backupConfig.dest).use {
                        it?.write(encodedString.encodeToByteArray())
                    }

                    backupConfig.dest
                }

                backupFile.postValue(
                    BackupResult(
                        uri = uri,
                        config = backupConfig,
                    ),
                )
            } catch (e: Exception) {
                backupFile.postError(e)
            }
        }
    }

    fun getBackupFileName(): String {
        val dateFormatter = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.US)

        return "backup-${dateFormatter.format(Calendar.getInstance().time)}.summitbackup"
    }

    fun saveToInternalBackups(backupName: String = "settings_backup_%datetime%") {
        val file = settingsBackupManager.saveBackup(preferences.generateCode(), backupName)

        backupFile.postValue(
            BackupResult(
                file.toUri(),
                BackupConfig(
                    BackupOption.SaveInternal,
                ),
            ),
        )
    }

    fun resetSettings() {
        preferences.clear()
    }

    data class BackupConfig(
        val backupOption: BackupOption,
        val dest: Uri? = null,
    )

    enum class BackupOption {
        Share,
        Save,
        Copy,
        SaveInternal,
    }

    data class BackupResult(
        val uri: Uri,
        val config: BackupConfig,
    )
}
