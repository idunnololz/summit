package com.idunnololz.summit.nsfwMode

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class NsfwModeManager @Inject constructor() {
    val nsfwModeEnabled = MutableStateFlow(false)
}
