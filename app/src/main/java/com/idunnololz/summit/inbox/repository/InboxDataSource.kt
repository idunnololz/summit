package com.idunnololz.summit.inbox.repository

import android.content.Context
import android.util.Log
import com.idunnololz.summit.lemmy.PostsRepository
import com.idunnololz.summit.inbox.LiteInboxItem
import com.idunnololz.summit.lemmy.utils.listSource.LemmyListSource
import com.idunnololz.summit.lemmy.utils.listSource.PageResult
import com.idunnololz.summit.util.ext.hasInternet
import com.idunnololz.summit.util.retry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.min

const val DEFAULT_PAGE_SIZE = 20

class InboxSource<O>(
  @ApplicationContext context: Context,
  defaultSortOrder: O,
  pageSize: Int = DEFAULT_PAGE_SIZE,
  private val fetchObjects: suspend (
    pageIndex: Int,
    sortOrder: O,
    limit: Int,
    force: Boolean,
  ) -> Result<List<LiteInboxItem>>,
) : LemmyListSource<LiteInboxItem, O, Long>(
  context,
  {
    id.toLong()
  },
  defaultSortOrder,
  fetchObjects,
  source = this,
  pageSize = pageSize,
) {

  fun markAsRead(id: Int, read: Boolean): LiteInboxItem? {
    val position = allObjects.indexOfFirst {
      it.obj.id == id
    }
    if (position == -1) {
      return null
    }
    val item = allObjects[position]
    val newItem = item.copy(obj = item.obj.updateIsRead(isRead = read))
    allObjects[position] = newItem

    return newItem.obj
  }

  fun removeItemWithId(id: Int): ObjectData<LiteInboxItem>? {
    val position = allObjects.indexOfFirst {
      it.obj.id == id
    }
    if (position >= 0) {
      val obj = removeItemAt(position)
      return obj
    }
    return null
  }
}