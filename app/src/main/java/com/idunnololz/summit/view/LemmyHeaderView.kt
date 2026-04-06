package com.idunnololz.summit.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.size
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import com.google.android.material.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.AbsoluteCornerSize
import com.idunnololz.summit.R as R2
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getTextSizeFromTextAppearance
import com.idunnololz.summit.util.ext.spToPx
import kotlin.math.max

/**
 * |---| textView1
 * |___| textView2 textView3
 */
class LemmyHeaderView : FrameLayout {

  companion object {
    const val DEFAULT_ICON_SIZE_DP = 32f
  }

  private var originalTypeface: Typeface? = null

  private var iconImageView: ImageView? = null
  val textView1: TextView
  val textView2: TextView
  val textView3: TextView

  var multiline: Boolean = false
    set(value) {
      if (field == value) {
        return
      }

      field = value

      updateTextViewVisibility()
      requestLayout()
    }
  var showTextView2: Boolean = true
    set(value) {
      if (field == value) {
        return
      }

      field = value

      updateTextViewVisibility()
    }
  var showTextView3: Boolean = true
    set(value) {
      if (field == value) {
        return
      }

      field = value

      updateTextViewVisibility()
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

    originalTypeface = textView1.typeface

    updateTextViewVisibility()

    addView(textView1)
    addView(textView2)
    addView(textView3)
  }

  fun setTextFirstPart(text: CharSequence) {
    textView1.text = text
  }

  fun setTextSecondPart(text: CharSequence) {
    textView2.text = text
  }

  var textSize: Float = context.getTextSizeFromTextAppearance(R.attr.textAppearanceBodySmall)
    set(value) {
      field = value

      textView1.textSize = value
      textView2.textSize = value
      textView3.textSize = value

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val lineHeight = (context.spToPx(value) * 1.33f).toInt()
        textView1.lineHeight = lineHeight
        textView2.lineHeight = lineHeight
        textView3.lineHeight = lineHeight
      }
    }

  fun getIconImageView(): ImageView {
    ensureIconView()
    return iconImageView!!
  }

  fun ensureNoIconImageView() {
    if (iconImageView == null) {
      return
    }
    iconImageView?.visibility = GONE
  }

  private fun LinkifyTextView.style(): LinkifyTextView {
    maxLines = 1
    includeFontPadding = false
    isSingleLine = true
    setTextColor(context.getColorCompat(R2.color.colorTextFaint))
    TextViewCompat.setCompoundDrawableTintList(
      this,
      ColorStateList.valueOf(
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
      ),
    )
    ellipsize = TextUtils.TruncateAt.END
    gravity = Gravity.CENTER_VERTICAL
    textDirection = TEXT_DIRECTION_INHERIT

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
      iconImageView?.visibility = VISIBLE
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
      context.getColorCompat(R2.color.colorTextFaint),
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
    if (multiline || iconImageView != null) {
      val count = size

      measureIconImageView(widthMeasureSpec, heightMeasureSpec)

      val nonIconWidthMeasureSpec = getMeasureSpecForNonIconViews(widthMeasureSpec)

      var maxHeight = 0
      var maxWidth = 0
      var childState = 0

      for (i in 0..<count) {
        val child = getChildAt(i)
        if (child.visibility != GONE) {
          if (child != iconImageView) {
            measureChildWithMargins(child, nonIconWidthMeasureSpec, 0, heightMeasureSpec, 0)
          } else {
            // view is already measured
            continue
          }
          val lp = child.layoutParams as LayoutParams
          maxWidth = max(
            maxWidth,
            child.measuredWidth + lp.marginStart + lp.marginEnd,
          )
          maxHeight = max(
            maxHeight,
            child.measuredHeight + lp.topMargin + lp.bottomMargin,
          )
          childState = combineMeasuredStates(childState, child.measuredState)
        }
      }

      // Account for padding too
      maxWidth += paddingStart + paddingEnd
      maxHeight += paddingTop + paddingBottom

      // Check against our minimum height and width
      maxHeight = max(maxHeight, suggestedMinimumHeight)
      maxWidth = max(maxWidth, suggestedMinimumWidth)

      // Check against our foreground's minimum height and width
      val drawable = foreground
      if (drawable != null) {
        maxHeight = max(maxHeight, drawable.getMinimumHeight())
        maxWidth = max(maxWidth, drawable.getMinimumWidth())
      }

      setMeasuredDimension(
        resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
        resolveSizeAndState(
          maxHeight,
          heightMeasureSpec,
          childState shl MEASURED_HEIGHT_STATE_SHIFT,
        ),
      )

      val iconImageView = iconImageView
      var totalTextHeight = 0

      fun getViewHeight(view: View): Int {
        val layoutParams = view.layoutParams as LayoutParams
        return view.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin
      }

      fun getViewWidth(view: View): Int {
        val layoutParams = view.layoutParams as LayoutParams
        return view.measuredWidth + layoutParams.marginStart + layoutParams.marginEnd
      }

      if (textView1.visibility != GONE) {
        totalTextHeight += getViewHeight(textView1)
      }
      if (textView2.visibility != GONE || textView3.visibility != GONE) {
        val textView2Height =
          if (textView2.visibility != GONE) {
            getViewHeight(textView2)
          } else {
            0
          }
        val textView3Height =
          if (textView3.visibility != GONE) {
            getViewHeight(textView3)
          } else {
            0
          }
        totalTextHeight += marginBetweenLines
        totalTextHeight += max(textView2Height, textView3Height)
      }

      var viewHeight = totalTextHeight
      var viewWidth = 0
      if (iconImageView != null && iconImageView.visibility != GONE) {
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
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
  }

  private fun measureIconImageView(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val iconImageView = iconImageView ?: return

    measureChildWithMargins(iconImageView, widthMeasureSpec, 0, heightMeasureSpec, 0)
  }

  private fun getMeasureSpecForNonIconViews(widthMeasureSpec: Int): Int {
    val iconImageView = iconImageView ?: return widthMeasureSpec

    val lp = iconImageView.layoutParams as LayoutParams
    return MeasureSpec.makeMeasureSpec(
      MeasureSpec.getSize(widthMeasureSpec) -
        iconImageView.measuredWidth -
        lp.leftMargin -
        lp.rightMargin,
      MeasureSpec.getMode(widthMeasureSpec),
    )
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)

    val children = children
    val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
    val height = bottom - top
    val childSpace = height - paddingTop - paddingBottom

    if (!multiline && iconImageView == null) {
      if (isRtl) {
        var start = paddingRight
        for (child in children) {
          if (child.visibility == GONE) continue

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
        var start = paddingStart
        for (child in children) {
          if (child.visibility == GONE) continue

          val layoutParams = child.layoutParams as LayoutParams
          val childTop = (
            paddingTop + (childSpace - child.measuredHeight) / 2 +
              layoutParams.topMargin
            ) - layoutParams.bottomMargin

          start += layoutParams.marginStart
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
      // multiline

      var start = paddingStart
      val iconImageView = iconImageView
      val singleLineVisible = textView2.visibility == GONE && textView3.visibility == GONE

      if (iconImageView != null && iconImageView.visibility != GONE) {
        val child = iconImageView
        val layoutParams = child.layoutParams as LayoutParams
        val childTop = paddingTop

        start += layoutParams.marginStart
        child.layout(
          start,
          childTop,
          start + child.measuredWidth,
          childTop + child.measuredHeight,
        )
        start += child.measuredWidth + layoutParams.marginEnd
      }

      val textChildrenTotalHeight =
        if (singleLineVisible) {
          textView1.measuredHeight
        } else {
          textView1.measuredHeight + max(textView2.measuredHeight, textView3.measuredHeight) +
            marginBetweenLines
        }

      var top: Int
      if (singleLineVisible) {
        val child = textView1
        val layoutParams = child.layoutParams as LayoutParams
        top = (
          paddingTop + (childSpace - textChildrenTotalHeight) / 2 +
            layoutParams.topMargin
          ) - layoutParams.bottomMargin
        child.layout(
          start + layoutParams.marginStart,
          top,
          start + layoutParams.marginStart + child.measuredWidth,
          top + child.measuredHeight,
        )
      } else {
        run {
          val child = textView1
          val layoutParams = child.layoutParams as LayoutParams
          top = (
            paddingTop + (childSpace - textChildrenTotalHeight) / 2 +
              layoutParams.topMargin
            ) - layoutParams.bottomMargin
          child.layout(
            start + layoutParams.marginStart,
            top,
            start + layoutParams.marginStart + child.measuredWidth,
            top + child.measuredHeight,
          )
          top += child.measuredHeight + marginBetweenLines
        }
        run {
          val child = textView2
          val layoutParams = child.layoutParams as LayoutParams
          child.layout(
            start + layoutParams.marginStart,
            top + layoutParams.topMargin,
            start + child.measuredWidth + layoutParams.marginStart,
            top + child.measuredHeight,
          )
          start += child.measuredWidth + layoutParams.marginStart
        }
        run {
          val child = textView3
          val layoutParams = child.layoutParams as LayoutParams
          child.layout(
            start + layoutParams.marginStart,
            top + layoutParams.topMargin,
            start + child.measuredWidth + layoutParams.marginStart,
            top + child.measuredHeight,
          )
        }
      }
    }
  }

  override fun generateDefaultLayoutParams(): LayoutParams = LayoutParams(
    LayoutParams.WRAP_CONTENT,
    LayoutParams.WRAP_CONTENT,
  )

  private fun updateTextViewVisibility() {
    if (showTextView2) {
      textView2.visibility = VISIBLE
    } else {
      textView2.visibility = GONE
    }

    if (showTextView3) {
      textView3.visibility = VISIBLE
    } else {
      textView3.visibility = GONE
    }
  }
}
