package com.idunnololz.summit.settings.accounts.perAccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingPerAccountBinding
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingPerAccountFragment : BaseFragment<FragmentSettingPerAccountBinding>() {

    private val args by navArgs<SettingPerAccountFragmentArgs>()

    private val viewModel: SettingPerAccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingPerAccountBinding.inflate(inflater, container, false))

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
            supportActionBar?.title = context.getString(R.string.account_settings)
        }

        with(binding) {
            viewModel.account.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        if (it.error is SettingPerAccountViewModel.NoAccountError) {
                            findNavController().navigateUp()
                        }
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                        getMainActivity()?.supportActionBar?.title = it.data.name
                    }
                }
            }

            viewModel.loadAccount(args.accountId)
        }
    }
}
