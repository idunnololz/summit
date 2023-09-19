package com.idunnololz.summit.accountUi

import android.content.Context
import android.net.Uri
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.idunnololz.summit.CommunityDirections
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.databinding.AccountItemBinding
import com.idunnololz.summit.databinding.AddAccountItemBinding
import com.idunnololz.summit.databinding.CurrentAccountItemBinding
import com.idunnololz.summit.databinding.DialogFragmentAccountsBinding
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint

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

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            val window = checkNotNull(dialog.window)
            window.setLayout(width, height)
        }
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
                signOut = {
                    viewModel.signOut(it.account)
                },
                onAccountClick = {
                    if (args.dontSwitchAccount) {
                        setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(
                                REQUEST_RESULT to it.account
                            )
                        )
                        dismiss()
                    } else {
                        viewModel.switchAccount(it.account)
                    }
                },
                onAddAccountClick = {
                    val direction = CommunityDirections.actionGlobalLogin()
                    findNavController().navigateSafe(direction)
                },
                onSettingClick = {
                    requireMainActivity().openAccountSettings()
                },
                onPersonClick = {
                    requireMainActivity().launchPage(
                        PersonRef.PersonRefByName(it.account.name, it.account.instance),
                    )
                },
            )

            recyclerView.setHasFixedSize(false)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            settings.setOnClickListener {
                requireMainActivity().openSettings()
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
