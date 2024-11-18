package com.idunnololz.summit.search

import android.app.SearchManager
import android.app.SearchableInfo
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.ItemCustomSearchSuggestionBinding
import com.idunnololz.summit.util.INVALID_INDEX
import com.idunnololz.summit.util.getStringOrNull
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

class CustomSearchSuggestionsAdapter(
    private val context: Context,
    private val searchableInfo: SearchableInfo?,
    private val coroutineScope: CoroutineScope,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {

        private val TAG = CustomSearchSuggestionsAdapter::class.java.simpleName

        private const val QUERY_LIMIT = 40
    }

    private sealed class Item {
        data class SuggestionItem(
            val suggestion: String,
        ) : Item()
        data object FooterItem : Item()
    }

    private var query: String? = null

    // Cached column indexes, updated when the cursor changes.
    private var text1Col = INVALID_INDEX

    private var suggestions: List<String> = ArrayList(QUERY_LIMIT)
    private var listener: OnSuggestionListener? = null

    var copyTextToSearchViewClickedListener: ((String) -> Unit)? = null

    private var refreshSuggestionsJob: Job? = null

    private val adapterHelper = AdapterHelper<Item>(
        { old, new ->
            old::class == new::class && when (old) {
                is Item.SuggestionItem ->
                    old.suggestion == (new as Item.SuggestionItem).suggestion
                Item.FooterItem -> true
            }
        },
    ).apply {
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
        addItemType(Item.FooterItem::class, GenericSpaceFooterItemBinding::inflate) { _, _, _ -> }
    }

    private fun getSearchManagerSuggestions(
        searchable: SearchableInfo?,
        query: String?,
        limit: Int,
    ): Cursor? {
        if (searchable == null) {
            return null
        }
        if (query == null) {
            return null
        }

        val authority = searchable.suggestAuthority ?: return null

        val uriBuilder = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(authority)
            .query("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
            .fragment("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()

        // if content path provided, insert it now
        val contentPath = searchable.suggestPath
        if (contentPath != null) {
            uriBuilder.appendEncodedPath(contentPath)
        }

        // append standard suggestion query path
        uriBuilder.appendPath(SearchManager.SUGGEST_URI_PATH_QUERY)

        // get the query selection, may be null
        val selection = searchable.suggestSelection
        // inject query, either as selection args or inline
        var selArgs: Array<String>? = null
        if (selection != null) { // use selection if provided
            selArgs = arrayOf(query)
        } else { // no selection, use REST pattern
            uriBuilder.appendPath(query)
        }

        if (limit > 0) {
            uriBuilder.appendQueryParameter("limit", limit.toString())
        }

        val uri = uriBuilder.build()

        // finally, make the query
        return context.contentResolver.query(uri, null, selection, selArgs, null)
    }

    fun clearSuggestions() {
        val searchable = searchableInfo ?: return
        val authority = searchable.suggestAuthority ?: return

        val uriBuilder = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(authority)
            .query("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
            .fragment("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
            .appendEncodedPath("suggestions")

        val uri = uriBuilder.build()

        // finally, make the query
        context.contentResolver.delete(uri, null, null)

        refreshSuggestions()
    }

    fun setQuery(query: String) {
        this.query = query

        refreshSuggestions()
    }

    fun refreshSuggestions() {
        refreshSuggestionsJob?.cancel()
        refreshSuggestionsJob = coroutineScope.launch {
            val seen = mutableSetOf<String>()
            val newSuggestions = ArrayList<String>()
            runInterruptible(Dispatchers.IO) {
                // Query 2x the limit because there might be case sensitive duplicates...
                getSearchManagerSuggestions(searchableInfo, query, QUERY_LIMIT).use { c ->
                    if (c != null) {
                        try {
                            text1Col = c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
                        } catch (e: Exception) {
                            Log.e(TAG, "error changing cursor and caching columns", e)
                        }

                        while (c.moveToNext()) {
                            c.getStringOrNull(text1Col)?.let {
                                if (seen.add(it.lowercase(Locale.US))) {
                                    newSuggestions.add(it)
                                    Log.d(TAG, "Got suggestion $it")
                                }
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                suggestions = newSuggestions
                refreshItems()

                listener?.onSuggestionsChanged(suggestions)
            }
        }
    }

    private fun refreshItems() {
        setNewItems(suggestions.map { Item.SuggestionItem(it) } + Item.FooterItem)
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

    private class SuggestionViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var text: TextView = v.findViewById(R.id.text)
        var copyTextToSearchView: View = v.findViewById(R.id.copy_text_to_search_view)
    }

    interface OnSuggestionListener {
        fun onSuggestionsChanged(newSuggestions: List<String>)
        fun onSuggestionSelected(query: String)
        fun onSuggestionLongClicked(query: String)
    }
}
