package com.idunnololz.summit.util.ext

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat

inline var ImageView.imageTintListCompat: ColorStateList?
  get() = imageTintList
  set(value) {
    ImageViewCompat.setImageTintList(this, value)
  }
