package com.idunnololz.summit.uploads

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.api.AccountAwareLemmyClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UploadsViewModel @Inject constructor(
  private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

  fun fetchUploads() {
//    apiClient.fetchPersonByIdWithRetry()
  }
}