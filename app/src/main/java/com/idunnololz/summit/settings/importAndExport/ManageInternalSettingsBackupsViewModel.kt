package com.idunnololz.summit.settings.importAndExport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ManageInternalSettingsBackupsViewModel @Inject constructor(
    private val settingsBackupManager: SettingsBackupManager,
) : ViewModel() {

    val backupsInfo = StatefulLiveData<List<SettingsBackupManager.BackupInfo>>()

    init {
        viewModelScope.launch {
            loadBackupsData()
        }
    }

    fun deleteBackup(backupInfo: SettingsBackupManager.BackupInfo) {
        viewModelScope.launch {
            settingsBackupManager.deleteBackup(backupInfo)
            loadBackupsData()
        }
    }

    private suspend fun loadBackupsData() = withContext(Dispatchers.Default) {
        backupsInfo.postValue(settingsBackupManager.getBackups())
    }
}
