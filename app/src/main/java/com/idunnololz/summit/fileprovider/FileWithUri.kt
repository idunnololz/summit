package com.idunnololz.summit.fileprovider

import android.net.Uri
import java.io.File

data class FileWithUri(
    val file: File,
    val uri: Uri,
)