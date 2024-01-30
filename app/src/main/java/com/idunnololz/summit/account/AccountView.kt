package com.idunnololz.summit.account

import android.content.res.ColorStateList
import android.net.Uri
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import coil.dispose
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorFromAttribute

data class AccountView(
    val account: Account,
    val profileImage: Uri,
)

fun AccountView?.loadProfileImageOrDefault(imageView: ImageView) {
    if (this == null) {
        imageView.dispose()
        imageView.setImageResource(R.drawable.baseline_account_circle_24)
        val color = imageView.context.getColorFromAttribute(
            androidx.appcompat.R.attr.colorControlNormal,
        )
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color))
    } else {
        ImageViewCompat.setImageTintList(imageView, null)
        imageView.load(profileImage) {
            allowHardware(false)
        }
    }
}
