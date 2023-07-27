package com.idunnololz.summit.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoroutineScopeFactory @Inject constructor() {
    fun create() =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
