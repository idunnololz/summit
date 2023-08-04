package com.idunnololz.summit.search

import android.app.SearchManager
import android.app.SearchableInfo
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
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

        private const val INVALID_INDEX = -1

        private fun getStringOrNull(cursor: Cursor, col: Int): String? {
            if (col == INVALID_INDEX) {
                return null
            }

            return try {
                cursor.getString(col)
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "unexpected error retrieving valid column from cursor, " +
                        "did the remote process die?",
                    e,
                )
                null
            }
        }
    }

    private sealed class Item {
        data class SuggestionItem(
            val suggestion: String,
        ) : Item()
        object FooterItem : Item()
    }

    private var query: String? = null

    // Cached column indexes, updated when the cursor changes.
    private var text1Col = INVALID_INDEX

    private var suggestions: List<String> = ArrayList(QUERY_LIMIT)
    private var items: List<Item> = listOf()
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var listener: OnSuggestionListener? = null

    var copyTextToSearchViewClickedListener: ((String) -> Unit)? = null

    private var refreshSuggestionsJob: Job? = null

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

    fun setQuery(query: String) {
        this.query = query

        refreshSuggestions()
    }

    private fun refreshSuggestions() {
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
                            getStringOrNull(c, text1Col)?.let {
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
        val oldItems = items

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldItems[oldItemPosition]
                val newItem = newItems[newItemPosition]

                if (oldItem::class != newItem::class) {
                    return false
                }

                return when (oldItem) {
                    Item.FooterItem -> true
                    is Item.SuggestionItem ->
                        oldItem.suggestion == (newItem as Item.SuggestionItem).suggestion
                }
            }

            override fun getOldListSize(): Int = oldItems.size

            override fun getNewListSize(): Int = newItems.size

            override fun areContentsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int,
            ): Boolean {
                return oldItems[oldItemPosition] == newItems[newItemPosition]
            }
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            Item.FooterItem -> R.layout.generic_space_footer_item
            is Item.SuggestionItem -> R.layout.custom_search_suggestion
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = inflater.inflate(viewType, parent, false)

        return when (viewType) {
            R.layout.custom_search_suggestion -> SuggestionViewHolder(v)
            else -> object : RecyclerView.ViewHolder(v) {}
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            Item.FooterItem -> {}
            is Item.SuggestionItem -> {
                val h = holder as SuggestionViewHolder
                val s = item.suggestion

                h.text.text = s
                h.copyTextToSearchView.setOnClickListener {
                    copyTextToSearchViewClickedListener?.invoke(s)
                }
                h.itemView.setOnClickListener {
                    listener?.onSuggestionSelected(s)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun setListener(listener: OnSuggestionListener) {
        this.listener = listener
    }

    private class SuggestionViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var text: TextView = v.findViewById(R.id.text)
        var copyTextToSearchView: View = v.findViewById(R.id.copyTextToSearchView)
    }

    interface OnSuggestionListener {
        fun onSuggestionsChanged(newSuggestions: List<String>)
        fun onSuggestionSelected(query: String)
    }
}