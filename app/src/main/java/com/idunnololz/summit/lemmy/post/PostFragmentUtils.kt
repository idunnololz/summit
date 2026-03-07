package com.idunnololz.summit.lemmy.post

import androidx.fragment.app.Fragment
import com.idunnololz.summit.history.HistoryFragment
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.lemmy.search.SearchTabbedFragment
import com.idunnololz.summit.saved.FilteredPostsAndCommentsTabbedFragment

fun Fragment.goBack() {
  when (val fragment = requireParentFragment()) {
    is CommunityFragment -> {
      fragment.closePost(this)
    }
    is PersonTabbedFragment -> {
      fragment.closePost(this)
    }
    is FilteredPostsAndCommentsTabbedFragment -> {
      fragment.closePost(this)
    }
    is SearchTabbedFragment -> {
      fragment.closePost(this)
    }
    is HistoryFragment -> {
      fragment.closePost(this)
    }
    is PostTabbedFragment -> {
      fragment.closePost(this)
    }
  }
}