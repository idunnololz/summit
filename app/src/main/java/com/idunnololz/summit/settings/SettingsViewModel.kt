package com.idunnololz.summit.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import info.debatty.java.stringsimilarity.NGram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val searchableSettings: Lazy<AllSettings>
) : ViewModel() {

    val showSearch = MutableLiveData<Boolean>()

    private var currentQuery = MutableStateFlow("")
    private val trigram = NGram(3)

    val searchResults = MutableLiveData<List<SettingItem>>()

    init {
        viewModelScope.launch {
            currentQuery.collect {
                generateSearchItems()
            }
        }
    }

    fun query(query: String?) {
        viewModelScope.launch {
            currentQuery.emit(query ?: "")
        }
    }

    private suspend fun generateSearchItems() = withContext(Dispatchers.Default) {
        val allSettings = searchableSettings.get().allSearchableSettings
        val query = currentQuery.value

        val results = mutableListOf<SettingItem>()

        allSettings.forEach {
            it.allSettings.forEach {
                recursiveSearch(it, query, results)
            }
        }

        results.sortBy {
            trigram.distance(
                it.title + (it.description ?: ""),
                query
            )
        }

        withContext(Dispatchers.Main) {
            searchResults.value = results
        }
    }

    private fun recursiveSearch(
        settingItem: SettingItem,
        query: String,
        result: MutableList<SettingItem>,
    ) {
        when (settingItem) {
            is BasicSettingItem,
            is ImageValueSettingItem,
            is OnOffSettingItem,
            is RadioGroupSettingItem,
            is SliderSettingItem,
            is TextOnlySettingItem,
            is TextValueSettingItem -> {
                if (settingItem.title.contains(query, ignoreCase = true) ||
                    settingItem.description?.contains(query, ignoreCase = true) == true) {
                    result += settingItem
                }
            }
            is SubgroupItem -> {
                if (settingItem.title.contains(query, ignoreCase = true) ||
                    settingItem.description?.contains(query, ignoreCase = true) == true) {
                    result += settingItem
                }

                settingItem.settings.forEach {
                    recursiveSearch(it, query, result)
                }
            }
        }
    }
}
