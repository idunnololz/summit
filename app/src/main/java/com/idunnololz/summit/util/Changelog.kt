package com.idunnololz.summit.util

import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.main.MainActivity

/*
 * Patch notes/changelog stuff.
 */

val changeLogPostRef
    get() = PostRef("lemmy.world", 25986794)

fun MainActivity.launchChangelog() {
    launchPage(changeLogPostRef, switchToNativeInstance = true)
}
fun BaseFragment<*>.launchChangelog() {
    getMainActivity()?.launchChangelog()
}
