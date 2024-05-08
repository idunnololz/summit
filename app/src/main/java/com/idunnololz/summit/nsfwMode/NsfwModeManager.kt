package com.idunnololz.summit.nsfwMode

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NsfwModeManager @Inject constructor() {
    val nsfwModeEnabled = MutableStateFlow(false)
}