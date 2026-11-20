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
     * Returns the smallest and largest `itemIndex` among the views carrying
     * `accessibilityCollectionItem` that intersect the visible part of [scrollView], or null when
     * none does.
     *
     * Items are found by searching the scroll view's descendants rather than its direct children,
     * so the result does not depend on how many views a list puts between the content container and
     * each item (cell wrappers, headers, spacers, separators). The search stops at an item, since
     * an item's descendants belong to it, and at a nested scroll view with its own
     * `accessibilityCollection`, whose items belong to that collection. Indices are compared rather
     * than taken in child order, which inverted lists and sticky headers do not follow. Fully and
     * partly visible items both count, as for AdapterView's first and last visible positions.
     */
    fun findVisibleItemRange(scrollView: ViewGroup): IntRange? {
      val viewport =
          Rect(
              scrollView.scrollX,
              scrollView.scrollY,
              scrollView.scrollX + scrollView.width,
              scrollView.scrollY + scrollView.height,
          )
      if (viewport.isEmpty) {
        return null
      }

      var first = Int.MAX_VALUE
      var last = Int.MIN_VALUE
      val itemRect = Rect()
      val pending = ArrayDeque<View>()
      for (index in 0..<scrollView.childCount) {
        pending.addLast(scrollView.getChildAt(index))
      }

      while (pending.isNotEmpty()) {
        val child = pending.removeFirst()
        if (child.visibility != View.VISIBLE) {
          continue
        }

        val item = child.getTag(R.id.accessibility_collection_item) as? ReadableMap
        if (item != null) {
          val itemIndex = item.getIntOrDefault("itemIndex", -1)
          if (itemIndex >= 0) {
            child.getDrawingRect(itemRect)
            // Moves the rect into the scroll view's content coordinates, the space its scrollX
            // and scrollY are in.
            scrollView.offsetDescendantRectToMyCoords(child, itemRect)
            if (Rect.intersects(itemRect, viewport)) {
              first = minOf(first, itemIndex)
              last = maxOf(last, itemIndex)
            }
          }
          continue
        }

        if (
            child is ReactAccessibleScrollView &&
                child.getTag(R.id.accessibility_collection) != null
        ) {
          continue
        }

        if (child is ViewGroup) {
          for (index in 0..<child.childCount) {
            pending.addLast(child.getChildAt(index))
          }
        }
      }

      return if (first <= last) first..last else null
    }

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
    val visibleRange = findVisibleItemRange(scrollView) ?: return
    event.fromIndex = visibleRange.first
    event.toIndex = visibleRange.last
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
