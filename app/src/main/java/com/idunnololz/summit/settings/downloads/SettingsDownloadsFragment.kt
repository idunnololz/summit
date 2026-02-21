package com.idunnololz.summit.settings.downloads

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.idunnololz.summit.preferences.PreferenceKeys.KEY_DOWNLOAD_DIRECTORY
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.DownloadSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.settings.util.asOnOffSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsDownloadsFragment : BaseSettingsFragment() {

  @Inject
  override lateinit var settings: DownloadSettings

  private val openDocumentTreeLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocumentTree(),
  ) { uri ->
    // Handle the returned Uri

    if (uri != null) {
      lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
          onDownloadDirectorySelected(uri)
        }
      }
    }
  }

  override fun generateData(): List<SettingModelItem> = listOf(
    settings.downloadDirectory.asCustomItem(
      { getCurrentDownloadDirectory() },
      { openDocumentTreeLauncher.launch(null) },
    ),
    settings.useCommunityDownloadFolder.asOnOffSwitch(
      { preferences.useCommunityDownloadFolder },
      { preferences.useCommunityDownloadFolder = it }
    ),
    settings.resetDownloadDirectory.asCustomItem {
      preferences.reset(KEY_DOWNLOAD_DIRECTORY)
      refresh()
    },
  )

  private fun getCurrentDownloadDirectory(): String {
    val context = context ?: return ""
    val downloadDirectory = preferences.downloadDirectory

    if (downloadDirectory == null) {
      return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
        ?: ""
    }

    val oldParentUri = Uri.parse(downloadDirectory)
    val id = DocumentsContract.getTreeDocumentId(oldParentUri)
    val parentFolderUri =
      DocumentsContract.buildChildDocumentsUriUsingTree(oldParentUri, id)
    val fileStructureReversed = mutableListOf<DocumentFile>()

    var currentFile = DocumentFile.fromTreeUri(context, parentFolderUri)

    while (currentFile != null) {
      fileStructureReversed.add(currentFile)
      currentFile = currentFile.parentFile
    }

    val fullPath = buildString {
      for (file in fileStructureReversed.reversed()) {
        append(file.name)
        append("/")
      }
    }

    return fullPath.ifBlank {
      downloadDirectory
    }
  }

  private fun onDownloadDirectorySelected(uri: Uri) {
    val context = context ?: return

    preferences.downloadDirectory = uri.toString()

    val contentResolver = context.contentResolver

    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
      Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    contentResolver.takePersistableUriPermission(uri, takeFlags)

    refresh()
  }
}
