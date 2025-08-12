package com.idunnololz.summit.templates

import android.content.Context
import android.widget.Toast
import com.idunnololz.summit.R
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.templates.db.TemplateData
import com.idunnololz.summit.templates.db.TemplateEntry
import com.idunnololz.summit.templates.db.TemplatesDao
import com.idunnololz.summit.templates.db.type
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class TemplatesManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val templatesDao: TemplatesDao,
  private val coroutineScopeFactory: CoroutineScopeFactory,
) {

  private val coroutineScope = coroutineScopeFactory.create()

  private val dbContext = Dispatchers.IO.limitedParallelism(1)

  private val _onTemplateChanged = MutableSharedFlow<Unit>()

  val onTemplateChanged
    get() = _onTemplateChanged.asSharedFlow()

  fun saveTemplateAsync(templateData: TemplateData, showToast: Boolean) {
    coroutineScope.launch {
      saveTemplate(templateData, showToast)
    }
  }

  suspend fun saveTemplate(templateData: TemplateData, showToast: Boolean): Long {
    val id = withContext(dbContext) {
      templatesDao.insert(
        TemplateEntry(
          id = 0,
          creationTs = System.currentTimeMillis(),
          updatedTs = System.currentTimeMillis(),
          templateType = templateData.type,
          data = templateData,
          accountId = templateData.accountId,
          accountInstance = templateData.accountInstance,
        ),
      )
    }
    if (showToast) {
      withContext(Dispatchers.Main) {
        Toast.makeText(context, context.getString(R.string.template_saved), Toast.LENGTH_LONG)
          .show()
      }
    }

    coroutineScope.launch {
      _onTemplateChanged.emit(Unit)
    }

    return id
  }

  fun updateTemplateAsync(entryId: Long, templateData: TemplateData, showToast: Boolean) {
    coroutineScope.launch {
      updateTemplate(
        entryId,
        templateData,
        showToast,
      )
    }
  }

  suspend fun updateTemplate(entryId: Long, templateData: TemplateData, showToast: Boolean) {
    if (entryId == 0L) {
      saveTemplate(templateData, showToast)
      return
    }

    withContext(dbContext) {
      templatesDao.update(
        entryId,
        System.currentTimeMillis(),
        templateData.type,
        templateData,
      )
    }
    if (showToast) {
      withContext(Dispatchers.Main) {
        Toast.makeText(
          context,
          context.getString(R.string.template_saved),
          Toast.LENGTH_LONG,
        )
          .show()
      }
    }

    coroutineScope.launch {
      _onTemplateChanged.emit(Unit)
    }
  }

  suspend fun getTemplatesByType(templateType: Int) = templatesDao.getTemplatesByType(templateType)

  fun deleteTemplateWithIdAsync(id: Long?) {
    id ?: return
    coroutineScope.launch {
      deleteTemplateWithId(id)
    }
  }

  suspend fun deleteTemplateWithId(entryId: Long) = withContext(dbContext) {
    templatesDao.deleteWithId(entryId)

    coroutineScope.launch {
      _onTemplateChanged.emit(Unit)
    }
  }
}
