package com.idunnololz.summit.settings.backupAndRestore

import com.idunnololz.summit.util.DirectoryHelper
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsBackupManager @Inject constructor(
    directoryHelper: DirectoryHelper,
) {

    private val settingBackupsDir: File = directoryHelper.settingBackupsDir

    fun saveBackup(
        backup: String,
        backupName: String = "settings_backup_%datetime%",
    ): File {
        val currentDateTimeString = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
        val fileName = backupName.replace("%datetime%", currentDateTimeString)
        val backupFile = File(settingBackupsDir, fileName)

        settingBackupsDir.mkdirs()

        backupFile.outputStream().use {
            it.write(backup.encodeToByteArray())
        }

        return backupFile
    }

    fun getBackups(): List<BackupInfo> =
        settingBackupsDir.listFiles()?.map {
            BackupInfo(it)
        } ?: listOf()

    fun deleteBackup(backupInfo: BackupInfo) {
        backupInfo.file.delete()
    }

    data class BackupInfo(
        val file: File,
    )
}
