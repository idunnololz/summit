package com.idunnololz.summit.lemmy.utils.mentions

import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.MentionQueryResultItemBinding
import com.idunnololz.summit.databinding.MentionsEmptyItemBinding
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class MentionsResultAdapter @AssistedInject constructor(
    private val avatarHelper: AvatarHelper,
) : Adapter<ViewHolder>() {

    @AssistedFactory
    interface Factory {
        fun create(): MentionsResultAdapter
    }

    var onResultSelected: ((ResultItem) -> Unit)? = null

    private var items: List<MentionsAutoCompleteItem> = listOf()

    private val adapterHelper = AdapterHelper<MentionsAutoCompleteItem>(
        areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
                EmptyItem -> true
                is CommunityResultItem ->
                    old.communityView.community.id ==
                        (new as CommunityResultItem).communityView.community.id
                is PersonResultItem ->
                    old.personView.person.id ==
                        (new as PersonResultItem).personView.person.id
            }
        },
    ).apply {
        addItemType(
            clazz = CommunityResultItem::class,
            inflateFn = MentionQueryResultItemBinding::inflate,
        ) { item, b, h ->
            avatarHelper.loadCommunityIcon(b.icon, item.communityView.community)

            b.text.text = item.communityView.community.fullName
            val mauString = LemmyUtils.abbrevNumber(
                item.communityView.counts.users_active_month.toLong(),
            )
            @Suppress("SetTextI18n")
            b.subtitle.text = buildSpannedString {
                append(b.root.context.getString(R.string.community))
                appendSeparator()
                append(b.root.context.getString(R.string.mau_format, mauString))
            }

            b.root.setOnClickListener {
                onResultSelected?.invoke(item)
            }
        }
        addItemType(
            clazz = PersonResultItem::class,
            inflateFn = MentionQueryResultItemBinding::inflate,
        ) { item, b, h ->
            avatarHelper.loadAvatar(b.icon, item.personView.person)
            b.text.text = item.personView.person.fullName
            @Suppress("SetTextI18n")
            b.subtitle.text = buildSpannedString {
                append(b.root.context.getString(R.string.person))
                appendSeparator()
                append(item.bio ?: b.root.context.getString(R.string.no_bio))
            }

            b.root.setOnClickListener {
                onResultSelected?.invoke(item)
            }
        }
        addItemType(
            clazz = EmptyItem::class,
            inflateFn = MentionsEmptyItemBinding::inflate,
        ) { item, b, h ->
        }
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        adapterHelper.onBindViewHolder(holder, position)

    fun setItems(items: List<MentionsAutoCompleteItem>, cb: () -> Unit = {}) {
        this.items = items

        refreshItems(cb)
    }

    private fun refreshItems(cb: () -> Unit) {
        adapterHelper.setItems(items, this, cb)
    }
}
