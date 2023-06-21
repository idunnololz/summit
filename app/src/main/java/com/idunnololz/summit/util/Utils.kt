package com.idunnololz.summit.util

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.text.Html
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import java.io.*
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.*
import java.util.zip.*
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

object Utils {
    private val TAG = Utils::class.java.simpleName
    const val EMAIL_FEEDBACK = "feedback@idunnololz.com"

    const val ANIMATION_DURATION_MS: Long = 300

    private val displayMetrics = Resources.getSystem().displayMetrics

    val gson: Gson by lazy {
        GsonBuilder()
            //.registerTypeAdapter(RedditObject::class.java, RedditObjectSerializer())
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

    fun convertSpToPixel(sp: Float): Float {
        return sp * displayMetrics.scaledDensity
    }

    fun lightenColor(color: Int, amount: Float): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return redistributeColor(
            255 * amount + r,
            255 * amount + g,
            255 * amount + b
        )
    }

    private fun redistributeColor(r: Float, g: Float, b: Float): Int {
        val threshold = 255.999f
        val max = Math.max(Math.max(r, g), b)
        if (max <= threshold) {
            return Color.rgb(r.toInt(), g.toInt(), b.toInt())
        }
        val total = r + g + b
        if (total >= 3 * threshold) {
            return Color.rgb(255, 255, 255)
        }
        val x = (3 * threshold - total) / (3 * max - total)
        val gray = threshold - x * max
        return Color.rgb((gray + x * r).toInt(), (gray + x * g).toInt(), (gray + x * b).toInt())
    }

    fun parseFirstInteger(str: String): Int {
        var i = 0
        var n = 0
        val len = str.length
        while (i < len) {
            val c = str[i]
            if (c in '0'..'9') break
            i++
        }
        while (i < len) {
            val c = str[i]
            if (c in '0'..'9') {
                n *= 10
                n += c - '0'
            } else {
                break
            }
            i++
        }
        return n
    }

    private fun createEmailOnlyChooserIntent(
        context: Context,
        source: Intent,
        chooserTitle: CharSequence
    ): Intent {
        val intents = Stack<Intent>()
        val i = Intent(
            Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto",
                "info@domain.com", null
            )
        )
        val activities = context.packageManager
            .queryIntentActivities(i, 0)

        for (ri in activities) {
            val target = Intent(source)
            target.setPackage(ri.activityInfo.packageName)
            intents.add(target)
        }

        return if (!intents.isEmpty()) {
            val chooserIntent = Intent.createChooser(
                intents.removeAt(0),
                chooserTitle
            )
            chooserIntent.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                intents.toTypedArray<Parcelable>()
            )

            chooserIntent
        } else {
            Intent.createChooser(source, chooserTitle)
        }
    }

    fun startFeedbackIntent(context: Context) {
        try {
            val versionName = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName

            val footer = "LoL Catalyst v" +
                    versionName +
                    "\nOS v" +
                    Build.VERSION.RELEASE +
                    "\nDevice " +
                    Build.MODEL +
                    "\n\nFeedback: \n"

            val i = Intent(Intent.ACTION_SEND)
            i.type = "*/*"
            //i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(crashLogFile));
            i.putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_FEEDBACK))
            i.putExtra(Intent.EXTRA_SUBJECT, "LoL Catalyst feedback")
            i.putExtra(Intent.EXTRA_TEXT, footer)

            context.startActivity(Utils.createEmailOnlyChooserIntent(context, i, "Send via email"))

        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "", e)
        }
    }

    fun isConnectedOrConnecting(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

    private fun startSlideUpAnimation(
        viewToAnimate: View,
        startDelay: Int
    ): ViewPropertyAnimator {
        viewToAnimate.alpha = 0f
        viewToAnimate.translationY = convertDpToPixel(100f)
        return viewToAnimate.animate().translationY(0f).alpha(1f)
            .setStartDelay(startDelay.toLong())
            .setDuration(ANIMATION_DURATION_MS)
    }

    fun isLightColor(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255
        return darkness < 0.5
    }

    @Suppress("SuspiciousEqualsCombination")
    fun equals(a: Any?, b: Any): Boolean {
        return a === b || a != null && a == b
    }

    fun tint(context: Context, @DrawableRes res: Int, @ColorRes color: Int): Drawable {
        val drawable = checkNotNull(ContextCompat.getDrawable(context, res))
        val c = ContextCompat.getColor(context, color)
        val wrappedDrawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(wrappedDrawable, c)
        return wrappedDrawable
    }

    fun getToolbarTextView(toolbar: Toolbar): TextView? {
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)

            // assuming that the title is the first instance of TextView
            // you can also check if the title string matches
            if (child is TextView) {
                return child
            }
        }
        return null
    }

    fun getToolbarHeight(context: Context): Int {
        val attrs = intArrayOf(androidx.appcompat.R.attr.actionBarSize)
        val ta = context.obtainStyledAttributes(attrs)
        val toolBarHeight = ta.getDimensionPixelSize(0, -1)
        ta.recycle()
        return toolBarHeight
    }

    fun toIntList(perks: IntArray): List<Int> {
        val list = ArrayList<Int>()
        for (p in perks) {
            list.add(p)
        }
        return list
    }

    fun setupRootViewForToolbarAndTabLayout(rootView: View) {
        val lp = rootView.layoutParams as ViewGroup.MarginLayoutParams
        lp.topMargin = (convertDpToPixel(48f) + getToolbarHeight(rootView.context)).toInt()
        rootView.requestLayout()
    }

    fun getNavigationBarInset(context: Context): Int {
        val appUsableSize = getAppUsableScreenSize(context)
        val realScreenSize = getRealScreenSize(context)

        // navigation bar on the right
        if (appUsableSize.x < realScreenSize.x) {
            return 0
        }

        // navigation bar at the bottom
        return if (appUsableSize.y < realScreenSize.y) {
            realScreenSize.y - appUsableSize.y
        } else 0

        // navigation bar is not present
    }

    private fun getAppUsableScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    private fun getRealScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size)
        } else {
            try {
                size.x = Display::class.java.getMethod("getRawWidth").invoke(display) as Int
                size.y = Display::class.java.getMethod("getRawHeight").invoke(display) as Int
            } catch (e: Exception) {/* do nothing */
            }

        }

        return size
    }

    fun folderSize(directory: File): Long {
        val files = directory.listFiles() ?: return 0

        var length: Long = 0
        for (file in files) {
            length += if (file.isFile)
                file.length()
            else
                folderSize(file)
        }
        return length
    }

    fun wipeCacheDir() {
        try {
            val dir = DataCache.instance.cacheDir
            if (dir.isDirectory) {
                deleteDir(dir)
            }
        } catch (e: Exception) {/* Do nothing */
        }

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
    fun unzip(zipFile: File, targetDirectory: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            val buffer = ByteArray(8192)
            while (true) {
                val ze = zis.nextEntry ?: break

                val file = File(targetDirectory, ze.name)
                val dir = if (ze.isDirectory) file else file.parentFile
                if (!dir.isDirectory && !dir.mkdirs())
                    throw FileNotFoundException("Failed to ensure directory: " + dir.absolutePath)
                if (ze.isDirectory)
                    continue
                FileOutputStream(file).use { fout ->
                    while (true) {
                        val count = zis.read(buffer)
                        if (count == -1) break

                        fout.write(buffer, 0, count)
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    fun decompressGzip(s: String): String {
        val decoded = Base64.decode(s, Base64.DEFAULT)
        return decompressGzip(ByteArrayInputStream(decoded))
    }

    @Throws(IOException::class)
    fun decompressGzip(inputStream: InputStream): String {
        val charset = "UTF-8" // You should determine it based on response header.
        var body: String

        GZIPInputStream(inputStream).use {
            val reader = InputStreamReader(it, charset)
            val writer = StringWriter()

            val buffer = CharArray(32)
            while (true) {
                val length = reader.read(buffer)
                if (length <= 0) break
                writer.write(buffer, 0, length)
            }
            body = writer.toString()

            return body
        }
    }

    @Throws(IOException::class)
    fun compress(data: String, flags: Int): String {
        return compress(data.toByteArray(), flags)
    }

    @Throws(IOException::class)
    private fun compress(data: ByteArray, flags: Int): String {
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
    fun decompressZlib(s: String): String {
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

        return String(output)
    }

    fun convertSecondsToHMmSs(seconds: Long): String {
        val s = seconds % 60
        val m = seconds / 60 % 60
        val h = seconds / (60 * 60) % 24
        return String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    }

    fun safeLaunchExternalIntent(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }


    fun openExternalLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        safeLaunchExternalIntent(context, intent)
    }

    fun launchAppPlayStore(activity: Activity) {
        val appPackageName =
            activity.packageName // getPackageName() from Context or Activity object
        try {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (anfe: android.content.ActivityNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getRemovableExternalCacheDir(context: Context): File? {
        val externalCacheDirs = ContextCompat.getExternalCacheDirs(context)
        for (f in externalCacheDirs) {
            if (f == null) continue
            if (Environment.isExternalStorageRemovable(f)) {
                return f
            }
        }
        return null
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getRemovableExternalFilesDir(context: Context): File? {
        val externalCacheDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (f in externalCacheDirs) {
            if (f == null) continue
            if (Environment.isExternalStorageRemovable(f)) {
                return f
            }
        }
        return null
    }

    fun trimSpannable(spannable: Spanned): SpannableStringBuilder {
        val sb: SpannableStringBuilder = if (spannable is SpannableStringBuilder) {
            spannable
        } else {
            SpannableStringBuilder(spannable)
        }
        var trimStart = 0
        var trimEnd = 0

        var text = spannable.toString()

        while (text.isNotEmpty() && text.startsWith("\n")) {
            text = text.substring(1)
            trimStart += 1
        }

        while (text.isNotEmpty() && text.endsWith("\n")) {
            text = text.substring(0, text.length - 1)
            trimEnd += 1
        }

        return sb.delete(0, trimStart).delete(spannable.length - trimEnd, spannable.length)
    }

    fun triggerRebirth(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        triggerRebirth(context, intent!!)
    }

    fun triggerRebirth(context: Context, intent: Intent) {
        val componentName = intent.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        mainIntent.putExtras(intent)
        context.startActivity(mainIntent)

        if (context is Activity) {
            context.overridePendingTransition(0, 0)
        }

        System.exit(0)
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
        checkNotNull(windowManager) { "WindowManager was null" }

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.y
    }

    /**
     * Converts a version string to a comparable long for easy version comparing. Only supports
     * version strings of the form "<0 - 999>.<0 - 999>.<0 - 999>"
     */
    fun getVersionId(version: String): Long {
        val multipliers = longArrayOf((1000 * 1000).toLong(), 1000, 1)

        val toks = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var newVersionId = 0L
        for ((i, s) in toks.withIndex()) {
            newVersionId += Utils.parseFirstInteger(s) * multipliers[i]
        }
        return newVersionId
    }

    fun bundleToString(bundle: Bundle?): String? {
        if (bundle == null) {
            return null
        }
        val sb = StringBuilder("Bundle {")
        for (key in bundle.keySet()) {
            sb.append(key)
            sb.append(":")
            sb.append(bundle.get(key))
            sb.append(",")
        }
        if (bundle.keySet().size != 0) {
            sb.setLength(sb.length - 1)
        }
        sb.append("}")
        return sb.toString()
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
    }

    fun permute(arr: List<Int>): MutableList<MutableList<Int>> {
        fun permute(arr: List<Int>, k: Int, results: MutableList<MutableList<Int>>) {
            for (i in k until arr.size) {
                java.util.Collections.swap(arr, i, k)
                permute(arr, k + 1, results)
                java.util.Collections.swap(arr, k, i)
            }
            if (k == arr.size - 1) {
                results.add(ArrayList(arr))
            }
        }

        val result = ArrayList<MutableList<Int>>()
        permute(arr, 0, result)
        return result
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

    fun fromHtml(
        source: String?,
        imageGetter: Html.ImageGetter?,
        tagHandler: Html.TagHandler?
    ): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY, imageGetter, tagHandler)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(source, imageGetter, tagHandler)
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

    fun canUserReadEn(resources: Resources): Boolean {
        val appLocale = resources.configuration.locale
        val systemLocale = Resources.getSystem().getConfiguration().locale

        return systemLocale.language == Locale.ENGLISH.language || appLocale.language == Locale.ENGLISH.language
    }

    fun getStatusBarHeight(context: Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    fun copyToClipboard(context: Context, toCopy: String) {
        val clipboard: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(null, toCopy)
        clipboard.setPrimaryClip(clip)
    }

    fun shareText(context: Context, text: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(sendIntent)
    }

    fun startIntentToRateApp(activity: Activity) {
        val uri = Uri.parse("market://details?id=${activity.packageName}")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            activity.startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=${activity.packageName}")
            )
            activity.startActivity(intent)
        }
    }

    fun hashSha256(text: String): String {
        val HEX_ARRAY = "0123456789ABCDEF".toCharArray();
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
        fm: androidx.fragment.app.FragmentManager?,
        intent: Intent
    ) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            fm ?: return
            AlertDialogFragment.Builder()
                .setMessage(
                    context.getString(
                        R.string.error_external_activity_not_found,
                        intent.type
                    )
                )
                .setPositiveButton(android.R.string.ok)
                .createAndShow(fm, "asdf")
        }
    }

    fun getMaxTextureSize(): Int {
        // Safe minimum default size
        val IMAGE_MAX_BITMAP_DIMENSION = 2048

        // Get EGL Display
        val egl = EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

        // Initialise
        val version = IntArray(2)
        egl.eglInitialize(display, version)

        // Query total number of configurations
        val totalConfigurations = IntArray(1)
        egl.eglGetConfigs(display, null, 0, totalConfigurations)

        // Query actual list configurations
        val configurationsList = Array<EGLConfig?>(totalConfigurations[0]) { null }
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations)

        val textureSize = IntArray(1)
        var maximumTextureSize = 0

        // Iterate through all the configurations to located the maximum texture size
        for (i in 0 until totalConfigurations[0]) {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(
                display,
                configurationsList[i],
                EGL10.EGL_MAX_PBUFFER_WIDTH,
                textureSize
            );

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0])
                maximumTextureSize = textureSize[0];
        }

        // Release
        egl.eglTerminate(display);

        // Return largest texture size found, or default
        return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
    }

    fun isUiThread(): Boolean = Looper.getMainLooper().getThread() == Thread.currentThread()

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
        var suffix: String = "B"
        var totalBytes = totalBytes

        if (totalBytes >= 1024) {
            suffix = "KB"
            totalBytes /= 1024

            if (totalBytes >= 1024) {
                suffix = "MB"
                totalBytes /= 1024

                if (totalBytes >= 1024) {
                    suffix = "GB"
                    totalBytes /= 1024
                }
            }
        }

        return "${nf.format(totalBytes)} $suffix"
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
    require(Looper.myLooper() == Looper.getMainLooper()) { "You must call this method on the main thread" }
}