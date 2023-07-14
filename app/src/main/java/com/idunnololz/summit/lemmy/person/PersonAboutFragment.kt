package com.idunnololz.summit.lemmy.person

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.FragmentPersonAboutBinding
import com.idunnololz.summit.databinding.FragmentPersonPostsBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.appendLink

class PersonAboutFragment : BaseFragment<FragmentPersonAboutBinding>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentPersonAboutBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentFragment = parentFragment as PersonTabbedFragment

        with(binding) {
            parentFragment.viewModel.personData.observe(viewLifecycleOwner) {
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

                        setup(it.data)
                    }
                }
            }
        }
    }

    private fun setup(data: PersonTabbedViewModel.PersonDetailsData) {
        binding.text.text = buildSpannedString {
            appendLine("Bio")
            appendLine(data.personView.person.bio ?: "No bio.")
            appendLine()

            append(PrettyPrintUtils.defaultDecimalFormat.format(data.personView.counts.post_score))
            appendLine(" post score")
            appendLine()

            append(PrettyPrintUtils.defaultDecimalFormat.format(data.personView.counts.comment_count))
            appendLine(" comment score")
            appendLine()

            appendLine("Is bot: ${data.personView.person.bot_account}")
            appendLine()

            if (data.moderates.isNotEmpty()) {
                append("Moderates")
                appendLine()
                for (m in data.moderates) {
                    appendLink(
                        m.community.name,
                        LinkUtils.getLinkForCommunity(CommunityRef.CommunityRefByName(m.community.name, m.community.instance)))
                    appendLine()
                }
                appendLine()
            }
        }
    }
}