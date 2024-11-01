package com.idunnololz.summit.util

import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.main.MainActivity

/*
 * Patch notes/changelog stuff.
 */

fun MainActivity.launchChangelog() {
    launchPage(PostRef("lemmy.world", 21526783), switchToNativeInstance = true)
}
fun BaseFragment<*>.launchChangelog() {
    getMainActivity()?.launchChangelog()
}
