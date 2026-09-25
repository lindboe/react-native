/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.uiapp.component

import android.util.Log
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import com.facebook.react.uimanager.PixelUtil
import com.facebook.react.uimanager.ThemedReactContext

/**
 * Verification helper: a plain Android TextView rendering text at an exact pixel size and integer
 * line height, used as the reference to compare React Native text against.
 */
internal class NativeTextReferenceView(context: ThemedReactContext) : AppCompatTextView(context) {
  fun setTextSizePx(sizePx: Float) {
    setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
    logMeasurements()
  }

  fun setLineHeightPx(lineHeightPx: Int) {
    lineHeight = lineHeightPx
  }

  fun setReferenceText(value: String) {
    text = value
    logMeasurements()
  }

  private fun logMeasurements() {
    val string = text?.toString() ?: return
    Log.i(
        "NativeTextReference",
        "PixelUtil.toPixelFromSP(13)=${PixelUtil.toPixelFromSP(13f)} " +
            "toPixelFromSP(18)=${PixelUtil.toPixelFromSP(18f)} " +
            "toPixelFromDIP(13)=${PixelUtil.toPixelFromDIP(13f)} " +
            "fontScale=${resources.configuration.fontScale} density=${resources.displayMetrics.density}",
    )
    val firstLine = string.lineSequence().firstOrNull() ?: return
    val paint = android.text.TextPaint(paint)
    val sp = PixelUtil.toPixelFromSP(13f)
    val sizes =
        listOf(
            textSize,
            Math.ceil(textSize.toDouble()).toFloat(),
            sp,
            Math.ceil(sp.toDouble()).toFloat(),
        )
    for (size in sizes) {
      paint.textSize = size
      Log.i(
          "NativeTextReference",
          "typeface=${paint.typeface} textSize=$size measureText(\"$firstLine\")=${paint.measureText(firstLine)}",
      )
    }
  }
}
