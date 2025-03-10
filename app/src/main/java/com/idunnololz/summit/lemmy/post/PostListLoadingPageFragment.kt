package com.idunnololz.summit.lemmy.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentCommunityBinding
import com.idunnololz.summit.databinding.FragmentPostListLoadingPageBinding
import com.idunnololz.summit.util.BaseFragment

class PostListLoadingPageFragment : BaseFragment<FragmentPostListLoadingPageBinding>() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentPostListLoadingPageBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loadingView.apply {
            showProgressBarWithMessage(
                context.getString(R.string.loading_more_posts)
            )
        }
    }
}