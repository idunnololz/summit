package com.idunnololz.summit.drafts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentDraftsTabbedBinding
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DraftsTabbedFragment :
    BaseFragment<FragmentDraftsTabbedBinding>() {

    companion object {
        const val REQUEST_KEY = "DraftsDialogFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        fun show(fragmentManager: FragmentManager, draftType: Int) {
            DraftsDialogFragment().apply {
                arguments = DraftsDialogFragmentArgs(draftType).toBundle()
            }.showAllowingStateLoss(fragmentManager, "DraftsDialogFragment")
        }
    }

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentDraftsTabbedBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            requireMainActivity().apply {
                insetViewAutomaticallyByPaddingAndNavUi(
                    viewLifecycleOwner,
                    coordinatorLayoutContainer,
                )
            }

            toolbar.title = getString(R.string.drafts)
            toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
            )

            if (viewPager.adapter == null) {
                viewPager.offscreenPageLimit = 5
                val adapter =
                    ViewPagerAdapter(context, childFragmentManager, viewLifecycleOwner.lifecycle)
                adapter.addFrag(
                    DraftsFragment::class.java,
                    getString(R.string.posts),
                    DraftsFragmentArgs(DraftTypes.Post).toBundle(),
                )
                adapter.addFrag(
                    DraftsFragment::class.java,
                    getString(R.string.comments),
                    DraftsFragmentArgs(DraftTypes.Comment).toBundle(),
                )
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)

                        val myFragment = childFragmentManager.findFragmentByTag("f$position")
                        (myFragment as? DraftsFragment)?.onSelected()
                    }
                })
                viewPager.adapter = adapter
            }

            TabLayoutMediator(
                tabLayout,
                binding.viewPager,
                binding.viewPager.adapter as ViewPagerAdapter,
            ).attachWithAutoDetachUsingLifecycle(viewLifecycleOwner)
        }
    }
}
