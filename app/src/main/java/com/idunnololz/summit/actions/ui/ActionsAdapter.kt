package com.idunnololz.summit.actions.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.databinding.ActionsItemEmptyBinding
import com.idunnololz.summit.databinding.PendingActionItemBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.toText
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.tsToConcise
import com.idunnololz.summit.video.VideoState

class ActionsAdapter(
  private val context: Context,
  private val lemmyTextHelper: LemmyTextHelper,
  private val onImageClick: (String, View?, String) -> Unit,
  private val onVideoClick: (String, VideoType, VideoState?) -> Unit,
  private val onPageClick: (PageRef) -> Unit,
  private val onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
  private val onLinkLongClick: (url: String, text: String) -> Unit,
  private val onActionClick: (Action) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private sealed interface Item {
    data class ActionItem(
      val action: Action,
    ) : Item

    data object EmptyItem : Item
  }

  var accountDictionary: Map<Long, Account?> = mapOf()
  var actions: List<Action> = listOf()
    set(value) {
      field = value

      refreshItems()
    }

  private val adapterHelper = AdapterHelper<Item>(
    areItemsTheSame = { old, new ->
      old::class == new::class &&
        when (old) {
          is Item.ActionItem ->
            old.action.id == (new as Item.ActionItem).action.id

          Item.EmptyItem -> true
        }
    },
  ).apply {
    addItemType(Item.ActionItem::class, PendingActionItemBinding::inflate) { item, b, h ->
      val actionInfo = item.action.info

      val accountId: Long? = actionInfo?.accountId
      val account = accountDictionary[accountId]
      val actionDate: Long = item.action.ts
      val actionDesc: String

      b.title.text = actionInfo?.getActionName(context)
        ?: context.getString(R.string.unknown)

      if (item.action.seen) {
        b.title.setCompoundDrawablesWithIntrinsicBounds(
          0,
          0,
          0,
          0,
        )
      } else {
        b.title.setCompoundDrawablesWithIntrinsicBounds(
          0,
          0,
          R.drawable.baseline_indicator_12,
          0,
        )
      }

      when (actionInfo) {
        is ActionInfo.CommentActionInfo -> {
          b.icon.setImageResource(R.drawable.outline_comment_24)
          actionDesc = buildString {
            append("Commented on post ")
            append("[${actionInfo.postRef.id}@${actionInfo.postRef.instance}]")
            append("(")
            append(
              LinkUtils.postIdToLink(
                instance = actionInfo.postRef.instance,
                postId = actionInfo.postRef.id,
              ),
            )
            append(")")
            append(".")
          }
        }
        is ActionInfo.DeleteCommentActionInfo -> {
          b.icon.setImageResource(R.drawable.outline_delete_24)
          actionDesc = buildString {
            append("Deleted comment on post ")
            append("[${actionInfo.postRef.id}@${actionInfo.postRef.instance}]")
            append("(")
            append(
              LinkUtils.postIdToLink(
                instance = actionInfo.postRef.instance,
                postId = actionInfo.postRef.id,
              ),
            )
            append(")")
            append(".")
          }
        }
        is ActionInfo.EditCommentActionInfo -> {
          b.icon.setImageResource(R.drawable.baseline_edit_24)
          actionDesc = buildString {
            append("Edited comment on post ")
            append("[${actionInfo.postRef.id}@${actionInfo.postRef.instance}]")
            append("(")
            append(
              LinkUtils.postIdToLink(
                instance = actionInfo.postRef.instance,
                postId = actionInfo.postRef.id,
              ),
            )
            append(")")
            append(".")
          }
        }
        is ActionInfo.MarkPostAsReadActionInfo -> {
          if (actionInfo.read) {
            b.icon.setImageResource(R.drawable.baseline_check_24)
          } else {
            b.icon.setImageResource(R.drawable.outline_thread_unread_24)
          }
          actionDesc = buildString {
            append("Marked a post as read (")
            append("[${actionInfo.postRef.id}@${actionInfo.postRef.instance}]")
            append("(")
            append(
              LinkUtils.postIdToLink(
                instance = actionInfo.postRef.instance,
                postId = actionInfo.postRef.id,
              ),
            )
            append(")")
            append(").")
          }
        }
        is ActionInfo.VoteActionInfo -> {
          actionDesc = buildString {
            if (actionInfo.dir > 0) {
              b.icon.setImageResource(R.drawable.baseline_arrow_upward_24)
              append("Upvoted")
            } else if (actionInfo.dir < 0) {
              b.icon.setImageResource(R.drawable.baseline_arrow_downward_24)
              append("Downvoted")
            } else {
              b.icon.setImageResource(R.drawable.baseline_remove_24)
              append("Removed vote from")
            }
            append(" ")
            when (actionInfo.ref) {
              is VotableRef.CommentRef -> {
                append("comment ")
                append("[${actionInfo.ref.commentId}@${actionInfo.instance}]")
                append("(")
                append(
                  LinkUtils.getLinkForComment(
                    instance = actionInfo.instance,
                    commentId = actionInfo.ref.commentId,
                  ),
                )
                append(")")
              }
              is VotableRef.PostRef -> {
                append("post ")
                append("[${actionInfo.ref.postId}@${actionInfo.instance}]")
                append("(")
                append(
                  LinkUtils.postIdToLink(
                    instance = actionInfo.instance,
                    postId = actionInfo.ref.postId,
                  ),
                )
                append(")")
              }
            }
            append(".")
          }
        }
        null -> {
          actionDesc = context.getString(R.string.error_unknown_action)
        }
      }

      b.user.text = if (account != null) {
        account.name
      } else if (actionInfo?.accountInstance != null) {
        context.getString(
          R.string.user_id_format,
          "$accountId@${actionInfo.accountInstance}",
        )
      } else {
        context.getString(R.string.user_id_format, accountId.toString())
      }
      b.date.text = tsToConcise(context, actionDate)

      lemmyTextHelper.bindText(
        b.actionDesc,
        actionDesc,
        account?.instance ?: "lemmy.world",
        onImageClick = {
          onImageClick("", null, it)
        },
        onVideoClick = {
          onVideoClick(it, VideoType.Unknown, null)
        },
        onPageClick = onPageClick,
        onLinkClick = onLinkClick,
        onLinkLongClick = onLinkLongClick,
      )

      when (val details = item.action.details) {
        is ActionDetails.FailureDetails -> {
          b.failureReason.visibility = View.VISIBLE
          b.failureReason.text = buildString {
            appendLine("This action failed for the following reason:")

            append(details.reason.toText(context))
          }
        }
        ActionDetails.PendingDetails -> {
          b.failureReason.visibility = View.GONE
        }
        ActionDetails.SuccessDetails -> {
          b.failureReason.visibility = View.GONE
        }
      }

      b.root.setOnClickListener {
        onActionClick(item.action)
      }
    }
    addItemType(Item.EmptyItem::class, ActionsItemEmptyBinding::inflate) { item, b, h ->
      b.text.setText(R.string.there_doesnt_seem_to_be_anything_here)
    }
  }

  override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
    adapterHelper.onCreateViewHolder(parent, viewType)

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
    adapterHelper.onBindViewHolder(holder, position)

  override fun getItemCount(): Int = adapterHelper.itemCount

  private fun refreshItems(cb: (() -> Unit)? = null) {
    val newItems = mutableListOf<Item>()

    if (actions.isNotEmpty()) {
      actions.mapTo(newItems) { Item.ActionItem(it) }
    } else {
      newItems += Item.EmptyItem
    }

    adapterHelper.setItems(newItems, this, cb)
  }
}
