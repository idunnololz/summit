package com.idunnololz.summit.lemmy

import android.content.Context
import android.graphics.Point
import android.icu.text.CompactDecimalFormat
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.core.text.BidiFormatter
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.ModlogActionType
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.api.dto.lemmy.SearchType
import com.idunnololz.summit.preferences.GlobalSettings.stringForUnknownScore
import com.idunnololz.summit.util.NumberFormatUtil
import com.idunnololz.summit.util.Size
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.ext.toBidiSafe
import com.idunnololz.summit.video.VideoSizeHint
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import java.util.Locale

object LemmyUtils {

  private var compactDecimalFormat: DecimalFormat? = null

  fun formatAuthor(author: String): String = "@%s".format(author.toBidiSafe())

  fun abbrevScore(number: Long?): String = abbrevNumber(number, stringForUnknownScore)

  fun abbrevNumber(number: Long?, valueOnNull: String = "⬤"): String {
    if (number == null) {
      return valueOnNull
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val formatter = compactDecimalFormat ?: CompactDecimalFormat.getInstance(
        Locale.getDefault(),
        CompactDecimalFormat.CompactStyle.SHORT,
      ).also {
        compactDecimalFormat = it
      }

      formatter.format(number)
    } else {
      NumberFormatUtil.format(number)
    }
  }

  fun calculateBestVideoSize(
    context: Context,
    videoSizeHint: VideoSizeHint,
    availableW: Int = Utils.getScreenWidth(context) - context.resources.getDimensionPixelOffset(
      R.dimen.padding_half,
    ) * 2,
    availableH: Int = Utils.getScreenHeight(context),
  ): Point {
    val w = videoSizeHint.width
    val h = videoSizeHint.height

    // find limiting factor
    val scale = availableW.toDouble() / w
    val scaledH = h * scale

    return if (scaledH > availableH) {
      Point((availableH.toDouble() / h * w).toInt(), availableH)
    } else {
      Point(availableW, scaledH.toInt())
    }
  }

  fun calculateMaxImagePreviewSize(
    context: Context,
    imageWidth: Int,
    imageHeight: Int,
    availableW: Int = Utils.getScreenWidth(context),
    availableH: Int = Utils.getScreenHeight(context),
  ): Size {
    if (availableW > imageWidth && availableH > imageHeight) {
      return Size(imageWidth, imageHeight)
    }

    val w = imageWidth
    val h = imageHeight

    // find limiting factor
    val scale = availableW.toDouble() / w
    val scaledH = h * scale
    if (scaledH > availableH) {
      return Size((availableH.toDouble() / h * w).toInt(), availableH)
    } else {
      return Size(availableW, scaledH.toInt())
    }
  }

  fun cleanUrl(url: String, desiredFormat: String = ""): String {
    val uri = Uri.parse(url)
    val path = uri.path ?: ""
    val cleanPath = if (path.endsWith(".xml")) {
      path.substring(0, path.lastIndexOf(".xml") - 1)
    } else if (path.endsWith(".json")) {
      path.substring(0, path.lastIndexOf(".json") - 1)
    } else {
      path
    }

    return uri.buildUpon().apply {
      path("$cleanPath$desiredFormat")
    }.build().toString()
  }
}

fun Int.toLemmyPageIndex() = this + 1 // lemmy pages are 1 indexed

fun SearchType.toLocalizedString(context: Context) = when (this) {
  SearchType.All -> context.getString(R.string.all)
  SearchType.Comments -> context.getString(R.string.comments)
  SearchType.Posts -> context.getString(R.string.posts)
  SearchType.Communities -> context.getString(R.string.communities)
  SearchType.Users -> context.getString(R.string.users)
  SearchType.Url -> context.getString(R.string.urls)
}

fun SpannableStringBuilder.appendNameWithInstance(
  context: Context,
  name: String,
  instance: String,
  url: String? = null,
) {
  val text = "$name@$instance"
  if (url != null) {
    appendLink(
      text = text,
      url = url,
      underline = false,
    )
  } else {
    append(text)
  }
  val end = length
  setSpan(
    ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorTextFaint)),
    end - instance.length - 1,
    end,
    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
  )
}

fun Person.getAccountAgeString(): String {
  val ts = dateStringToTs(published)
  val accountCreationTime = LocalDateTime
    .ofEpochSecond(ts / 1000, 0, ZoneOffset.UTC)
    .toLocalDate()
  val period = Period.between(accountCreationTime, LocalDate.now())

  val years = period.years
  val months = period.months
  val days = period.days

  return buildString {
    if (years > 0) {
      append(years)
      append("y ")
    }
    if (months > 0) {
      append(months)
      append("m ")
    }
    if (days > 0 && years == 0) {
      append(days)
      append("d ")
    }
    if (years == 0 && months == 0 && days == 0) {
      append("0d")
    }
  }.trim()
}

fun Person.isCakeDay(): Boolean {
  val ts = dateStringToTs(published)
  val accountCreationTime = LocalDateTime
    .ofEpochSecond(ts / 1000, 0, ZoneOffset.UTC)
    .toLocalDate()
  val period = Period.between(accountCreationTime, LocalDate.now())

  val years = period.years
  val months = period.months
  val days = period.days
  return years > 0 && months == 0 && days == 0
}

fun ModlogActionType.getName(context: Context) = when (this) {
  ModlogActionType.All -> context.getString(R.string.all)
  ModlogActionType.ModRemovePost -> context.getString(R.string.remove_post)
  ModlogActionType.ModLockPost -> context.getString(R.string.lock_post)
  ModlogActionType.ModFeaturePost -> context.getString(R.string.feature_post)
  ModlogActionType.ModRemoveComment -> context.getString(R.string.remove_comment)
  ModlogActionType.ModRemoveCommunity -> context.getString(R.string.remove_community)
  ModlogActionType.ModBanFromCommunity -> context.getString(R.string.ban_user_from_community)
  ModlogActionType.ModAddCommunity -> context.getString(R.string.add_mod)
  ModlogActionType.ModTransferCommunity -> context.getString(
    R.string.transferred_ownership_of_community,
  )
  ModlogActionType.ModAdd -> context.getString(R.string.add_admin)
  ModlogActionType.ModBan -> context.getString(R.string.ban_user_from_site)
  ModlogActionType.ModHideCommunity -> context.getString(R.string.hide_community)
  ModlogActionType.AdminPurgePerson -> context.getString(R.string.purge_person)
  ModlogActionType.AdminPurgeCommunity -> context.getString(R.string.purge_community)
  ModlogActionType.AdminPurgePost -> context.getString(R.string.purge_post)
  ModlogActionType.AdminPurgeComment -> context.getString(R.string.purge_comment)
}

fun ModlogActionType.getName2(context: Context, isUndo: Boolean) = when (this) {
  ModlogActionType.All -> context.getString(R.string.all)
  ModlogActionType.ModRemovePost ->
    if (isUndo) {
      context.getString(R.string.undo_remove_post)
    } else {
      context.getString(R.string.remove_post)
    }
  ModlogActionType.ModLockPost ->
    if (isUndo) {
      context.getString(R.string.unlock_post)
    } else {
      context.getString(R.string.lock_post)
    }
  ModlogActionType.ModFeaturePost ->
    if (isUndo) {
      context.getString(R.string.remove_feature_post)
    } else {
      context.getString(R.string.feature_post)
    }
  ModlogActionType.ModRemoveComment ->
    if (isUndo) {
      context.getString(R.string.undo_remove_comment)
    } else {
      context.getString(R.string.remove_comment)
    }
  ModlogActionType.ModRemoveCommunity ->
    if (isUndo) {
      context.getString(R.string.undo_remove_community)
    } else {
      context.getString(R.string.remove_community)
    }
  ModlogActionType.ModBanFromCommunity ->
    if (isUndo) {
      context.getString(R.string.unban_user)
    } else {
      context.getString(R.string.ban_user_from_community)
    }
  ModlogActionType.ModAddCommunity ->
    if (isUndo) {
      context.getString(R.string.unmod_user)
    } else {
      context.getString(R.string.add_mod)
    }
  ModlogActionType.ModTransferCommunity -> context.getString(
    R.string.transferred_ownership_of_community,
  )
  ModlogActionType.ModAdd ->
    if (isUndo) {
      context.getString(R.string.remove_admin)
    } else {
      context.getString(R.string.add_admin)
    }
  ModlogActionType.ModBan ->
    if (isUndo) {
      context.getString(R.string.unban_user_from_site)
    } else {
      context.getString(R.string.ban_user_from_site)
    }
  ModlogActionType.ModHideCommunity ->
    if (isUndo) {
      context.getString(R.string.undo_hide_community)
    } else {
      context.getString(R.string.hide_community)
    }
  ModlogActionType.AdminPurgePerson -> context.getString(R.string.purge_person)
  ModlogActionType.AdminPurgeCommunity -> context.getString(R.string.purge_community)
  ModlogActionType.AdminPurgePost -> context.getString(R.string.purge_post)
  ModlogActionType.AdminPurgeComment -> context.getString(R.string.purge_comment)
}
