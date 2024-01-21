package com.idunnololz.summit.saveForLater

import android.content.Context
import com.idunnololz.summit.util.DirectoryHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.math.max

class SaveForLaterManager @Inject constructor(
    @ApplicationContext private val context: Context,
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

    fun getFreeSlots(): Int {
        val usedSlots = getSlotFiles().count { it.exists() }

        return max(SLOTS - usedSlots, 0)
    }
}
