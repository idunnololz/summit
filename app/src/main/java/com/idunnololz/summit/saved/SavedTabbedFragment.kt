package com.idunnololz.summit.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.account_ui.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.account_ui.SignInNavigator
import com.idunnololz.summit.databinding.TabbedFragmentSavedBinding
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.community.ViewPagerController
import com.idunnololz.summit.lemmy.person.PersonAboutFragment
import com.idunnololz.summit.lemmy.person.PersonCommentsFragment
import com.idunnololz.summit.lemmy.person.PersonPostsFragment
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.lemmy.post.PostFragmentDirections
import com.idunnololz.summit.lemmy.utils.installOnActionResultHandler
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SavedTabbedFragment : BaseFragment<TabbedFragmentSavedBinding>(), SignInNavigator {

    val viewModel: SavedViewModel by viewModels()
    val actionsViewModel: MoreActionsViewModel by viewModels()
    var viewPagerController: ViewPagerController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(TabbedFragmentSavedBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SavedTabbedFragment>()

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.saved)

//            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
//            insetViewExceptTopAutomaticallyByPaddingAndNavUi(viewLifecycleOwner, binding.viewPager)
            insetViewAutomaticallyByPaddingAndNavUi(viewLifecycleOwner, binding.coordinatorLayoutContainer)
        }

        with(binding) {
            fab.visibility = View.GONE

            viewModel.currentAccountView.observe(viewLifecycleOwner) {
                accountImageView.load(it?.profileImage)
            }
            accountImageView.setOnClickListener {
                AccountsAndSettingsDialogFragment.newInstance()
                    .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
            }

            if (viewPager.adapter == null) {
                viewPager.offscreenPageLimit = 5
                val adapter =
                    ViewPagerAdapter(context, childFragmentManager, viewLifecycleOwner.lifecycle)
                adapter.addFrag(SavedPostsFragment::class.java, getString(R.string.posts))
                adapter.addFrag(SavedCommentsFragment::class.java, getString(R.string.comments))
                viewPager.adapter = adapter
            }

            TabLayoutMediator(
                tabLayout,
                binding.viewPager,
                binding.viewPager.adapter as ViewPagerAdapter,
            ).attachWithAutoDetachUsingLifecycle(viewLifecycleOwner)

            viewPagerController = ViewPagerController(
                this@SavedTabbedFragment,
                topViewPager,
                childFragmentManager,
                viewModel,
                true,
            ) {
                if (it == 0) {
                    val lastSelectedPost = viewModel.lastSelectedPost
                    if (lastSelectedPost != null) {
                        // We came from a post...
//                        adapter?.highlightPost(lastSelectedPost)
                        viewModel.lastSelectedPost = null
                    }
                } else {
                    val lastSelectedPost = viewModel.lastSelectedPost
                    if (lastSelectedPost != null) {
//                        adapter?.highlightPostForever(lastSelectedPost)
                    }
                }
            }.apply {
                init()
            }
            topViewPager.disableLeftSwipe = true

            installOnActionResultHandler(
                actionsViewModel = actionsViewModel,
                snackbarContainer = binding.coordinatorLayout,
                onSavePostChanged = {
                    viewModel.onSavePostChanged(it)
                },
                onSaveCommentChanged = {
                    viewModel.onSaveCommentChanged(it)
                }
            )
        }
    }

    override fun navigateToSignInScreen() {
        val direction = PostFragmentDirections.actionGlobalLogin()
        findNavController().navigateSafe(direction)
    }

    override fun proceedAnyways(tag: Int) {
    }
}