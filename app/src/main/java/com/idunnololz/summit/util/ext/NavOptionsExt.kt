package com.idunnololz.summit.util.ext

import androidx.navigation.NavOptions
import com.idunnololz.summit.R

fun NavOptions.Builder.addDefaultAnim(): NavOptions.Builder {
    setEnterAnim(R.animator.fade_in)
    setExitAnim(R.animator.fade_out)
    setPopEnterAnim(R.animator.fade_in)
    setPopExitAnim(R.animator.fade_out)
    return this
}
