package com.idunnololz.summit.redirect

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentRedirectHandlerBinding
import com.idunnololz.summit.reddit.LemmyUtils
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.view.LoadingView

/**
 * Dialog that displays UI while resolving a redirect URL
 */
class RedirectHandlerDialogFragment : BaseDialogFragment<DialogFragmentRedirectHandlerBinding>() {

    companion object {

        private const val ARG_URL = "ARG_URL"

        fun newInstance(url: String): RedirectHandlerDialogFragment =
            RedirectHandlerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                }
            }
    }

    private lateinit var viewModel: RedirectHandlerViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)

        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.dialog_fragment_redirect_handler, null)
        val loadingView = rootView.findViewById<LoadingView>(R.id.loadingView)

        builder.setView(rootView)

        val url = requireArguments().getString(ARG_URL)

        viewModel = ViewModelProvider(this).get(RedirectHandlerViewModel::class.java)

        if (url == null) {
            return builder.create().also {
                dismiss()
            }
        }

        viewModel.redirectResult.observe(this, Observer {
            when (it) {
                is StatefulData.Error -> {
                    loadingView.showDefaultErrorMessageFor(it.error)
                }
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    LemmyUtils.openRedditUrl(requireContext(), it.data.finalUrl)
                    dismiss()
                }
            }
        })

        if (viewModel.redirectResult.value == null) {
            viewModel.resolveRedirectUrl(url)
        }

        return builder.create().also { dialog ->
        }
    }


}