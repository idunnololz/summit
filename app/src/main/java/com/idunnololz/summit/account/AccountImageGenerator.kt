package com.idunnololz.summit.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.collection.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.PersonId
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
    private const val TAG = "AccountImageGenerator"

    private const val IMAGE_DIR = "image"
  }

  private val memoryCache: LruCache<String, Bitmap>? = newLruCache()

  private fun newLruCache(): LruCache<String, Bitmap>? {
    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSize = maxMemory / 32

    Log.d(TAG, "cache size: $cacheSize kb")

    if (cacheSize < 500) {
      return null
    }

    return object : LruCache<String, Bitmap>(cacheSize) {
      override fun sizeOf(key: String, value: Bitmap): Int {
        // The cache size will be measured in kilobytes rather than
        // number of items.
        return value.byteCount / 1024
      }
    }
  }

  fun getOrGenerateImageForAccount(account: Account): File {
    val imageFile = getImageForAccount(account)
    if (imageFile.exists()) {
      return imageFile
    }

    val bitmap = generateDrawableForPerson(
      personName = account.name,
      personId = account.id,
      personInstance = account.instance,
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
  ): BitmapDrawable = generateDrawableForGeneric(
    key = personKey(personName, personId, personInstance),
    circleClip = circleClip,
  )

  fun generateDrawableForKey(key: String): Drawable = generateDrawableForGeneric(key)

  fun generateDrawableForGeneric(
    key: String,
    drawable: Drawable? = context.getDrawableCompat(
      R.drawable.lemmy_profile_4,
    ),
    circleClip: Boolean = false,
  ): BitmapDrawable {
    val cacheKey = "$key|$circleClip"
    val bitmap = if (memoryCache != null) {
      memoryCache[cacheKey]
        ?: generateBitmapForGeneric(key, drawable, circleClip).also {
          memoryCache.put(cacheKey, it)
        }
    } else {
      generateBitmapForGeneric(key, drawable, circleClip)
    }
    return bitmap.toDrawable(context.resources)
  }

  fun getColorForPerson(personName: String, personId: PersonId, personInstance: String): Int =
    getColorForKey(personKey(personName, personId, personInstance))

  private fun generateBitmapForGeneric(
    key: String,
    drawable: Drawable?,
    circleClip: Boolean,
  ): Bitmap {
    val accountImageSize = context.resources.getDimensionPixelSize(R.dimen.account_image_size)
    val bitmap = createBitmap(width = accountImageSize, height = accountImageSize)

    with(Canvas(bitmap)) {
      val bgPaint = Paint().apply {
        color = getColorForKey(key)
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

      drawable?.setBounds(0, 0, accountImageSize, accountImageSize)
      drawable?.draw(this)
    }
    return bitmap
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

  @Suppress("NOTHING_TO_INLINE")
  private inline fun personKey(personName: String, personId: PersonId, personInstance: String) =
    "$personName@$personId@$personInstance"
}
