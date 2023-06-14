package com.idunnololz.summit.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentAuthBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.PreferenceUtil
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class AuthDialogFragment : BaseDialogFragment<DialogFragmentAuthBinding>() {

    companion object {

        private val ARG_DATA = "ARG_DATA"

        fun newInstance(authData: Uri): AuthDialogFragment = AuthDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_DATA, authData.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentAuthBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadingView.showProgressBar()

        val arguments = requireArguments()
        val data = Uri.parse(arguments.getString(ARG_DATA))

        val theirStateToken = data.getQueryParameter("state")
        val ourStateToken =
            PreferenceUtil.preferences.getString(PreferenceUtil.KEY_STATE_TOKEN, null)

        if (theirStateToken != ourStateToken) {
            showError(getString(R.string.error_auth_state_tokens_do_not_match))
        } else {
            val error = data.getQueryParameter("error")

            when (error) {
                "access_denied" ->
                    showError(getString(R.string.error_auth_access_denied))
                "unsupported_response_type" ->
                    showError(getString(R.string.error_auth_unsupported_response_type))
                "invalid_scope" ->
                    showError(getString(R.string.error_auth_invalid_scope))
                "invalid_request" ->
                    showError(getString(R.string.error_unknown))
                else -> {
                    val code = data.getQueryParameter("code")
                    if (code == null) {
                        showError(getString(R.string.error_unknown))
                    } else {
                        doSuccessAction(code)
                    }
                }
            }
        }
    }

    private fun doSuccessAction(code: String) {
        // We want this request to go through no matter what...
        val disposable = Single
            .fromCallable {
                RedditAuthManager.instance.fetchToken(code)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                binding.loadingView.hideAll()

                binding.successTextView.setText(R.string.auth_success)
                binding.successTextView.visibility = View.VISIBLE

                binding.successTextView.postDelayed({
                    dismiss()
                }, 3000)
            }, {
                showError(getString(R.string.error_network))
            })
    }

    fun showError(text: String) {
        binding.apply {
            loadingView.hideAll()
            bodyTextView.text = text
            bodyTextView.visibility = View.VISIBLE

            positiveButton.visibility = View.VISIBLE
            positiveButton.setText(android.R.string.ok)
            positiveButton.setOnClickListener {
                dismiss()
            }
        }
    }
}