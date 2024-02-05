package com.idunnololz.summit.lemmy.utils

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.R
import com.google.android.material.button.MaterialButton
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.ext.getResIdFromAttribute

fun makeUpAndDownVoteButtons(
    context: Context,
    makeButtonLp: () -> ViewGroup.LayoutParams = {
        ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            Utils.convertDpToPixel(48f).toInt(),
        )
    }
): UpAndDownVoteUi {
    val downvoteButton = MaterialButton(
        context,
        null,
        context.getResIdFromAttribute(R.attr.materialIconButtonStyle),
    ).apply {
        id = View.generateViewId()
        layoutParams = makeButtonLp()
        iconPadding = context.getDimen(com.idunnololz.summit.R.dimen.padding_half)
        setPadding(
            context.getDimen(com.idunnololz.summit.R.dimen.padding_half),
            context.getDimen(com.idunnololz.summit.R.dimen.padding),
            context.getDimen(com.idunnololz.summit.R.dimen.padding),
            context.getDimen(com.idunnololz.summit.R.dimen.padding),
        )
        this.insetTop = 0
        setIconResource(com.idunnololz.summit.R.drawable.baseline_expand_more_18)
        backgroundTintList = null
        gravity = Gravity.CENTER
        setBackgroundResource(com.idunnololz.summit.R.drawable.downvote_chip_bg2)
    }

    val upvoteButton = MaterialButton(
        context,
        null,
        context.getResIdFromAttribute(R.attr.materialIconButtonStyle),
    ).apply {
        id = View.generateViewId()
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            Utils.convertDpToPixel(48f).toInt(),
        )
        iconPadding = context.getDimen(com.idunnololz.summit.R.dimen.padding_half)
        setPadding(
            context.getDimen(com.idunnololz.summit.R.dimen.padding_half),
            context.getDimen(com.idunnololz.summit.R.dimen.padding),
            context.getDimen(com.idunnololz.summit.R.dimen.padding),
            context.getDimen(com.idunnololz.summit.R.dimen.padding),
        )
        this.insetTop = 0
        setIconResource(com.idunnololz.summit.R.drawable.baseline_expand_less_18)
        backgroundTintList = null
        gravity = Gravity.CENTER
        setBackgroundResource(com.idunnololz.summit.R.drawable.upvote_chip_bg2)
    }

    return UpAndDownVoteUi(
        upvoteButton,
        downvoteButton,
    )
}

class UpAndDownVoteUi(
    val upvoteButton: MaterialButton,
    val downvoteButton: MaterialButton,
)
