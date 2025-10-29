package com.idunnololz.summit.lemmy.utils.mentions

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.databinding.MentionsPopupBinding
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.ext.setup

class MentionsAutoCompletePopupWindow(
  context: Context,
  adapterFactory: MentionsResultAdapter.Factory,
  animationsHelper: AnimationsHelper,
  val onItemSelected: (String) -> Unit,
  val onItemLongClick: (ResultItem) -> Unit,
) : PopupWindow(context) {

  val adapter = adapterFactory.create()
  val binding = MentionsPopupBinding.inflate(LayoutInflater.from(context))

  init {
    contentView = binding.apply {
      recyclerView.setup(animationsHelper)
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(false)
      recyclerView.adapter = adapter
    }.root

    height = ViewGroup.LayoutParams.WRAP_CONTENT
    width = ViewGroup.LayoutParams.MATCH_PARENT
    isOutsideTouchable = true
    inputMethodMode = INPUT_METHOD_NEEDED

    adapter.onResultSelected = {
      when (it) {
        is CommunityResultItem -> {
          val text = "!${it.communityView.community.fullName}"

          onItemSelected(text)
        }
        is PersonResultItem -> {
          val text = "@${it.personView.person.fullName}"

          onItemSelected(text)
        }
      }
    }
    adapter.onResultLongClick = {
      onItemLongClick(it)
    }

    setBackgroundDrawable(null)

    binding.root.setOnClickListener {
      dismiss()
    }
  }
}
