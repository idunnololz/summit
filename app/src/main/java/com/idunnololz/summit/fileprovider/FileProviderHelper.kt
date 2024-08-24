package com.idunnololz.summit.fileprovider

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream

class FileProviderHelper(
    private val context: Context,
) {
    val fileProviderDir = File(context.cacheDir, "fileprovider")

    suspend fun openTempFile(fileName: String, writeFn: suspend (OutputStream) -> Unit): Uri {
        val file = File(fileProviderDir, fileName)

        if (file.isDirectory) {
            file.delete()
        }

        file.parentFile?.mkdirs()

        file.outputStream().use {
            writeFn(it)
        }

        return FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".fileprovider",
            file,
        )
    }

    fun getTempFile(fileName: String): Pair<File, Uri> {
        val file = File(fileProviderDir, fileName)

        if (file.isDirectory) {
            file.delete()
        }

        file.parentFile?.mkdirs()

        return file to FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".fileprovider",
            file,
        )
    }
}
