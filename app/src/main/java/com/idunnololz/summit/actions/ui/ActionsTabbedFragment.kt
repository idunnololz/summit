package com.idunnololz.summit.actions.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.TabbedFragmentActionsBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActionsTabbedFragment :
    BaseFragment<TabbedFragmentActionsBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    val viewModel: ActionsViewModel by viewModels()

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

        requireMainActivity().apply {
            setupForFragment<ActionsTabbedFragment>()

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.user_actions)

            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
        }

        val actions = mutableListOf<ActionsFragment.ActionType>(
            ActionsFragment.ActionType.Pending,
            ActionsFragment.ActionType.Completed,
            ActionsFragment.ActionType.Failed,
        )

        with(binding) {
            viewModel.actionsDataLiveData.observe(viewLifecycleOwner) {
            }

            if (viewPager.adapter == null) {
                viewPager.offscreenPageLimit = 5
                val adapter =
                    ViewPagerAdapter(context, childFragmentManager, viewLifecycleOwner.lifecycle)

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
                        AlertDialogFragment.Builder()
                            .setMessage(R.string.delete_completed_actions_history)
                            .setPositiveButton(android.R.string.ok)
                            .setNegativeButton(R.string.cancel)
                            .createAndShow(childFragmentManager, "delete_completed_actions")
                    }

                    ActionsFragment.ActionType.Pending -> {
                        AlertDialogFragment.Builder()
                            .setMessage(R.string.clear_pending_actions)
                            .setPositiveButton(android.R.string.ok)
                            .setNegativeButton(R.string.cancel)
                            .createAndShow(childFragmentManager, "delete_pending_actions")
                    }

                    ActionsFragment.ActionType.Failed -> {
                        AlertDialogFragment.Builder()
                            .setMessage(R.string.delete_failed_actions_history)
                            .setPositiveButton(android.R.string.ok)
                            .setNegativeButton(R.string.cancel)
                            .createAndShow(childFragmentManager, "delete_failed_actions")
                    }
                    null -> {}
                }
            }
        }
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            "delete_completed_actions" -> viewModel.deleteCompletedActions()
            "delete_pending_actions" -> viewModel.deletePendingActions()
            "delete_failed_actions" -> viewModel.deleteFailedActions()
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }
}
