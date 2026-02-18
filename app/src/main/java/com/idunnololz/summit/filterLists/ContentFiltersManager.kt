package com.idunnololz.summit.filterLists

import android.util.Log
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.crashLogger.crashLogger
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class ContentFiltersManager @Inject constructor(
  private val dao: ContentFiltersDao,
  private val coroutineScopeFactory: CoroutineScopeFactory,
) {

  companion object {
    private const val TAG = "ContentFiltersManager"
  }

  private val coroutineScope = coroutineScopeFactory.create()

  private var postListFilters: Filters = Filters(listOf())
  private var commentListFilters: Filters = Filters(listOf())

  private var regexCache = mutableMapOf<Long, Pattern>()

  init {
    coroutineScope.launch {
      refreshFilters()

      withContext(Dispatchers.IO) {
        val filterCount = dao.count()

        if (filterCount > 1000) {
          val e = TooManyFiltersException(filterCount)

          Log.e(TAG, "", e)
          crashLogger?.recordException(e)
        }
      }
    }
  }

  private suspend fun refreshFilters() {
    val filters = withContext(Dispatchers.IO) {
      dao.getAllFilters()
    }

    val postListFilters = mutableListOf<FilterEntry>()
    val commentListFilters = mutableListOf<FilterEntry>()
    for (filter in filters) {
      when (filter.contentType) {
        ContentTypeIds.PostListType -> {
          postListFilters.add(filter)
        }
        ContentTypeIds.CommentListType -> {
          commentListFilters.add(filter)
        }
      }
    }

    this.postListFilters = Filters(postListFilters)
    this.commentListFilters = Filters(commentListFilters)
  }

  /**
   * Tests a [PostView] to see if it matches any of the filters.
   * @return true if the [PostView] matches at least one filter.
   */
  fun testPostView(postView: PostView): Boolean =
    postListFilters.patternByType.any { (type, pattern) ->
      when (type) {
        FilterTypeIds.KeywordFilter -> {
          pattern.matcher(postView.post.name).find()
        }
        FilterTypeIds.InstanceFilter -> {
          pattern.matcher(postView.community.instance).find()
        }
        FilterTypeIds.CommunityFilter -> {
          pattern.matcher(postView.community.name).find()
        }
        FilterTypeIds.UserFilter -> {
          pattern.matcher(postView.creator.name).find()
        }
        FilterTypeIds.UrlFilter -> {
          val url = postView.post.url
          if (url != null) {
            pattern.matcher(url).find()
          } else {
            false
          }
        }
        else -> false
      }
    }

  /**
   * Tests a [CommentView] to see if it matches any of the filters.
   * @return true if the [CommentView] matches at least one filter.
   */
  fun testCommentView(commentView: CommentView): Boolean =
    commentListFilters.patternByType.any { (type, pattern) ->
      when (type) {
        FilterTypeIds.KeywordFilter -> {
          pattern.matcher(commentView.comment.content).find()
        }
        FilterTypeIds.InstanceFilter -> {
          pattern.matcher(commentView.creator.instance).find()
        }
        FilterTypeIds.UserFilter -> {
          pattern.matcher(commentView.creator.name).find()
        }
        else -> false
      }
    }

  fun getFilters(contentTypeId: ContentTypeId, filterTypeId: FilterTypeId): List<FilterEntry> =
    when (contentTypeId) {
      ContentTypeIds.PostListType -> {
        postListFilters.filters.filter { it.filterType == filterTypeId }
      }
      ContentTypeIds.CommentListType -> {
        commentListFilters.filters.filter { it.filterType == filterTypeId }
      }
      else -> listOf()
    }

  suspend fun addFilter(filter: FilterEntry) {
    dao.insertFilter(filter)

    refreshFilters()

    regexCache.remove(filter.id)
  }

  suspend fun deleteFilter(filter: FilterEntry) {
    dao.delete(filter)

    refreshFilters()
  }

  private class Filters(
    val filters: List<FilterEntry>,
  ) {

    val patternByType: Map<Int, Pattern>

    init {
      val typeToRegex = mutableMapOf<Int, StringBuilder>()

      for (filter in filters) {
        val sb = typeToRegex.getOrPut(filter.filterType) { StringBuilder() }

        if (filter.isRegex) {
          sb.append(filter.filter)
        } else {
          if (filter.options?.matchWholeWord == true) {
            sb.append("\\b")
            sb.append(Pattern.quote(filter.filter))
            sb.append("\\b")
          } else {
            sb.append(Pattern.quote(filter.filter))
          }
        }
        sb.append("|")
      }

      val patternByType = mutableMapOf<Int, Pattern>()
      listOf(
        FilterTypeIds.KeywordFilter,
        FilterTypeIds.InstanceFilter,
        FilterTypeIds.CommunityFilter,
        FilterTypeIds.UserFilter,
        FilterTypeIds.UrlFilter,
      ).forEach { filterType ->
        val regex = typeToRegex[filterType]
        if (regex != null) {
          regex.setLength(regex.length - 1)

          try {
            patternByType[filterType] = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE)
              .also {
                Log.d(TAG, "Compiling filter regex for type $filterType: $it")
              }
          } catch (e: Exception) {
            Log.w(TAG, "Error compiling regex.", e)
          }
        }
      }

      this.patternByType = patternByType
    }
  }
}

class TooManyFiltersException(
  filtersCount: Long,
) : RuntimeException(
  "Has $filtersCount filters!",
)
