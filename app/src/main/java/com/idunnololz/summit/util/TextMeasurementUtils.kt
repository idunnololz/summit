package com.idunnololz.summit.util

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextPaint
import android.widget.TextView

object TextMeasurementUtils {
    /**
     * Split text into lines using specified parameters and the same algorithm
     * as used by the [TextView] component
     *
     * @param text   the text to split
     * @param params the measurement parameters
     * @return
     */
    fun getTextLines(text: CharSequence, params: TextMeasurementParams): List<CharSequence> {
        val layout: StaticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = StaticLayout.Builder
                .obtain(text, 0, text.length, params.textPaint!!, params.width)
                .setAlignment(params.alignment!!)
                .setLineSpacing(params.lineSpacingExtra, params.lineSpacingMultiplier)
                .setIncludePad(params.includeFontPadding)
                .setBreakStrategy(params.breakStrategy)
                .setHyphenationFrequency(params.hyphenationFrequency)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setJustificationMode(params.justificationMode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setUseLineSpacingFromFallbacks(params.useFallbackLineSpacing)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setTextDirection(
                    (params.textDirectionHeuristic as TextDirectionHeuristic?)!!,
                )
            }
            builder.build()
        } else {
            StaticLayout(
                text,
                params.textPaint,
                params.width,
                params.alignment,
                params.lineSpacingMultiplier,
                params.lineSpacingExtra,
                params.includeFontPadding,
            )
        }
        val result: MutableList<CharSequence> = ArrayList()
        for (i in 0 until layout.lineCount) {
            result.add(layout.text.subSequence(layout.getLineStart(i), layout.getLineEnd(i)))
        }
        return result
    }

    fun <T> requireNonNull(obj: T?): T {
        if (obj == null) throw NullPointerException()
        return obj
    }

    /**
     * The text measurement parameters
     */
    class TextMeasurementParams private constructor(builder: Builder) {
        val textPaint: TextPaint?
        val alignment: Layout.Alignment?
        val lineSpacingExtra: Float
        val lineSpacingMultiplier: Float
        val includeFontPadding: Boolean
        val breakStrategy: Int
        val hyphenationFrequency: Int
        val justificationMode: Int
        val useFallbackLineSpacing: Boolean
        val textDirectionHeuristic: Any?
        val width: Int

        init {
            textPaint = requireNonNull<TextPaint?>(builder.textPaint)
            alignment = requireNonNull<Layout.Alignment?>(builder.alignment)
            lineSpacingExtra = builder.lineSpacingExtra
            lineSpacingMultiplier = builder.lineSpacingMultiplier
            includeFontPadding = builder.includeFontPadding
            breakStrategy = builder.breakStrategy
            hyphenationFrequency = builder.hyphenationFrequency
            justificationMode = builder.justificationMode
            useFallbackLineSpacing = builder.useFallbackLineSpacing
            textDirectionHeuristic = builder.textDirectionHeuristic
            width = builder.width
        }

        class Builder {
            var textPaint: TextPaint? = null
            var alignment: Layout.Alignment? = null
            var lineSpacingExtra = 0f
            var lineSpacingMultiplier = 1.0f
            var includeFontPadding = true
            var breakStrategy = 0
            var hyphenationFrequency = 0
            var justificationMode = 0
            var useFallbackLineSpacing = false
            var textDirectionHeuristic: Any? = null
            var width = 0

            constructor()
            constructor(copy: TextMeasurementParams) {
                textPaint = copy.textPaint
                alignment = copy.alignment
                lineSpacingExtra = copy.lineSpacingExtra
                lineSpacingMultiplier = copy.lineSpacingMultiplier
                includeFontPadding = copy.includeFontPadding
                breakStrategy = copy.breakStrategy
                hyphenationFrequency = copy.hyphenationFrequency
                justificationMode = copy.justificationMode
                useFallbackLineSpacing = copy.useFallbackLineSpacing
                textDirectionHeuristic = copy.textDirectionHeuristic
                width = copy.width
            }

            fun textPaint(`val`: TextPaint?): Builder {
                textPaint = `val`
                return this
            }

            fun alignment(`val`: Layout.Alignment?): Builder {
                alignment = `val`
                return this
            }

            fun lineSpacingExtra(`val`: Float): Builder {
                lineSpacingExtra = `val`
                return this
            }

            fun lineSpacingMultiplier(`val`: Float): Builder {
                lineSpacingMultiplier = `val`
                return this
            }

            fun includeFontPadding(`val`: Boolean): Builder {
                includeFontPadding = `val`
                return this
            }

            fun breakStrategy(`val`: Int): Builder {
                breakStrategy = `val`
                return this
            }

            fun hyphenationFrequency(`val`: Int): Builder {
                hyphenationFrequency = `val`
                return this
            }

            fun justificationMode(`val`: Int): Builder {
                justificationMode = `val`
                return this
            }

            fun useFallbackLineSpacing(`val`: Boolean): Builder {
                useFallbackLineSpacing = `val`
                return this
            }

            fun textDirectionHeuristic(`val`: Any?): Builder {
                textDirectionHeuristic = `val`
                return this
            }

            fun width(`val`: Int): Builder {
                width = `val`
                return this
            }

            fun build(): TextMeasurementParams {
                return TextMeasurementParams(this)
            }

            companion object {
                fun from(view: TextView): Builder {
                    val layout = view.layout
                    val result = Builder()
                        .textPaint(layout.paint)
                        .alignment(layout.alignment)
                        .width(
                            view.width -
                                view.compoundPaddingLeft - view.compoundPaddingRight,
                        )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        result.lineSpacingExtra(view.lineSpacingExtra)
                            .lineSpacingMultiplier(view.lineSpacingMultiplier)
                            .includeFontPadding(view.includeFontPadding)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            result.breakStrategy(view.breakStrategy)
                                .hyphenationFrequency(view.hyphenationFrequency)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            result.justificationMode(view.justificationMode)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            result.useFallbackLineSpacing(view.isFallbackLineSpacing)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            result.textDirectionHeuristic(view.textDirectionHeuristic)
                        }
                    }
                    return result
                }
            }
        }
    }
}
