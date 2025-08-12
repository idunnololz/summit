package com.idunnololz.summit.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.LinearLayout
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute

class LinearLayoutWithFadingEdge : LinearLayout {

  var maxHeight: Int = 0
    set(value) {
      field = value

      if (value <= 0) {
        setWillNotDraw(true)
      } else {
        prepareFadingPaintIfNeeded()
        setWillNotDraw(false)
      }
    }

  private var fadingPaintReady = false
  private val fadingEdgePaint = Paint()
  private val fadingEdgeSize = Utils.convertDpToPixel(16f)
  private val gradientColor = context.getColorFromAttribute(
    com.google.android.material.R.attr.backgroundColor,
  )
  private val shader = LinearGradient(0f, 0f, 0f, 1f, 0, gradientColor, Shader.TileMode.CLAMP)
  private val matrix = Matrix()

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr,
  )

  private fun prepareFadingPaintIfNeeded() {
    if (fadingPaintReady) return
    fadingPaintReady = true

    fadingEdgePaint.apply {
      shader = this.shader
    }
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)

    if (measuredHeight == maxHeight && maxHeight > 0) {
      shader.setLocalMatrix(
        matrix.apply {
          setScale(1f, fadingEdgeSize)
          postTranslate(0f, measuredHeight - fadingEdgeSize)
        },
      )
      fadingEdgePaint.shader = shader

      canvas.drawRect(
        0f,
        measuredHeight - fadingEdgeSize,
        measuredWidth.toFloat(),
        measuredHeight.toFloat(),
        fadingEdgePaint,
      )
    }
  }
}
