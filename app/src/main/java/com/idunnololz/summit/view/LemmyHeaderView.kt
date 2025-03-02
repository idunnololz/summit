package com.idunnololz.summit.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import com.google.android.material.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.AbsoluteCornerSize
import com.idunnololz.summit.R as R2
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.convertSpToPixel
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import kotlin.math.max

class LemmyHeaderView : FrameLayout {

    companion object {
        const val DEFAULT_ICON_SIZE_DP = 30f
    }

    private var originalTypeface: Typeface? = null

    private var iconImageView: ImageView? = null
    val textView1: TextView
    val textView2: TextView
    val textView3: TextView
    private val flairView: FlairView

    var multiline: Boolean = false
        set(value) {
            if (field == value) {
                return
            }

            field = value

            requestLayout()
        }

    var iconSize = Utils.convertDpToPixel(DEFAULT_ICON_SIZE_DP).toInt()
    private val marginBetweenLines = Utils.convertDpToPixel(4f).toInt()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    init {
        textView1 = LinkifyTextView(context, null, R.attr.textAppearanceBodySmall)
            .style()
        textView2 = LinkifyTextView(context, null, R.attr.textAppearanceBodySmall)
            .style()
        textView3 = LinkifyTextView(context, null, R.attr.textAppearanceBodySmall)
            .style()
        flairView = FlairView(context)

        originalTypeface = textView1.typeface

        addView(textView1)
        addView(flairView)
        addView(textView2)
        addView(textView3)

        flairView.visibility = View.GONE
    }

    fun setTextFirstPart(text: CharSequence) {
        textView1.text = text
    }

    fun setTextSecondPart(text: CharSequence) {
        textView2.text = text
    }

    var textSize: Float = 0f
        set(value) {
            field = value

            textView1.textSize = value
            textView2.textSize = value
            textView3.textSize = value

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val lineHeight = (convertSpToPixel(value) * 1.33f).toInt()
                textView1.lineHeight = lineHeight
                textView2.lineHeight = lineHeight
                textView3.lineHeight = lineHeight
            }
        }

    fun getFlairView(): FlairView = flairView

    fun getIconImageView(): ImageView {
        ensureIconView()
        return iconImageView!!
    }

    fun ensureNoIconImageView() {
        if (iconImageView == null) {
            return
        }
        iconImageView?.visibility = View.GONE
    }

    private fun LinkifyTextView.style(): LinkifyTextView {
        maxLines = 1
        includeFontPadding = false
//        isSingleLine = true
        setTextColor(context.getColorCompat(R2.color.colorTextFaint))
        TextViewCompat.setCompoundDrawableTintList(
            this,
            ColorStateList.valueOf(
                context.getColorFromAttribute(R.attr.colorControlNormal),
            ),
        )
        ellipsize = TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL

        return this
    }

    fun dim(dim: Boolean) {
        val alpha = if (dim) {
            0.8f
        } else {
            1f
        }

        textView1.alpha = alpha
        textView2.alpha = alpha
        textView3.alpha = alpha
    }

    fun setTypeface(typeface: Typeface?) {
        if (typeface == null) {
            textView1.typeface = originalTypeface
        } else {
            textView1.typeface = typeface
        }
    }

    private fun ensureIconView() {
        if (iconImageView != null) {
            return
        }

        val strokeWidth = Utils.convertDpToPixel(1f)
        val strokeWidthHalf = (strokeWidth / 2f).toInt()
        val cornerSize = Utils.convertDpToPixel(8f)

        val iconImageView = ShapeableImageView(context, null, R2.style.RoundImageView)
        addView(iconImageView, 0)
        iconImageView.shapeAppearanceModel = iconImageView.shapeAppearanceModel
            .toBuilder()
            .setAllCornerSizes(AbsoluteCornerSize(cornerSize))
            .build()
        iconImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        iconImageView.strokeWidth = strokeWidth
        iconImageView.strokeColor = ColorStateList.valueOf(
            context.getColorFromAttribute(R.attr.colorOnSurface),
        )
        iconImageView.setPadding(strokeWidthHalf, strokeWidthHalf, strokeWidthHalf, strokeWidthHalf)

        iconImageView.updateLayoutParams<LayoutParams> {
            width = iconSize
            height = iconSize
            marginEnd = Utils.convertDpToPixel(8f).toInt()
        }
        this.iconImageView = iconImageView
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (multiline) {
            val iconImageView = iconImageView
            var totalTextHeight = 0

            fun getViewHeight(view: View): Int {
                val layoutParams = view.layoutParams as LayoutParams
                return view.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin
            }

            fun getViewWidth(view: View): Int {
                val layoutParams = view.layoutParams as LayoutParams
                return view.measuredWidth + layoutParams.leftMargin + layoutParams.rightMargin
            }

            if (textView1.visibility != View.GONE) {
                totalTextHeight += getViewHeight(textView1)
            }
            if (textView2.visibility != View.GONE || textView3.visibility != View.GONE) {
                val textView2Height =
                    if (textView2.visibility != View.GONE) {
                        getViewHeight(textView2)
                    } else {
                        0
                    }
                val textView3Height =
                    if (textView3.visibility != View.GONE) {
                        getViewHeight(textView3)
                    } else {
                        0

                    }
                totalTextHeight += marginBetweenLines
                totalTextHeight += max(textView2Height, textView3Height)
            }

            var viewHeight = totalTextHeight
            var viewWidth = 0
            if (iconImageView != null) {
                val iconImageViewHeight = getViewHeight(iconImageView)
                viewHeight = max(viewHeight, iconImageViewHeight)

                viewWidth += getViewWidth(iconImageView)
            }

            viewWidth += max(
                getViewWidth(textView1),
                getViewWidth(textView2) + getViewWidth(textView3),
            )

            setMeasuredDimension(
                viewWidth + paddingStart + paddingEnd,
                viewHeight + paddingTop + paddingBottom,
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val children = children
        val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val height = bottom - top
        val childSpace = height - paddingTop - paddingBottom

        if (!multiline) {
            if (isRtl) {
                var start = paddingRight
                for (child in children) {
                    if (child.visibility == View.GONE) continue

                    val layoutParams = child.layoutParams as LayoutParams
                    val childTop = (
                        paddingTop + (childSpace - child.measuredHeight) / 2 +
                            layoutParams.topMargin
                        ) - layoutParams.bottomMargin

                    start -= layoutParams.rightMargin
                    child.layout(
                        start - child.measuredWidth,
                        childTop,
                        start,
                        childTop + child.measuredHeight,
                    )
                    start -= child.measuredWidth - layoutParams.rightMargin
                }
            } else {
                var start = paddingLeft
                for (child in children) {
                    if (child.visibility == View.GONE) continue

                    val layoutParams = child.layoutParams as LayoutParams
                    val childTop = (
                        paddingTop + (childSpace - child.measuredHeight) / 2 +
                            layoutParams.topMargin
                        ) - layoutParams.bottomMargin

                    start += layoutParams.leftMargin
                    child.layout(
                        start,
                        childTop,
                        start + child.measuredWidth,
                        childTop + child.measuredHeight,
                    )
                    start += child.measuredWidth + layoutParams.rightMargin
                }
            }
        } else {
            var start = paddingLeft
            val iconImageView = iconImageView

            if (iconImageView != null) {
                val child = iconImageView
                val layoutParams = child.layoutParams as LayoutParams
                val childTop = (
                    paddingTop + (childSpace - child.measuredHeight) / 2 +
                        layoutParams.topMargin
                    ) - layoutParams.bottomMargin

                start += layoutParams.leftMargin
                child.layout(
                    start,
                    childTop,
                    start + child.measuredWidth,
                    childTop + child.measuredHeight,
                )
                start += child.measuredWidth + layoutParams.rightMargin
            }

            val textChildrenTotalHeight =
                textView1.measuredHeight + max(textView2.measuredHeight, textView3.measuredHeight) +
                    marginBetweenLines

            var top: Int
            run {
                val child = textView1
                val layoutParams = child.layoutParams as LayoutParams
                top = (
                    paddingTop + (childSpace - textChildrenTotalHeight) / 2 +
                        layoutParams.topMargin
                    ) - layoutParams.bottomMargin
                child.layout(
                    start + layoutParams.leftMargin,
                    top,
                    start + layoutParams.leftMargin + child.measuredWidth,
                    top + child.measuredHeight,
                )
                top += child.measuredHeight + marginBetweenLines
            }
            run {
                val child = textView2
                val layoutParams = child.layoutParams as LayoutParams
                child.layout(
                    start + layoutParams.leftMargin,
                    top + layoutParams.topMargin,
                    start + child.measuredWidth + layoutParams.leftMargin,
                    top + child.measuredHeight,
                )
                start += child.measuredWidth + layoutParams.leftMargin
            }
            run {
                val child = textView3
                val layoutParams = child.layoutParams as LayoutParams
                child.layout(
                    start + layoutParams.leftMargin,
                    top + layoutParams.topMargin,
                    start + child.measuredWidth + layoutParams.leftMargin,
                    top + child.measuredHeight,
                )
            }
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT,
    )
}
