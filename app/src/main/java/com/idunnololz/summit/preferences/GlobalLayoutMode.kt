package com.idunnololz.summit.preferences

typealias GlobalLayoutMode = Int

object GlobalLayoutModes {
    const val Auto = 0
    const val SmallScreen = 1

    /**
     * TODO: [BigScreen] is currently not supported because it would require [SlidingPaneLayout]'s
     * pane widths to be configurable which we currently do not support.
     */
    const val BigScreen = 2
}
