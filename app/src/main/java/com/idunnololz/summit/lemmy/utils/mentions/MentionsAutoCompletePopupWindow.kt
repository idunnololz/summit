package com.idunnololz.summit.lemmy.utils.mentions

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.databinding.MentionsPopupBinding
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.ext.getDrawableCompat

class MentionsAutoCompletePopupWindow(
    context: Context,
    adapterFactory: MentionsResultAdapter.Factory,
    val onItemSelected: (String) -> Unit,
) : PopupWindow(context) {

    val adapter = adapterFactory.create()
    val binding = MentionsPopupBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binding.apply {
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
                    val link = LinkUtils.getLinkForCommunity(it.communityView.community.toCommunityRef())
                    val text = "${it.mentionPrefix}${it.communityView.community.fullName}"

                    onItemSelected("[$text]($link)")
                }
                is PersonResultItem -> {
                    val link = LinkUtils.getLinkForPerson(it.personView.person.toPersonRef())
                    val text = "${it.mentionPrefix}${it.personView.person.fullName}"

                    onItemSelected("[$text]($link)")
                }
            }
        }

        setBackgroundDrawable(context.getDrawableCompat(R.drawable.mentions_autocomplete_popup_bg))
    }
}
