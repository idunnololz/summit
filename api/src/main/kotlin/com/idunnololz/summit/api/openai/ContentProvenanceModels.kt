package com.idunnololz.summit.api.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContentProvenanceCheck(
  @SerialName("created_at") val createdAt: Long,
  val results: List<ContentProvenanceResult>,
) {
  val hasDetectedSignal: Boolean
    get() = results.any { it.isDetected }
}

@Serializable
data class ContentProvenanceResult(
  val type: String,
  val outcome: String,
  @SerialName("validation_state") val validationState: String? = null,
  val issuer: String? = null,
  val model: String? = null,
  @SerialName("generated_at") val generatedAt: String? = null,
) {
  val isDetected: Boolean
    get() = outcome == "detected"
}

class MissingOpenAiApiKeyException : IllegalStateException("An OpenAi Api key is required.")

class UnsupportedProvenanceImageException(
  val mimeType: String?,
) : IllegalArgumentException("Unsupported image format: ${mimeType ?: "unknown"}")

class ProvenanceImageTooLargeException :
  IllegalArgumentException("The image exceeds the 50 MiB limit.")

class ContentProvenanceApiException(
  val statusCode: Int,
  message: String,
) : RuntimeException(message)
