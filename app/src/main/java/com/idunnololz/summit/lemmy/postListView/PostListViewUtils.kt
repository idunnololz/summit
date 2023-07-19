package com.idunnololz.summit.lemmy.postListView

import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu

fun BaseFragment<*>.showMorePostOptions(postView: PostView, actionsViewModel: MoreActionsViewModel) {
    if (!isBindingAvailable()) {
        return
    }

    val context = requireContext()

    val bottomMenu = BottomMenu(context).apply {
        setTitle(R.string.more_actions)

        if (postView.post.creator_id == actionsViewModel.accountManager.currentAccount.value?.id) {
            addItemWithIcon(R.id.edit_post, R.string.edit_post, R.drawable.baseline_edit_24)
            addItemWithIcon(R.id.delete, R.string.delete_post, R.drawable.baseline_delete_24)
        }

        addItemWithIcon(
            R.id.hide_post,
            getString(R.string.hide_post),
            R.drawable.baseline_hide_24
        )

        if (postView.saved) {
            addItemWithIcon(
                R.id.remove_from_saved,
                getString(R.string.remove_from_saved),
                R.drawable.baseline_bookmark_remove_24
            )
        } else {
            addItemWithIcon(
                R.id.save,
                getString(R.string.save),
                R.drawable.baseline_bookmark_add_24
            )
        }

        addItemWithIcon(
            R.id.block_community,
            getString(R.string.block_this_community_format, postView.community.name),
            R.drawable.baseline_block_24
        )
        addItemWithIcon(
            R.id.block_user,
            getString(R.string.block_this_user_format, postView.creator.name),
            R.drawable.baseline_person_off_24
        )

        setOnMenuItemClickListener {
            when (it.id) {
                R.id.hide_post -> {
                    actionsViewModel.hidePost(postView.post.id)
                }
                R.id.save -> {
                    actionsViewModel.savePost(postView.post.id, save = true)
                }
                R.id.remove_from_saved -> {
                    actionsViewModel.savePost(postView.post.id, save = false)
                }
                R.id.block_community -> {
                    actionsViewModel.blockCommunity(postView.community.id)
                }
                R.id.block_user -> {
                    actionsViewModel.blockPerson(postView.creator.id)
                }
            }
        }
    }

    getMainActivity()?.showBottomMenu(bottomMenu)
}