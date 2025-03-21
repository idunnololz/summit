package com.idunnololz.summit.goTo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.databinding.DialogFragmentGoToBinding
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.LinkResolver
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getPlainTextFromClipboard
import com.idunnololz.summit.util.ext.setSizeDynamically
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GoToDialogFragment : BaseDialogFragment<DialogFragmentGoToBinding>() {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            GoToDialogFragment().show(fragmentManager, "GoToDialogFragment")
        }
    }

    @Inject
    lateinit var apiClient: LemmyApiClient

    override fun onStart() {
        super.onStart()

        setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentGoToBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            toolbar.apply {
                title = getString(R.string.go_to)
                setNavigationIcon(R.drawable.baseline_close_24)
                setNavigationOnClickListener {
                    dismiss()
                }
                setNavigationIconTint(
                    context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
                )
            }

            linkInputLayout.setEndIconOnClickListener {
                val pasteData: String? = context.getPlainTextFromClipboard()
                if (pasteData != null) {
                    linkEditText.setText(pasteData)
                }
            }

            positiveButton.setOnClickListener {
                val url = linkEditText.text.toString()
                val pageRef = LinkResolver.parseUrl(url, apiClient.instance)

                if (pageRef != null) {
                    requireMainActivity().launchPage(pageRef)
                    dismiss()
                } else {
                    if (URLUtil.isValidUrl(url)) {
                        onLinkClick(url, null, LinkContext.Action)
                        dismiss()
                    } else {
                        OldAlertDialogFragment.Builder()
                            .setMessage(R.string.error_invalid_link)
                            .setPositiveButton(android.R.string.ok)
                            .createAndShow(childFragmentManager, "adssad")
                    }
                }
            }
        }
    }
}
