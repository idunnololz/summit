package com.idunnololz.summit.settings.webSettings.blockList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsAccountBlockListBinding
import com.idunnololz.summit.settings.BasicSettingItem
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByMargins
import com.idunnololz.summit.util.setupForFragment

class SettingsAccountBlockListFragment : BaseFragment<FragmentSettingsAccountBlockListBinding>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsAccountBlockListBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireSummitActivity().apply {
            insetViewExceptTopAutomaticallyByMargins(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = context.getString(R.string.account_block_settings)
        }

        with(binding) {
            BasicSettingItem(
                R.drawable.baseline_person_24,
                getString(R.string.blocked_users),
                null,
            ).bindTo(
                blockedUsers,
                onValueChanged = {
                    val directions = SettingsAccountBlockListFragmentDirections
                        .actionSettingsAccountBlockListFragmentToSettingsUserBlockListFragment()
                    findNavController().navigateSafe(directions)
                },
            )

            BasicSettingItem(
                R.drawable.ic_default_community,
                getString(R.string.blocked_communities),
                null,
            ).bindTo(
                blockedCommunities,
                onValueChanged = {
                    val directions = SettingsAccountBlockListFragmentDirections
                        .actionSettingsAccountBlockListFragmentToSettingsCommunityBlockListFragment()
                    findNavController().navigateSafe(directions)
                },
            )

            BasicSettingItem(
                R.drawable.ic_lemmy_24,
                getString(R.string.blocked_instances),
                null,
            ).bindTo(
                blockedInstances,
                onValueChanged = {
                    val directions = SettingsAccountBlockListFragmentDirections
                        .actionSettingsAccountBlockListFragmentToSettingsInstanceBlockListFragment()
                    findNavController().navigateSafe(directions)
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()

        setupForFragment<SettingsFragment>()
    }
}
