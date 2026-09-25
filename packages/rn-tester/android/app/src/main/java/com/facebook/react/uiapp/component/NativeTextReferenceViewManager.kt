/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.uiapp.component

import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.RNTNativeTextReferenceManagerDelegate
import com.facebook.react.viewmanagers.RNTNativeTextReferenceManagerInterface

/** View manager for the NativeTextReference verification component. */
@ReactModule(name = NativeTextReferenceViewManager.REACT_CLASS)
internal class NativeTextReferenceViewManager :
    SimpleViewManager<NativeTextReferenceView>(),
    RNTNativeTextReferenceManagerInterface<NativeTextReferenceView> {

  companion object {
    const val REACT_CLASS = "RNTNativeTextReference"
  }

  private val delegate: ViewManagerDelegate<NativeTextReferenceView> =
      RNTNativeTextReferenceManagerDelegate(this)

  override fun getDelegate(): ViewManagerDelegate<NativeTextReferenceView> = delegate

  override fun getName(): String = REACT_CLASS

  override fun createViewInstance(reactContext: ThemedReactContext): NativeTextReferenceView =
      NativeTextReferenceView(reactContext)

  override fun setText(view: NativeTextReferenceView, value: String?) {
    view.setReferenceText(value ?: "")
  }

  override fun setTextSizePx(view: NativeTextReferenceView, value: Float) {
    view.setTextSizePx(value)
  }

  override fun setLineHeightPx(view: NativeTextReferenceView, value: Int) {
    view.setLineHeightPx(value)
  }
}
