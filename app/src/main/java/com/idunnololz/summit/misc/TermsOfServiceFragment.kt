package com.idunnololz.summit.misc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentTermsOfServiceBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar

class TermsOfServiceFragment :
  BaseDialogFragment<FragmentTermsOfServiceBinding>(),
  FullscreenDialogFragment {

  companion object {
    private const val TERMS_OF_SERVICE_URL = "https://summit.idunnololz.com/terms.html"

    fun show(fragmentManager: FragmentManager) {
      TermsOfServiceFragment()
        .showAllowingStateLoss(
          fragmentManager,
          "CreateOrEditPostFragment",
        )
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentTermsOfServiceBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      requireSummitActivity().apply {
        insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, toolbar)
        insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, webView)
        setupToolbar(toolbar, getString(R.string.terms_of_service))
      }

      webView.webViewClient = object : WebViewClient() {
        override fun onReceivedError(
          view: WebView,
          request: WebResourceRequest,
          error: WebResourceError,
        ) {
          if (!request.isForMainFrame) return

          showLoadError(error.description.toString().ifBlank { getString(R.string.error_website) })
        }

        override fun onReceivedHttpError(
          view: WebView,
          request: WebResourceRequest,
          errorResponse: WebResourceResponse,
        ) {
          if (!request.isForMainFrame) return

          showLoadError(getString(R.string.error_server, errorResponse.statusCode.toString()))
        }
      }
      webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
          loadingProgress.setProgressCompat(newProgress, false)
          loadingProgress.isVisible = view.isVisible && newProgress < loadingProgress.max
        }
      }
      loadingView.setOnRefreshClickListener { loadTerms() }
      loadTerms()
    }
  }

  private fun loadTerms() {
    with(binding) {
      loadingView.hideAll(animate = false, makeViewGone = true)
      webView.isVisible = true
      loadingProgress.setProgressCompat(0, false)
      loadingProgress.isVisible = true
      webView.loadUrl(TERMS_OF_SERVICE_URL)
    }
  }

  private fun showLoadError(message: String) {
    with(binding) {
      webView.isVisible = false
      loadingProgress.isVisible = false
      loadingView.showErrorWithRetry(message)
    }
  }

  override fun onDestroyView() {
    with(binding) {
      webView.webChromeClient = null
      webView.webViewClient = WebViewClient()
      webView.stopLoading()
      contentContainer.removeView(webView)
      webView.destroy()
    }
    super.onDestroyView()
  }
}
