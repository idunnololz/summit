package com.idunnololz.summit.settings.post_and_comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingPostAndCommentsBinding
import com.idunnololz.summit.databinding.FragmentSettingThemeBinding
import com.idunnololz.summit.util.BaseFragment

class SettingPostAndCommentsFragment : BaseFragment<FragmentSettingPostAndCommentsBinding>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingPostAndCommentsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}