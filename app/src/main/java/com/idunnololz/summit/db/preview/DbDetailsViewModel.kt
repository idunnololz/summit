package com.idunnololz.summit.db.preview

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.db.raw.DbHelper
import com.idunnololz.summit.db.raw.TableInfo
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DbDetailsViewModel @Inject constructor() : ViewModel() {

    data class Model(
        val databaseName: String?,
        val tablesInfo: List<TableInfo>,
    )

    val model = StatefulLiveData<Model>()

    fun loadDbDetails(dbUri: Uri, tablesToShow: Set<String>?) {
        model.setIsLoading()

        viewModelScope.launch {
            val dbHelper = DbHelper(
                SQLiteDatabase.openDatabase(dbUri.toFile().path, null, 0),
            )

            val dbName = dbHelper.getDbName()
            val tablesInfo = dbHelper.getTableNames()
                .let {
                    if (tablesToShow != null) {
                        it.filter { tablesToShow.contains(it) }
                    } else {
                        it
                    }
                }
                .map {
                    dbHelper.getTableInfo(it)
                }

            model.postValue(
                Model(
                    databaseName = dbName,
                    tablesInfo = tablesInfo,
                ),
            )

            dbHelper.close()
        }
    }
}
