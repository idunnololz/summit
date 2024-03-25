package com.idunnololz.summit.util.markwon

import org.commonmark.internal.Bracket
import org.commonmark.internal.Delimiter
import org.commonmark.internal.InlineParserImpl
import org.commonmark.internal.util.Escaping
import org.commonmark.internal.util.Html5Entities
import org.commonmark.internal.util.LinkScanner
import org.commonmark.internal.util.Parsing
import org.commonmark.node.Code
import org.commonmark.node.HardLineBreak
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text
import org.commonmark.parser.InlineParser
import org.commonmark.parser.InlineParserContext
import org.commonmark.parser.delimiter.DelimiterProcessor
import java.util.BitSet
import java.util.regex.Pattern

class SummitInlineParser(private val context: InlineParserContext) : InlineParser {
    private val specialCharacters: BitSet
    private val delimiterCharacters: BitSet
    private val delimiterProcessors: Map<Char, DelimiterProcessor>
    private var input: String = ""
    private var index = 0

    /**
     * Top delimiter (emphasis, strong emphasis or custom emphasis). (Brackets are on a separate stack, different
     * from the algorithm described in the spec.)
     */
    private var lastDelimiter: Delimiter? = null

    /**
     * Top opening bracket (`[` or `![)`).
     */
    private var lastBracket: Bracket? = null

    init {
        delimiterProcessors = InlineParserImpl.calculateDelimiterProcessors(
            context.customDelimiterProcessors,
        )
        delimiterCharacters = calculateDelimiterCharacters(
            delimiterProcessors.keys,
        )
        specialCharacters = calculateSpecialCharacters(delimiterCharacters)
    }

    /**
     * Parse content in block into inline children, using reference map to resolve references.
     */
    override fun parse(content: String, block: Node) {
        reset(content.trim { it <= ' ' })
        var previous: Node? = null
        while (true) {
            val node = parseInline(previous)
            previous = node
            if (node != null) {
                block.appendChild(node)
            } else {
                break
            }
        }
        processDelimiters(null)
        mergeChildTextNodes(block)
    }

    fun reset(content: String) {
        input = content
        index = 0
        lastDelimiter = null
        lastBracket = null
    }

    private fun text(text: String?, beginIndex: Int, endIndex: Int): Text {
        return Text(text!!.substring(beginIndex, endIndex))
    }

    private fun text(text: String): Text {
        return Text(text)
    }

    /**
     * Parse the next inline element in subject, advancing input index.
     * On success, return the new inline node.
     * On failure, return null.
     */
    private fun parseInline(previous: Node?): Node? {
        val c = peek()
        if (c == '\u0000') {
            return null
        }
        val node: Node?
        when (c) {
            '\n' -> node = parseNewline(previous)
            '\\' -> node = parseBackslash()
            '`' -> node = parseBackticks()
            '[' -> node = parseOpenBracket()
            '!' -> node = parseBang()
            ']' -> node = parseCloseBracket()
            '<' -> {
                node = parseAutolink()
            }

            '&' -> node = parseEntity()
            else -> {
                val isDelimiter = delimiterCharacters[c.code]
                node = if (isDelimiter) {
                    val delimiterProcessor = delimiterProcessors[c]
                    parseDelimiters(delimiterProcessor, c)
                } else {
                    parseString()
                }
            }
        }
        return if (node != null) {
            node
        } else {
            index++
            // When we get here, it's only for a single special character that turned out to not have a special meaning.
            // So we shouldn't have a single surrogate here, hence it should be ok to turn it into a String.
            val literal = c.toString()
            text(literal)
        }
    }

    /**
     * If RE matches at current index in the input, advance index and return the match; otherwise return null.
     */
    private fun match(re: Pattern): String? {
        if (index >= input.length) {
            return null
        }
        val matcher = re.matcher(input)
        matcher.region(index, input.length)
        val m = matcher.find()
        return if (m) {
            index = matcher.end()
            matcher.group()
        } else {
            null
        }
    }

    /**
     * Returns the char at the current input index, or `'\0'` in case there are no more characters.
     */
    private fun peek(): Char {
        return if (index < input.length) {
            input[index]
        } else {
            '\u0000'
        }
    }

    /**
     * Parse zero or more space characters, including at most one newline.
     */
    private fun spnl() {
        match(SPNL)
    }

    /**
     * Parse a newline. If it was preceded by two spaces, return a hard line break; otherwise a soft line break.
     */
    private fun parseNewline(previous: Node?): Node {
        index++ // assume we're at a \n

        // Check previous text for trailing spaces.
        // The "endsWith" is an optimization to avoid an RE match in the common case.
        return if (previous is Text && previous.literal.endsWith(
                " ",
            )
        ) {
            val text = previous
            val literal = text.literal
            val matcher =
                FINAL_SPACE.matcher(literal)
            val spaces = if (matcher.find()) matcher.end() - matcher.start() else 0
            if (spaces > 0) {
                text.literal = literal.substring(0, literal.length - spaces)
            }
            if (spaces >= 2) {
                HardLineBreak()
            } else {
                SoftLineBreak()
            }
        } else {
            SoftLineBreak()
        }
    }

    /**
     * Parse a backslash-escaped special character, adding either the escaped  character, a hard line break
     * (if the backslash is followed by a newline), or a literal backslash to the block's children.
     */
    private fun parseBackslash(): Node {
        index++
        val node: Node
        if (peek() == '\n') {
            node = HardLineBreak()
            index++
        } else if (index < input.length && ESCAPABLE.matcher(
                input.substring(index, index + 1),
            ).matches()
        ) {
            node = text(input, index, index + 1)
            index++
        } else {
            node = text("\\")
        }
        return node
    }

    /**
     * Attempt to parse backticks, returning either a backtick code span or a literal sequence of backticks.
     */
    private fun parseBackticks(): Node? {
        val ticks = match(TICKS_HERE)
            ?: return null
        val afterOpenTicks = index
        var matched: String?
        while (match(TICKS).also { matched = it } != null) {
            if (matched == ticks) {
                val node = Code()
                var content = input.substring(afterOpenTicks, index - ticks.length)
                content = content.replace('\n', ' ')

                // spec: If the resulting string both begins and ends with a space character, but does not consist
                // entirely of space characters, a single space character is removed from the front and back.
                if (content.length >= 3 && content[0] == ' ' && content[content.length - 1] == ' ' &&
                    Parsing.hasNonSpace(content)
                ) {
                    content = content.substring(1, content.length - 1)
                }
                node.literal = content
                return node
            }
        }
        // If we got here, we didn't match a closing backtick sequence.
        index = afterOpenTicks
        return text(ticks)
    }

    /**
     * Attempt to parse delimiters like emphasis, strong emphasis or custom delimiters.
     */
    private fun parseDelimiters(
        delimiterProcessor: DelimiterProcessor?,
        delimiterChar: Char,
    ): Node? {
        val res = scanDelimiters(delimiterProcessor, delimiterChar) ?: return null
        val length = res.count
        val startIndex = index
        index += length
        val node = text(input, startIndex, index)

        // Add entry to stack for this opener
        lastDelimiter = Delimiter(node, delimiterChar, res.canOpen, res.canClose, lastDelimiter)
        lastDelimiter!!.length = length
        lastDelimiter!!.originalLength = length
        if (lastDelimiter!!.previous != null) {
            lastDelimiter!!.previous.next = lastDelimiter
        }
        return node
    }

    /**
     * Add open bracket to delimiter stack and add a text node to block's children.
     */
    private fun parseOpenBracket(): Node {
        val startIndex = index
        index++
        val node = text("[")

        // Add entry to stack for this opener
        addBracket(Bracket.link(node, startIndex, lastBracket, lastDelimiter))
        return node
    }

    /**
     * If next character is [, and ! delimiter to delimiter stack and add a text node to block's children.
     * Otherwise just add a text node.
     */
    private fun parseBang(): Node {
        val startIndex = index
        index++
        return if (peek() == '[') {
            index++
            val node = text("![")

            // Add entry to stack for this opener
            addBracket(
                Bracket.image(
                    node,
                    startIndex + 1,
                    lastBracket,
                    lastDelimiter,
                ),
            )
            node
        } else {
            text("!")
        }
    }

    /**
     * Try to match close bracket against an opening in the delimiter stack. Return either a link or image, or a
     * plain [ character. If there is a matching delimiter, remove it from the delimiter stack.
     */
    private fun parseCloseBracket(): Node {
        index++
        val startIndex = index

        // Get previous `[` or `![`
        val opener = lastBracket
            ?: // No matching opener, just return a literal.
            return text("]")
        if (!opener.allowed) {
            // Matching opener but it's not allowed, just return a literal.
            removeLastBracket()
            return text("]")
        }

        // Check to see if we have a link/image
        var dest: String? = null
        var title: String? = null
        var isLinkOrImage = false

        // Maybe a inline link like `[foo](/uri "title")`
        if (peek() == '(') {
            index++
            spnl()
            if (parseLinkDestination().also { dest = it } != null) {
                spnl()
                // title needs a whitespace before
                if (WHITESPACE.matcher(input.substring(index - 1, index)).matches()) {
                    title = parseLinkTitle()
                    spnl()
                }
                if (peek() == ')') {
                    index++
                    isLinkOrImage = true
                } else {
                    index = startIndex
                }
            }
        }

        // Maybe a reference link like `[foo][bar]`, `[foo][]` or `[foo]`
        if (!isLinkOrImage) {
            // See if there's a link label like `[bar]` or `[]`
            val beforeLabel = index
            parseLinkLabel()
            val labelLength = index - beforeLabel
            var ref: String? = null
            if (labelLength > 2) {
                ref = input.substring(beforeLabel, beforeLabel + labelLength)
            } else if (!opener.bracketAfter) {
                // If the second label is empty `[foo][]` or missing `[foo]`, then the first label is the reference.
                // But it can only be a reference when there's no (unescaped) bracket in it.
                // If there is, we don't even need to try to look up the reference. This is an optimization.
                ref = input.substring(opener.index, startIndex)
            }
            if (ref != null) {
                val label = Escaping.normalizeReference(ref)
                val definition = context.getLinkReferenceDefinition(label)
                if (definition != null) {
                    dest = definition.destination
                    title = definition.title
                    isLinkOrImage = true
                }
            }
        }
        return if (isLinkOrImage) {
            // If we got here, open is a potential opener
            val linkOrImage = if (opener.image) Image(dest, title) else Link(dest, title)
            var node = opener.node.next
            while (node != null) {
                val next = node.next
                linkOrImage.appendChild(node)
                node = next
            }

            // Process delimiters such as emphasis inside link/image
            processDelimiters(opener.previousDelimiter)
            mergeChildTextNodes(linkOrImage)
            // We don't need the corresponding text node anymore, we turned it into a link/image node
            opener.node.unlink()
            removeLastBracket()

            // Links within links are not allowed. We found this link, so there can be no other link around it.
            if (!opener.image) {
                var bracket = lastBracket
                while (bracket != null) {
                    if (!bracket.image) {
                        // Disallow link opener. It will still get matched, but will not result in a link.
                        bracket.allowed = false
                    }
                    bracket = bracket.previous
                }
            }
            linkOrImage
        } else { // no link or image
            index = startIndex
            removeLastBracket()
            text("]")
        }
    }

    private fun addBracket(bracket: Bracket) {
        if (lastBracket != null) {
            lastBracket!!.bracketAfter = true
        }
        lastBracket = bracket
    }

    private fun removeLastBracket() {
        lastBracket = lastBracket!!.previous
    }

    /**
     * Attempt to parse link destination, returning the string or null if no match.
     */
    private fun parseLinkDestination(): String? {
        val afterDest = LinkScanner.scanLinkDestination(input, index)
        if (afterDest == -1) {
            return null
        }
        val dest: String = if (peek() == '<') {
            // chop off surrounding <..>:
            input.substring(index + 1, afterDest - 1)
        } else {
            input.substring(index, afterDest)
        }
        index = afterDest
        return Escaping.unescapeString(dest)
    }

    /**
     * Attempt to parse link title (sans quotes), returning the string or null if no match.
     */
    private fun parseLinkTitle(): String? {
        val afterTitle = LinkScanner.scanLinkTitle(input, index)
        if (afterTitle == -1) {
            return null
        }

        // chop off ', " or parens
        val title = input.substring(index + 1, afterTitle - 1)
        index = afterTitle
        return Escaping.unescapeString(title)
    }

    /**
     * Attempt to parse a link label, returning number of characters parsed.
     */
    private fun parseLinkLabel(): Int {
        if (index >= input.length || input[index] != '[') {
            return 0
        }
        val startContent = index + 1
        val endContent = LinkScanner.scanLinkLabelContent(input, startContent)
        // spec: A link label can have at most 999 characters inside the square brackets.
        val contentLength = endContent - startContent
        if (endContent == -1 || contentLength > 999) {
            return 0
        }
        if (endContent >= input.length || input[endContent] != ']') {
            return 0
        }
        index = endContent + 1
        return contentLength + 2
    }

    /**
     * Attempt to parse an autolink (URL or email in pointy brackets).
     */
    private fun parseAutolink(): Node? {
        var m: String? = match(EMAIL_AUTOLINK)
        if (m != null) {
            val dest = m.substring(1, m.length - 1)
            val node = Link("mailto:$dest", null)
            node.appendChild(Text(dest))
            return node
        }

        m = match(AUTOLINK)
        if (m != null) {
            val dest = m.substring(1, m.length - 1)
            val node = Link(dest, null)
            node.appendChild(Text(dest))
            return node
        }

        return null
    }

    /**
     * Attempt to parse inline HTML.
     */
    private fun parseHtmlInline(): Node? {
        val m = match(HTML_TAG)
        return if (m != null) {
            val node = HtmlInline()
            node.literal = m
            node
        } else {
            null
        }
    }

    /**
     * Attempt to parse a HTML style entity.
     */
    private fun parseEntity(): Node? {
        var m: String?
        return if (match(ENTITY_HERE).also { m = it } != null) {
            text(Html5Entities.entityToString(m))
        } else {
            null
        }
    }

    /**
     * Parse a run of ordinary characters, or a single character with a special meaning in markdown, as a plain string.
     */
    private fun parseString(): Node? {
        val begin = index
        val length = input.length
        while (index != length) {
            if (specialCharacters[input[index].code]) {
                break
            }
            index++
        }
        return if (begin != index) {
            text(input, begin, index)
        } else {
            null
        }
    }

    /**
     * Scan a sequence of characters with code delimiterChar, and return information about the number of delimiters
     * and whether they are positioned such that they can open and/or close emphasis or strong emphasis.
     *
     * @return information about delimiter run, or `null`
     */
    private fun scanDelimiters(
        delimiterProcessor: DelimiterProcessor?,
        delimiterChar: Char,
    ): DelimiterData? {
        val startIndex = index
        var delimiterCount = 0
        while (peek() == delimiterChar) {
            delimiterCount++
            index++
        }
        if (delimiterCount < delimiterProcessor!!.minLength) {
            index = startIndex
            return null
        }
        val before = if (startIndex == 0) "\n" else input.substring(startIndex - 1, startIndex)
        val charAfter = peek()
        val after = if (charAfter == '\u0000') "\n" else charAfter.toString()

        // We could be more lazy here, in most cases we don't need to do every match case.
        val beforeIsPunctuation = PUNCTUATION.matcher(before).matches()
        val beforeIsWhitespace = UNICODE_WHITESPACE_CHAR.matcher(before).matches()
        val afterIsPunctuation = PUNCTUATION.matcher(after).matches()
        val afterIsWhitespace = UNICODE_WHITESPACE_CHAR.matcher(after).matches()
        val leftFlanking = !afterIsWhitespace &&
            (!afterIsPunctuation || beforeIsWhitespace || beforeIsPunctuation)
        val rightFlanking = !beforeIsWhitespace &&
            (!beforeIsPunctuation || afterIsWhitespace || afterIsPunctuation)
        val canOpen: Boolean
        val canClose: Boolean
        if (delimiterChar == '_') {
            canOpen = leftFlanking && (!rightFlanking || beforeIsPunctuation)
            canClose = rightFlanking && (!leftFlanking || afterIsPunctuation)
        } else {
            canOpen = leftFlanking && delimiterChar == delimiterProcessor.openingCharacter
            canClose = rightFlanking && delimiterChar == delimiterProcessor.closingCharacter
        }
        index = startIndex
        return DelimiterData(delimiterCount, canOpen, canClose)
    }

    private fun processDelimiters(stackBottom: Delimiter?) {
        val openersBottom: MutableMap<Char, Delimiter?> = HashMap()

        // find first closer above stackBottom:
        var closer = lastDelimiter
        while (closer != null && closer.previous !== stackBottom) {
            closer = closer.previous
        }
        // move forward, looking for closers, and handling each
        while (closer != null) {
            val delimiterChar = closer.delimiterChar
            val delimiterProcessor = delimiterProcessors[delimiterChar]
            if (!closer.canClose || delimiterProcessor == null) {
                closer = closer.next
                continue
            }
            val openingDelimiterChar = delimiterProcessor.openingCharacter

            // Found delimiter closer. Now look back for first matching opener.
            var useDelims = 0
            var openerFound = false
            var potentialOpenerFound = false
            var opener = closer.previous
            while (opener != null && opener !== stackBottom && opener !== openersBottom[delimiterChar]) {
                if (opener.canOpen && opener.delimiterChar == openingDelimiterChar) {
                    potentialOpenerFound = true
                    useDelims = delimiterProcessor.getDelimiterUse(opener, closer)
                    if (useDelims > 0) {
                        openerFound = true
                        break
                    }
                }
                opener = opener.previous
            }
            if (!openerFound) {
                if (!potentialOpenerFound) {
                    // Set lower bound for future searches for openers.
                    // Only do this when we didn't even have a potential
                    // opener (one that matches the character and can open).
                    // If an opener was rejected because of the number of
                    // delimiters (e.g. because of the "multiple of 3" rule),
                    // we want to consider it next time because the number
                    // of delimiters can change as we continue processing.
                    openersBottom[delimiterChar] = closer.previous
                    if (!closer.canOpen) {
                        // We can remove a closer that can't be an opener,
                        // once we've seen there's no matching opener:
                        removeDelimiterKeepNode(closer)
                    }
                }
                closer = closer.next
                continue
            }
            val openerNode = opener!!.node
            val closerNode = closer.node

            // Remove number of used delimiters from stack and inline nodes.
            opener.length -= useDelims
            closer.length -= useDelims
            openerNode.literal = openerNode.literal.substring(
                0,
                openerNode.literal.length - useDelims,
            )
            closerNode.literal = closerNode.literal.substring(
                0,
                closerNode.literal.length - useDelims,
            )
            removeDelimitersBetween(opener, closer)
            // The delimiter processor can re-parent the nodes between opener and closer,
            // so make sure they're contiguous already. Exclusive because we want to keep opener/closer themselves.
            mergeTextNodesBetweenExclusive(openerNode, closerNode)
            delimiterProcessor.process(openerNode, closerNode, useDelims)

            // No delimiter characters left to process, so we can remove delimiter and the now empty node.
            if (opener.length == 0) {
                removeDelimiterAndNode(opener)
            }
            if (closer.length == 0) {
                val next = closer.next
                removeDelimiterAndNode(closer)
                closer = next
            }
        }

        // remove all delimiters
        while (lastDelimiter != null && lastDelimiter !== stackBottom) {
            removeDelimiterKeepNode(lastDelimiter!!)
        }
    }

    private fun removeDelimitersBetween(opener: Delimiter?, closer: Delimiter) {
        var delimiter = closer.previous
        while (delimiter != null && delimiter !== opener) {
            val previousDelimiter = delimiter.previous
            removeDelimiterKeepNode(delimiter)
            delimiter = previousDelimiter
        }
    }

    /**
     * Remove the delimiter and the corresponding text node. For used delimiters, e.g. `*` in `*foo*`.
     */
    private fun removeDelimiterAndNode(delim: Delimiter?) {
        val node = delim!!.node
        node.unlink()
        removeDelimiter(delim)
    }

    /**
     * Remove the delimiter but keep the corresponding node as text. For unused delimiters such as `_` in `foo_bar`.
     */
    private fun removeDelimiterKeepNode(delim: Delimiter) {
        removeDelimiter(delim)
    }

    private fun removeDelimiter(delim: Delimiter?) {
        if (delim!!.previous != null) {
            delim.previous.next = delim.next
        }
        if (delim.next == null) {
            // top of stack
            lastDelimiter = delim.previous
        } else {
            delim.next.previous = delim.previous
        }
    }

    private fun mergeTextNodesBetweenExclusive(fromNode: Node, toNode: Node) {
        // No nodes between them
        if (fromNode === toNode || fromNode.next === toNode) {
            return
        }
        mergeTextNodesInclusive(fromNode.next, toNode.previous)
    }

    private fun mergeChildTextNodes(node: Node) {
        // No children or just one child node, no need for merging
        if (node.firstChild === node.lastChild) {
            return
        }
        mergeTextNodesInclusive(node.firstChild, node.lastChild)
    }

    private fun mergeTextNodesInclusive(fromNode: Node, toNode: Node) {
        var first: Text? = null
        var last: Text? = null
        var length = 0
        var node: Node? = fromNode
        while (node != null) {
            if (node is Text) {
                val text = node
                if (first == null) {
                    first = text
                }
                length += text.literal.length
                last = text
            } else {
                mergeIfNeeded(first, last, length)
                first = null
                last = null
                length = 0
            }
            if (node === toNode) {
                break
            }
            node = node.next
        }
        mergeIfNeeded(first, last, length)
    }

    private fun mergeIfNeeded(first: Text?, last: Text?, textLength: Int) {
        if (first != null && last != null && first !== last) {
            val sb = StringBuilder(textLength)
            sb.append(first.literal)
            var node = first.next
            val stop = last.next
            while (node !== stop) {
                sb.append((node as Text).literal)
                val unlink = node
                node = node.getNext()
                unlink.unlink()
            }
            val literal = sb.toString()
            first.literal = literal
        }
    }

    private class DelimiterData internal constructor(
        val count: Int,
        val canOpen: Boolean,
        val canClose: Boolean,
    )

    companion object {
        private const val HTMLCOMMENT = "<!---->|<!--(?:-?[^>-])(?:-?[^-])*-->"
        private const val PROCESSINGINSTRUCTION = "[<][?].*?[?][>]"
        private const val DECLARATION = "<![A-Z]+\\s+[^>]*>"
        private const val CDATA = "<!\\[CDATA\\[[\\s\\S]*?\\]\\]>"
        private const val HTMLTAG =
            (
                "(?:" + Parsing.OPENTAG + "|" + Parsing.CLOSETAG + "|" + HTMLCOMMENT +
                    "|" + PROCESSINGINSTRUCTION + "|" + DECLARATION + "|" + CDATA + ")"
                )
        private const val ASCII_PUNCTUATION =
            "!\"#\\$%&'\\(\\)\\*\\+,\\-\\./:;<=>\\?@\\[\\\\\\]\\^_`\\{\\|\\}~"
        private val PUNCTUATION = Pattern
            .compile("^[$ASCII_PUNCTUATION\\p{Pc}\\p{Pd}\\p{Pe}\\p{Pf}\\p{Pi}\\p{Po}\\p{Ps}]")
        private val HTML_TAG = Pattern.compile("^$HTMLTAG", Pattern.CASE_INSENSITIVE)
        private val ESCAPABLE = Pattern.compile('^'.toString() + Escaping.ESCAPABLE)
        private val ENTITY_HERE =
            Pattern.compile('^'.toString() + Escaping.ENTITY, Pattern.CASE_INSENSITIVE)
        private val TICKS = Pattern.compile("`+")
        private val TICKS_HERE = Pattern.compile("^`+")
        private val EMAIL_AUTOLINK = Pattern
            .compile("^<([a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)>")
        private val AUTOLINK = Pattern
            .compile("^<[a-zA-Z][a-zA-Z0-9.+-]{1,31}:[^<>\u0000-\u0020]*>")
        private val SPNL = Pattern.compile("^ *(?:\n *)?")
        private val UNICODE_WHITESPACE_CHAR = Pattern.compile("^[\\p{Zs}\t\r\n\u000c]")
        private val WHITESPACE = Pattern.compile("\\s+")
        private val FINAL_SPACE = Pattern.compile(" *$")
        fun calculateDelimiterCharacters(characters: Set<Char>): BitSet {
            val bitSet = BitSet()
            for (character in characters) {
                bitSet.set(character.code)
            }
            return bitSet
        }

        fun calculateSpecialCharacters(delimiterCharacters: BitSet?): BitSet {
            val bitSet = BitSet()
            bitSet.or(delimiterCharacters)
            bitSet.set('\n'.code)
            bitSet.set('`'.code)
            bitSet.set('['.code)
            bitSet.set(']'.code)
            bitSet.set('\\'.code)
            bitSet.set('!'.code)
            bitSet.set('<'.code)
            bitSet.set('&'.code)
            return bitSet
        }

        private fun addDelimiterProcessorForChar(
            delimiterChar: Char,
            toAdd: DelimiterProcessor,
            delimiterProcessors: MutableMap<Char, DelimiterProcessor>,
        ) {
            val existing = delimiterProcessors.put(delimiterChar, toAdd)
            require(existing == null) { "Delimiter processor conflict with delimiter char '$delimiterChar'" }
        }
    }
}
