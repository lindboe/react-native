/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// AccessibilityEvent.obtain is deprecated, but its replacement needs API 30.
@file:Suppress("DEPRECATION")

package com.facebook.react.views.scroll

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.react.R
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactTestHelper.createTestReactApplicationContext
import com.facebook.react.internal.featureflags.ReactNativeFeatureFlagsForTests
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.testutils.shadows.ShadowSoLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSoLoader::class])
class ReactScrollViewAccessibilityDelegateTest {

  private lateinit var context: ReactApplicationContext
  private lateinit var themedContext: ThemedReactContext

  @Before
  fun setup() {
    ReactNativeFeatureFlagsForTests.setUp()
    context = createTestReactApplicationContext()
    themedContext = ThemedReactContext(context, context, null, -1)
  }

  private fun item(itemIndex: Int): View =
      FrameLayout(themedContext).apply {
        setTag(R.id.accessibility_collection_item, JavaOnlyMap.of("itemIndex", itemIndex))
      }

  /**
   * Adds [count] items to [scrollView], each [itemSize] long along the scroll axis and 100px across
   * it, and lays the scroll view out with a viewport of [viewportSize] along the axis.
   */
  private fun <T : ViewGroup> collection(
      scrollView: T,
      horizontal: Boolean,
      count: Int,
      itemSize: Int,
      viewportSize: Int,
  ): T {
    val content = FrameLayout(themedContext)
    val items = (0..<count).map { item(it) }
    items.forEach { content.addView(it) }
    scrollView.addView(content)
    scrollView.setTag(R.id.accessibility_collection, JavaOnlyMap.of("itemCount", count))
    if (horizontal) {
      scrollView.layout(0, 0, viewportSize, 100)
      content.layout(0, 0, count * itemSize, 100)
      items.forEachIndexed { i, it -> it.layout(i * itemSize, 0, (i + 1) * itemSize, 100) }
    } else {
      scrollView.layout(0, 0, 100, viewportSize)
      content.layout(0, 0, 100, count * itemSize)
      items.forEachIndexed { i, it -> it.layout(0, i * itemSize, 100, (i + 1) * itemSize) }
    }
    return scrollView
  }

  private fun scrollEventOf(scrollView: View): AccessibilityEvent {
    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED)
    scrollView.onInitializeAccessibilityEvent(event)
    return event
  }

  @Test
  fun verticalScrollEvent_reportsTheFullyVisibleItems() {
    // Items 0 to 3 fill the 200px viewport; item 4 is just below it.
    val scrollView = collection(ReactScrollView(themedContext), false, 10, 50, 200)

    val event = scrollEventOf(scrollView)

    assertThat(event.fromIndex).isEqualTo(0)
    assertThat(event.toIndex).isEqualTo(3)
    assertThat(event.itemCount).isEqualTo(10)
  }

  @Test
  fun verticalScrollEvent_includesPartlyVisibleItemsAtBothEdges() {
    // Shows the bottom half of item 0, items 1 to 3, and the top half of item 4.
    val scrollView = collection(ReactScrollView(themedContext), false, 10, 50, 200)
    scrollView.scrollTo(0, 25)

    val event = scrollEventOf(scrollView)

    assertThat(event.fromIndex).isEqualTo(0)
    assertThat(event.toIndex).isEqualTo(4)
  }

  @Test
  fun verticalScrollEvent_reportsTheItemsAtTheEndOfTheList() {
    val scrollView = collection(ReactScrollView(themedContext), false, 10, 50, 200)
    scrollView.scrollTo(0, 300)

    val event = scrollEventOf(scrollView)

    assertThat(event.fromIndex).isEqualTo(6)
    assertThat(event.toIndex).isEqualTo(9)
  }

  @Test
  fun horizontalScrollEvent_reportsTheFullyVisibleItems() {
    val scrollView = collection(ReactHorizontalScrollView(themedContext), true, 10, 50, 200)

    val event = scrollEventOf(scrollView)

    assertThat(event.fromIndex).isEqualTo(0)
    assertThat(event.toIndex).isEqualTo(3)
  }

  @Test
  fun horizontalScrollEvent_includesPartlyVisibleItemsAtBothEdges() {
    val scrollView = collection(ReactHorizontalScrollView(themedContext), true, 10, 50, 200)
    scrollView.scrollTo(25, 0)

    val event = scrollEventOf(scrollView)

    assertThat(event.fromIndex).isEqualTo(0)
    assertThat(event.toIndex).isEqualTo(4)
  }

  private fun nodeInfoOf(scrollView: View): AccessibilityNodeInfoCompat =
      AccessibilityNodeInfoCompat.wrap(scrollView.createAccessibilityNodeInfo())

  @Test
  fun collectionInfo_isTakenFromTheCollectionMap() {
    val scrollView = ReactScrollView(themedContext)
    scrollView.setTag(
        R.id.accessibility_collection,
        JavaOnlyMap.of("rowCount", 40, "columnCount", 1, "hierarchical", true, "itemCount", 40),
    )

    val collectionInfo = nodeInfoOf(scrollView).collectionInfo

    assertThat(collectionInfo).isNotNull
    assertThat(collectionInfo!!.rowCount).isEqualTo(40)
    assertThat(collectionInfo.columnCount).isEqualTo(1)
    assertThat(collectionInfo.isHierarchical).isTrue()
  }

  @Test
  fun collectionInfo_absentMembersAreUnknownInsteadOfThrowing() {
    val scrollView = ReactScrollView(themedContext)
    scrollView.setTag(R.id.accessibility_collection, JavaOnlyMap.of("rowCount", 3))

    val collectionInfo = nodeInfoOf(scrollView).collectionInfo

    assertThat(collectionInfo).isNotNull
    assertThat(collectionInfo!!.rowCount).isEqualTo(3)
    assertThat(collectionInfo.columnCount).isEqualTo(-1)
    assertThat(collectionInfo.isHierarchical).isFalse()
  }

  @Test
  fun scrollEvent_absentItemCountIsUnknownInsteadOfThrowing() {
    val scrollView = ReactScrollView(themedContext)
    scrollView.setTag(R.id.accessibility_collection, JavaOnlyMap.of("rowCount", 3))

    assertThat(scrollEventOf(scrollView).itemCount).isEqualTo(-1)
  }

  @Test
  fun scrollEvent_itemsWithoutAnItemIndexAreSkipped() {
    val scrollView = collection(ReactScrollView(themedContext), false, 3, 50, 200)
    (scrollView.getChildAt(0) as ViewGroup)
        .getChildAt(0)
        .setTag(R.id.accessibility_collection_item, JavaOnlyMap.of("rowIndex", 0))

    val event = scrollEventOf(scrollView)

    assertThat(event.fromIndex).isEqualTo(1)
    assertThat(event.toIndex).isEqualTo(2)
  }
}
