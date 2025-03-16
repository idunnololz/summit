package com.idunnololz.summit.db.preview

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.database.getIntOrNull
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.db.raw.DbHelper
import com.idunnololz.summit.db.raw.TableRow
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.getStringOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TableDetailsViewModel @Inject constructor(
) : ViewModel() {

    data class Model(
        val tableName: String,
        val rowCount: Int,
        val columnNames: List<String>,
        val rows: List<TableRow>,
    )

    val model = StatefulLiveData<Model>()

    fun loadDbDetails(dbUri: Uri, tableName: String) {
        model.setIsLoading()

        viewModelScope.launch {
            val dbHelper = DbHelper(
                SQLiteDatabase.openDatabase(dbUri.toFile().path, null, 0)
            )

            val rowCount = dbHelper.getTableRowCount(tableName)
            val fullTable = dbHelper.getTableRows(tableName)

            model.postValue(
                Model(
                    tableName = tableName,
                    rowCount = rowCount,
                    columnNames = fullTable.columnNames,
                    rows = fullTable.tableRows,
                )
            )

            dbHelper.close()
        }
    }
}