package com.idunnololz.summit.lemmy.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSearchBinding
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.community.ViewPagerController
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.lemmy.utils.installOnActionResultHandler
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.saved.SavedCommentsFragment
import com.idunnololz.summit.saved.SavedPostsFragment
import com.idunnololz.summit.saved.SavedTabbedFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchTabbedFragment : BaseFragment<FragmentSearchBinding>() {

    val viewModel: SearchViewModel by viewModels()
    val actionsViewModel: MoreActionsViewModel by viewModels()
    var viewPagerController: ViewPagerController? = null

    @Inject
    lateinit var preferences: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSearchBinding.inflate(inflater, container, false))

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
            supportActionBar?.title = getString(R.string.search)

            insetViewAutomaticallyByPaddingAndNavUi(viewLifecycleOwner, binding.coordinatorLayoutContainer)
        }

        with(binding) {
//            if (viewPager.adapter == null) {
//                viewPager.offscreenPageLimit = 5
//                val adapter =
//                    ViewPagerAdapter(context, childFragmentManager, viewLifecycleOwner.lifecycle)
//                adapter.addFrag(SavedPostsFragment::class.java, getString(R.string.posts))
//                adapter.addFrag(SavedCommentsFragment::class.java, getString(R.string.comments))
//                viewPager.adapter = adapter
//            }
//
//            TabLayoutMediator(
//                tabLayout,
//                binding.viewPager,
//                binding.viewPager.adapter as ViewPagerAdapter,
//            ).attachWithAutoDetachUsingLifecycle(viewLifecycleOwner)


            viewPagerController = ViewPagerController(
                this@SearchTabbedFragment,
                topViewPager,
                childFragmentManager,
                viewModel,
                true,
                compatibilityMode = preferences.compatibilityMode,
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
            )
        }
    }
}