package com.idunnololz.summit.actions.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.TabbedFragmentActionsBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActionsTabbedFragment :
    BaseFragment<TabbedFragmentActionsBinding>() {

    val viewModel: ActionsViewModel by viewModels()

    private val deleteCompletedActionsDialogLauncher = newAlertDialogLauncher(
        "delete_completed_actions",
    ) {
        if (it.isOk) {
            viewModel.deleteCompletedActions()
        }
    }
    private val deletePendingActionsDialogLauncher = newAlertDialogLauncher(
        "delete_pending_actions",
    ) {
        if (it.isOk) {
            viewModel.deletePendingActions()
        }
    }
    private val deleteFailedActionsDialogLauncher = newAlertDialogLauncher(
        "delete_failed_actions",
    ) {
        if (it.isOk) {
            viewModel.deleteFailedActions()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(TabbedFragmentActionsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireSummitActivity().apply {
            setupForFragment<ActionsTabbedFragment>()

            setupToolbar(binding.toolbar, getString(R.string.user_actions))

            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
        }

        val actions = mutableListOf(
            ActionsFragment.ActionType.Failed,
            ActionsFragment.ActionType.Completed,
            ActionsFragment.ActionType.Pending,
        )

        with(binding) {
            viewModel.actionsDataLiveData.observe(viewLifecycleOwner) {
            }

            if (viewPager.adapter == null) {
                viewPager.offscreenPageLimit = 5
                val adapter =
                    ViewPagerAdapter(
                        context = context,
                        fragmentManager = childFragmentManager,
                        lifecycle = viewLifecycleOwner.lifecycle,
                    )

                actions.forEach { action ->
                    adapter.addFrag(
                        clazz = ActionsFragment::class.java,
                        args = ActionsFragmentArgs(action).toBundle(),
                        title = when (action) {
                            ActionsFragment.ActionType.Completed ->
                                getString(R.string.completed_actions)
                            ActionsFragment.ActionType.Pending ->
                                getString(R.string.pending_actions)
                            ActionsFragment.ActionType.Failed ->
                                getString(R.string.failed_actions)
                        },
                    )
                }
                viewPager.adapter = adapter
            }

            TabLayoutMediator(
                tabLayout,
                binding.viewPager,
                binding.viewPager.adapter as ViewPagerAdapter,
            ).attachWithAutoDetachUsingLifecycle(viewLifecycleOwner)

            fab.setOnClickListener {
                val action = actions.getOrNull(viewPager.currentItem)

                when (action) {
                    ActionsFragment.ActionType.Completed -> {
                        deleteCompletedActionsDialogLauncher.launchDialog {
                            messageResId = R.string.delete_completed_actions_history
                            positionButtonResId = android.R.string.ok
                            negativeButtonResId = R.string.cancel
                        }
                    }

                    ActionsFragment.ActionType.Pending -> {
                        deletePendingActionsDialogLauncher.launchDialog {
                            messageResId = R.string.clear_pending_actions
                            positionButtonResId = android.R.string.ok
                            negativeButtonResId = R.string.cancel
                        }
                    }

                    ActionsFragment.ActionType.Failed -> {
                        deleteFailedActionsDialogLauncher.launchDialog {
                            messageResId = R.string.delete_failed_actions_history
                            positionButtonResId = android.R.string.ok
                            negativeButtonResId = R.string.cancel
                        }
                    }
                    null -> {}
                }
            }
        }
    }

    fun openActionDetails(action: Action) {
        val direction = ActionsTabbedFragmentDirections
            .actionActionsTabbedFragmentToActionDetailsFragment(action)
        findNavController().navigateSafe(direction)
    }
}
