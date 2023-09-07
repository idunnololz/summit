package com.idunnololz.summit.lemmy.utils

import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils

fun BaseFragment<*>.onLinkClick(url: String, text: String?) {
    val context = context ?: return

    Utils.openExternalLink(context, url)
}