package com.idunnololz.summit.lemmy.inbox.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentReportDetailsBinding
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.InboxTabbedFragment
import com.idunnololz.summit.lemmy.inbox.ReportItem
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportDetailsFragment : BaseFragment<FragmentReportDetailsBinding>() {

  private val args: ReportDetailsFragmentArgs by navArgs()
  private val viewModel: ReportDetailsViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentReportDetailsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val inboxItem = args.reportItem
    val reportItem = args.reportItem as ReportItem

    with(binding) {
      requireSummitActivity().apply {
        insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, toolbar)
        insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, mainContainer)
        insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, fabContainer)
      }

      val title = when (reportItem) {
        is InboxItem.ReportCommentInboxItem -> {
          getString(R.string.report_on_comment)
        }
        is InboxItem.ReportMessageInboxItem -> {
          getString(R.string.report_on_private_message)
        }
        is InboxItem.ReportPostInboxItem -> {
          getString(R.string.report_on_post)
        }
      }

      setupToolbar(toolbar, title)
      toolbar.setNavigationOnClickListener {
        (parentFragment as? InboxTabbedFragment)?.closeMessage()
      }

      author.text = getString(R.string.from_format, inboxItem.authorName)
      author.setOnClickListener {
        getMainActivity()?.launchPage(
          PersonRef.PersonRefByName(
            name = inboxItem.authorName,
            instance = inboxItem.authorInstance,
          ),
        )
      }

      content.text = inboxItem.content
    }

  }
}