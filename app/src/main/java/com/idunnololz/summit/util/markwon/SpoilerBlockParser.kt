package com.idunnololz.summit.util.markwon

import android.util.Log
import org.commonmark.node.Block
import org.commonmark.node.CustomBlock
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.parser.InlineParser
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState

class SpoilerBlock : CustomBlock() {
  var title: String = ""
  var titleNode: Node? = null
}

class SpoilerBlockParser(private val title: String) : AbstractBlockParser() {

  private val block = SpoilerBlock().also { it.title = title }
  private var closed = false

  override fun getBlock(): Block = block

  override fun tryContinue(parserState: ParserState?): BlockContinue? {
    if (closed) return BlockContinue.none()

    val line = parserState?.line ?: return BlockContinue.atIndex(parserState?.index ?: 0)
    val trimmed = line.toString().trim()

    // Closing delimiter
    if (trimmed.endsWith(":::")) {
      closed = true
      return BlockContinue.atIndex(parserState.index)
    }

    return BlockContinue.atIndex(parserState.index)
  }

  override fun isContainer(): Boolean = true

  override fun canContain(childBlock: Block?): Boolean = true

  override fun parseInlines(inlineParser: InlineParser?) {
    super.parseInlines(inlineParser)
    val textNode = Paragraph()
    inlineParser?.parse(title, textNode)

    block.titleNode = textNode
  }

  class SpoilerBlockParserFactory : AbstractBlockParserFactory() {

    // Matches: ::: spoiler <title>
    private val OPENING_PATTERN = Regex(""":::\s+spoiler\s+(.+)$""")

    override fun tryStart(
      state: ParserState?,
      matchedBlockParser: MatchedBlockParser?,
    ): BlockStart? {
      val line = state?.line?.toString()?.trim() ?: return BlockStart.none()

      val match = OPENING_PATTERN.find(line) ?: return BlockStart.none()
      val title = match.groupValues[1].trim()

      return BlockStart.of(SpoilerBlockParser(title))
        .atIndex(state.index + line.length)
    }
  }
}