package com.idunnololz.summit.fileprovider

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream

class FileProviderHelper(
    private val context: Context,
) {
    fun openTempFile(fileName: String, writeFn: (OutputStream) -> Unit): Uri {
        val file = File(context.cacheDir, "fileprovider/$fileName")

        if (file.isDirectory) {
            file.delete()
        }

        file.parentFile?.mkdirs()

        file.outputStream().use(writeFn)

        return FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".fileprovider",
            file,
        )
    }
}
