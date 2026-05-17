package com.idunnololz.summit.util

import com.idunnololz.summit.BuildConfig

val isFdroidBuild: Boolean
  @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions") // TARGET is set at compile time.
  get() = BuildConfig.TARGET == "fdroid"
