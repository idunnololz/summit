package com.idunnololz.summit.emoji

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.emoji.db.TextEmojiEntry
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmojiPopupEditorViewModel @Inject constructor(
    private val textEmojisManager: TextEmojisManager
) : ViewModel() {

    val model = StatefulLiveData<EmojiPopupEditorModel>()

    fun loadData(force: Boolean = false) {
        model.setIsLoading()

        viewModelScope.launch {
            model.postValue(
                EmojiPopupEditorModel(
                    emojis = textEmojisManager.getAllEmojis(reload = force)
                )
            )

        }
    }

    fun addOrUpdateTextEmoji(id: Long, textEmoji: String) {
        viewModelScope.launch {
            textEmojisManager.addOrUpdateTextEmoji(id, textEmoji)
            loadData(force = true)
        }
    }

    fun commitChanges(map: List<TextEmojiEntry>) {

    }

}

data class EmojiPopupEditorModel(
    val emojis: List<TextEmoji>
) {
}