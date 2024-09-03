package com.idunnololz.summit.lemmy.fastAccountSwitcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.GuestAccountManager
import com.idunnololz.summit.accountUi.AccountAdapter
import com.idunnololz.summit.databinding.DialogFragmentFastAccountSwitchBinding
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FastAccountSwitcherDialogFragment :
    BaseBottomSheetDialogFragment<DialogFragmentFastAccountSwitchBinding>(),
    FullscreenDialogFragment {

    companion object {
        const val REQUEST_KEY = "FastAccountSwitcherDialogFragment_req"
        const val RESULT_ACCOUNT = "RESULT_ACCOUNT"

        fun show(fragmentManager: FragmentManager) {
            FastAccountSwitcherDialogFragment()
                .showAllowingStateLoss(fragmentManager, "FastAccountSwitcherDialogFragment")
        }
    }

    private val viewModel: FastAccountSwitcherViewModel by viewModels()

    private var adapter: AccountAdapter? = null

    @Inject
    lateinit var guestAccountManager: GuestAccountManager

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentFastAccountSwitchBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            adapter = AccountAdapter(
                context,
                isSimple = true,
                showGuestAccount = false,
                guestAccountManager = guestAccountManager,
                signOut = {},
                onAccountClick = {
                    setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACCOUNT to it as? Account))
                    dismiss()
                },
                onAddAccountClick = {},
                onSettingClick = {},
                onPersonClick = {},
            )

            recyclerView.setHasFixedSize(true)
            recyclerView.setup(animationsHelper)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

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

                        setupUi(it.data)
                    }
                }
            }
        }

        viewModel.refreshAccounts()
    }

    private fun setupUi(data: List<AccountView>) {
        if (!isBindingAvailable()) return

        adapter?.setAccounts(data) {
            binding.recyclerView.requestLayout()
        }
    }
}
