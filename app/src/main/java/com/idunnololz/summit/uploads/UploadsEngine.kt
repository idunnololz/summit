package com.idunnololz.summit.uploads

import android.content.Context
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.LocalImageView
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.lemmy.inbox.repository.MultiLemmyListSource
import com.idunnololz.summit.util.dateStringToTs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UploadsEngine @Inject constructor(
  @ApplicationContext private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
) {

  private val source =
    MultiLemmyListSource(
      listOf(
        LemmyListSource<LocalImageView, Unit, String>(
          context = context,
          id = { this.local_image.pictrs_alias },
          defaultSortOrder = Unit,
          fetchObjects = {
              pageIndex: Int,
              sortOrder: Unit,
              limit: Int,
              force: Boolean,
            ->
            apiClient.listMedia(
              page = pageIndex.toLong(),
              limit = limit.toLong(),
              force = force,
            ).map { it.images }
          },
        ),
      ),
      sortValue = { dateStringToTs(it.local_image.published) },
      id = { it.local_image.pictrs_alias },

    )

  fun fetchPage(pageIndex: Int) {
//    source.getPage(pageIndex, force = false, retainItemsOnForce = true)
  }
}
