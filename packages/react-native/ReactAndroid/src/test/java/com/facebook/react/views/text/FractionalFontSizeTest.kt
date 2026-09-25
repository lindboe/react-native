/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.views.text

import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.util.DisplayMetrics
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.internal.featureflags.ReactNativeFeatureFlags
import com.facebook.react.internal.featureflags.ReactNativeFeatureFlagsDefaults
import com.facebook.react.internal.featureflags.ReactNativeFeatureFlagsForTests
import com.facebook.react.uimanager.DisplayMetricsHolder
import com.facebook.react.uimanager.ReactStylesDiffMap
import com.facebook.react.views.text.internal.span.CustomLineHeightSpan
import com.facebook.react.views.text.internal.span.ReactAbsoluteSizeSpan
import com.facebook.yoga.YogaMeasureMode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * Covers the `enableFractionalFontSizeAndroid` feature flag: with the flag off every font size is
 * rounded up to a whole pixel (the historical behaviour), with the flag on the unrounded `fontSize
 * x fontScale x density` value is used everywhere.
 *
 * Density 2.625 (420 dpi, Pixel 4a through Pixel 9) is used because integer densities hide the
 * rounding: `fontSize: 13` is 34.125 px there, which used to render as 35 px.
 */
@RunWith(RobolectricTestRunner::class)
class FractionalFontSizeTest {

  @After
  fun tearDown() {
    ReactNativeFeatureFlags.dangerouslyReset()
    DisplayMetricsHolder.setScreenDisplayMetrics(null)
  }

  private fun setFlag(enabled: Boolean) {
    ReactNativeFeatureFlagsForTests.setUp()
    ReactNativeFeatureFlags.override(
        object : ReactNativeFeatureFlagsDefaults() {
          override fun enableFractionalFontSizeAndroid(): Boolean = enabled
        },
    )
  }

  private fun setDensity(density: Float, fontScale: Float = 1f) {
    DisplayMetricsHolder.setScreenDisplayMetrics(
        DisplayMetrics().apply {
          this.density = density
          this.scaledDensity = density * fontScale
        },
    )
  }

  private fun propsFor(vararg keysAndValues: Any): TextAttributeProps =
      TextAttributeProps.fromReadableMap(ReactStylesDiffMap(JavaOnlyMap.of(*keysAndValues)))

  // --- TextAttributeProps (Fabric measurement and rendering) ---

  @Test
  fun `TextAttributeProps keeps the fractional pixel size with the flag on`() {
    setFlag(true)
    setDensity(2.625f)

    assertThat(propsFor("fontSize", 13.0).fontSize).isEqualTo(34.125f)
  }

  @Test
  fun `TextAttributeProps rounds the pixel size up with the flag off`() {
    setFlag(false)
    setDensity(2.625f)

    assertThat(propsFor("fontSize", 13.0).fontSize).isEqualTo(35f)
  }

  @Test
  fun `TextAttributeProps rounds up with allowFontScaling off and the flag off`() {
    setFlag(false)
    setDensity(2.625f, fontScale = 1.3f)

    assertThat(propsFor("fontSize", 13.0, "allowFontScaling", false).fontSize).isEqualTo(35f)
  }

  @Test
  fun `TextAttributeProps ignores the font scale with allowFontScaling off`() {
    setFlag(true)
    setDensity(2.625f, fontScale = 1.3f)

    assertThat(propsFor("fontSize", 13.0, "allowFontScaling", false).fontSize).isEqualTo(34.125f)
  }

  @Test
  fun `TextAttributeProps applies the font scale without rounding with the flag on`() {
    setFlag(true)
    setDensity(2.625f, fontScale = 1.3f)

    // 13 x 1.3 x 2.625
    assertThat(propsFor("fontSize", 13.0).fontSize).isCloseTo(44.3625f, within(0.001f))
  }

  @Test
  fun `TextAttributeProps still caps the font scale with maxFontSizeMultiplier`() {
    setFlag(true)
    setDensity(2.625f, fontScale = 1.3f)

    // 13 x 1.1 x 2.625
    assertThat(propsFor("fontSize", 13.0, "maxFontSizeMultiplier", 1.1).fontSize)
        .isCloseTo(37.5375f, within(0.001f))
  }

  @Test
  fun `TextAttributeProps letter spacing em conversion uses the fractional size`() {
    setFlag(true)
    setDensity(2.625f)

    // letterSpacing: 1 => 2.625 px, divided by the 34.125 px font size.
    assertThat(propsFor("fontSize", 13.0, "letterSpacing", 1.0).letterSpacing)
        .isCloseTo(2.625f / 34.125f, within(1e-6f))
  }

  @Test
  fun `TextAttributeProps letter spacing em conversion uses the rounded size with the flag off`() {
    setFlag(false)
    setDensity(2.625f)

    assertThat(propsFor("fontSize", 13.0, "letterSpacing", 1.0).letterSpacing)
        .isCloseTo(2.625f / 35f, within(1e-6f))
  }

  // --- TextAttributes (TextInput) ---

  @Test
  fun `TextAttributes effectiveFontSize keeps the fractional pixel size with the flag on`() {
    setFlag(true)
    setDensity(2.625f)

    val attributes = TextAttributes().apply { fontSize = 13f }

    assertThat(attributes.effectiveFontSize).isEqualTo(34.125f)
    assertThat(attributes.apply { letterSpacing = 1f }.effectiveLetterSpacing)
        .isCloseTo(2.625f / 34.125f, within(1e-6f))
  }

  @Test
  fun `TextAttributes effectiveFontSize rounds up with the flag off`() {
    setFlag(false)
    setDensity(2.625f)

    val attributes = TextAttributes().apply { fontSize = 13f }

    assertThat(attributes.effectiveFontSize).isEqualTo(35f)
    assertThat(attributes.apply { letterSpacing = 1f }.effectiveLetterSpacing)
        .isCloseTo(2.625f / 35f, within(1e-6f))
  }

  // --- ReactTextView (ellipsis workaround must use the same size as the spans) ---

  @Test
  fun `ReactTextView setFontSize applies the fractional size with the flag on`() {
    setFlag(true)
    setDensity(2.625f)

    val view = ReactTextView(RuntimeEnvironment.getApplication())
    view.setFontSize(13f)

    assertThat(view.textSize).isEqualTo(34.125f)
  }

  @Test
  fun `ReactTextView setFontSize rounds up with the flag off`() {
    setFlag(false)
    setDensity(2.625f)

    val view = ReactTextView(RuntimeEnvironment.getApplication())
    view.setFontSize(13f)

    assertThat(view.textSize).isEqualTo(35f)
  }

  // --- Spans ---

  @Test
  fun `ReactAbsoluteSizeSpan sets the exact float size when measuring and drawing`() {
    val span = ReactAbsoluteSizeSpan(34.125f)

    val measurePaint = TextPaint().apply { textSize = 12f }
    span.updateMeasureState(measurePaint)
    assertThat(measurePaint.textSize).isEqualTo(34.125f)

    val drawPaint = TextPaint().apply { textSize = 12f }
    span.updateDrawState(drawPaint)
    assertThat(drawPaint.textSize).isEqualTo(34.125f)
  }

  @Test
  fun `CustomLineHeightSpan rounds to nearest with the flag on`() {
    setFlag(true)

    assertThat(CustomLineHeightSpan(47.25f).lineHeight).isEqualTo(47)
    assertThat(CustomLineHeightSpan(47.5f).lineHeight).isEqualTo(48)
    assertThat(CustomLineHeightSpan(47.75f).lineHeight).isEqualTo(48)
  }

  @Test
  fun `CustomLineHeightSpan rounds up with the flag off`() {
    setFlag(false)

    assertThat(CustomLineHeightSpan(47.25f).lineHeight).isEqualTo(48)
    assertThat(CustomLineHeightSpan(47.5f).lineHeight).isEqualTo(48)
    assertThat(CustomLineHeightSpan(47.75f).lineHeight).isEqualTo(48)
  }

  // --- adjustsFontSizeToFit ---

  /**
   * Runs the shrink loop against a height constraint (the loop only checks width for single
   * character text) and checks the invariants that must hold after it.
   */
  private fun shrinkToFit(
      text: SpannableString,
      width: Float,
      height: Float,
      minimumFontSize: Float,
  ) {
    val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
    TextLayoutManager.adjustSpannableFontToFit(
        text,
        width,
        YogaMeasureMode.EXACTLY,
        height,
        YogaMeasureMode.EXACTLY,
        minimumFontSize,
        0,
        true,
        Layout.BREAK_STRATEGY_HIGH_QUALITY,
        Layout.HYPHENATION_FREQUENCY_NONE,
        Layout.Alignment.ALIGN_NORMAL,
        Layout.JUSTIFICATION_MODE_NONE,
        paint,
    )
    val spanSizes = text.getSpans(0, text.length, ReactAbsoluteSizeSpan::class.java).map { it.size }
    assertThat(spanSizes).isNotEmpty
    assertThat(spanSizes).allMatch { it >= minimumFontSize }
    assertThat(paint.textSize).isGreaterThanOrEqualTo(minimumFontSize)
  }

  private fun singleSpanSize(text: SpannableString): Float =
      text.getSpans(0, text.length, ReactAbsoluteSizeSpan::class.java).single().size

  // Native graphics so that text metrics scale with the font size.
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  @Test
  fun `adjustsFontSizeToFit shrinks to a size that fits with the flag on`() {
    setFlag(true)
    setDensity(2.625f)

    val text = SpannableString("Hello")
    text.setSpan(ReactAbsoluteSizeSpan(34.125f), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    shrinkToFit(text, width = 200f, height = 20f, minimumFontSize = 4f)

    assertThat(singleSpanSize(text)).isLessThan(34.125f)
    assertThat(singleSpanSize(text)).isGreaterThan(4f)
  }

  @Test
  fun `adjustsFontSizeToFit keeps the exact original size when the text already fits`() {
    setFlag(true)
    setDensity(2.625f)

    val text = SpannableString("Hello")
    text.setSpan(ReactAbsoluteSizeSpan(34.125f), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    shrinkToFit(text, width = 200f, height = 200f, minimumFontSize = 4f)

    // Scaling must not accumulate float error across iterations: 34.125, not 34.125004.
    assertThat(singleSpanSize(text)).isEqualTo(34.125f)
  }

  @Test
  fun `adjustsFontSizeToFit never goes below minimumFontSize with the flag on`() {
    setFlag(true)
    setDensity(2.625f)

    val text = SpannableString("Hello")
    text.setSpan(ReactAbsoluteSizeSpan(34.125f), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    shrinkToFit(text, width = 200f, height = 1f, minimumFontSize = 12f)

    assertThat(singleSpanSize(text)).isEqualTo(12f)
  }

  // Native graphics so that text metrics scale with the font size.
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  @Test
  fun `adjustsFontSizeToFit keeps nested spans proportional with the flag on`() {
    setFlag(true)
    setDensity(2.625f)

    val text = SpannableString("Big and small")
    text.setSpan(ReactAbsoluteSizeSpan(34.125f), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    text.setSpan(ReactAbsoluteSizeSpan(17.0625f), 3, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    shrinkToFit(text, width = 200f, height = 30f, minimumFontSize = 1f)

    val sizes =
        text
            .getSpans(0, text.length, ReactAbsoluteSizeSpan::class.java)
            .sortedBy { text.getSpanStart(it) }
            .map { it.size }
    assertThat(sizes).hasSize(2)
    assertThat(sizes[0]).isLessThan(34.125f)
    assertThat(sizes[0] / sizes[1]).isCloseTo(2f, within(0.001f))
  }

  // Native graphics so that text metrics scale with the font size.
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  @Test
  fun `adjustsFontSizeToFit truncates to whole pixels with the flag off`() {
    setFlag(false)
    setDensity(2.625f)

    val text = SpannableString("Hello")
    text.setSpan(ReactAbsoluteSizeSpan(35f), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    shrinkToFit(text, width = 200f, height = 20f, minimumFontSize = 4f)

    val size = singleSpanSize(text)
    assertThat(size).isLessThan(35f)
    assertThat(size).isEqualTo(size.toInt().toFloat())
  }
}
