/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.views.text.internal.span

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * Sets an absolute text size in pixels, like [android.text.style.AbsoluteSizeSpan], but accepts a
 * fractional size so text can be measured and drawn at the exact `fontSize x density` value.
 */
internal class ReactAbsoluteSizeSpan(val size: Float) : MetricAffectingSpan(), ReactSpan {

  override fun updateDrawState(ds: TextPaint) {
    ds.textSize = size
  }

  override fun updateMeasureState(paint: TextPaint) {
    paint.textSize = size
  }
}
