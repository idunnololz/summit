package com.idunnololz.summit.lemmy.utils

import androidx.lifecycle.SavedStateHandle
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.Language
import com.idunnololz.summit.api.dto.lemmy.LanguageId
import com.idunnololz.summit.util.arrow.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.collections.ifEmpty

class LanguageHelper(
  private val coroutineScope: CoroutineScope,
  private val apiClient: AccountAwareLemmyClient,
  private val state: SavedStateHandle,
) {

  private var siteLanguages: List<Language> = listOf()
  private var communityLanguages: List<Int> = listOf()

  private var fetchSiteLanguagesJob: Job? = null
  private var fetchCommunityLanguagesJob: Job? = null

  val languageOptions = state.getLiveData<List<Language>>("LanguageHelper.languages")

  init {
    fetchSiteLanguagesJob = coroutineScope.launch {
      apiClient.fetchSiteWithRetry(force = false)
        .onSuccess { site ->
          val discussionLanguages = site.all_languages
            .filter { site.discussion_languages.contains(it.id) }

          siteLanguages = discussionLanguages.ifEmpty {
            site.all_languages
          }

          updateLanguageOptions()
        }
    }
  }

  fun setCommunityIdOrName(idOrName: Either<Int, String>) {
    fetchCommunityLanguagesJob = coroutineScope.launch {
      apiClient.fetchCommunityWithRetry(idOrName, false)
        .onSuccess { community ->
          val discussionLanguages = community.discussion_languages

          communityLanguages = discussionLanguages
          updateLanguageOptions()
        }
    }
  }

  suspend fun getValidLanguage(preferredLanguageId: LanguageId?): LanguageId? {
    if (preferredLanguageId == null) return null

    fetchSiteLanguagesJob?.join()
    fetchCommunityLanguagesJob?.join()

    val languageOptions = languageOptions.value
      ?: return null
    return if (languageOptions.any { it.id == preferredLanguageId }) {
      preferredLanguageId
    } else {
      null
    }
  }

  private fun updateLanguageOptions() {
    val siteLanguages = siteLanguages
    val communityLanguages = communityLanguages

    if (siteLanguages.isEmpty()) return

    languageOptions.value = siteLanguages
      .filter { communityLanguages.contains(it.id) }
      .ifEmpty { siteLanguages }
  }
}