package com.idunnololz.summit.search

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.ItemCustomSearchSuggestionBinding
import com.idunnololz.summit.databinding.ItemCustomSearchSuggestionCommunityBinding
import com.idunnololz.summit.databinding.ItemCustomSearchSuggestionsDividerBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.search.SearchSuggestionsHelper
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CustomSearchSuggestionsAdapter(
  private val coroutineScope: CoroutineScope,
  private val searchSuggestionsHelper: SearchSuggestionsHelper,
  private val avatarHelper: AvatarHelper,
  private val onCommunityClick: (CommunityRef) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  companion object {
    private val TAG = CustomSearchSuggestionsAdapter::class.java.simpleName
  }

  private sealed class Item {
    data object HeaderItem : Item()
    data class SuggestionItem(
      val suggestion: String,
    ) : Item()
    data object DividerItem : Item()
    data class CommunitySuggestionItem(
      val communityRef: CommunityRef,
      val communityView: CommunityView,
    ) : Item()
    data object FooterItem : Item()
  }

  private var result: SearchSuggestionsHelper.Result? = null
  private var listener: OnSuggestionListener? = null

  var copyTextToSearchViewClickedListener: ((String) -> Unit)? = null

  private val adapterHelper = AdapterHelper<Item>(
    { old, new ->
      old::class == new::class &&
        when (old) {
          is Item.SuggestionItem ->
            old.suggestion == (new as Item.SuggestionItem).suggestion
          is Item.CommunitySuggestionItem ->
            old.communityView.community.id ==
              (new as Item.CommunitySuggestionItem).communityView.community.id
          Item.FooterItem -> true
          Item.HeaderItem -> true
          Item.DividerItem -> true
        }
    },
  ).apply {
    addItemType(Item.HeaderItem::class, ItemGenericHeaderBinding::inflate) { _, _, _ -> }
    addItemType(
      clazz = Item.DividerItem::class,
      inflateFn = ItemCustomSearchSuggestionsDividerBinding::inflate,
    ) { _, _, _ -> }
    addItemType(
      clazz = Item.SuggestionItem::class,
      inflateFn = ItemCustomSearchSuggestionBinding::inflate,
    ) { item, b, h ->
      val s = item.suggestion

      b.text.text = s
      b.copyTextToSearchView.setOnClickListener {
        copyTextToSearchViewClickedListener?.invoke(s)
      }
      h.itemView.setOnClickListener {
        listener?.onSuggestionSelected(s)
      }
      h.itemView.setOnLongClickListener {
        listener?.onSuggestionLongClicked(s)
        true
      }
    }
    addItemType(
      clazz = Item.CommunitySuggestionItem::class,
      inflateFn = ItemCustomSearchSuggestionCommunityBinding::inflate,
    ) { item, b, h ->
      avatarHelper.loadCommunityIcon(b.icon, item.communityRef, item.communityView.community.icon)

      b.title.text = item.communityRef.getLocalizedFullNameSpannable(b.title.context)

      b.monthlyActives.visibility = View.VISIBLE

      val mau = item.communityView.counts.users_active_month
      val mauString = LemmyUtils.abbrevNumber(mau.toLong())
      @Suppress("SetTextI18n")
      b.monthlyActives.text = b.monthlyActives.context.getString(R.string.mau_format, mauString)

      h.itemView.setOnClickListener {
        onCommunityClick(item.communityRef)
      }
    }
    addItemType(Item.FooterItem::class, GenericSpaceFooterItemBinding::inflate) { _, _, _ -> }
  }

  init {
    coroutineScope.launch {
      searchSuggestionsHelper.result.collect {
        result = it
        refreshItems()
      }
    }
  }

  fun clearSuggestions() {
    searchSuggestionsHelper.clearSuggestions()
  }

  fun setQuery(query: String) {
    searchSuggestionsHelper.query.value = query
  }

  private fun refreshItems() {
    val result = result ?: return
    val suggestions = result.suggestions ?: return

    setNewItems(
      buildList {
        add(Item.HeaderItem)

        suggestions.mapTo(this) { Item.SuggestionItem(it) }

        if (!result.communityResults.isNullOrEmpty()) {
          if (suggestions.isNotEmpty()) {
            add(Item.DividerItem)
          }
          result.communityResults.mapTo(this) {
            Item.CommunitySuggestionItem(
              communityRef = it.community.toCommunityRef(),
              communityView = it,
            )
          }
        }

        add(Item.FooterItem)
      },
    )
  }

  private fun setNewItems(newItems: List<Item>) {
    adapterHelper.setItems(newItems, this)
  }

  override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
    adapterHelper.onCreateViewHolder(parent, viewType)

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
    adapterHelper.onBindViewHolder(holder, position)

  override fun getItemCount(): Int = adapterHelper.itemCount

  fun setListener(listener: OnSuggestionListener) {
    this.listener = listener
  }

  interface OnSuggestionListener {
    fun onSuggestionsChanged(newSuggestions: List<String>)
    fun onSuggestionSelected(query: String)
    fun onSuggestionLongClicked(query: String)
  }
}
