package com.idunnololz.summit.lemmy

import android.app.Application
import android.net.Uri
import android.util.Log
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.ApiListenerManager
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.UploadImageResult
import com.idunnololz.summit.cache.CachePolicyManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.imgur.ImgurApiClient
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadHelper @Inject constructor(
    private val context: Application,
    private val accountManager: AccountManager,
    private val apiListenerManager: ApiListenerManager,
    private val preferences: Preferences,
    private val directoryHelper: DirectoryHelper,
    private val imgurApiClient: ImgurApiClient,
    private val cachePolicyManager: CachePolicyManager,
) {

    companion object {
        const val TAG = "UploadHelper"

        private var nextId = 0
    }

    private val id = nextId++

    private val uploaderApiClient = LemmyApiClient(
        context = context,
        apiListenerManager = apiListenerManager,
        preferences = preferences,
        cachePolicyManager = cachePolicyManager,
        directoryHelper = directoryHelper,
    )

    private var uploadJob: Job? = null

    val isUploading: Boolean
        get() {
            val uploadJob = uploadJob

            return uploadJob != null && uploadJob.isActive
        }

    fun uploadAsync(
        coroutineScope: CoroutineScope,
        uri: Uri,
        rotateAccounts: Boolean,
        onSuccess: (UploadImageResult) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val file = File(directoryHelper.miscDir, "upload_file_$id")
        file.parentFile?.mkdirs()

        val inputStream = context.contentResolver.openInputStream(uri)

        uploadJob = coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    inputStream.use { input ->
                        file.outputStream().use { output ->
                            input?.copyTo(output)
                        }
                    }
                }

                uploadFile(file, rotateAccounts)
                    .onSuccess {
                        onSuccess(it)
                    }
                    .onFailure {
                        onFailure(it)
                    }
            } finally {
                file.delete()
            }
        }
    }

    suspend fun uploadFile(file: File, rotateAccounts: Boolean): Result<UploadImageResult> {
        try {
            val currentAccount = accountManager.currentAccount.asAccount

            val allAccounts = mutableListOf<Account>()
            if (currentAccount != null) {
                allAccounts.add(currentAccount)
            }
            if (rotateAccounts) {
                allAccounts.addAll(accountManager.getAccounts())
            }

            val attemptedInstances = mutableSetOf<String>()

            var uploadResult: Result<UploadImageResult>? = null

            if (!file.exists()) {
                return Result.failure(RuntimeException("file_not_found"))
            }

            if (preferences.uploadImagesToImgur) {
                return imgurApiClient.uploadFile(file)
                    .fold(
                        {
                            if (it.link != null) {
                                Result.success(UploadImageResult(it.link))
                            } else {
                                Result.failure(
                                    RuntimeException("Upload success but no link returned."),
                                )
                            }
                        },
                        {
                            Result.failure(it)
                        },
                    )
            }

            for (account in allAccounts) {
                val uploadInstance = account.instance

                if (attemptedInstances.contains(uploadInstance)) {
                    continue
                }
                attemptedInstances.add(uploadInstance)

                Log.d(TAG, "Uploading onto instance $uploadInstance.")
                uploaderApiClient.changeInstance(uploadInstance)

                val thisUploadResult = file.inputStream().use { inputStream ->
                    uploaderApiClient.uploadImage(account, "image", inputStream)
                }

                if (uploadResult == null) {
                    uploadResult = thisUploadResult
                }

                if (thisUploadResult.isSuccess) {
                    uploadResult = thisUploadResult
                    break
                }
            }

            return uploadResult!!
        } catch (e: SecurityException) {
            Log.e(TAG, "Upload failed with an exception.", e)
            return Result.failure(e)
        }
    }
}
