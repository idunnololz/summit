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
import com.idunnololz.summit.lemmy.inbox.InboxFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.PrettyPrintUtils.defaultDecimalFormat
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.Calendar

@AndroidEntryPoint
class PersonTabbedFragment : BaseFragment<FragmentPersonBinding>() {

    private val args by navArgs<PersonTabbedFragmentArgs>()

    private val viewModel: PersonTabbedViewModel by viewModels()

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

            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.appBar)
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
        }
    }

    private fun setup() {
        if (!isBindingAvailable()) return

        val data = viewModel.personData.valueOrNull ?: return
        val context = requireContext()

        requireMainActivity().apply {
            supportActionBar?.title = data.personView.person.display_name
        }

        with(binding) {
            profileIcon.load(data.personView.person.avatar)
            name.text = data.personView.person.display_name
                ?: data.personView.person.name
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