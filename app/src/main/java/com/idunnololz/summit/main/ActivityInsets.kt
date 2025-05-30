package com.idunnololz.summit.main

data class ActivityInsets(
  val imeHeight: Int = 0,
  val topInset: Int = 0,
  val bottomInset: Int = 0,
  val leftInset: Int = 0,
  val rightInset: Int = 0,
  val mainLeftInset: Int = 0,
  val mainRightInset: Int = 0,
  val gestureRegionLeft: Int = 0,
  val gestureRegionRight: Int = 0,
  val isStatusBarVisible: Boolean = true,
  val isNavigationBarVisible: Boolean = true,
)
