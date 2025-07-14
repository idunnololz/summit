package com.idunnololz.summit.api.dto.lemmy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalSite(
  val id: LocalSiteId = 0,
  val site_id: SiteId = 0,
  val site_setup: Boolean = true,
  val enable_downvotes: Boolean = true,
  val enable_nsfw: Boolean = true,
  val community_creation_admin_only: Boolean = false,
  val require_email_verification: Boolean = true,
  val application_question: String? = null,
  val private_instance: Boolean = false,
  val default_theme: String? = null,
  val default_post_listing_type: ListingType /* "All" | "Local" | "Subscribed" */ = ListingType.All,
  val legal_information: String? = null,
  val hide_modlog_mod_names: Boolean = true,
  val application_email_admins: Boolean = false,
  val slur_filter_regex: String? = null,
  val actor_name_max_length: Int = -1,
  val federation_enabled: Boolean = true,
  val federation_debug: Boolean = false,
  val federation_worker_count: Int = 0,
  val captcha_enabled: Boolean = false,
  val captcha_difficulty: String? = null,
  val published: String? = null,
  val updated: String? = null,
  val registration_mode: RegistrationMode /* "Closed" | "RequireApplication" | "Open" */ = RegistrationMode.Open,
  val reports_email_admins: Boolean = false,
) : Parcelable
