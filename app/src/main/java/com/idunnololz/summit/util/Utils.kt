package com.idunnololz.summit.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.window.layout.WindowMetricsCalculator
import coil3.asImage
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.preferences.DefaultAppPreference
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.tint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.LinkedList
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

object Utils {
    private val TAG = Utils::class.java.simpleName
    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

    const val ANIMATION_DURATION_MS: Long = 300

    val displayMetrics = Resources.getSystem().displayMetrics

    val gson: Gson by lazy {
        GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    fun convertDpToPixel(dp: Float): Float {
        return dp * (displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @return A float value to represent dp equivalent to px value
     */
    fun convertPixelsToDp(px: Float): Float {
        return px / (displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    @Suppress("SuspiciousEqualsCombination")
    fun equals(a: Any?, b: Any): Boolean {
        return a === b || a != null && a == b
    }

    fun tint(context: Context, @DrawableRes res: Int, @ColorInt color: Int): Drawable {
        val drawable = checkNotNull(ContextCompat.getDrawable(context, res))
        val wrappedDrawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(wrappedDrawable, color)
        return wrappedDrawable
    }

    fun deleteDir(dir: File?): Boolean {
        return clearDir(dir, true)
    }

    fun clearDir(dir: File?, removeRootDir: Boolean): Boolean {
        if (dir == null) return false
        if (dir.isDirectory) {
            val children = dir.list() ?: return false
            for (child in children) {
                val success = deleteDir(File(dir, child))
                if (!success) {
                    return false
                }
            }
        }

        // Only delete the dir if removeRootDir is true
        return !removeRootDir || dir.delete()
    }

    @Throws(IOException::class)
    fun compress(data: String, flags: Int): String {
        return compress(data.toByteArray(), flags)
    }

    @Throws(IOException::class)
    fun compress(data: ByteArray, flags: Int): String {
        val deflater = Deflater()
        deflater.setInput(data)

        val outputStream = ByteArrayOutputStream(data.size)
        deflater.finish()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer) // returns the generated code... index
            outputStream.write(buffer, 0, count)
        }
        outputStream.close()
        val output = outputStream.toByteArray()

        return Base64.encodeToString(output, flags)
    }

    @Throws(DataFormatException::class, IOException::class)
    fun decompressZlibRaw(s: String): ByteArray {
        val bytes = Base64.decode(s, Base64.DEFAULT)

        val decompresser = Inflater()
        decompresser.setInput(bytes)

        val outputStream = ByteArrayOutputStream(bytes.size)
        val buffer = ByteArray(1024)
        while (!decompresser.finished() && !decompresser.needsInput()) {
            val count = decompresser.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        outputStream.close()
        val output = outputStream.toByteArray()
        decompresser.end()

        return output
    }

    @Throws(DataFormatException::class, IOException::class)
    fun decompressZlib(s: String): String {
        return String(decompressZlibRaw(s))
    }

    fun safeLaunchExternalIntent(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    var openExternalLinksInBrowser = false
    var defaultWebApp: DefaultAppPreference? = null
    fun openExternalLink(
        context: Context,
        url: String,
        openNewIncognitoTab: Boolean = false,
    ) {
        val defaultAppPackage = defaultWebApp?.packageName
        if (defaultAppPackage != null) {
            val componentName = defaultWebApp?.componentName
            val intent =
//                if (defaultAppPackage == "org.mozilla.firefox") {
//                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
//                        setComponent(ComponentName(
//                            "org.mozilla.firefox","org.mozilla.fenix.IntentReceiverActivity"))
//                    }
//                } else
                    if (componentName != null) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        setComponent(
                            ComponentName(
                                defaultAppPackage,
                                componentName,
                            )
                        )
                    }
                } else {
                    context.packageManager.getLaunchIntentForPackage(defaultAppPackage)
                        ?.apply {
                            action = Intent.ACTION_VIEW
                            data = Uri.parse(url)
                        }
                }
            if (intent != null) {
                safeLaunchExternalIntent(context, intent)
                return
            }
        }

        if (openExternalLinksInBrowser) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            safeLaunchExternalIntent(context, intent)
        } else {
            try {
                val intent = CustomTabsIntent.Builder()
                    .build()

                if (openNewIncognitoTab) {
                    intent.intent.putExtra(
                        "com.google.android.apps.chrome.EXTRA_OPEN_NEW_INCOGNITO_TAB",
                        true,
                    )
                }

                intent.launchUrl(context, Uri.parse(url))
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "Unable to open link.", e)

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                safeLaunchExternalIntent(context, intent)
            }
        }
    }

    fun getScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    fun getScreenWidth(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.x
    }

    fun getScreenHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.y
    }

    fun hideKeyboard(activity: Activity?) {
        if (activity == null) {
            return
        }

        val currentFocus = activity.currentFocus
        if (currentFocus != null) {
            currentFocus.clearFocus()

            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }

//        WindowInsetsControllerCompat(activity.window, activity.window.decorView)
//            .hide(WindowInsetsCompat.Type.ime())
    }

    fun fromHtml(source: String?): Spanned {
        if (source == null) return SpannableString("")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY, null, null)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(source)
        }
    }

    fun getNonconflictingFile(dir: File, name: String, extension: String): File {
        val validatedName = name.replace("[/]".toRegex(), "")
        val validatedExt = if (extension.startsWith('.')) extension else ".$extension"
        var i = 0
        var f = File(dir, validatedName + validatedExt)
        var conflict: Boolean

        do {
            if (f.exists()) {
                conflict = true
                f = File(dir, validatedName + " (" + ++i + ")" + validatedExt)
            } else {
                conflict = false
            }
        } while (conflict)
        return f
    }

    fun readFile(file: File): String = StringBuilder().also { sb ->
        file.forEachLine {
            sb.append(it)
        }
    }.toString()

    fun copyToClipboard(context: Context, toCopy: String) {
        val clipboard: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(null, toCopy)
        clipboard.setPrimaryClip(clip)
    }

    fun getFromClipboard(context: Context): CharSequence? {
        val clipboard: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val primaryClip = clipboard.primaryClip ?: return null

        return if (primaryClip.itemCount > 0) {
            primaryClip.getItemAt(0).coerceToText(context)
        } else {
            null
        }
    }

    fun shareLink(context: Context, link: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, link)
            type = "text/plain"
        }
        context.startActivity(
            Intent.createChooser(sendIntent, context.getString(R.string.share_link)),
        )
    }

    fun hashSha256(text: String): String {
        fun ByteArray.toHex(): String {
            val hexChars = CharArray(size * 2)
            for (j in 0 until size) {
                val v = this[j].toInt() and 0xFF
                hexChars[j * 2] = HEX_ARRAY[v ushr 4]
                hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
            }
            return String(hexChars)
        }
        return MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).toHex()
    }

    /**
     * A method to launch an external intent safely. Safe-ness in this context refers to the fact
     * that any runtime exceptions will be handled so crashing will not occur.
     */
    fun safeLaunchExternalIntentWithErrorDialog(
        context: Context,
        fm: FragmentManager?,
        intent: Intent,
    ) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            fm ?: return
            OldAlertDialogFragment.Builder()
                .setMessage(
                    context.getString(
                        R.string.error_external_activity_not_found,
                        intent.type,
                    ),
                )
                .setPositiveButton(android.R.string.ok)
                .createAndShow(fm, "asdf")
        }
    }

    fun getSizeOfFile(file: File): Long {
        val toCount = LinkedList<File>()
        toCount.push(file)
        var totalSize: Long = 0

        while (toCount.isNotEmpty()) {
            val curFile = toCount.pop()
            if (curFile.isFile) {
                totalSize += curFile.length()
            } else {
                toCount.addAll(curFile.listFiles() ?: arrayOf())
            }
        }
        return totalSize
    }

    fun fileSizeToHumanReadableString(totalBytes: Double, nf: NumberFormat): String {
        var suffix = "B"
        var remainingBytes = totalBytes

        if (remainingBytes >= 1024) {
            suffix = "KB"
            remainingBytes /= 1024

            if (remainingBytes >= 1024) {
                suffix = "MB"
                remainingBytes /= 1024

                if (remainingBytes >= 1024) {
                    suffix = "GB"
                    remainingBytes /= 1024
                }
            }
        }

        return "${nf.format(remainingBytes)} $suffix"
    }

    // convert a data class to a map
    fun <T> T.serializeToMap(): Map<String, String> {
        return convert()
    }

    // convert an object of type I to type O
    private inline fun <I, reified O> I.convert(): O {
        val json = gson.toJson(this)
        return gson.fromJson(
            json,
            object : TypeToken<O>() {}.type,
        )
    }
}

/**
 * Throws an [java.lang.IllegalArgumentException] if called on a thread other than the main
 * thread.
 */
fun assertMainThread() {
    if (BuildConfig.DEBUG) {
        require(Looper.myLooper() == Looper.getMainLooper()) {
            "You must call this method on the main thread"
        }
    }
}

fun convertSpToPixel(sp: Float): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP,
    sp,
    Utils.displayMetrics,
)

fun convertPixelToSp(px: Float): Float = px / Utils.displayMetrics.scaledDensity

fun Context.isLightTheme(): Boolean = resources.getBoolean(R.bool.isLightTheme)

private const val EMAIL_FEEDBACK = "feedback@idunnololz.com"
fun startFeedbackIntent(context: Context) {
    try {
        val versionName = context.packageManager
            .getPackageInfo(context.packageName, 0).versionName

        val footer = "Summit for Lemmy v" +
            versionName +
            "\n\nFeedback: \n"

        val address = EMAIL_FEEDBACK
        val subject = "Summit feedback"

        val i = Intent(Intent.ACTION_SENDTO)
        // i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(crashLogFile));
        i.putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        i.putExtra(Intent.EXTRA_SUBJECT, subject)
        i.putExtra(Intent.EXTRA_TEXT, footer)
        i.selector = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$address")
        }

        context.startActivity(Intent.createChooser(i, "Send via email"))
    } catch (e: PackageManager.NameNotFoundException) {
        // do nothing
    }
}

val summitCommunityPage = CommunityRef.CommunityRefByName("summit", "lemmy.world")

fun getColorWithAlpha(yourColor: Int, alpha: Int): Int {
    val red = Color.red(yourColor)
    val blue = Color.blue(yourColor)
    val green = Color.green(yourColor)
    return Color.argb(alpha, red, green, blue)
}

fun Activity.computeWindowMetrics() =
    WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)

fun Fragment.openAppOnPlayStore() = try {
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=com.idunnololz.summit"),
        ),
    )
} catch (e: ActivityNotFoundException) {
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "https://play.google.com/store/apps/details?id=com.idunnololz.summit",
            ),
        ),
    )
}

fun Context.getErrorDrawable() = getDrawableCompat(R.drawable.broken_image_with_padding)
    ?.apply {
        tint(getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal))
    }

fun Context.getErrorImage() = getErrorDrawable()
    ?.asImage()
