package com.idunnololz.summit.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_LEMMY_INSTANCES
import com.idunnololz.summit.databinding.FragmentLoginBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentLoginBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().apply {
            insetViewAutomaticallyByMargins(viewLifecycleOwner, view)
            setupForFragment<LoginFragment>()
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
                            AlertDialogFragment.Builder()
                                .setMessage(it.error.message ?: getString(R.string.error_unknown))
                                .createAndShow(childFragmentManager, "asdf")
                        }
                    } else {
                        AlertDialogFragment.Builder()
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
                        DEFAULT_LEMMY_INSTANCES
                    )
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
            login.setOnClickListener {
                val instance = instance.editText?.text?.toString() ?: return@setOnClickListener
                val username = username.editText?.text?.toString() ?: return@setOnClickListener
                val password = password.editText?.text?.toString() ?: return@setOnClickListener

                viewModel.login(
                    instance = instance,
                    username = username,
                    password = password,
                )
            }
            updateLoginButtonState()
        }

        hideProgressBar()
    }

    private fun disableAllFields() {
        with(binding) {
            instance.isEnabled = false
            username.isEnabled = false
            password.isEnabled = false
            login.isEnabled = false
        }
    }
    private fun enableAllFields() {
        with(binding) {
            instance.isEnabled = true
            username.isEnabled = true
            password.isEnabled = true
            updateLoginButtonState()
        }
    }

    private fun updateLoginButtonState() {
        with(binding) {
            login.isEnabled =
                !instance.editText?.text.isNullOrBlank() &&
                        !username.editText?.text.isNullOrBlank() &&
                        !password.editText?.text.isNullOrBlank()
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