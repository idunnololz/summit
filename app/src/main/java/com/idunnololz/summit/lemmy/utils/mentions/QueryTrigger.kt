package com.idunnololz.summit.lemmy.utils.mentions

/**
 * What triggered a query within an [android.widget.EditText].
 */
data class QueryTrigger(
  val type: QueryType,
  /**
   * Eg. "!a@b.com"
   */
  val rawQuery: String,
  val position: Int,
) {
  val queryPrefix
    get() = rawQuery.take(1)
  val actualQuery
    get() = rawQuery.substring(1)
}

enum class QueryType {
  Emoji,
  CommunityOrPerson,
}