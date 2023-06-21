package com.idunnololz.summit.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSavedBinding
import com.idunnololz.summit.settings.AccountSettingsFragment
import com.idunnololz.summit.util.BaseFragment

class SavedFragment : BaseFragment<FragmentSavedBinding>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSavedBinding.inflate(inflater, container, false))

        requireMainActivity().apply {
            setupForFragment<SavedFragment>()
        }

        return binding.root
    }


}