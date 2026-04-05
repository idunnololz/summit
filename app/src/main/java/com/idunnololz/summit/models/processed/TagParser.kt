package com.idunnololz.summit.models.processed

import kotlin.text.iterator

object TagParser {

  private const val STATE_SCANNING = 1
  private const val STATE_MATCH_END_BRACKET = 2

  class ParsedTagsResult(
    val tags: List<PostTag>,
    val title: String,
  )

  fun parseTags(name: String): ParsedTagsResult {
    val tags = mutableSetOf<PostTag>()

    var titleStartIndex = 0
    var titleEndIndex = name.lastIndex
    var curState = STATE_SCANNING

    var tagStartIndex = 0

    var index = 0
    for (c in name) {
      when (curState) {
        STATE_SCANNING -> {
          if (c == '[' && lookAheadIfTagValid(name, index + 1)) {
            curState = STATE_MATCH_END_BRACKET
            tagStartIndex = index
            titleStartIndex = index
          } else if (c.isWhitespace()) {
          } else {
            break
          }
        }
        STATE_MATCH_END_BRACKET -> {
          if (c == ']') {
            curState = STATE_SCANNING
            titleStartIndex = index + 1

            val rawTag = name.substring(tagStartIndex + 1, index).trim()
            if (rawTag.length > 1) {
              tags.add(rawTag.toPostTag())
            }
          }
        }
      }

      index++
    }

    val endIndex = index
    index = name.lastIndex
    curState = STATE_SCANNING
    while (index > endIndex) {
      val c = name[index]
      when (curState) {
        STATE_SCANNING -> {
          if (c == ']' && lookBehindIfTagValid(name, index - 1)) {
            curState = STATE_MATCH_END_BRACKET
            tagStartIndex = index
            titleEndIndex = index
          } else if (c.isWhitespace()) {
          } else {
            break
          }
        }
        STATE_MATCH_END_BRACKET -> {
          if (c == '[') {
            curState = STATE_SCANNING
            titleEndIndex = index - 1

            val rawTag = name.substring(index + 1, tagStartIndex).trim()
            if (rawTag.length > 1) {
              tags.add(rawTag.toPostTag())
            }
          }
        }
      }

      index--
    }

    return ParsedTagsResult(
      tags.toList(),
      name.substring(titleStartIndex, titleEndIndex + 1).trim(),
    )
  }

  private fun lookAheadIfTagValid(name: String, startIndex: Int): Boolean {
    if (startIndex >= name.length) {
      return false
    }
    if (name[startIndex] == ']') {
      return false
    }

    return true
  }

  private fun lookBehindIfTagValid(name: String, startIndex: Int): Boolean {
    if (startIndex < 0) {
      return false
    }
    if (name[startIndex] == '[') {
      return false
    }

    return true
  }

  private fun String.toPostTag() = when (this.lowercase()) {
    "spoiler" -> PostTag.Spoiler
    else -> PostTag.CustomTag(this)
  }
}
