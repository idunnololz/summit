package com.idunnololz.summit.util

import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.main.MainActivity

object Changelog {
    fun MainActivity.launchChangelog() {
        launchPage(PostRef("lemmy.world", 8761879), switchToNativeInstance = true)
    }
    fun BaseFragment<*>.launchChangelog() {
        getMainActivity()?.launchChangelog()
    }
}