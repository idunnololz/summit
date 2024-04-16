package com.idunnololz.summit.settings.downloads

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.idunnololz.summit.databinding.FragmentSettingDownloadsBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.DownloadSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PreferenceUtil.KEY_DOWNLOAD_DIRECTORY
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingDownloadsFragment : BaseFragment<FragmentSettingDownloadsBinding>() {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: DownloadSettings

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingDownloadsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = settings.getPageName(context)
        }

        updateRendering()
    }

    private fun updateRendering() {
        val context = context ?: return

        settings.downloadDirectory.bindTo(
            binding.downloadDirectory,
            { getCurrentDownloadDirectory() },
            { setting, currentValue ->
                openDocumentTreeLauncher.launch(null)
            },
        )
        settings.resetDownloadDirectory.bindTo(
            binding.resetDownloadDirectory,
        ) {
            preferences.reset(KEY_DOWNLOAD_DIRECTORY)
            updateRendering()
        }
    }

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

        updateRendering()
    }
}
