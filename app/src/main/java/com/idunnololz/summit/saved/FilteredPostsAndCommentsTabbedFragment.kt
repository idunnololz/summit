package com.idunnololz.summit.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.account.loadProfileImageOrDefault
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.databinding.TabbedFragmentSavedBinding
import com.idunnololz.summit.lemmy.community.SlidingPaneController
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.post.PostFragmentDirections
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.actions.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Displays a list of posts and comments with some filtering criteria
 */
@AndroidEntryPoint
class FilteredPostsAndCommentsTabbedFragment :
    BaseFragment<TabbedFragmentSavedBinding>(), SignInNavigator {

    private val args: FilteredPostsAndCommentsTabbedFragmentArgs by navArgs()

    val viewModel: FilteredPostAndCommentsViewModel by viewModels()
    var slidingPaneController: SlidingPaneController? = null

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var preferences: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(TabbedFragmentSavedBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<FilteredPostsAndCommentsTabbedFragment>()

            setupToolbar(
                toolbar = binding.toolbar,
                title = when (args.type) {
                    FilteredPostAndCommentsType.Saved -> getString(R.string.saved)
                    FilteredPostAndCommentsType.Upvoted -> getString(R.string.upvoted)
                    FilteredPostAndCommentsType.Downvoted -> getString(R.string.downvoted)
                },
            )

            insetViewAutomaticallyByPaddingAndNavUi(
                viewLifecycleOwner,
                binding.coordinatorLayoutContainer,
            )
        }

        with(binding) {
            fab.visibility = View.GONE
            binding.fab.setup(preferences)

            viewModel.currentAccountView.observe(viewLifecycleOwner) {
                it.loadProfileImageOrDefault(binding.accountImageView)
            }
            accountImageView.setOnClickListener {
                AccountsAndSettingsDialogFragment.newInstance()
                    .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
            }

            if (viewPager.adapter == null) {
                viewPager.offscreenPageLimit = 5
                val adapter =
                    ViewPagerAdapter(context, childFragmentManager, viewLifecycleOwner.lifecycle)
                adapter.addFrag(FilteredPostsFragment::class.java, getString(R.string.posts))
                adapter.addFrag(FilteredCommentsFragment::class.java, getString(R.string.comments))
                viewPager.adapter = adapter
            }

            TabLayoutMediator(
                tabLayout,
                binding.viewPager,
                binding.viewPager.adapter as ViewPagerAdapter,
            ).attachWithAutoDetachUsingLifecycle(viewLifecycleOwner)

            slidingPaneController = SlidingPaneController(
                fragment = this@FilteredPostsAndCommentsTabbedFragment,
                slidingPaneLayout = slidingPaneLayout,
                childFragmentManager = childFragmentManager,
                viewModel = viewModel,
                globalLayoutMode = preferences.globalLayoutMode,
                lockPanes = true,
                retainClosedPosts = preferences.retainLastPost,
                emptyScreenText = getString(R.string.select_a_post_or_comment),
                fragmentContainerId = R.id.post_fragment_container,
            ).apply {
                onPageSelectedListener = a@{ isOpen ->
                    if (!isBindingAvailable()) {
                        return@a
                    }

                    if (!isOpen) {
                        val lastSelectedPost = viewModel.lastSelectedItem
                        if (lastSelectedPost != null) {
                            // We came from a post...
//                        adapter?.highlightPost(lastSelectedPost)
                            viewModel.lastSelectedItem = null
                        }
                    } else {
                        val lastSelectedPost = viewModel.lastSelectedItem
                        if (lastSelectedPost != null) {
//                        adapter?.highlightPostForever(lastSelectedPost)
                        }
                    }
                }
                init()
            }

            installOnActionResultHandler(
                context = context,
                moreActionsHelper = moreActionsHelper,
                snackbarContainer = binding.coordinatorLayout,
                onSavePostChanged = {
                    viewModel.onSavePostChanged(it)
                },
                onSaveCommentChanged = {
                    viewModel.onSaveCommentChanged(it)
                },
            )
        }

        viewModel.initializeIfNeeded(args.type)
    }

    override fun navigateToSignInScreen() {
        val direction = PostFragmentDirections.actionGlobalLogin()
        findNavController().navigateSafe(direction)
    }

    override fun proceedAnyways(tag: Int) {
    }

    fun closePost(postFragment: PostFragment) {
        slidingPaneController?.closePost(postFragment)
    }
}
