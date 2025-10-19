package com.idunnololz.summit.lemmy.actions

import androidx.room.util.getColumnIndexOrThrow
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteDatabase

fun getAllFailedActions(
  db: SupportSQLiteDatabase,
  converters: LemmyActionConverters
): List<OldLemmyFailedAction> {
  val _sql = "SELECT * FROM lemmy_failed_actions"
  return db.query(_sql).let { _connection ->
    val cursor = _connection
    try {
      val _columnIndexOfId: Int = _connection.getColumnIndexOrThrow("id")
      val _columnIndexOfTs: Int = _connection.getColumnIndexOrThrow("ts")
      val _columnIndexOfCreationTs: Int = _connection.getColumnIndexOrThrow("cts")
      val _columnIndexOfFailedTs: Int = _connection.getColumnIndexOrThrow("fts")
      val _columnIndexOfError: Int = _connection.getColumnIndexOrThrow("error")
      val _columnIndexOfInfo: Int = _connection.getColumnIndexOrThrow("info")
      val _columnIndexOfSeen: Int = _connection.getColumnIndexOrThrow("seen")
      val _result: MutableList<OldLemmyFailedAction> = mutableListOf()
      while (cursor.moveToNext()) {
        val _item: OldLemmyFailedAction
        val _tmpId: Long
        _tmpId = cursor.getLong(_columnIndexOfId)
        val _tmpTs: Long
        _tmpTs = cursor.getLong(_columnIndexOfTs)
        val _tmpCreationTs: Long
        _tmpCreationTs = cursor.getLong(_columnIndexOfCreationTs)
        val _tmpFailedTs: Long
        _tmpFailedTs = cursor.getLong(_columnIndexOfFailedTs)
        val _tmpError: LemmyActionFailureReason
        val _tmp: String
        _tmp = cursor.getString(_columnIndexOfError)

        val _tmp_1: LemmyActionFailureReason? =
          converters.stringToLemmyActionFailureReason(_tmp)
        if (_tmp_1 == null) {
          error("Expected NON-NULL 'com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason', but it was NULL.")
        } else {
          _tmpError = _tmp_1
        }
        val _tmpInfo: ActionInfo?
        val _tmp_2: String?
        if (cursor.isNull(_columnIndexOfInfo)) {
          _tmp_2 = null
        } else {
          _tmp_2 = cursor.getString(_columnIndexOfInfo)
        }
        if (_tmp_2 == null) {
          _tmpInfo = null
        } else {
          _tmpInfo = converters.stringToActionInfo(_tmp_2)
        }
        val _tmpSeen: Boolean?
        val _tmp_3: Int?
        if (cursor.isNull(_columnIndexOfSeen)) {
          _tmp_3 = null
        } else {
          _tmp_3 = cursor.getLong(_columnIndexOfSeen).toInt()
        }
        _tmpSeen = _tmp_3?.let { it != 0 }
        _item =
          OldLemmyFailedAction(_tmpId,_tmpTs,_tmpCreationTs,_tmpFailedTs,_tmpError,_tmpInfo,_tmpSeen)
        _result.add(_item)
      }
      _result
    } finally {
      cursor.close()
    }
  }
}

fun getAllCompletedActions(
  db: SupportSQLiteDatabase,
  converters: LemmyActionConverters
): List<OldLemmyCompletedAction> {
  val _sql = "SELECT * FROM lemmy_completed_actions"
  return db.query(_sql).let { _connection ->
    val cursor = _connection
    try {
      val _columnIndexOfId: Int = _connection.getColumnIndexOrThrow("id")
      val _columnIndexOfTs: Int = _connection.getColumnIndexOrThrow("ts")
      val _columnIndexOfCreationTs: Int = _connection.getColumnIndexOrThrow("cts")
      val _columnIndexOfInfo: Int = _connection.getColumnIndexOrThrow("info")
      val _result: MutableList<OldLemmyCompletedAction> = mutableListOf()
      while (cursor.moveToNext()) {
        val _item: OldLemmyCompletedAction
        val _tmpId: Long
        _tmpId = cursor.getLong(_columnIndexOfId)
        val _tmpTs: Long
        _tmpTs = cursor.getLong(_columnIndexOfTs)
        val _tmpCreationTs: Long
        _tmpCreationTs = cursor.getLong(_columnIndexOfCreationTs)
        val _tmpInfo: ActionInfo?
        val _tmp: String?
        if (cursor.isNull(_columnIndexOfInfo)) {
          _tmp = null
        } else {
          _tmp = cursor.getString(_columnIndexOfInfo)
        }
        if (_tmp == null) {
          _tmpInfo = null
        } else {
          _tmpInfo = converters.stringToActionInfo(_tmp)
        }
        _item = OldLemmyCompletedAction(_tmpId,_tmpTs,_tmpCreationTs,_tmpInfo)
        _result.add(_item)
      }
      _result
    } finally {
      cursor.close()
    }
  }
}