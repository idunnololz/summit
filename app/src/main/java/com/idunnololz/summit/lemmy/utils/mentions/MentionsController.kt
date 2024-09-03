package com.idunnololz.summit.lemmy.utils.mentions

import android.graphics.Rect
import android.text.Editable
import android.text.Layout
import android.util.Log
import android.view.Gravity
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.idunnololz.summit.R
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.view.CustomTextInputEditText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import info.debatty.java.stringsimilarity.NGram
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MentionsController"

/**
 * This class should live no longer than the view.
 */
class MentionsController @AssistedInject constructor(
    @Assisted private val lifecycleOwner: LifecycleOwner,
    @Assisted private val editText: CustomTextInputEditText,
    private val apiClient: AccountAwareLemmyClient,
    private val mentionsAdapterFactory: MentionsResultAdapter.Factory,
    private val animationsHelper: AnimationsHelper,
) {
    @AssistedFactory
    interface Factory {
        fun create(
            lifecycleOwner: LifecycleOwner,
            editText: CustomTextInputEditText,
        ): MentionsController
    }

    private val coroutineScope = lifecycleOwner.lifecycleScope
    private val mentionsAutoCompleteRepository = MentionsAutoCompleteRepository(
        lifecycleOwner.lifecycleScope,
        apiClient,
    )

    private var currentQueryPopupWindow: MentionsAutoCompletePopupWindow? = null

    init {
        mentionsAutoCompleteRepository.queryResult.observe(lifecycleOwner) {
            it ?: return@observe

            if (editText.isFocused) {
                showPopupWindow(editText, it)
            }
        }
    }

    fun setup() {
        editText.handleMentions()
    }

    private fun CustomTextInputEditText.handleMentions() {
        selectionChangedListener = a@{
            val cursorPosition = selectionStart

            if (selectionEnd != cursorPosition) {
                Log.d("HAHA", "end not eq to start.. exiting")
                return@a
            }

            val query = extractQueryIfExists(text, cursorPosition)

            if (query != null) {
                mentionsAutoCompleteRepository.submitQuery(query)
            } else {
                if (hideQueryPopup()) {
                    mentionsAutoCompleteRepository.resetQuery()
                }
            }
        }
    }

    /**
     * @return true if the popup is hidden. False is the popup was not showing and this was a no-op.
     */
    private fun hideQueryPopup(): Boolean {
        if (currentQueryPopupWindow?.isShowing == true) {
            currentQueryPopupWindow?.dismiss()
            currentQueryPopupWindow = null

            return true
        }
        return false
    }

    private fun extractQueryIfExists(text: Editable?, cursorPosition: Int): String? {
        text ?: return null

        var position = cursorPosition - 1
        while (true) {
            if (position == -1) break

            if (position >= text.length) {
                position--
                continue
            }

            val char = text[position]

            if (char.isWhitespace()) break

            if (char == '!' || char == '@') {
                // mention detected!

                val lookAhead = text.getOrNull(position - 1)

                if (lookAhead == null || lookAhead.isWhitespace()) {
                    return text.substring(position, cursorPosition)
                }
            }

            position--
        }
        return null
    }

    private fun showPopupWindow(anchor: EditText, queryResult: QueryResult) {
        if (queryResult.results.isEmpty()) {
            hideQueryPopup()
            return
        }

        if (currentQueryPopupWindow?.isShowing == true) {
            currentQueryPopupWindow?.adapter?.setItems(queryResult.results) {
                currentQueryPopupWindow?.binding?.recyclerView?.scrollToPosition(0)
                editText.post {
                    currentQueryPopupWindow?.apply {
                        update()
                    }
                }
            }
            return
        }

        val popupMargin = 0 // margin is built into the background
        val backgroundPadding = anchor.context.resources.getDimensionPixelSize(R.dimen.padding) * 2

        currentQueryPopupWindow = MentionsAutoCompletePopupWindow(
            context = anchor.context,
            adapterFactory = mentionsAdapterFactory,
            animationsHelper = animationsHelper,
            onItemSelected = a@{
                val query = extractQueryIfExists(editText.text, editText.selectionStart)
                    ?: return@a
                val position = editText.selectionStart
                editText.text?.replace(
                    position - query.length,
                    position,
                    "$it ",
                )
            },
        ).apply {
            adapter.setItems(queryResult.results) {
                currentQueryPopupWindow?.binding?.recyclerView?.scrollToPosition(0)
                binding.root.post {
                    currentQueryPopupWindow?.apply {
                        update()
                    }
                }
            }
        }.also { popupWindow ->
            val anchorPos = IntArray(2).apply {
                anchor.getLocationOnScreen(this)
            }
            val pos = anchor.selectionStart
            val layout: Layout = anchor.layout
            val line: Int = layout.getLineForOffset(pos)
            val lineTop = layout.getLineTop(line)
            val lineBottom = layout.getLineBottom(line)
            val lineHeight = lineBottom - lineTop

            val visibleDisplayFrame = Rect()
            var bottomInset = 0
            requireNotNull(editText.rootView).apply {
                getWindowVisibleDisplayFrame(visibleDisplayFrame)
                bottomInset = editText.rootView.height - visibleDisplayFrame.bottom
            }

            val yOffset = lineTop + anchorPos[1] + anchor.paddingTop

            val distanceToBottom = visibleDisplayFrame.bottom - yOffset
            val distanceToTop: Int = yOffset - visibleDisplayFrame.top

            // anchorPos[1] is distance from anchor to top of screen
            val maxHeight =
                max(distanceToBottom.toDouble(), distanceToTop.toDouble()).toInt()

//            popupWindow.height = returnedHeight
            popupWindow.binding.cardView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                matchConstraintMaxHeight = maxHeight - popupMargin - backgroundPadding
            }

            if (distanceToBottom > distanceToTop) {
                popupWindow.showAtLocation(
                    editText.rootView,
                    Gravity.TOP,
                    0,
                    yOffset + lineHeight + popupMargin,
                )
            } else {
                popupWindow.showAtLocation(
                    editText.rootView,
                    Gravity.BOTTOM,
                    0,
//                    (getMainActivity()?.insets?.value?.imeHeight ?: 0) +
                    bottomInset + (visibleDisplayFrame.bottom - (yOffset)) + popupMargin,
                )
            }
        }
    }
}

@OptIn(FlowPreview::class)
class MentionsAutoCompleteRepository(
    private val coroutineScope: CoroutineScope,
    private val apiClient: AccountAwareLemmyClient,
) {

    private val queryFlow = MutableSharedFlow<String>()
    private var queryJob: Job? = null
    private val trigram = NGram(3)

    private val internalResult = MutableStateFlow<QueryResult?>(null)

    private val queryContext = Dispatchers.Default.limitedParallelism(1)

    val queryResult = MutableLiveData<QueryResult?>()

    init {
        coroutineScope.launch(queryContext) {
            queryFlow
                .debounce(100)
                .collect {
                    doQuery(it)
                }
        }
        coroutineScope.launch {
            internalResult.collect { result ->
                withContext(Dispatchers.Main) {
                    Log.d("HAHA", "query: ${result?.rawQuery}")
                    queryResult.value = result
                }
            }
        }
    }

    private suspend fun doQuery(queryString: String) = withContext(queryContext) a@{
        if (queryString.isBlank()) {
            internalResult.emit(null)
            return@a
        }

        if (queryString.length == 1) {
            internalResult.emit(
                QueryResult(
                    results = listOf(),
                    fullQuery = "",
                    nameQuery = "",
                    instanceQuery = "",
                    rawQuery = queryString,
                    isLoading = false,
                    isQueryBlank = true,
                ),
            )
            return@a
        }

        val queryPrefix = queryString.take(1)
        val actualQuery = queryString.substring(1)
        val tokens = actualQuery.split("@")
        val nameQuery = tokens[0]
        val instanceQuery = tokens.getOrElse(1) { "" }

        val currentResult = withContext(Dispatchers.Main) {
            internalResult.value
        }

        if (currentResult != null) {
            if (actualQuery == currentResult.fullQuery) {
                return@a
            }
            internalResult.emit(currentResult.copy(isLoading = true))
        }
        queryJob?.cancel()
        queryJob = coroutineScope.launch a@{
            coroutineScope {
                val results = if (currentResult?.nameQuery == nameQuery) {
                    currentResult.results
                } else {
                    val communityQuery = async(Dispatchers.IO) {
                        apiClient
                            .search(
                                sortType = SortType.TopMonth,
                                listingType = ListingType.All,
                                searchType = SearchType.Communities,
                                query = nameQuery,
                                limit = 10,
                            )
                    }
                    val personQuery = async(Dispatchers.IO) {
                        apiClient
                            .search(
                                sortType = SortType.TopMonth,
                                listingType = ListingType.All,
                                searchType = SearchType.Users,
                                query = nameQuery,
                                limit = 10,
                            )
                    }

                    val results = mutableListOf<ResultItem>()

                    listOf(
                        communityQuery.await(),
                        personQuery.await(),
                    ).forEach {
                        it
                            .onSuccess {
                                it.communities.mapTo(results) {
                                    CommunityResultItem(
                                        communityView = it,
                                        mentionPrefix = queryPrefix,
                                        sortKey = it.community.fullName,
                                    )
                                }
                                it.users.mapTo(results) {
                                    PersonResultItem(
                                        personView = it,
                                        mentionPrefix = queryPrefix,
                                        sortKey = it.person.fullName,
                                        bio = it.person.bio?.take(100),
                                    )
                                }
                            }
                            .onFailure {
                                // do nothing...
                            }
                    }

                    results
                }

                internalResult.emit(
                    QueryResult(
                        results = results.sortedBy {
                            trigram.distance(
                                it.sortKey,
                                actualQuery,
                            )
                        },
                        fullQuery = actualQuery,
                        nameQuery = nameQuery,
                        instanceQuery = instanceQuery,
                        rawQuery = queryString,
                        isLoading = false,
                        isQueryBlank = false,
                    ),
                )
            }
        }
    }

    fun submitQuery(queryString: String) {
        coroutineScope.launch(Dispatchers.Default) {
            queryFlow.emit(queryString)
        }
    }

    fun resetQuery() {
        coroutineScope.launch(Dispatchers.Default) {
            queryFlow.emit("")
        }
    }
}

data class QueryResult(
    val results: List<MentionsAutoCompleteItem>,
    /**
     * Eg. "a@b.com"
     */
    val fullQuery: String,
    /**
     * Eg. "a"
     */
    val nameQuery: String,
    /**
     * Eg. "b.com"
     */
    val instanceQuery: String,
    /**
     * Eg. "!a@b.com"
     */
    val rawQuery: String,
    val isLoading: Boolean,
    val isQueryBlank: Boolean,
)

sealed interface MentionsAutoCompleteItem {
    val sortKey: String
}

data object EmptyItem : MentionsAutoCompleteItem {
    override val sortKey: String
        get() = ""
}

sealed interface ResultItem : MentionsAutoCompleteItem
data class CommunityResultItem(
    override val sortKey: String,
    val mentionPrefix: String,
    val communityView: CommunityView,
) : ResultItem

data class PersonResultItem(
    override val sortKey: String,
    val mentionPrefix: String,
    val personView: PersonView,
    val bio: String?,
) : ResultItem
