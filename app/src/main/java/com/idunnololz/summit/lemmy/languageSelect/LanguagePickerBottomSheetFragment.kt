package com.idunnololz.summit.lemmy.languageSelect

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.Language
import com.idunnololz.summit.api.dto.lemmy.LanguageId
import com.idunnololz.summit.databinding.CommunitySelectorNoResultsItemBinding
import com.idunnololz.summit.databinding.FragmentLanguagePickerBottomSheetBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.databinding.ItemLanguagePickerChoiceBinding
import com.idunnololz.summit.databinding.ItemLanguagePickerDividerItemBinding
import com.idunnololz.summit.databinding.ItemLanguagePickerGroupTitleBinding
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.toBidiSafe
import com.idunnololz.summit.util.fixBottomSheetFling
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class LanguagePickerBottomSheetFragment :
  BaseBottomSheetDialogFragment<FragmentLanguagePickerBottomSheetBinding>(),
  FullscreenDialogFragment {

  companion object {
    const val REQUEST_KEY = "LanguagePickerBottomSheetFragment.request"
    const val RESULT_KEY = "LanguagePickerBottomSheetFragment.result"

    private const val NO_LANGUAGE_ID = -1
    private const val MAX_RECENT_LANGUAGES = 5

    fun show(
      fragmentManager: FragmentManager,
      languages: List<Language>,
      selectedLanguageId: LanguageId?,
    ) {
      LanguagePickerBottomSheetFragment()
        .apply {
          arguments = LanguagePickerBottomSheetFragmentArgs(
            languages = languages.toTypedArray(),
            selectedLanguageId = selectedLanguageId ?: NO_LANGUAGE_ID,
          ).toBundle()
        }
        .show(fragmentManager, "LanguagePickerBottomSheetFragment")
    }
  }

  @Parcelize
  data class Result(
    val languageId: LanguageId?,
  ) : Parcelable

  private val args by navArgs<LanguagePickerBottomSheetFragmentArgs>()

  @Inject
  lateinit var globalStateStorage: GlobalStateStorage

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    setBinding(FragmentLanguagePickerBottomSheetBinding.inflate(inflater, container, false))
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      fixBottomSheetFling(recyclerView)

      val languages = args.languages.toList()
      val selectedLanguageId = args.selectedLanguageId
        .takeIf { it != NO_LANGUAGE_ID }
      val recentLanguagesById = languages.associateBy { it.id }
      val recentLanguages = globalStateStorage.recentLanguageIds
        .mapNotNull(recentLanguagesById::get)
        .take(MAX_RECENT_LANGUAGES)

      val adapter = LanguagesAdapter(
        context = context,
        languages = languages,
        recentLanguages = recentLanguages,
        selectedLanguageId = selectedLanguageId,
        onLanguageSelected = {
          onLanguageSelected(it)
        },
      )

      recyclerView.apply {
        layoutManager = LinearLayoutManager(context)
        setHasFixedSize(true)
        this.adapter = adapter
      }
      searchEditText.doOnTextChanged { text, _, _, _ ->
        adapter.updateQuery(text?.toString().orEmpty()) {
          recyclerView.scrollToPosition(0)
        }
      }
    }
  }

  private fun onLanguageSelected(languageId: LanguageId?) {
    if (languageId != null) {
      globalStateStorage.recentLanguageIds = buildList {
        add(languageId)
        addAll(globalStateStorage.recentLanguageIds.filterNot { it == languageId })
      }.take(MAX_RECENT_LANGUAGES)
    }

    setFragmentResult(
      REQUEST_KEY,
      Bundle().apply {
        putParcelable(RESULT_KEY, Result(languageId))
      },
    )
    dismiss()
  }

  private class LanguagesAdapter(
    private val context: Context,
    private val languages: List<Language>,
    private val recentLanguages: List<Language>,
    private val selectedLanguageId: LanguageId?,
    private val onLanguageSelected: (LanguageId?) -> Unit,
  ) : Adapter<ViewHolder>() {

    private sealed interface Item {
      data object FirstItem : Item

      data class Header(
        val title: String,
      ) : Item

      data object NoResults : Item

      data class LanguageChoice(
        val section: Section,
        val name: String,
        val languageId: LanguageId?,
        val isSelected: Boolean,
      ) : Item

      data object DividerItem : Item
    }

    private enum class Section {
      RECENT,
      ALL,
    }

    private val adapterHelper = AdapterHelper<Item>(
      areItemsTheSame = { old, new ->
        old::class == new::class &&
          when (old) {
            is Item.Header -> old.title == (new as Item.Header).title
            Item.NoResults -> true
            is Item.LanguageChoice -> {
              new as Item.LanguageChoice
              old.section == new.section && old.languageId == new.languageId
            }
            Item.FirstItem -> true
            Item.DividerItem -> false
          }
      },
    ).apply {
      addItemType(
        Item.FirstItem::class,
        ItemGenericHeaderBinding::inflate,
      ) { _, _, _ -> }
      addItemType(
        Item.Header::class,
        ItemLanguagePickerGroupTitleBinding::inflate,
      ) { item, b, _ ->
        b.titleTextView.text = item.title
      }
      addItemType(
        Item.NoResults::class,
        CommunitySelectorNoResultsItemBinding::inflate,
      ) { _, b, _ ->
        b.text.setText(R.string.no_results_found)
      }
      addItemType(
        Item.LanguageChoice::class,
        ItemLanguagePickerChoiceBinding::inflate,
      ) { item, b, _ ->
        b.text.text = item.name
        b.selected.isVisible = item.isSelected
        b.root.setOnClickListener { onLanguageSelected(item.languageId) }
      }
      addItemType(
        Item.DividerItem::class,
        ItemLanguagePickerDividerItemBinding::inflate,
      ) { _, _, _ -> }
    }

    private var query = ""

    init {
      refreshItems {}
    }

    fun updateQuery(query: String, cb: () -> Unit) {
      this.query = query
      refreshItems(cb)
    }

    fun refreshItems(cb: () -> Unit) {
      val query = query.trim()
      val filteredRecentLanguages = recentLanguages.filter { it.matches(query) }
      val filteredLanguages = languages.filter { it.matches(query) }
      val showUnspecified = context.getString(R.string.unspecified)
        .contains(query, ignoreCase = true)
      val items = mutableListOf<Item>()
      var resultsCount = 0

      items += Item.FirstItem

      if (filteredRecentLanguages.isNotEmpty()) {
        items += Item.Header(context.getString(R.string.recents))

        for ((index, filteredRecentLanguage) in filteredRecentLanguages.withIndex()) {
          resultsCount++

          items += Item.LanguageChoice(
            section = Section.RECENT,
            name = filteredRecentLanguage.name.toBidiSafe(),
            languageId = filteredRecentLanguage.id,
            isSelected = filteredRecentLanguage.id == selectedLanguageId,
          )
          if (index != filteredRecentLanguages.lastIndex) {
            items += Item.DividerItem
          }
        }
      }

      if (showUnspecified || filteredLanguages.isNotEmpty()) {
        items += Item.Header(context.getString(R.string.all_languages))
        if (showUnspecified) {
          resultsCount++
          items += Item.LanguageChoice(
            section = Section.ALL,
            name = context.getString(R.string.unspecified),
            languageId = null,
            isSelected = selectedLanguageId == null,
          )

          if (filteredLanguages.isNotEmpty()) {
            items += Item.DividerItem
          }
        }

        for ((index, filteredLanguage) in filteredLanguages.withIndex()) {
          resultsCount++
          items += Item.LanguageChoice(
            section = Section.ALL,
            name = filteredLanguage.name.toBidiSafe(),
            languageId = filteredLanguage.id,
            isSelected = filteredLanguage.id == selectedLanguageId,
          )
          if (index != filteredLanguages.lastIndex) {
            items += Item.DividerItem
          }
        }
      }

      if (resultsCount == 0) {
        items += Item.NoResults
      }

      adapterHelper.setItems(items, this, cb)
    }

    private fun Language.matches(query: String): Boolean = query.isEmpty() ||
      name.contains(query, ignoreCase = true) ||
      code.contains(query, ignoreCase = true)

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    override fun getItemCount(): Int = adapterHelper.itemCount
  }
}
