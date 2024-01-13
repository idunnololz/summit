package com.idunnololz.summit.lemmy.mod

import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import kotlinx.parcelize.Parcelize

object ModActionResult {

    const val REQUEST_KEY = "ModActionsDialogFragment_req"
    const val RESULT_UPDATED_OBJ = "RESULT_UPDATED_OBJ"

    fun Fragment.setModActionResult(updatedObject: UpdatedObject?) {
        setFragmentResult(
            REQUEST_KEY,
            bundleOf(
                RESULT_UPDATED_OBJ to updatedObject,
            ),
        )
    }

    sealed interface UpdatedObject : Parcelable {
        @Parcelize
        data class PostObject(
            val postId: Int,
        ) : UpdatedObject

        @Parcelize
        data class CommentObject(
            val commentId: Int,
        ) : UpdatedObject
    }
}
