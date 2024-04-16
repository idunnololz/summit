package com.idunnololz.summit.drafts

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.idunnololz.summit.R
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class DraftsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val draftsDao: DraftsDao,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dbContext = Dispatchers.IO.limitedParallelism(1)

    init {
        coroutineScope.launch {
            Log.d("dbdb", "draftsDao: ${draftsDao.count()}")
        }
    }

    fun saveDraftAsync(draftData: DraftData, showToast: Boolean) {
        coroutineScope.launch {
            saveDraft(draftData, showToast)
        }
    }

    suspend fun saveDraft(draftData: DraftData, showToast: Boolean): Long {
        val id = withContext(dbContext) {
            draftsDao.insert(
                DraftEntry(
                    0,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    draftData.type,
                    draftData,
                ),
            )
        }
        if (showToast) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.draft_saved), Toast.LENGTH_LONG)
                    .show()
            }
        }

        return id
    }

    fun updateDraftAsync(entryId: Long, draftData: DraftData, showToast: Boolean) {
        if (entryId == 0L) {
            saveDraftAsync(draftData, showToast)
            return
        }

        coroutineScope.launch {
            draftsDao.update(
                entryId,
                System.currentTimeMillis(),
                draftData.type,
                draftData,
            )
            if (showToast) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.draft_saved),
                        Toast.LENGTH_LONG,
                    )
                        .show()
                }
            }
        }
    }

    suspend fun getDraftsByType(draftType: Int, limit: Int, updateTs: Long) =
        draftsDao.getDraftsByType(draftType, limit, updateTs)

    suspend fun getAllDrafts(limit: Int, updateTs: Long) = draftsDao.getAllDrafts(limit, updateTs)

    suspend fun deleteAll(draftType: Int) = draftsDao.deleteAll(draftType)

    suspend fun deleteDraft(entry: DraftEntry) = withContext(dbContext) {
        draftsDao.delete(entry)
    }
}
