package com.idunnololz.summit.settings.webSettings.blockList.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsCommunityBlockListBinding
import com.idunnololz.summit.util.BaseFragment

class SettingsCommunityBlockListFragment : BaseFragment<FragmentSettingsCommunityBlockListBinding>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsCommunityBlockListBinding.inflate(inflater, container, false))

        return binding.root
    }
}