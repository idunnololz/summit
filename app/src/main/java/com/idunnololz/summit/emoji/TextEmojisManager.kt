package com.idunnololz.summit.emoji

import com.idunnololz.summit.emoji.db.TextEmojiDao
import com.idunnololz.summit.emoji.db.TextEmojiEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextEmojisManager @Inject constructor(
    private val textEmojiDao: TextEmojiDao
) {

    companion object {
        val TEXT_EMOJIS = listOf(
            "( ͡° ͜ʖ ͡° )",
            "ಠ_ಠ",
            "(╯°□°）╯︵ ┻━┻",
            "┬─┬ノ( º _ ºノ)",
            "¯\\_(ツ)_/¯",
            "༼ つ ◕_◕ ༽つ",
            "ᕕ( ᐛ )ᕗ",
            "(•_•) ( •_•)>⌐■-■ (⌐■_■)",
        )
    }

    private var allEmojis: List<TextEmoji>? = null

    private suspend fun loadEmojis() {
        var allEntries = textEmojiDao.getAll()

        if (allEntries.isEmpty()) {
            for ((index, emoji) in TEXT_EMOJIS.withIndex()) {
                textEmojiDao.insert(
                    TextEmojiEntry(
                        id = 0,
                        createdTs = System.currentTimeMillis(),
                        modifiedTs = System.currentTimeMillis(),
                        emoji = emoji,
                        order = index,
                        modifiable = false,
                    )
                )
            }

            allEntries = textEmojiDao.getAll()
        }

        val emojis = allEntries.map {
            TextEmoji(it)
        }

        allEmojis = emojis
    }

    suspend fun getAllEmojis(reload: Boolean = false): List<TextEmoji> {
        if (!reload) {
            allEmojis?.let {
                return it
            }
        }

        loadEmojis()

        return requireNotNull(allEmojis)
    }

    suspend fun addOrUpdateTextEmoji(id: Long, textEmoji: String) {
        val entry = textEmojiDao.getEntry(id).firstOrNull()

        if (entry == null) {
            textEmojiDao.insert(
                TextEmojiEntry(
                    id = 0,
                    createdTs = System.currentTimeMillis(),
                    modifiedTs = System.currentTimeMillis(),
                    emoji = textEmoji,
                    order = getAllEmojis(true).maxOfOrNull { it.order } ?: 0,
                    modifiable = true,
                )
            )
        } else {
            textEmojiDao.insert(
                entry.copy(
                    modifiedTs = System.currentTimeMillis(),
                    emoji = textEmoji,
                )
            )
        }
    }
}

data class TextEmoji(
    val entry: TextEmojiEntry
) {

    val text: String
        get() = entry.emoji

    val id: Long
        get() = entry.id

    val order: Int
        get() = entry.order
}