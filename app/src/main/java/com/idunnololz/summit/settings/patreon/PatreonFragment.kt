package com.idunnololz.summit.settings.patreon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.databinding.FragmentPatreonBinding
import com.idunnololz.summit.links.LinkType
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils

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

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
        }

        binding.link.setOnClickListener {
            onLinkClick("https://www.patreon.com/SummitforLemmy", null, LinkType.Action)
        }
    }
}
