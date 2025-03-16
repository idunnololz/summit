package com.idunnololz.summit.db.raw

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getIntOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import arrow.core.Either
import com.idunnololz.summit.util.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.Closeable

class DbHelper : Closeable {
    private val db: Either<SupportSQLiteDatabase, SQLiteDatabase>
    private val shouldClose: Boolean

    constructor(
        db: SupportSQLiteDatabase,
        shouldClose: Boolean,
    ) {
        this.db = Either.Left(db)
        this.shouldClose = shouldClose
    }

    constructor(db: SQLiteDatabase) {
        this.db = Either.Right(db)
        this.shouldClose = true
    }

    suspend fun getTableNames(): List<String> =
        runInterruptible(Dispatchers.Default) {
            val tableNames = mutableListOf<String>()
            query("SELECT name FROM sqlite_master WHERE type='table'", arrayOf()).use { cursor ->
                while(cursor.moveToNext()) {
                    val tableName = cursor.getString(0)
                    tableNames.add(tableName)
                }
            }

            tableNames
        }

    suspend fun getTableInfo(tableName: String): TableInfo =
        runInterruptible(Dispatchers.Default) {
            query("SELECT COUNT(*) FROM ${tableName}", arrayOf()).use { cursor ->
                val count = if (cursor.moveToNext()) {
                    cursor.getInt(0)
                } else {
                    0
                }

                TableInfo(
                    tableName = tableName,
                    rowCount = count
                )
            }
        }

    suspend fun getTableRowCount(tableName: String): Int =
        runInterruptible(Dispatchers.Default) {
            query("SELECT COUNT(*) FROM $tableName", arrayOf())
                .use { cursor ->
                    if (cursor.moveToNext()) {
                        cursor.getInt(0)
                    } else {
                        0
                    }
                }
        }

    suspend fun getTableColumnsInfo(tableName: String): List<ColumnInfo> =
        runInterruptible(Dispatchers.Default) {
            val columnsInfo = mutableListOf<ColumnInfo>()
            query("PRAGMA table_info($tableName)", arrayOf()).use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val typeIndex = cursor.getColumnIndex("type")
                val notNullIndex = cursor.getColumnIndex("notnull")
                val defaultValueIndex = cursor.getColumnIndex("dflt_value")
                val primaryKeyIndex = cursor.getColumnIndex("pk")

                while(cursor.moveToNext()) {
                    val columnName = cursor.getString(nameIndex)
                    val columnInfo = ColumnInfo(
                        columnName = columnName,
                        type = cursor.getString(typeIndex),
                        notNull = cursor.getIntOrNull(notNullIndex) == 1,
                        defaultValue = cursor.getStringOrNull(defaultValueIndex),
                        primaryKey = cursor.getIntOrNull(primaryKeyIndex) == 1,
                        isSensitive = columnName.equals("jwt", ignoreCase = true)
                    )

                    columnsInfo.add(columnInfo)
                }
            }

            columnsInfo
        }

    suspend fun getTableRows(tableName: String): TabletSelectionResult {
        val columnsInfo = getTableColumnsInfo(tableName).associateBy { it.columnName }
        var columnNames: List<String> = listOf()
        val rows: MutableList<TableRow> = mutableListOf()

        runInterruptible(Dispatchers.Default) {
            query("SELECT * FROM $tableName", arrayOf()).use { cursor ->
                val primaryKeyColumnName =
                    columnsInfo.values.firstOrNull { it.primaryKey }?.columnName

                val primaryKeyIndex = if (primaryKeyColumnName == null) {
                    -1
                } else {
                    cursor.getColumnIndex(primaryKeyColumnName)
                }
                columnNames =
                    listOfNotNull(primaryKeyColumnName) + cursor.columnNames.mapNotNull {
                        if (it.equals(primaryKeyColumnName, ignoreCase = true)) {
                            null
                        } else {
                            it
                        }
                    }

                while(cursor.moveToNext()) {
                    val pk = if (primaryKeyIndex >= 0) {
                        cursor.getString(primaryKeyIndex)
                    } else {
                        null
                    }

                    val columns: List<String> = (0 until columnNames.size).mapNotNull {
                        val columnInfo = columnsInfo[cursor.columnNames[it]]

                        if (it == primaryKeyIndex) {
                            null
                        } else {
                            val columnStr = cursor.getStringOrNull(it)
                            if (columnStr?.startsWith("eyJ") == true || columnInfo?.isSensitive == true) {
                                "[redacted]"
                            } else if (columnStr != null) {
                                columnStr
                            } else {
                                "null"
                            }
                        }
                    }
                    rows.add(
                        TableRow(
                            primaryKey = pk,
                            columns = columns,
                        )
                    )

                }
            }
        }

        return TabletSelectionResult(
            columnNames,
            rows,
        )
    }

    suspend fun keepTables(tablesToKeep: Set<String>) {
        val tableNames = getTableNames()

        runInterruptible(Dispatchers.Default) {
            tableNames.forEach { tableName ->
                if (!tablesToKeep.contains(tableName)) {
                    execSql("DELETE FROM $tableName")
                }
            }
            execSql("VACUUM")
        }
    }

    suspend fun getDbName(): String? =
        runInterruptible(Dispatchers.Default) {
            query("PRAGMA database_list", arrayOf())
                .use { cursor ->
                    if (cursor.moveToNext()) {
                        cursor.getStringOrNull(1)
                    } else {
                        null
                    }
                }
        }

    override fun close() {
        if (!shouldClose) {
            return
        }

        db.fold(
            { it.close() },
            { it.close() }
        )
    }

    private fun query(q: String, bindArgs: Array<out String>): Cursor =
        db.fold(
            { it.query(q, bindArgs) },
            { it.rawQuery(q, bindArgs) }
        )

    private fun execSql(sql: String): Unit =
        db.fold(
            { it.execSQL(sql) },
            { it.execSQL(sql) }
        )
}