package com.idunnololz.summit.util.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob


fun CoroutineScope.newChildScope() =
  CoroutineScope(this.coroutineContext + SupervisorJob(this.coroutineContext[Job]))