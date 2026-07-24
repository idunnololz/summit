package com.idunnololz.summit.models.processed.utils

import com.idunnololz.summit.filterLists.ContentFiltersDao
import com.idunnololz.summit.filterLists.ContentTypeId
import com.idunnololz.summit.filterLists.FilterEntry

class FakeContentFilterDao(
  val filterEntries: List<FilterEntry>,
) : ContentFiltersDao {

  private val _filterEntries = filterEntries.toMutableList()

  override suspend fun getAllFilters(): List<FilterEntry> = _filterEntries

  override suspend fun getFiltersForContentType(contentTypeId: ContentTypeId): List<FilterEntry> =
    _filterEntries.filter {
      it.contentType == contentTypeId
    }

  override suspend fun insertFilter(entry: FilterEntry): Long = 0

  override suspend fun count(): Long = _filterEntries.size.toLong()

  override suspend fun delete(entry: FilterEntry) {
    _filterEntries.remove(entry)
  }

  override suspend fun clear() {
    _filterEntries.clear()
  }
}
