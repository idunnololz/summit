package com.idunnololz.summit.lemmy.modlogs

import android.content.Context
import androidx.annotation.DrawableRes
import com.idunnololz.summit.R
import com.idunnololz.summit.api.local.ModEvent
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute

fun ModEvent.getColor(context: Context): Int {
  val modEvent = this

  return when (modEvent) {
    is ModEvent.AdminPurgeCommentViewEvent,
    is ModEvent.AdminPurgeCommunityViewEvent,
    is ModEvent.AdminPurgePersonViewEvent,
    is ModEvent.AdminPurgePostViewEvent,
    ->
      context.getColorFromAttribute(androidx.appcompat.R.attr.colorError)
    is ModEvent.ModAddCommunityViewEvent -> {
      if (modEvent.removed) {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorError)
      } else {
        context.getColorCompat(R.color.style_green)
      }
    }
    is ModEvent.ModAddViewEvent -> {
      if (modEvent.removed) {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorError)
      } else {
        context.getColorCompat(R.color.style_green)
      }
    }
    is ModEvent.ModBanFromCommunityViewEvent -> {
      if (modEvent.banned) {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorError)
      } else {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModBanViewEvent -> {
      if (modEvent.banned) {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorError)
      } else {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModFeaturePostViewEvent -> {
      if (modEvent.featured) {
        context.getColorCompat(R.color.style_green)
      } else {
        context.getColorCompat(R.color.style_green)
      }
    }
    is ModEvent.ModHideCommunityViewEvent -> {
      if (modEvent.hidden) {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary)
      } else {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModLockPostViewEvent -> {
      if (modEvent.locked) {
        context.getColorCompat(R.color.style_amber)
      } else {
        context.getColorCompat(R.color.style_amber)
      }
    }
    is ModEvent.ModRemoveCommentViewEvent -> {
      if (modEvent.removed) {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorError)
      } else {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModRemoveCommunityViewEvent -> {
      if (modEvent.removed) {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorError)
      } else {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModRemovePostViewEvent -> {
      if (modEvent.removed) {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorError)
      } else {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary)
      }
    }
    is ModEvent.ModTransferCommunityViewEvent -> {
      context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary)
    }
    is ModEvent.AdminBlockInstanceEvent ->
      if (modEvent.blocked) {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorError)
      } else {
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorPrimary)
      }
    is ModEvent.AdminFeaturePostViewEvent ->
      context.getColorCompat(R.color.style_green)
    is ModEvent.ModLockCommentViewEvent ->
      context.getColorCompat(R.color.style_amber)
    is ModEvent.ModWarnCommentViewEvent ->
      context.getColorCompat(R.color.style_blue)
    is ModEvent.ModWarnPostViewEvent ->
      context.getColorCompat(R.color.style_blue)
  }
}

@DrawableRes
fun ModEvent.getIconRes(): Int = when (this) {
  is ModEvent.AdminPurgeCommentViewEvent ->
    R.drawable.baseline_delete_24
  is ModEvent.AdminPurgeCommunityViewEvent ->
    R.drawable.baseline_delete_24
  is ModEvent.AdminPurgePersonViewEvent ->
    R.drawable.baseline_delete_24
  is ModEvent.AdminPurgePostViewEvent ->
    R.drawable.baseline_delete_24
  is ModEvent.ModAddCommunityViewEvent -> {
    if (this.removed) {
      R.drawable.outline_remove_moderator_24
    } else {
      R.drawable.outline_add_moderator_24
    }
  }
  is ModEvent.ModAddViewEvent -> {
    if (this.removed) {
      R.drawable.outline_remove_moderator_24
    } else {
      R.drawable.outline_add_moderator_24
    }
  }
  is ModEvent.ModBanFromCommunityViewEvent -> {
    if (this.banned) {
      R.drawable.outline_person_remove_24
    } else {
      R.drawable.baseline_person_add_alt_24
    }
  }
  is ModEvent.ModBanViewEvent -> {
    if (this.banned) {
      R.drawable.outline_person_remove_24
    } else {
      R.drawable.baseline_person_add_alt_24
    }
  }
  is ModEvent.ModFeaturePostViewEvent -> {
    if (this.featured) {
      R.drawable.baseline_push_pin_24
    } else {
      R.drawable.ic_unpin_24
    }
  }
  is ModEvent.ModHideCommunityViewEvent -> {
    if (this.hidden) {
      R.drawable.baseline_hide_24
    } else {
      R.drawable.baseline_expand_content_24
    }
  }
  is ModEvent.ModLockPostViewEvent -> {
    if (this.locked) {
      R.drawable.outline_lock_24
    } else {
      R.drawable.baseline_lock_open_24
    }
  }
  is ModEvent.ModRemoveCommentViewEvent -> {
    if (this.removed) {
      R.drawable.baseline_remove_circle_outline_24
    } else {
      R.drawable.baseline_undo_24
    }
  }
  is ModEvent.ModRemoveCommunityViewEvent -> {
    if (this.removed) {
      R.drawable.baseline_remove_circle_outline_24
    } else {
      R.drawable.baseline_undo_24
    }
  }
  is ModEvent.ModRemovePostViewEvent -> {
    if (this.removed) {
      R.drawable.baseline_remove_circle_outline_24
    } else {
      R.drawable.baseline_undo_24
    }
  }
  is ModEvent.ModTransferCommunityViewEvent ->
    R.drawable.baseline_swap_horiz_24

  is ModEvent.AdminBlockInstanceEvent ->
    if (this.blocked) {
      R.drawable.baseline_remove_circle_outline_24
    } else {
      R.drawable.baseline_undo_24
    }
  is ModEvent.AdminFeaturePostViewEvent ->
    if (this.featured) {
      R.drawable.baseline_push_pin_24
    } else {
      R.drawable.ic_unpin_24
    }
  is ModEvent.ModLockCommentViewEvent ->
    if (this.locked) {
      R.drawable.outline_lock_24
    } else {
      R.drawable.baseline_lock_open_24
    }
  is ModEvent.ModWarnCommentViewEvent ->
    if (this.warned) {
      R.drawable.outline_warning_24
    } else {
      R.drawable.outline_warning_off_24
    }
  is ModEvent.ModWarnPostViewEvent ->
    if (this.warned) {
      R.drawable.outline_warning_24
    } else {
      R.drawable.outline_warning_off_24
    }
}
