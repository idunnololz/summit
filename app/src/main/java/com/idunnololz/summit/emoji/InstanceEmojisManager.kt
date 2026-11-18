package com.idunnololz.summit.emoji

import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.lemmy.CustomEmoji
import com.idunnololz.summit.api.dto.lemmy.CustomEmojiView
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.dateStringToTs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstanceEmojisManager @Inject constructor(
  private val accountManager: AccountManager,
  private val lemmyApiClient: AccountAwareLemmyClient,
  private val coroutineScopeFactory: CoroutineScopeFactory,
) {

  data class InstanceEmoji(
    val id: Long,
    val shortcode: String,
    val imageUrl: String,
    val altText: String,
    val category: String,
    val publishedMs: Long,
    val updatedMs: Long? = null,
  )

  private val coroutineScope: CoroutineScope = coroutineScopeFactory.create()

  private var allEmojis: List<InstanceEmoji>? = null

  private var keyToEmojiMapping: Map<String, InstanceEmoji> = mapOf()

  init {
    coroutineScope.launch {
      lemmyApiClient.instanceFlow.collect { instance ->
        lemmyApiClient.fetchSiteWithRetry(force = false)
          .onSuccess {
            loadEmojisFromSite(instance, it.custom_emojis)
          }
      }
    }
  }

  fun queryEmojis(startsWith: String): List<InstanceEmoji> {
    return keyToEmojiMapping.entries
      .filter {
        it.key.startsWith(startsWith, ignoreCase = true)
      }
      .map { it.value }
      .distinct()
  }

  fun loadEmojisFromSite(instance: String, emojis: List<CustomEmojiView>) {
    val newEmojis = mutableListOf<InstanceEmoji>()
    val newKeyToEmojiMapping = mutableMapOf<String, InstanceEmoji>()
    emojis.forEach { emojiView ->
      val emoji = emojiView.custom_emoji
      val instanceEmoji = InstanceEmoji(
        id = emoji.id.toLong(),
        shortcode = emoji.shortcode,
        imageUrl = emoji.image_url,
        altText = emoji.alt_text,
        category = emoji.category,
        publishedMs = dateStringToTs(emoji.published),
        updatedMs = emoji.updated?.let { dateStringToTs(it) },
      )

      newEmojis.add(instanceEmoji)

      emojiView.keywords.forEach {
        newKeyToEmojiMapping[it.keyword] = instanceEmoji
      }
    }

    allEmojis = newEmojis
    keyToEmojiMapping = newKeyToEmojiMapping
  }
}