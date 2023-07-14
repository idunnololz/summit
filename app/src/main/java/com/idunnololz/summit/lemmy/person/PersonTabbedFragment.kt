package com.idunnololz.summit.lemmy.person

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentPersonBinding
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.community.ViewPagerController
import com.idunnololz.summit.lemmy.inbox.InboxFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.PrettyPrintUtils.defaultDecimalFormat
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimenFromAttribute
import dagger.hilt.android.AndroidEntryPoint
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.Calendar

@AndroidEntryPoint
class PersonTabbedFragment : BaseFragment<FragmentPersonBinding>() {

    private val args by navArgs<PersonTabbedFragmentArgs>()

    val viewModel: PersonTabbedViewModel by viewModels()
    var viewPagerController: ViewPagerController? = null

    private var isAnimatingTitleIn: Boolean = false
    private var isAnimatingTitleOut: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentPersonBinding.inflate(inflater, container, false))

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
            supportActionBar?.title = ""

            insetViewAutomaticallyByPaddingAndNavUi(viewLifecycleOwner, binding.coordinatorLayout)
        }

        viewModel.fetchPersonIfNotDone(args.personRef)

        with(binding) {
            viewModel.personData.observe(viewLifecycleOwner) {
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

                        setup()
                    }
                }
            }

            viewPagerController = ViewPagerController(
                this@PersonTabbedFragment,
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

            binding.title.alpha = 0f
            isAnimatingTitleIn = false
            isAnimatingTitleOut = false

            binding.cakeDate.visibility = View.GONE

            val actionBarHeight = context.getDimenFromAttribute(androidx.appcompat.R.attr.actionBarSize)
            binding.appBar.addOnOffsetChangedListener { _, verticalOffset ->
                if (!isBindingAvailable()) {
                    return@addOnOffsetChangedListener
                }

                val percentCollapsed =
                    -verticalOffset / binding.collapsingToolbarLayout.height.toDouble()
                val absPixelsShowing = binding.collapsingToolbarLayout.height + verticalOffset

                if (absPixelsShowing <= actionBarHeight) {
                    if (!isAnimatingTitleIn) {
                        isAnimatingTitleIn = true
                        isAnimatingTitleOut = false
                        binding.title.animate().alpha(1f)
                    }
                } else if (percentCollapsed < 0.66) {
                    if (!isAnimatingTitleOut) {
                        isAnimatingTitleOut = true
                        isAnimatingTitleIn = false
                        binding.title.animate().alpha(0f)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (binding.viewPager.currentItem == 0) {
            getMainActivity()?.setNavUiOpenness(0f)
        }
    }

    private fun setup() {
        if (!isBindingAvailable()) return

        val data = viewModel.personData.valueOrNull ?: return
        val context = requireContext()

        with(binding) {
            val displayName = data.personView.person.display_name
                ?: data.personView.person.name

            binding.title.text = displayName

            profileIcon.load(data.personView.person.avatar) {
                fallback(R.drawable.thumbnail_placeholder)
                allowHardware(false)
            }
            name.text = displayName
            subtitle.text = buildSpannedString {
                append("@")
                append(data.personView.person.name)

                appendSeparator()

                append(context.getString(R.string.posts_format,
                    defaultDecimalFormat.format(data.personView.counts.post_count)))

                appendSeparator()

                append(context.getString(R.string.comments_format,
                    defaultDecimalFormat.format(data.personView.counts.comment_count)))
            }

            val dateTime = LocalDateTime.ofEpochSecond(
                dateStringToTs(data.personView.person.published) / 1000,
                0,
                ZoneOffset.UTC)
            val dateStr = dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            cakeDate.text = getString(R.string.cake_day_on_format, dateStr)
            binding.cakeDate.visibility = View.VISIBLE

            viewPager.offscreenPageLimit = 5
            val adapter = ViewPagerAdapter(context, childFragmentManager, viewLifecycleOwner.lifecycle)
            adapter.addFrag(PersonPostsFragment::class.java, getString(R.string.posts))
            adapter.addFrag(PersonCommentsFragment::class.java, getString(R.string.comments))
            adapter.addFrag(PersonAboutFragment::class.java, getString(R.string.about))
            viewPager.adapter = adapter

            TabLayoutMediator(
                tabLayout,
                binding.viewPager,
                binding.viewPager.adapter as ViewPagerAdapter,
            ).attachWithAutoDetachUsingLifecycle(viewLifecycleOwner)
        }
    }
}