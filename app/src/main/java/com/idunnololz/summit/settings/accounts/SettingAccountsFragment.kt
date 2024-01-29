package com.idunnololz.summit.settings.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.accountUi.AccountAdapter
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.databinding.FragmentSettingAccountsBinding
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingAccountsFragment : BaseFragment<FragmentSettingAccountsBinding>() {

    private val viewModel: SettingAccountsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingAccountsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = context.getString(R.string.choose_an_account)
        }

        val adapter = AccountAdapter(
            context = context,
            isSimple = true,
            showGuestAccount = false,
            signOut = {},
            onAccountClick = {
                if (it != null) {
                    onAccountClick(it.account.id)
                }
            },
            onAddAccountClick = {},
            onSettingClick = {},
            onPersonClick = {
                onAccountClick(it.account.id)
            },
        )

        with(binding) {
            viewModel.accounts.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                        adapter.setAccounts(it.data) {}
                    }
                }
            }

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
        }
    }

    private fun onAccountClick(accountId: PersonId) {
        val direction = SettingAccountsFragmentDirections
            .actionSettingAccountsFragmentToSettingPerAccountFragment(
                accountId,
            )
        findNavController().navigateSafe(direction)
    }
}
