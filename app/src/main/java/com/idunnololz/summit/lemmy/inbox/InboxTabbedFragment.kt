package com.idunnololz.summit.lemmy.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import coil.load
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.TabbedFragmentInboxBinding
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InboxTabbedFragment : BaseFragment<TabbedFragmentInboxBinding>() {

    val viewModel: InboxViewModel by viewModels()

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(TabbedFragmentInboxBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {

            setupForFragment<PersonTabbedFragment>()

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.inbox)

            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.appBar)
        }

        viewModel.fetchInboxIfNeeded()

        viewModel.currentAccountView?.let {
            binding.accountImageView.load(it.profileImage)
        }

        binding.viewPager.offscreenPageLimit = 99
        if (binding.viewPager.adapter == null) {
            val pagerAdapter = ViewPagerAdapter(
                context,
                childFragmentManager,
                viewLifecycleOwner.lifecycle,
            ).also { adapter ->
                listOf(
                    InboxViewModel.PageType.All,
                    InboxViewModel.PageType.Replies,
                    InboxViewModel.PageType.Mentions,
                    InboxViewModel.PageType.Messages,
                ).forEach {
                    adapter.addFrag(
                        InboxFragment::class.java,
                        it.getName(context),
                        InboxFragmentArgs(it).toBundle(),
                    )
                }
            }
            binding.viewPager.adapter = pagerAdapter

            TabLayoutMediator(binding.tabLayout, binding.viewPager, pagerAdapter)
                .attachWithAutoDetachUsingLifecycle(viewLifecycleOwner)
        }
    }
}