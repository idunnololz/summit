package com.idunnololz.summit.util.coil

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.util.Log
import android.widget.TextView
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.size.Dimension
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.shimmer.newShimmerDrawable16to9
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableLoader
import io.noties.markwon.image.DrawableUtils
import io.noties.markwon.image.ImageSpanFactory
import io.noties.markwon.image.OnDimensionsKnownListener
import java.util.concurrent.atomic.AtomicBoolean
import org.commonmark.node.Image

class CoilImagesPlugin(
    private val context: Context,
    coilStore: CoilStore,
    imageLoader: ImageLoader,
) : AbstractMarkwonPlugin() {

    interface CoilStore {
        fun load(drawable: AsyncDrawable): ImageRequest
        fun cancel(disposable: Disposable)
    }

    private val coilAsyncDrawableLoader: CoilAsyncDrawableLoader =
        CoilAsyncDrawableLoader(context, coilStore, imageLoader)

    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
        builder.setFactory(Image::class.java, ImageSpanFactory())
    }

    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
        builder.asyncDrawableLoader(coilAsyncDrawableLoader)
    }

    override fun beforeSetText(textView: TextView, markdown: Spanned) {
        AsyncDrawableSchedulerFixed.unschedule(textView)
    }

    override fun afterSetText(textView: TextView) {
        AsyncDrawableSchedulerFixed.schedule(textView)
    }

    private class CoilAsyncDrawableLoader(
        private val context: Context,
        private val coilStore: CoilStore,
        private val imageLoader: ImageLoader,
    ) : AsyncDrawableLoader() {

        private val cache: MutableMap<AsyncDrawable, Disposable?> = HashMap(2)
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        private val onDimensionsKnownListener = object : OnDimensionsKnownListener {
            override fun onDimensionsKnown(drawable: AsyncDrawable) {
                drawable.unregisterOnDimensionsKnownListener(this)
                load(drawable)
            }
        }

        override fun load(drawable: AsyncDrawable) {
            if (!drawable.hasKnownDimensions()) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    mainThreadHandler.post {
                        drawable.registerOnDimensionsKnownListener(onDimensionsKnownListener)
                        drawable.invalidateSelf()
                    }
                } else {
                    drawable.registerOnDimensionsKnownListener(onDimensionsKnownListener)
                    drawable.invalidateSelf()
                }
                return
            }

            val loaded = AtomicBoolean(false)
            val target = AsyncDrawableTarget(drawable, loaded, drawable.destination)
            val request = coilStore.load(drawable).newBuilder()
                .target(target)
                .size(Dimension.Pixels(drawable.lastKnownCanvasWidth), Dimension.Undefined)
                .build()
            // @since 4.5.1 execute can return result _before_ disposable is created,
            //  thus `execute` would finish before we put disposable in cache (and thus result is
            //  not delivered)
            val disposable = imageLoader.enqueue(request)
            // if flag was not set, then job is running (else - finished before we got here)
            if (!loaded.get()) {
                // mark flag
                loaded.set(true)
                cache[drawable] = disposable
            }
        }

        override fun cancel(drawable: AsyncDrawable) {
            val disposable = cache.remove(drawable)
            if (disposable != null) {
                coilStore.cancel(disposable)
            }
        }

        override fun placeholder(drawable: AsyncDrawable): Drawable? {
            return null
        }

        private inner class AsyncDrawableTarget(
            private val drawable: AsyncDrawable,
            private val loaded: AtomicBoolean,
            private val source: String,
        ) : coil3.target.Target {

            override fun onSuccess(image: coil3.Image) {
                // @since 4.5.1 check finished flag (result can be delivered _before_ disposable is created)
                if (cache.remove(drawable) != null ||
                    !loaded.get()
                ) {
                    // mark
                    loaded.set(true)
                    if (drawable.isAttached) {
                        val loadedDrawable = image.asDrawable(context.resources)

//                        val maxScaleFactor = drawable.lastKnownCanvasWidth / image.width.toFloat()
//                        if (loadedDrawable.bounds.isEmpty) {
//                            loadedDrawable.bounds = Rect(
//                                0,
//                                0,
//                                (image.width * scaleFactor).toInt(),
//                                (image.height * scaleFactor).toInt(),
//                            )
//                        }

                        if (drawable.imageText?.startsWith("emoji", ignoreCase = true) == true) {
                            loadedDrawable.bounds = Rect(
                                0,
                                0,
                                Utils.convertDpToPixel(24f).toInt(),
                                Utils.convertDpToPixel(24f).toInt(),
                            )
                        } else {
                            DrawableUtils.applyIntrinsicBoundsIfEmpty(loadedDrawable)
                        }

                        drawable.result = loadedDrawable

                        if (loadedDrawable is Animatable) {
                            loadedDrawable.start()
                        }
                    }
                }
            }

            override fun onStart(placeholder: coil3.Image?) {
                if (placeholder != null && drawable.isAttached) {
                    val loadedDrawable = placeholder.asDrawable(context.resources)
                    DrawableUtils.applyIntrinsicBoundsIfEmpty(loadedDrawable)
                    drawable.result = loadedDrawable
                }
            }

            override fun onError(errorDrawable: coil3.Image?) {
                if (cache.remove(drawable) != null) {
                    if (errorDrawable != null && drawable.isAttached) {
                        val loadedDrawable = errorDrawable.asDrawable(context.resources)
                        DrawableUtils.applyIntrinsicBoundsIfEmpty(loadedDrawable)
                        drawable.result = loadedDrawable
                    }
                }
            }
        }
    }
}
