package com.idunnololz.summit.accountUi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.CommunityDirections
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.GuestAccountManager
import com.idunnololz.summit.account.toPersonRef
import com.idunnololz.summit.databinding.DialogFragmentAccountsBinding
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.ext.setup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountsAndSettingsDialogFragment : BaseDialogFragment<DialogFragmentAccountsBinding>() {

    companion object {

        const val REQUEST_KEY = "AccountsAndSettingsDialogFragment_req"
        const val REQUEST_RESULT = "REQUEST_RESULT"

        fun newInstance(dontSwitchAccount: Boolean = false) =
            AccountsAndSettingsDialogFragment().apply {
                arguments = AccountsAndSettingsDialogFragmentArgs(dontSwitchAccount)
                    .toBundle()
            }
    }

    private val args by navArgs<AccountsAndSettingsDialogFragmentArgs>()

    private val viewModel: AccountsViewModel by viewModels()

    @Inject
    lateinit var guestAccountManager: GuestAccountManager

    @Inject
    lateinit var animationsHelper: AnimationsHelper

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

        setBinding(DialogFragmentAccountsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            val adapter = AccountAdapter(
                context,
                isSimple = false,
                showGuestAccount = true,
                guestAccountManager = guestAccountManager,
                signOut = {
                    viewModel.signOut(it.account)
                },
                onAccountClick = {
                    if (args.dontSwitchAccount) {
                        setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(
                                REQUEST_RESULT to it as? Account,
                            ),
                        )
                        dismiss()
                    } else {
                        viewModel.switchAccount(it)
                    }
                },
                onAddAccountClick = {
                    val direction = CommunityDirections.actionGlobalLogin()
                    findNavController().navigateSafe(direction)
                    dismiss()
                },
                onSettingClick = {
                    requireMainActivity().openAccountSettings()
                    dismiss()
                },
                onPersonClick = {
                    requireMainActivity().launchPage(
                        it.account.toPersonRef(),
                    )
                    dismiss()
                },
            )

            recyclerView.setHasFixedSize(false)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            recyclerView.setup(animationsHelper)

            settings.setOnClickListener {
                requireMainActivity().openSettings()
                dismiss()
            }

            viewModel.accounts.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {}
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()

                        binding.recyclerView.scrollToPosition(0)
                        adapter.setAccounts(it.data)
                    }
                }
            }
            viewModel.signOut.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {}
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.refreshAccounts()
    }
}
