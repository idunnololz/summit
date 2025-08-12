package com.idunnololz.summit.util.coil3.video.internal

import android.media.MediaDataSource
import androidx.annotation.RequiresApi
import okio.FileHandle

@RequiresApi(23)
internal class FileHandleMediaDataSource(
  private val handle: FileHandle,
) : MediaDataSource() {

  override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int =
    handle.read(position, buffer, offset, size)

  override fun getSize(): Long = handle.size()

  override fun close() {
    handle.close()
  }
}
