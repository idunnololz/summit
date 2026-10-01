package com.idunnololz.summit.api.openai
import com.idunnololz.summit.cache.CborDiskCache
import com.idunnololz.summit.network.GenericApi
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.Utils.hashSha256
import com.idunnololz.summit.util.extensionForMimeType
import com.idunnololz.summit.util.guessMimeType
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject

class OpenAiClient @Inject constructor(
  private val preferences: Preferences,
  private val openAiApi: OpenAiApi,
  directoryHelper: DirectoryHelper,
) {
  companion object {
    private const val MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L // 50mb
    private val SUPPORTED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
  }

  val diskCache = CborDiskCache
    .create(directoryHelper.openAiCacheDir, 1, 10L * 1024L * 1024L /* 10MB */)

  suspend fun checkImage(
    imageKey: String,
    file: File,
    force: Boolean,
  ): Result<ContentProvenanceCheck> = withContext(Dispatchers.IO) {
    val cacheKey = hashSha256("img-key:${imageKey}")

    if (!force) {
      diskCache.getCachedObject<ContentProvenanceCheck>(cacheKey)?.let {
        return@withContext Result.success(it)
      }
    }

    val apiKey = preferences.openAiApiKey?.trim()
    if (apiKey.isNullOrBlank()) {
      return@withContext Result.failure(MissingOpenAiApiKeyException())
    }
    if (file.length() > MAX_FILE_SIZE_BYTES) {
      return@withContext Result.failure(ProvenanceImageTooLargeException())
    }

    val mimeType = guessMimeType(file)
    if (mimeType !in SUPPORTED_IMAGE_TYPES || mimeType == null) {
      return@withContext Result.failure(UnsupportedProvenanceImageException(mimeType))
    }

    val extension = extensionForMimeType(mimeType)
    val fileName = if (extension != null) {
      "image.$extension"
    } else {
      "image"
    }
    val filePart = MultipartBody.Part.createFormData(
      "file",
      fileName,
      file.asRequestBody(mimeType.toMediaType()),
    )

    val response = runInterruptible {
      openAiApi.createContentProvenanceCheck(
        authorization = "Bearer $apiKey",
        file = filePart,
      ).execute()
    }
    if (response.isSuccessful) {
      Result.success(
        requireNotNull(response.body()).also {
          diskCache.cacheObject(cacheKey, it)
        }
      )
    } else {
      val responseBody = response.errorBody()?.string().orEmpty()
      Result.failure(
        ContentProvenanceApiException(
          statusCode = response.code(),
          message = parseErrorMessage(responseBody, response.code()),
        )
      )
    }
  }

  private fun parseErrorMessage(body: String, statusCode: Int): String = try {
    JSONObject(body)
      .optJSONObject("error")
      ?.optString("message")
      ?.takeIf { it.isNotBlank() }
      ?: "OpenAI request failed (HTTP $statusCode)."
  } catch (_: Exception) {
    "OpenAI request failed (HTTP $statusCode)."
  }
}
