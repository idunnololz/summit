package com.idunnololz.summit.settings.importAndExport

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.db.MainDatabase
import com.idunnololz.summit.db.raw.DbHelper
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DATABASE_MAIN
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.importAndExport.export.defaultTablesToExport
import com.idunnololz.summit.util.PiiDetector
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject

@HiltViewModel
class ExportSettingsViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val preferences: Preferences,
  private val settingsBackupManager: SettingsBackupManager,
  private val json: Json,
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
        val generateCodeResult = generateCode(backupConfig)
        val encodedString = generateCodeResult.code
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
            issues = generateCodeResult.issues,
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

  fun saveToInternalBackups(
    backupConfig: BackupConfig,
    backupName: String = "settings_backup_%datetime%",
  ) {
    viewModelScope.launch {
      val generateCodeResult = generateCode(backupConfig)
      val file = settingsBackupManager.saveBackup(generateCodeResult.code, backupName)

      backupFile.postValue(
        BackupResult(
          uri = file.toUri(),
          config = BackupConfig(
            BackupOption.SaveInternal,
          ),
          issues = generateCodeResult.issues,
        ),
      )
    }
  }

  fun resetSettings() {
    preferences.clear()
  }

  class GenerateCodeResult(
    val code: String,
    val issues: List<PiiDetector.PiiIssue>?,
  )

  private suspend fun generateCode(backupConfig: BackupConfig): GenerateCodeResult {
    val prefJson = preferences.asJson()

    if (backupConfig.includeDatabase) {
      val file = context.getDatabasePath(MainDatabase.DATABASE_NAME)
      val tempDb = context.getDatabasePath("_temp_main_db")

      tempDb.delete()

      file.copyTo(tempDb)

      val tablesToKeep = defaultTablesToExport

      val mainDatabase = MainDatabase.buildDatabase(context, tempDb.name, json)
      DbHelper(mainDatabase.openHelper.writableDatabase, true).use { dbHelper ->
        dbHelper.keepTables(tablesToKeep)
      }
      mainDatabase.close()

      val byteArr = ByteArrayOutputStream().use { outputStream ->
        tempDb.inputStream().use { inputStream ->
          inputStream.copyTo(outputStream)
        }
        outputStream.toByteArray()
      }

      prefJson.put(
        KEY_DATABASE_MAIN,
        JSONObject().apply {
          put("db_1", Utils.compress(byteArr, Base64.NO_WRAP))
        },
      )

      tempDb.delete()
    }

    return GenerateCodeResult(
      code = Utils.compress(prefJson.toString(), Base64.NO_WRAP),
      issues = if (backupConfig.runPiiDetector) {
        PiiDetector().isJsonSafe(prefJson)
      } else {
        null
      },
    )
  }

  data class BackupConfig(
    val backupOption: BackupOption,
    val includeDatabase: Boolean = true,
    val dest: Uri? = null,
    val runPiiDetector: Boolean = false,
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
    val issues: List<PiiDetector.PiiIssue>?,
  )
}
