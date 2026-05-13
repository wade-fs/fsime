/*
  Copyright 2021 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
/*
  This file contains bytes copied from the deprecated `Keyboard` class,
  i.e. `core/java/android/inputmethodservice/Keyboard.java`
  from <https://android.googlesource.com/platform/frameworks/base>,
  which is licensed under the Apache License 2.0,
  see <https://www.apache.org/licenses/LICENSE-2.0.html>.
  ---
  Take your pick from the following out-of-date notices:
  In `core/java/android/inputmethodservice/Keyboard.java`:
    Copyright (C) 2008-2009 Google Inc.
  In `NOTICE`:
    Copyright 2005-2008 The Android Open Source Project
*/
package com.wade.fsime

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.Xml
import com.wade.utilities.Valuey.getDimensionOrFraction

/*
  A row that holds keys.
*/
class Row2(// Row properties
    @JvmField val parentKeyboard: Keyboard, resources: Resources, xmlResourceParser: XmlResourceParser?
) {
    @JvmField
    val offsetX: Int

    // Key properties
    @JvmField
    val keysAreShiftable: Boolean
    @JvmField
    val keyWidth: Int
    @JvmField
    val keyHeight: Int
    @JvmField
    val keyFillColour: Int
    @JvmField
    val keyBorderColour: Int
    @JvmField
    val keyBorderThickness: Int
    @JvmField
    val keyTextColour: Int
    @JvmField
    val keyOtherColour: Int
    @JvmField
    val keyTextSwipeColour: Int
    @JvmField
    val keyTextSize: Int
    @JvmField
    val keyTextOffsetX: Int
    @JvmField
    val keyTextOffsetY: Int
    @JvmField
    val keyPreviewMagnification: Float
    @JvmField
    val keyPreviewMarginY: Int

    init {
        val attributesArray =
            resources.obtainAttributes(Xml.asAttributeSet(xmlResourceParser), R.styleable.Row)
        offsetX = getDimensionOrFraction(
            attributesArray,
            R.styleable.Row_rowOffsetX,
            parentKeyboard.screenWidth,
            DEFAULT_OFFSET_X
        )
        keysAreShiftable = attributesArray.getBoolean(
            R.styleable.Row_keysAreShiftable,
            parentKeyboard.keysAreShiftable
        )
        keyWidth = getDimensionOrFraction(
            attributesArray,
            R.styleable.Row_keyWidth,
            parentKeyboard.screenWidth,
            parentKeyboard.keyWidth
        )
        keyHeight = getDimensionOrFraction(
            attributesArray,
            R.styleable.Row_keyHeight,
            parentKeyboard.screenHeight,
            parentKeyboard.keyHeight
        )
        keyFillColour =
            attributesArray.getColor(R.styleable.Row_keyFillColour, parentKeyboard.keyFillColour)
        keyBorderColour = attributesArray.getColor(
            R.styleable.Row_keyBorderColour,
            parentKeyboard.keyBorderColour
        )
        keyBorderThickness = attributesArray.getDimensionPixelSize(
            R.styleable.Row_keyBorderThickness,
            parentKeyboard.keyBorderThickness
        )
        keyTextColour =
            attributesArray.getColor(R.styleable.Row_keyTextColour, parentKeyboard.keyTextColour)
        keyOtherColour =
            attributesArray.getColor(R.styleable.Row_keyOtherColour, parentKeyboard.keyOtherColour)
        keyTextSwipeColour = attributesArray.getColor(
            R.styleable.Row_keyTextSwipeColour,
            parentKeyboard.keyTextSwipeColour
        )
        keyTextSize = attributesArray.getDimensionPixelSize(
            R.styleable.Row_keyTextSize,
            parentKeyboard.keyTextSize
        )
        keyTextOffsetX = attributesArray.getDimensionPixelSize(
            R.styleable.Row_keyTextOffsetX,
            parentKeyboard.keyTextOffsetX
        )
        keyTextOffsetY = attributesArray.getDimensionPixelSize(
            R.styleable.Row_keyTextOffsetY,
            parentKeyboard.keyTextOffsetY
        )
        keyPreviewMagnification = attributesArray.getFloat(
            R.styleable.Row_keyPreviewMagnification,
            parentKeyboard.keyPreviewMagnification
        )
        keyPreviewMarginY = getDimensionOrFraction(
            attributesArray,
            R.styleable.Row_keyPreviewMarginY,
            parentKeyboard.screenHeight,
            parentKeyboard.keyPreviewMarginY
        )
        attributesArray.recycle()
    }

    companion object {
        private const val DEFAULT_OFFSET_X = 0
    }
}