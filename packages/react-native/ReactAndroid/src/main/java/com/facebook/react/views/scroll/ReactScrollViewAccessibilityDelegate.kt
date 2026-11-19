/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.views.scroll

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.react.R
import com.facebook.react.bridge.AssertionException
import com.facebook.react.bridge.ReactSoftExceptionLogger
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.ReactAccessibilityDelegate
import com.facebook.react.uimanager.ReactAccessibilityDelegate.AccessibilityRole
import com.facebook.react.uimanager.ReactAccessibilityDelegate.Companion.getIntOrDefault

internal class ReactScrollViewAccessibilityDelegate : AccessibilityDelegateCompat() {

  private val TAG: String = ReactScrollViewAccessibilityDelegate::class.java.simpleName

  override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
    super.onInitializeAccessibilityEvent(host, event)
    if (host is ReactAccessibleScrollView) {
      onInitializeAccessibilityEventInternal(host, event)
    } else {
      ReactSoftExceptionLogger.logSoftException(
          TAG,
          AssertionException(
              "ReactScrollViewAccessibilityDelegate should only be used with ReactAccessibleScrollView, not with class: ${host.javaClass.simpleName}",
          ),
      )
    }
  }

  override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
    super.onInitializeAccessibilityNodeInfo(host, info)
    if (host is ReactAccessibleScrollView) {
      onInitializeAccessibilityNodeInfoInternal(host, info)
    } else {
      ReactSoftExceptionLogger.logSoftException(
          TAG,
          AssertionException(
              "ReactScrollViewAccessibilityDelegate should only be used with ReactAccessibleScrollView, not with class: ${host.javaClass.simpleName}",
          ),
      )
    }
  }

  internal companion object {
    /**
     * Builds the [AccessibilityNodeInfoCompat.CollectionInfoCompat] for an
     * `accessibilityCollection` map. A row or column count the map leaves out is -1, which Android
     * and TalkBack treat as unknown.
     */
    fun getCollectionInfo(
        collection: ReadableMap
    ): AccessibilityNodeInfoCompat.CollectionInfoCompat =
        AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
            collection.getIntOrDefault("rowCount", -1),
            collection.getIntOrDefault("columnCount", -1),
            collection.hasKey("hierarchical") &&
                !collection.isNull("hierarchical") &&
                collection.getBoolean("hierarchical"),
        )
  }

  private fun onInitializeAccessibilityEventInternal(view: View, event: AccessibilityEvent) {
    val accessibilityCollection =
        view.getTag(R.id.accessibility_collection) as? ReadableMap ?: return

    // -1 is AccessibilityRecord's value for an unknown count.
    event.itemCount = accessibilityCollection.getIntOrDefault("itemCount", -1)

    val scrollView = view as? ViewGroup ?: return
    val contentView = scrollView.getChildAt(0) as? ViewGroup ?: return

    // The visible part of the scroll view's content, in the coordinates of its scrolled content.
    val viewport =
        Rect(view.scrollX, view.scrollY, view.scrollX + view.width, view.scrollY + view.height)

    var firstVisibleIndex: Int? = null
    var lastVisibleIndex: Int? = null

    for (index in 0..<contentView.childCount) {
      val nextChild = contentView.getChildAt(index)
      val isVisible = isInViewport(scrollView, nextChild, viewport)
      var accessibilityCollectionItem: ReadableMap? =
          nextChild.getTag(R.id.accessibility_collection_item) as? ReadableMap

      if (nextChild !is ViewGroup) {
        return
      }

      // If this child's accessibilityCollectionItem is null, we'll check one more
      // nested child.
      // Happens when getItemLayout is not passed in FlatList which adds an additional
      // View in the hierarchy.
      if (nextChild.childCount > 0 && accessibilityCollectionItem == null) {
        val nestedNextChild = nextChild.getChildAt(0)
        if (nestedNextChild != null) {
          val nestedChildAccessibility =
              nestedNextChild.getTag(R.id.accessibility_collection_item) as? ReadableMap
          if (nestedChildAccessibility != null) {
            accessibilityCollectionItem = nestedChildAccessibility
          }
        }
      }

      val itemIndex = accessibilityCollectionItem?.getIntOrDefault("itemIndex", -1) ?: -1
      if (isVisible && itemIndex >= 0) {
        if (firstVisibleIndex == null) {
          firstVisibleIndex = itemIndex
        }
        lastVisibleIndex = itemIndex
      }

      if (firstVisibleIndex != null && lastVisibleIndex != null) {
        event.fromIndex = firstVisibleIndex
        event.toIndex = lastVisibleIndex
      }
    }
  }

  /**
   * Whether any part of [child], a descendant of [scrollView], is inside [viewport].
   *
   * Fully visible children count as well as partly visible ones: the scroll event's fromIndex and
   * toIndex are the first and last visible items, as AdapterView reports them.
   * [ReactAccessibleScrollView.isPartiallyScrolledInView] does not fit, since it is true only for a
   * child that is cut off by the viewport.
   */
  private fun isInViewport(scrollView: ViewGroup, child: View, viewport: Rect): Boolean {
    val childRect = Rect()
    child.getDrawingRect(childRect)
    scrollView.offsetDescendantRectToMyCoords(child, childRect)
    return Rect.intersects(childRect, viewport)
  }

  private fun onInitializeAccessibilityNodeInfoInternal(
      view: View,
      info: AccessibilityNodeInfoCompat,
  ) {
    val accessibilityRole = AccessibilityRole.fromViewTag(view)

    if (accessibilityRole != null) {
      ReactAccessibilityDelegate.setRole(info, accessibilityRole, view.context)
    }

    val accessibilityCollection = view.getTag(R.id.accessibility_collection) as? ReadableMap

    if (accessibilityCollection != null) {
      val collectionInfo = getCollectionInfo(accessibilityCollection)
      info.setCollectionInfo(collectionInfo)

      // TalkBack announces a collection ("in list", "2 of 40") only when the collection's role is
      // a list or a grid, and it gives any android.widget.ScrollView the scroll view role before
      // looking at its CollectionInfo. Report the class the matching role would, unless the app
      // set a role itself.
      if (accessibilityRole == null) {
        info.className =
            AccessibilityRole.getValue(
                if (collectionInfo.rowCount > 1 && collectionInfo.columnCount > 1) {
                  AccessibilityRole.GRID
                } else {
                  AccessibilityRole.LIST
                }
            )
      }
    }

    if (view is ReactAccessibleScrollView) {
      info.isScrollable = view.scrollEnabled
    }
  }
}
