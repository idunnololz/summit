package com.idunnololz.summit.util

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.widget.TextView
import coil.Coil.imageLoader
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import coil.target.Target
import com.idunnololz.summit.util.shimmer.newShimmerDrawableSquare
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableLoader
import io.noties.markwon.image.DrawableUtils
import io.noties.markwon.image.ImageSpanFactory
import java.util.concurrent.atomic.AtomicBoolean
import org.commonmark.node.Image

/**
 * @author Tyler Wong
 * @since 4.2.0
 */
class CoilImagesPlugin internal constructor(coilStore: CoilStore, imageLoader: ImageLoader) :
    AbstractMarkwonPlugin() {

    interface CoilStore {
        fun load(drawable: AsyncDrawable): ImageRequest
        fun cancel(disposable: Disposable)
    }

    private val coilAsyncDrawableLoader: CoilAsyncDrawableLoader

    init {
        coilAsyncDrawableLoader = CoilAsyncDrawableLoader(coilStore, imageLoader)
    }

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
        private val coilStore: CoilStore,
        private val imageLoader: ImageLoader,
    ) : AsyncDrawableLoader() {
        private val cache: MutableMap<AsyncDrawable, Disposable?> = HashMap(2)
        override fun load(drawable: AsyncDrawable) {
            val loaded = AtomicBoolean(false)
            val target: Target = AsyncDrawableTarget(drawable, loaded, drawable.destination)
            val request = coilStore.load(drawable).newBuilder()
                .target(target)
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
        ) : Target {
            override fun onSuccess(loadedDrawable: Drawable) {
                // @since 4.5.1 check finished flag (result can be delivered _before_ disposable is created)
                if (cache.remove(drawable) != null ||
                    !loaded.get()
                ) {
                    // mark
                    loaded.set(true)
                    if (drawable.isAttached) {
                        DrawableUtils.applyIntrinsicBoundsIfEmpty(loadedDrawable)
                        drawable.result = loadedDrawable

                        if (loadedDrawable is Animatable) {
                            loadedDrawable.start()
                        }
                    }
                }
            }

            override fun onStart(placeholder: Drawable?) {
                if (placeholder != null && drawable.isAttached) {
                    DrawableUtils.applyIntrinsicBoundsIfEmpty(placeholder)
                    drawable.result = placeholder
                }
            }

            override fun onError(errorDrawable: Drawable?) {
                if (cache.remove(drawable) != null) {
                    if (errorDrawable != null && drawable.isAttached) {
                        DrawableUtils.applyIntrinsicBoundsIfEmpty(errorDrawable)
                        drawable.result = errorDrawable
                    }
                }
            }
        }
    }

    companion object {
        fun create(context: Context): CoilImagesPlugin {
            return create(
                object : CoilStore {
                    override fun load(drawable: AsyncDrawable): ImageRequest {
                        return ImageRequest.Builder(context)
                            .data(drawable.destination)
                            .build()
                    }

                    override fun cancel(disposable: Disposable) {
                        disposable.dispose()
                    }
                },
                imageLoader(context),
            )
        }

        fun create(context: Context, imageLoader: ImageLoader): CoilImagesPlugin {
            return create(
                object : CoilStore {
                    override fun load(drawable: AsyncDrawable): ImageRequest {
                        return ImageRequest.Builder(context)
                            .allowHardware(false) // Needed for the "take screenshot" feature
                            .data(drawable.destination)
                            .placeholder(newShimmerDrawableSquare(context))
                            .build()
                    }

                    override fun cancel(disposable: Disposable) {
                        disposable.dispose()
                    }
                },
                imageLoader,
            )
        }

        fun create(coilStore: CoilStore, imageLoader: ImageLoader): CoilImagesPlugin {
            return CoilImagesPlugin(coilStore, imageLoader)
        }
    }
}
