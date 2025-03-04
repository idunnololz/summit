package com.idunnololz.summit.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.ext.getDrawableCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AccountImageGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directoryHelper: DirectoryHelper,
) {
    companion object {
        private const val IMAGE_DIR = "image"
    }

    fun getOrGenerateImageForAccount(account: Account): File {
        val imageFile = getImageForAccount(account)
        if (imageFile.exists()) {
            return imageFile
        }

        val bitmap = generateDrawableForPerson(
            account.name,
            account.id,
            account.instance,
        ).bitmap
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageFile.outputStream())

        return imageFile
    }

    fun getImageForAccount(account: Account): File {
        val imageDir = File(directoryHelper.accountDir, IMAGE_DIR)
        imageDir.mkdirs()

        return File(imageDir, "${account.id}_profile_image.jpg")
    }

    fun generateDrawableForPerson(
        personName: String,
        personId: PersonId,
        personInstance: String,
        circleClip: Boolean = false,
    ): BitmapDrawable {
        val accountImageSize = context.resources.getDimensionPixelSize(R.dimen.account_image_size)
        val bitmap = Bitmap.createBitmap(
            accountImageSize,
            accountImageSize,
            Bitmap.Config.ARGB_8888,
        )
        val personDrawable = context.getDrawableCompat(R.drawable.lemmy_profile_4)

        with(Canvas(bitmap)) {
            val bgPaint = Paint().apply {
                color = getColorForPerson(
                    personName,
                    personId,
                    personInstance,
                )
            }

            if (circleClip) {
                clipPath(
                    Path().apply {
                        addCircle(
                            accountImageSize.toFloat() / 2f,
                            accountImageSize.toFloat() / 2,
                            accountImageSize.toFloat() / 2f,
                            Path.Direction.CW,
                        )
                    },
                )
            }

            drawRect(0f, 0f, accountImageSize.toFloat(), accountImageSize.toFloat(), bgPaint)

            personDrawable?.setBounds(0, 0, accountImageSize, accountImageSize)
            personDrawable?.draw(this)
        }
        return BitmapDrawable(context.resources, bitmap)
    }

    fun generateDrawableForKey(key: String): Drawable {
        return generateDrawableForGeneric(key)
    }

    fun generateDrawableForGeneric(
        key: String,
        drawable: Drawable? = context.getDrawableCompat(
            R.drawable.lemmy_profile_4,
        ),
    ): Drawable {
        val accountImageSize = context.resources.getDimensionPixelSize(R.dimen.account_image_size)
        val bitmap = Bitmap.createBitmap(
            accountImageSize,
            accountImageSize,
            Bitmap.Config.ARGB_8888,
        )

        with(Canvas(bitmap)) {
            val bgPaint = Paint().apply {
                color = getColorForKey(key)
            }

            drawRect(0f, 0f, accountImageSize.toFloat(), accountImageSize.toFloat(), bgPaint)

            drawable?.setBounds(0, 0, accountImageSize, accountImageSize)
            drawable?.draw(this)
        }
        return BitmapDrawable(context.resources, bitmap)
    }

    fun getColorForPerson(personName: String, personId: PersonId, personInstance: String): Int {
        return getColorForKey("$personName@$personId@$personInstance")
    }

    private fun getColorForKey(key: String): Int {
        // Ported from https://dev.to/admitkard/auto-generate-avatar-colors-randomly-138j

        fun hash(key: String): Int {
            var hash = 0
            for (char in key) {
                hash = char.code + ((hash shl 5) - hash)
            }
            hash = abs(hash)

            return hash
        }

        val accountHash = hash(key)

        fun normalizeHash(hash: Int, min: Int, max: Int) = hash % (max - min) + min

        val h = normalizeHash(accountHash, 0, 3600)
        val s = normalizeHash(accountHash / 100, 500, 800)
        val l = normalizeHash(accountHash * 31, 600, 900)

        return Color.HSVToColor(
            floatArrayOf(
                h.toFloat() / 10f,
                s / 1000f,
                l / 1000f,
            ),
        )
    }
}
