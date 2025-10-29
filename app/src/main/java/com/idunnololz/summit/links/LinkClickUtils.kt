package com.idunnololz.summit.links

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.links.PreviewLinkOptions.PreviewAllLinks
import com.idunnololz.summit.links.PreviewLinkOptions.PreviewNoLinks
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import androidx.core.net.toUri

fun BaseFragment<*>.onLinkClick(url: String, text: String?, linkContext: LinkContext) {
  getBaseActivity()?.onLinkClick(
    context ?: return,
    (activity?.application as? MainApplication) ?: return,
    childFragmentManager,
    url,
    text,
    linkContext,
  )
}

fun BaseDialogFragment<*>.onLinkClick(url: String, text: String?, linkContext: LinkContext) {
  getBaseActivity()?.onLinkClick(
    context ?: return,
    (activity?.application as? MainApplication) ?: return,
    childFragmentManager,
    url,
    text,
    linkContext,
  )
}

fun BaseActivity.onLinkClick(
  context: Context,
  application: MainApplication,
  fragmentManager: FragmentManager,
  url: String,
  text: String?,
  linkContext: LinkContext,
) {
  val preferences = application.preferences

  try {
    val uri = url.toUri()

    if (this is MainActivity) {
      if (uri.host.equals("loops.video", ignoreCase = true) &&
        uri.path?.startsWith("/v/", ignoreCase = true) == true
      ) {
        openVideo(url, VideoType.Unknown, null)
        return
      }
    }
  } catch (_: Exception) {
    // do nothing
  }

  if (linkContext == LinkContext.Force) {
    Utils.openExternalLink(context, url)
    return
  }

  when (preferences.previewLinks) {
    PreviewNoLinks -> {
      Utils.openExternalLink(context, url)
    }
    PreviewAllLinks -> {
      LinkPreviewDialogFragment.show(fragmentManager, url)
    }
    else -> {
      if (linkContext == LinkContext.Text) {
        LinkPreviewDialogFragment.show(fragmentManager, url)
      } else {
        Utils.openExternalLink(context, url)
      }
    }
  }
}

/**
 * The location of the link. Special logic may be triggered based on where the click was.
 */
enum class LinkContext {
  Text,

  /**
   * Eg. the user tapped on a button that said "Open link"
   */
  Action,

  /**
   * Eg. the user tapped on a special view with an image and other information.
   */
  Rich,

  /**
   * Means to open the link without any bs.
   */
  Force,
}

object PreviewLinkOptions {
  const val PreviewTextLinks = 0
  const val PreviewNoLinks = 1
  const val PreviewAllLinks = 2
}
