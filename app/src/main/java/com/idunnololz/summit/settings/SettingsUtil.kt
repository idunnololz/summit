package com.idunnololz.summit.settings

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.idunnololz.summit.R
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.reddit_objects.UserInfo

fun Preference.bindUserInfo(fragment: Fragment, userInfo: UserInfo) {
    apply {
        title = userInfo.name
        summary = context.getString(
            R.string.karma_format, RedditUtils.abbrevNumber(
                (userInfo.commentKarma + userInfo.linkKarma).toLong()
            )
        )

        layoutResource = R.layout.custom_preference

        Glide.with(fragment)
            .asBitmap()
            .load(userInfo.iconImg)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onResourceReady(
                    bitmap: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    icon = RoundedBitmapDrawableFactory.create(context.resources, bitmap).apply {
                        isCircular = true
                    }
                }
            })
    }
}