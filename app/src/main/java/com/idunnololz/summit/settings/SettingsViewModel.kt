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
    private val searchableSettings: Lazy<AllSettings>,
) : ViewModel() {

    val showSearch = MutableLiveData<Boolean>()

    private var currentQuery = MutableStateFlow("")
    private val trigram = NGram(3)

    val searchResults = MutableLiveData<List<SettingSearchResultItem>>()
    var searchIdToPage: Map<Int, SearchableSettings> = mapOf()

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

        val results = mutableListOf<SettingSearchResultItem>()
        val settingIdToSettingPage = mutableMapOf<Int, SearchableSettings>()

        allSettings.forEach { page ->
            page.allSettings.forEach {
                recursiveSearch(it, query, page, results, settingIdToSettingPage)
            }
        }

        results.sortBy {
            trigram.distance(
                it.settingItem.title + (it.settingItem.description ?: ""),
                query,
            )
        }

        searchIdToPage = settingIdToSettingPage

        withContext(Dispatchers.Main) {
            searchResults.value = results
        }
    }

    private fun recursiveSearch(
        settingItem: SettingItem,
        query: String,
        settingPage: SearchableSettings,
        result: MutableList<SettingSearchResultItem>,
        settingIdToSettingPage: MutableMap<Int, SearchableSettings>,
    ) {
        fun addItem() {
            if (settingItem.title.contains(query, ignoreCase = true) ||
                settingItem.description?.contains(query, ignoreCase = true) == true
            ) {
                result += SettingSearchResultItem(
                    settingItem,
                    settingPage,
                )
            }
            settingIdToSettingPage[settingItem.id] = settingPage
        }

        when (settingItem) {
            is BasicSettingItem,
            is ImageValueSettingItem,
            is OnOffSettingItem,
            is RadioGroupSettingItem,
            is SliderSettingItem,
            is TextOnlySettingItem,
            is TextValueSettingItem,
            is ColorSettingItem,
            -> {
                addItem()
            }
            is SubgroupItem -> {
                addItem()

                settingItem.settings.forEach {
                    recursiveSearch(it, query, settingPage, result, settingIdToSettingPage)
                }
            }
        }
    }

    data class SettingSearchResultItem(
        val settingItem: SettingItem,
        val page: SearchableSettings,
    )
}
