package com.idunnololz.summit.login

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_LEMMY_INSTANCES
import com.idunnololz.summit.databinding.FragmentLoginBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    private val viewModel: LoginViewModel by viewModels()

    private val onBackPressedHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            viewModel.onBackPress()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentLoginBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().apply {
            insetViewAutomaticallyByPadding(viewLifecycleOwner, view)

            insets.observe(viewLifecycleOwner) {
                binding.instanceEditText.post {
                    if (!isBindingAvailable()) return@post
                    if (binding.instanceEditText.isPopupShowing) {
                        binding.instanceEditText.dismissDropDown()
                        binding.instanceEditText.showDropDown()
                    }
                }
            }
        }

        viewModel.state.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(binding.root)
            when (it) {
                LoginViewModel.State.Login -> {
                    binding.title.visibility = View.VISIBLE
                    binding.instance.visibility = View.VISIBLE
                    binding.username.visibility = View.VISIBLE
                    binding.password.visibility = View.VISIBLE
                    binding.signUpText.visibility = View.VISIBLE

                    binding.title2fa.visibility = View.GONE
                    binding.body2fa.visibility = View.GONE
                    binding.twoFactorInput.visibility = View.GONE

                    binding.login.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        topToBottom = binding.password.id
                        bottomToTop = binding.signUpText.id
                    }

                    onBackPressedHandler.remove()
                    updateLoginButtonState()
                }
                is LoginViewModel.State.TwoFactorAuth -> {
                    binding.title.visibility = View.GONE
                    binding.instance.visibility = View.GONE
                    binding.username.visibility = View.GONE
                    binding.password.visibility = View.GONE
                    binding.signUpText.visibility = View.GONE

                    binding.title2fa.visibility = View.VISIBLE
                    binding.body2fa.visibility = View.VISIBLE
                    binding.twoFactorInput.visibility = View.VISIBLE

                    binding.login.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        topToBottom = binding.twoFactorInput.id
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    }

                    hideProgressBar()
                    enableAllFields()

                    updateLoginButtonState()
                    requireActivity().onBackPressedDispatcher.addCallback(
                        viewLifecycleOwner,
                        onBackPressedHandler,
                    )
                }
            }
        }

        viewModel.accountLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    hideProgressBar()
                    enableAllFields()

                    if (it.error is ClientApiException) {
                        if (it.error.errorMessage == "password_incorrect") {
                            binding.password.error = getString(R.string.incorrect_password)
                        } else {
                            OldAlertDialogFragment.Builder()
                                .setMessage(it.error.message ?: getString(R.string.error_unknown))
                                .createAndShow(childFragmentManager, "asdf")
                        }
                    } else {
                        OldAlertDialogFragment.Builder()
                            .setMessage(it.error.message ?: getString(R.string.error_unknown))
                            .createAndShow(childFragmentManager, "asdf")
                    }
                }
                is StatefulData.Loading -> {
                    disableAllFields()
                    showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    hideProgressBar()
                    enableAllFields()
                    findNavController().navigateUp()
                }
            }
        }

        val context = requireContext()
        with(binding) {
            (instance.editText as AutoCompleteTextView).apply {
                setAdapter(
                    ArrayAdapter(
                        context,
                        com.google.android.material.R.layout.m3_auto_complete_simple_item,
                        DEFAULT_LEMMY_INSTANCES,
                    ),
                )
            }

            instance.editText?.doOnTextChanged { text, start, before, count ->
                updateLoginButtonState()
            }
            username.editText?.doOnTextChanged { text, start, before, count ->
                updateLoginButtonState()
            }
            password.editText?.doOnTextChanged { text, start, before, count ->
                updateLoginButtonState()
            }
            twoFactorInput.editText?.doOnTextChanged { text, start, before, count ->
                updateLoginButtonState()
            }
            login.setOnClickListener {
                when (viewModel.state.value) {
                    LoginViewModel.State.Login -> {
                        val instance = instance.editText?.text?.toString()?.trim()
                            ?: return@setOnClickListener
                        val username = username.editText?.text?.toString()
                            ?: return@setOnClickListener
                        val password = password.editText?.text?.toString()
                            ?: return@setOnClickListener

                        viewModel.login(
                            instance = instance,
                            username = username,
                            password = password,
                            twoFactorCode = null,
                        )
                    }
                    is LoginViewModel.State.TwoFactorAuth -> {
                        val twoFactorCode = twoFactorInput.editText?.text?.toString()
                            ?: return@setOnClickListener
                        viewModel.login2fa(twoFactorCode)
                    }
                    null -> {}
                }
            }
            signUpText.text =
                LemmyTextHelper.getSpannable(context, getString(R.string.sign_up_title))
            signUpText.setOnClickListener {
                val direction = LoginFragmentDirections.actionLoginFragmentToSignUpFragment()
                findNavController().navigateSafe(direction)
            }
            updateLoginButtonState()
        }

        hideProgressBar()
    }

    override fun onResume() {
        super.onResume()

        setupForFragment<LoginFragment>()
    }

    private fun disableAllFields() {
        with(binding) {
            instance.isEnabled = false
            username.isEnabled = false
            password.isEnabled = false
            login.isEnabled = false

            twoFactorInput.isEnabled = false
        }
    }
    private fun enableAllFields() {
        with(binding) {
            instance.isEnabled = true
            username.isEnabled = true
            password.isEnabled = true

            twoFactorInput.isEnabled = true

            updateLoginButtonState()
        }
    }

    private fun updateLoginButtonState() {
        if (!isBindingAvailable()) {
            return
        }

        with(binding) {
            when (viewModel.state.value) {
                LoginViewModel.State.Login -> {
                    login.isEnabled =
                        !instance.editText?.text.isNullOrBlank() &&
                        !username.editText?.text.isNullOrBlank() &&
                        !password.editText?.text.isNullOrBlank()
                }
                is LoginViewModel.State.TwoFactorAuth -> {
                    login.isEnabled =
                        !twoFactorInput.editText?.text.isNullOrBlank()
                }
                null -> {}
            }
        }
    }

    private fun showProgressBar() {
        with(binding) {
            progressBar.visibility = View.VISIBLE
            login.text = ""
        }
    }

    private fun hideProgressBar() {
        with(binding) {
            progressBar.visibility = View.GONE
            login.text = getString(R.string.login)
        }
    }
}
