package com.idunnololz.summit.offline

import java.io.File

typealias TaskListener = (File) -> Unit
typealias TaskFailedListener = (e: Throwable) -> Unit

typealias OfflineDownloadProgressListener = (message: String, progress: Double) -> Unit