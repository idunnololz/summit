package com.idunnololz.summit.saveForLater

import com.idunnololz.summit.util.DirectoryHelper
import java.io.File
import javax.inject.Inject

class SaveForLaterManager @Inject constructor(
    private val directoryHelper: DirectoryHelper,
) {
    companion object {
        private const val SLOTS = 3
    }

    fun getSlotFiles(): List<File> {
        val files = mutableListOf<File>()
        repeat(SLOTS) {
            files.add(File(directoryHelper.saveForLaterDir, "slot_$it"))
        }

        return files
    }
}
