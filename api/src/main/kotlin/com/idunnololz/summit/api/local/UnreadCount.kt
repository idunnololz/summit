package com.idunnololz.summit.api.local

import com.google.gson.annotations.SerializedName

class UnreadCount(
  val notificationCount: Int,
  val registrationApplicationCount: Int,
  val pendingFollowCount: Int,
  val reportCount: Int,

  val mentions: Int?,
  val privateMessages: Int?,
  val replies: Int?,

  val commentReports: Int?,
  val postReports: Int?,
  val privateMessageReports: Int?,

)