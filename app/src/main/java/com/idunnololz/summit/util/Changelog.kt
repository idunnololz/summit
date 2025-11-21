package com.idunnololz.summit.util

import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.main.MainActivity

/*
 * Patch notes/changelog stuff.
 */

val changeLogPostRef
  get() = PostRef("lemmy.world", 38688302)

fun MainActivity.launchChangelog() {
  launchPage(changeLogPostRef, switchToNativeInstance = true)
}
fun BaseFragment<*>.launchChangelog() {
  getMainActivity()?.launchChangelog()
}
