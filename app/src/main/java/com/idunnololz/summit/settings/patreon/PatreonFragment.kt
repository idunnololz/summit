package com.idunnololz.summit.settings.patreon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentPatreonBinding
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import kotlinx.coroutines.launch

class PatreonFragment : BaseFragment<FragmentPatreonBinding>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentPatreonBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireSummitActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
        }

        with(binding) {
            viewLifecycleOwner.lifecycleScope.launch {
                names.text = context.resources.openRawResource(R.raw.patreon)
                    .bufferedReader().use { it.readText() }
            }
            link.setOnClickListener {
                onLinkClick(
                    url = "https://www.patreon.com/SummitforLemmy",
                    text = null,
                    linkContext = LinkContext.Action
                )
            }
        }
    }
}
