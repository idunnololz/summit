package com.idunnololz.summit.lemmy.search

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.ViewPagerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(

) : ViewModel(), ViewPagerController.PostViewPagerViewModel {

    override var lastSelectedPost: PostRef? = null
    override val viewPagerAdapter = ViewPagerController.ViewPagerAdapter()
}