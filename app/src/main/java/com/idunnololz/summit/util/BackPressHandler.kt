package com.idunnololz.summit.util

interface BackPressHandler {
    /**
     *
     * @return true if the back press was handled.
     */
    fun onBackPressed(): Boolean
}
