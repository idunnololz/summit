package com.idunnololz.summit.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDrawableCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountImageGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val IMAGE_DIR = "image"
    }

    fun getOrGenerateImageForAccount(accountDir: File, account: Account): File {
        val imageFile = getImageForAccount(accountDir, account)
        if (imageFile.exists()) {
            return imageFile
        }

        val bitmap = generateImageForAccount(account)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageFile.outputStream())

        return imageFile
    }

    fun getImageForAccount(accountDir: File, account: Account): File {
        val imageDir = File(accountDir, IMAGE_DIR)
        imageDir.mkdirs()

        return File(imageDir, "${account.id}_profile_image.jpg")
    }

    fun generateDrawableForPerson(person: Person): Drawable {
        val accountImageSize = context.resources.getDimensionPixelSize(R.dimen.account_image_size)
        val bitmap = Bitmap.createBitmap(accountImageSize, accountImageSize, Bitmap.Config.ARGB_8888)
        val personDrawable = context.getDrawableCompat(R.drawable.lemmy_profile_4)

        with(Canvas(bitmap)) {
            val bgPaint = Paint().apply {
                color = getColorForKey("${person.name}@${person.id}@${person.instance}")
            }

            drawRect(0f, 0f, accountImageSize.toFloat(), accountImageSize.toFloat(), bgPaint)

            personDrawable?.setBounds(0, 0, accountImageSize, accountImageSize)
            personDrawable?.draw(this)
        }
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun generateImageForAccount(account: Account): Bitmap {
        val accountImageSize = context.resources.getDimensionPixelSize(R.dimen.account_image_size)
        val bitmap = Bitmap.createBitmap(accountImageSize, accountImageSize, Bitmap.Config.ARGB_8888)

        val accountFirstCharacter = account.name.firstOrNull {
            val lowerChar = it.lowercase(Locale.US)[0]
            lowerChar in 'a'..'z' || lowerChar in '0'..'9'
        }

        with(Canvas(bitmap)) {
            val bgPaint = Paint().apply {
                color = getColorForAccount(account)
            }
            val textPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                textSize = Utils.convertDpToPixel(26f)
            }

            drawRect(0f, 0f, accountImageSize.toFloat(), accountImageSize.toFloat(), bgPaint)

            if (accountFirstCharacter != null) {
                val inRect = Rect()
                val textToDraw = accountFirstCharacter.toString()

                textPaint.getTextBounds(textToDraw, 0, textToDraw.length, inRect)
                val width = textPaint.measureText(textToDraw)
                drawText(
                    textToDraw,
                    (bitmap.width - width) / 2f,
                    (bitmap.height + inRect.height()) / 2f,
                    textPaint,
                )
            }
        }
        return bitmap
    }

    private fun getColorForAccount(account: Account): Int {
        return getColorForKey("${account.name}_${account.id}")
    }

    private fun getColorForKey(key: String): Int {
        // Ported from https://dev.to/admitkard/auto-generate-avatar-colors-randomly-138j
        val hRange = 0 until 360
        val sRange = 10 until 100
        val lRange = 70 until 100

        val accountHash = key.hashCode()

        fun normalizeHash(hash: Int, min: Int, max: Int) =
            hash % (max - min) + min

        val h = normalizeHash(accountHash, hRange.first, hRange.last)
        val s = normalizeHash(accountHash, sRange.first, sRange.last)
        val l = normalizeHash(accountHash, lRange.first, lRange.last)

        return Color.HSVToColor(
            floatArrayOf(
                h.toFloat(),
                s / 100f,
                l / 100f,
            ),
        )
    }

    private fun getPastelColorForKey(key: String): Int {
        // Ported from https://medium.com/@pppped/compute-an-arbitrary-color-for-user-avatar-starting-from-his-username-with-javascript-cd0675943b66
        val hRange = 0 until 360

        val accountHash = key.hashCode()

        fun normalizeHash(hash: Int, min: Int, max: Int) =
            hash % (max - min) + min

        val h = normalizeHash(accountHash, hRange.first, hRange.last)

        return Color.HSVToColor(
            floatArrayOf(
                h.toFloat(),
                30f,
                80f,
            ),
        )
    }
}
