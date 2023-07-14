package com.idunnololz.summit.lemmy.utils

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.community.usesDividers
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.VerticalSpaceItemDecoration
import com.idunnololz.summit.util.ext.getDimen

fun RecyclerView.setupDecoratorsForPostList(preferences: Preferences) {
    while (this.itemDecorationCount != 0) {
        this.removeItemDecorationAt(this.itemDecorationCount - 1)
    }
    if (preferences.getPostsLayout().usesDividers()) {
        this.addItemDecoration(
            CustomDividerItemDecoration(
                this.context,
                DividerItemDecoration.VERTICAL
            ).apply {
                setDrawable(
                    checkNotNull(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.vertical_divider
                        )
                    )
                )
            }
        )
    } else {
        this.addItemDecoration(
            VerticalSpaceItemDecoration(
                this.context.getDimen(R.dimen.padding),
                false
            )
        )
    }
}