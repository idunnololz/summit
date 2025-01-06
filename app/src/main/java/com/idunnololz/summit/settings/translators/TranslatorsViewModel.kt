package com.idunnololz.summit.settings.translators

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.moshi
import com.squareup.moshi.Types
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class TranslatorsViewModel @Inject constructor(
    @Suppress("StaticFieldLeak") @ApplicationContext private val context: Context,
) : ViewModel() {

    val translatorStats = StatefulLiveData<Map<String, List<TranslationTranslatorStats>>>()

    fun loadTranslatorsJsonIfNeeded() {
        translatorStats.setIsLoading()

        viewModelScope.launch {
            val inputStream = context.resources.openRawResource(R.raw.translate)
            val json = BufferedReader(inputStream.reader()).use {
                it.readText()
            }

            val adapter = moshi.adapter<List<Map<String, Any>>>(
                Types.newParameterizedType(
                    List::class.java,
                    Map::class.java,
                ),
            )

            val translationStatsObject = adapter.fromJson(json)

            val languageToTranslators =
                mutableMapOf<String, MutableList<TranslationTranslatorStats>>()

            for (item in translationStatsObject ?: listOf()) {
                for ((locale, v) in item.entries) {
                    @Suppress("UNCHECKED_CAST")
                    val translatorsStats = v as List<List<Any>>

                    val translators = languageToTranslators.getOrPut(locale) { mutableListOf() }

                    for (translatorStats in translatorsStats) {
                        val (email, name, stringsTranslated) = translatorStats

                        if (email == "garyguo9@gmail.com" ||
                            email == "noreply-mt-google-translate-api-v3@weblate.org" ||
                            email == "noreply-mt-weblate@weblate.org") {
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
