package com.idunnololz.summit.drafts.addOrEditTemplate

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TemplateToEdit(
  val entryId: Long?
): Parcelable