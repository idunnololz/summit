package com.idunnololz.summit.lemmy.person

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.databinding.FragmentPersonAboutBinding
import com.idunnololz.summit.databinding.FragmentPersonPostsBinding
import com.idunnololz.summit.util.BaseFragment

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
}