package com.idunnololz.summit.settings.webSettings.changePassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.DialogFragmentChangePasswordBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordDialogFragment : BaseDialogFragment<DialogFragmentChangePasswordBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener{

    companion object {
        fun show(fragmentManager: FragmentManager) {
            ChangePasswordDialogFragment()
                .show(fragmentManager, "ChangePasswordDialogFragment")
        }
    }

    private val viewModel: ChangePasswordViewModel by viewModels()

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

        setBinding(DialogFragmentChangePasswordBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            toolbar.setTitle(R.string.change_password)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(android.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                dismiss()
            }

            val inputs = listOf(
                currentPasswordInput,
                newPasswordInput,
                newPasswordAgainInput,
            )

            fun enableInputs() {
                inputs.forEach { it.isEnabled = true }
            }

            fun disableInputs() {
                inputs.forEach { it.isEnabled = false }
            }

            positiveButton.setOnClickListener {
                val currentPassword = currentPasswordEditText.text?.toString()
                val newPassword = newPasswordEditText.text?.toString()
                val newPasswordAgain = newPasswordAgainEditText.text?.toString()

                if (currentPassword.isNullOrBlank()) {
                    currentPasswordInput.error = getString(R.string.error_cannot_be_blank)
                    return@setOnClickListener
                }
                if (newPassword.isNullOrBlank()) {
                    newPasswordInput.error = getString(R.string.error_cannot_be_blank)
                    return@setOnClickListener
                }
                if (newPasswordAgain.isNullOrBlank()) {
                    newPasswordAgainInput.error = getString(R.string.error_cannot_be_blank)
                    return@setOnClickListener
                }
                if (newPassword != newPasswordAgain) {
                    newPasswordAgainInput.error = getString(R.string.error_passwords_do_not_match)
                    return@setOnClickListener
                }

                inputs.forEach { it.error = null }

                viewModel.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    newPasswordAgain = newPasswordAgain,
                )
            }

            viewModel.changePasswordState.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.hideAll()
                        enableInputs()

                        ErrorDialogFragment.show(
                            it.error.toErrorMessage(context),
                            it.error,
                            childFragmentManager,
                        )
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                        disableInputs()
                    }
                    is StatefulData.NotStarted -> {
                        loadingView.hideAll()
                        enableInputs()
                    }
                    is StatefulData.Success -> {
                        AlertDialogFragment.Builder()
                            .setMessage(R.string.password_changed)
                            .setPositiveButton(android.R.string.ok)
                            .createAndShow(childFragmentManager, "password_changed")
                    }
                }
            }
        }
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        dismiss()
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }
}