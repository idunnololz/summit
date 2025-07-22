package com.idunnololz.summit.uploads

import android.content.Context
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.LocalImageView
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.lemmy.inbox.repository.MultiLemmyListSource
import com.idunnololz.summit.lemmy.modlogs.ModEvent
import com.idunnololz.summit.util.dateStringToTs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class UploadsViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

  fun fetchUploads() {
//    MultiLemmyListSource<LocalImageView, Unit>(
//      listOf(
//        LemmyListSource<LocalImageView, Unit>(
//          context,
//          { 0 },
//
//
//        )
//      ),
//      sortValue = { dateStringToTs(it.local_image.published) },
//      id = { it.local_image.pictrs_alias }
//    )
//    apiClient.listMedia()
  }
}
