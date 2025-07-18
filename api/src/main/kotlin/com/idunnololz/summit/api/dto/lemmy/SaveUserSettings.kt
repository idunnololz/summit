package com.idunnololz.summit.api.dto.lemmy

data class SaveUserSettings(
  val show_nsfw: Boolean? = null,
  val show_scores: Boolean? = null,
  val theme: String? = null,
  val default_sort_type: SortType? /* "Active" | "Hot" | "New" | "Old" | "TopDay" | "TopWeek" | "TopMonth" | "TopYear" | "TopAll" | "MostComments" | "NewComments" */ =
    null,
  val default_listing_type: ListingType? /* "All" | "Local" | "Subscribed" */ = null,
  val interface_language: String? = null,
  val avatar: String? = null,
  val banner: String? = null,
  val display_name: String? = null,
  val email: String? = null,
  val bio: String? = null,
  val matrix_user_id: String? = null,
  val show_avatars: Boolean? = null,
  val send_notifications_to_email: Boolean? = null,
  val bot_account: Boolean? = null,
  val show_bot_accounts: Boolean? = null,
  val show_read_posts: Boolean? = null,
  val show_new_post_notifs: Boolean? = null,
  val discussion_languages: List<LanguageId>? = null,
  val generate_totp_2fa: Boolean? = null,
  val auth: String,
)
