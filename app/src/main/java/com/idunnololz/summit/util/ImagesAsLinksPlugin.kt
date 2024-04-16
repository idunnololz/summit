package com.idunnololz.summit.util

import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.image.ImageProps
import org.commonmark.node.Image
import org.commonmark.node.Link

class ImagesAsLinksPlugin : AbstractMarkwonPlugin() {

    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
        builder.setFactory(Image::class.java) { configuration, props ->
            LinkSpan(
                configuration.theme(),
                ImageProps.DESTINATION.require(props),
                configuration.linkResolver(),
            )
        }
    }

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(Image::class.java) { visitor, node ->
            val spanFactory = visitor.configuration().spansFactory()[Image::class.java]
            if (spanFactory == null) {
                visitor.visitChildren(node)
                return@on
            }

            val length = visitor.length()

            visitor.visitChildren(node)

            // we must check if anything _was_ added, as we need at least one char to render
            if (length == visitor.length()) {
                visitor.builder().append("[image]")
            }

            val configuration = visitor.configuration()

            val parent = node.parent
            val link = parent is Link

            val destination = configuration
                .imageDestinationProcessor()
                .process(node.destination)

            val props = visitor.renderProps()

            // apply image properties
            // Please note that we explicitly set IMAGE_SIZE to null as we do not clear
            // properties after we applied span (we could though)
            ImageProps.DESTINATION.set(props, destination)
            ImageProps.REPLACEMENT_TEXT_IS_LINK.set(props, link)
            ImageProps.IMAGE_SIZE.set(props, null)

            visitor.setSpans(length, spanFactory.getSpans(configuration, props))
        }
    }
}
