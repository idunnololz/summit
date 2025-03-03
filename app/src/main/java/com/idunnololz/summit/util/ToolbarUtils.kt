package com.idunnololz.summit.util

import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorFromAttribute

fun BaseFragment<*>.setupToolbar(
    toolbar: MaterialToolbar,
    title: String,
    subtitle: String? = null,
) {
    toolbar.title = title
    toolbar.subtitle = subtitle
    toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
    toolbar.setNavigationIconTint(
        toolbar.context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
    )
    toolbar.setNavigationOnClickListener {
        findNavController().navigateUp()
    }
}