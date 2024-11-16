package com.idunnololz.summit.emoji

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.emoji.db.TextEmojiEntry
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class EmojiPopupEditorViewModel @Inject constructor(
    private val textEmojisManager: TextEmojisManager,
) : ViewModel() {

    val model = StatefulLiveData<EmojiPopupEditorModel>()

    fun loadData(force: Boolean = false) {
        model.setIsLoading()

        viewModelScope.launch {
            model.postValue(
                EmojiPopupEditorModel(
                    emojis = textEmojisManager.getAllEmojis(reload = force),
                ),
            )
        }
    }

    fun addOrUpdateTextEmoji(id: Long, textEmoji: String, delete: Boolean) {
        viewModelScope.launch {
            if (delete) {
                textEmojisManager.delete(id)
            } else {
                textEmojisManager.addOrUpdateTextEmoji(id, textEmoji)
            }
            loadData(force = true)
        }
    }

    fun resetEmojis() {
        viewModelScope.launch {
            textEmojisManager.reset()
            loadData(force = true)
        }
    }

    fun commitChanges(items: List<TextEmojiEntry>) {
        viewModelScope.launch {
            textEmojisManager.updateItems(items)
            loadData(force = true)
        }
    }
}

data class EmojiPopupEditorModel(
    val emojis: List<TextEmoji>,
)
