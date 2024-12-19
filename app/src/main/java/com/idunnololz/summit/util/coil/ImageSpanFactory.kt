package com.idunnololz.summit.util.coil

import android.util.Log
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.SpanFactory
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.ImageProps

class ImageSpanFactory : SpanFactory {
    override fun getSpans(configuration: MarkwonConfiguration, props: RenderProps): Any {
        return AsyncDrawableSpan(
            configuration.theme(),
            AsyncDrawable(
                ImageProps.DESTINATION.require(props),
                configuration.asyncDrawableLoader(),
                configuration.imageSizeResolver(),
                ImageProps.IMAGE_SIZE[props],
            ),
            AsyncDrawableSpan.ALIGN_BOTTOM,
            ImageProps.REPLACEMENT_TEXT_IS_LINK[props, false],
        )
    }
}
