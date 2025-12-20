package com.idunnololz.summit.linkEditor

data class LinkEditorModel(
  val linkParts: List<LinkPart>
)

sealed interface LinkPart {
  data class Scheme(
    val value: String
  ): LinkPart
  data class Authority(
    val value: String
  ): LinkPart
  data class Path(
    val value: String
  ): LinkPart
  data class QueryParam(
    val internalKey: Int,
    val key: String,
    val value: String
  ): LinkPart
  data class Fragment(
    val value: String
  ): LinkPart
}