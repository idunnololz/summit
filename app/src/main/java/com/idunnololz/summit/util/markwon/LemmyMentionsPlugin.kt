package com.idunnololz.summit.util.markwon
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.util.LinkUtils
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SpannableBuilder
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.CoreProps
import java.util.Locale
import java.util.regex.Pattern
import org.commonmark.node.Link

class LemmyMentionsPlugin : AbstractMarkwonPlugin() {

  companion object {
    private const val MENTIONS_REGEX =
      """(]\()?(!|/?[cC]/|@|/?[uU]/)([^@\s]+)@([^@\s]+\.[^@\s)]*\w)"""
  }

  private val mentionsMatcher = Pattern.compile(MENTIONS_REGEX)

  override fun configure(registry: MarkwonPlugin.Registry) {
    registry.require(CorePlugin::class.java) {
      it.addOnTextAddedListener(object : CorePlugin.OnTextAddedListener {
        override fun onTextAdded(visitor: MarkwonVisitor, text: String, start: Int) {
          val matcher = mentionsMatcher.matcher(text)
          while (matcher.find()) {
            val linkStart = matcher.group(1)
            val referenceTypeToken = matcher.group(2)
            val name = matcher.group(3)
            val instance = matcher.group(4)

            if (linkStart == null &&
              referenceTypeToken != null &&
              name != null &&
              instance != null &&
              !name.contains("]") &&
              !instance.contains("]") /* make sure we are not within a link def */
            ) {
              when (referenceTypeToken.lowercase(Locale.US)) {
                "!", "c/", "/c/" -> {
                  val communityRef = CommunityRef.CommunityRefByName(name, instance)

                  CoreProps.LINK_DESTINATION.set(
                    visitor.renderProps(),
                    LinkUtils.getLinkForCommunity(communityRef),
                  )

                  val spanFactory = visitor.configuration().spansFactory().require(Link::class.java)
                  SpannableBuilder.setSpans(
                    visitor.builder(),
                    spanFactory.getSpans(visitor.configuration(), visitor.renderProps()),
                    start + matcher.start(),
                    start + matcher.end(),
                  )
                }
                "@", "u/", "/u/" -> {
                  CoreProps.LINK_DESTINATION.set(
                    visitor.renderProps(),
                    LinkUtils.getLinkForPerson(instance = instance, name = name),
                  )

                  val spanFactory = visitor.configuration().spansFactory().require(Link::class.java)
                  SpannableBuilder.setSpans(
                    visitor.builder(),
                    spanFactory.getSpans(visitor.configuration(), visitor.renderProps()),
                    start + matcher.start(),
                    start + matcher.end(),
                  )
                }
              }
            }
          }
        }
      })
    }
  }
}
