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
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getTextSizeFromTextAppearance
import com.idunnololz.summit.util.ext.spToPx
import kotlin.math.max
import com.idunnololz.summit.R as R2
import androidx.core.view.size

class LemmyHeaderView : FrameLayout {

  companion object {
    const val DEFAULT_ICON_SIZE_DP = 32f
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
      iconImageView?.visibility = View.VISIBLE
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

  private val mMatchParentChildren = ArrayList<View>()
  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    // We only support match parent width...
    if (multiline) {
      ///

      val count = size

      val measureMatchParentChildren =
        MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
          MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
      mMatchParentChildren.clear()

      measureIconImageView(widthMeasureSpec, heightMeasureSpec)

      val nonIconWidthMeasureSpec = getMeasureSpecForNonIconViews(widthMeasureSpec)

      var maxHeight = 0
      var maxWidth = 0
      var childState = 0

      for (i in 0..<count) {
        val child = getChildAt(i)
        if (child.getVisibility() != GONE) {
          if (child != iconImageView) {
            measureChildWithMargins(child, nonIconWidthMeasureSpec, 0, heightMeasureSpec, 0)
          } else {
            // view is already measured
            continue
          }
          val lp = child.getLayoutParams() as LayoutParams
          maxWidth = max(
            maxWidth,
            child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin,
          )
          maxHeight = max(
            maxHeight,
            child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin,
          )
          childState = combineMeasuredStates(childState, child.getMeasuredState())
          if (measureMatchParentChildren) {
            if (lp.width == LayoutParams.MATCH_PARENT ||
              lp.height == LayoutParams.MATCH_PARENT
            ) {
              mMatchParentChildren.add(child)
            }
          }
        }
      }

      // Account for padding too
      maxWidth += paddingLeft + paddingRight
      maxHeight += paddingTop + paddingBottom


      // Check against our minimum height and width
      maxHeight = max(maxHeight, getSuggestedMinimumHeight())
      maxWidth = max(maxWidth, getSuggestedMinimumWidth())


      // Check against our foreground's minimum height and width
      val drawable = getForeground()
      if (drawable != null) {
        maxHeight = max(maxHeight, drawable.getMinimumHeight())
        maxWidth = max(maxWidth, drawable.getMinimumWidth())
      }

      setMeasuredDimension(
        resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
        resolveSizeAndState(
          maxHeight, heightMeasureSpec,
          childState shl MEASURED_HEIGHT_STATE_SHIFT,
        ),
      )

      ///

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
      if (iconImageView != null && iconImageView.visibility != View.GONE) {
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

    val lp = iconImageView.getLayoutParams() as LayoutParams
    return MeasureSpec.makeMeasureSpec(
      MeasureSpec.getSize(widthMeasureSpec)
        - iconImageView.measuredWidth
        - lp.leftMargin
        - lp.rightMargin,
      MeasureSpec.getMode(widthMeasureSpec)
    )
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
      // multiline

      var start = paddingLeft
      val iconImageView = iconImageView

      if (iconImageView != null && iconImageView.visibility != View.GONE) {
        val child = iconImageView
        val layoutParams = child.layoutParams as LayoutParams
        val childTop = paddingTop
//          (
//          paddingTop + (childSpace - child.measuredHeight) / 2 +
//            layoutParams.topMargin
//          ) - layoutParams.bottomMargin

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
