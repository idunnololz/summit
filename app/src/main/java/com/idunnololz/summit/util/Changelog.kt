package com.idunnololz.summit.util

import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.main.MainActivity

/*
 * Patch notes/changelog stuff.
 */

val changeLogPostRef
  get() = PostRef("lemmy.world", 29836089)

fun MainActivity.launchChangelog() {
  launchPage(changeLogPostRef, switchToNativeInstance = true)
}
fun BaseFragment<*>.launchChangelog() {
  getMainActivity()?.launchChangelog()
}
