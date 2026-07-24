package com.idunnololz.summit.models.processed

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.idunnololz.summit.filterLists.ContentFiltersManager
import com.idunnololz.summit.filterLists.ContentTypeIds
import com.idunnololz.summit.filterLists.FilterEntry
import com.idunnololz.summit.filterLists.FilterEntryOptions
import com.idunnololz.summit.filterLists.FilterTypeIds
import com.idunnololz.summit.models.processed.utils.FakeContentFilterDao
import com.idunnololz.summit.models.processed.utils.TestCoroutineScopeFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ContentFiltersManagerTest {

  @Test
  fun a() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    val fakeContentFiltersDao = FakeContentFilterDao(
      listOf(
        FilterEntry(
          id = 0,
          contentType = ContentTypeIds.PostListType,
          filterType = FilterTypeIds.KeywordFilter,
          filter = "someterm",
          isRegex = false,
          options = FilterEntryOptions(matchWholeWord = true),
        ),
      ),
    )
    val contentFiltersManager = ContentFiltersManager(
      dao = fakeContentFiltersDao,
      coroutineScopeFactory = TestCoroutineScopeFactory(testDispatcher),
      ioDispatcher = testDispatcher,
    )
    contentFiltersManager.initialize()

    advanceUntilIdle()

    assertThat(contentFiltersManager.testPostTitle("someterm yay!")).isTrue()
  }

  @Test
  fun b() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    val fakeContentFiltersDao = FakeContentFilterDao(
      listOf(
        FilterEntry(
          id = 0,
          contentType = ContentTypeIds.PostListType,
          filterType = FilterTypeIds.KeywordFilter,
          filter = "[brackets]",
          isRegex = false,
          options = FilterEntryOptions(matchWholeWord = false),
        ),
      ),
    )
    val contentFiltersManager = ContentFiltersManager(
      dao = fakeContentFiltersDao,
      coroutineScopeFactory = TestCoroutineScopeFactory(testDispatcher),
      ioDispatcher = testDispatcher,
    )
    contentFiltersManager.initialize()

    advanceUntilIdle()

    assertThat(contentFiltersManager.testPostTitle("[brackets] yay!")).isTrue()
  }
}
