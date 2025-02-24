package com.idunnololz.summit.settings.translators

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.json.JSONArray

@HiltViewModel
class TranslatorsViewModel @Inject constructor(
    @Suppress("StaticFieldLeak") @ApplicationContext private val context: Context,
) : ViewModel() {

    val translatorStats = StatefulLiveData<Map<String, List<TranslationTranslatorStats>>>()

    fun loadTranslatorsJsonIfNeeded() {
        translatorStats.setIsLoading()

        viewModelScope.launch {
            val inputStream = context.resources.openRawResource(R.raw.translate)
            val jsonStr = BufferedReader(inputStream.reader()).use {
                it.readText()
            }

            // List<Map<String, Any>>?
            val translationStatsObject = JSONArray(jsonStr)

            val languageToTranslators =
                mutableMapOf<String, MutableList<TranslationTranslatorStats>>()

            for (index in 0 until translationStatsObject.length()) {
                val item = translationStatsObject.getJSONObject(index)

                for (locale in item.keys()) {
                    val valueJson = item.getJSONArray(locale)
                    val translators = languageToTranslators.getOrPut(locale) { mutableListOf() }

                    for (index2 in 0 until valueJson.length()) {
                        val translatorStats = valueJson.getJSONArray(index2)
                        val email = translatorStats.getString(0)
                        val name = translatorStats.getString(1)
                        val stringsTranslated = translatorStats.getDouble(2)

                        if (email == "garyguo9@gmail.com" ||
                            email == "noreply-mt-google-translate-api-v3@weblate.org" ||
                            email == "noreply-mt-weblate@weblate.org"
                        ) {
                            continue
                        }

                        translators.add(
                            TranslationTranslatorStats(
                                translatorName = name as? String ?: continue,
                                stringsTranslated = stringsTranslated as? Double ?: continue,
                            ),
                        )
                    }
                }
            }

            translatorStats.setValue(languageToTranslators)
        }
    }
}

data class TranslationLanguageStats(
    val language: String,
    val translators: List<TranslationTranslatorStats>,
)

data class TranslationTranslatorStats(
    val translatorName: String,
    val stringsTranslated: Double,
)
