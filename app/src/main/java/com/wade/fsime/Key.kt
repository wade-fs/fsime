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
import java.util.Locale

/*
  An individual key.
*/
class Key(parentRow: Row) {
    // Key behaviour
    @JvmField
    var isLongPressable = false
    @JvmField
    var isRepeatable = false // overrides isLongPressable
    @JvmField
    var isSwipeable = false
    @JvmField
    var isShiftable = false
    var isExtendedLeft = false
    var isExtendedRight = false
    @JvmField
    var isPreviewable = false
    @JvmField
    var valueText: String? = null
    @JvmField
    var displayText: String? = null // overrides valueText drawn
    @JvmField
    var shiftText: String? = null
    @JvmField
    var cjText: String? = null
    @JvmField
    var jiText: String? = null
    @JvmField
    var strokeText: String? = null
    @JvmField
    var upText: String? = null
    @JvmField
    var downText: String? = null
    @JvmField
    var leftText: String? = null
    @JvmField
    var rightText: String? = null

    // Key dimensions
    @JvmField
    var width: Int
    @JvmField
    var height: Int

    // Key styles
    @JvmField
    var fillColour = 0
    @JvmField
    var borderColour = 0
    @JvmField
    var borderThickness = 0
    @JvmField
    var textColour = 0
    @JvmField
    var otherColour = 0
    @JvmField
    var textSwipeColour = 0
    @JvmField
    var textSize = 0
    @JvmField
    var textOffsetX = 0
    @JvmField
    var textOffsetY = 0
    var previewMagnification = 0f
    @JvmField
    var previewMarginY = 0

    // Key position
    @JvmField
    var x = 0
    @JvmField
    var y = 0

    // Key meta-properties
    private val grandparentKeyboard: Keyboard

    init {
        grandparentKeyboard = parentRow.parentKeyboard
        width = parentRow.keyWidth
        height = parentRow.keyHeight
    }

    constructor(
        parentRow: Row,
        x: Int,
        y: Int,
        resources: Resources,
        xmlResourceParser: XmlResourceParser?
    ) : this(parentRow) {
        this.x = x
        this.y = y
        val attributesArray =
            resources.obtainAttributes(Xml.asAttributeSet(xmlResourceParser), R.styleable.Key)
        isLongPressable = attributesArray.getBoolean(R.styleable.Key_keyIsLongPressable, true)
        isRepeatable = attributesArray.getBoolean(R.styleable.Key_keyIsRepeatable, false)
        isSwipeable = attributesArray.getBoolean(R.styleable.Key_keyIsSwipeable, false)
        isShiftable =
            attributesArray.getBoolean(R.styleable.Key_keyIsShiftable, parentRow.keysAreShiftable)
        isExtendedLeft = attributesArray.getBoolean(R.styleable.Key_keyIsExtendedLeft, false)
        isExtendedRight = attributesArray.getBoolean(R.styleable.Key_keyIsExtendedRight, false)
        isPreviewable = attributesArray.getBoolean(R.styleable.Key_keyIsPreviewable, true)
        valueText = attributesArray.getString(R.styleable.Key_keyValueText)
        displayText = attributesArray.getString(R.styleable.Key_keyDisplayText)
        if (displayText == null) {
            displayText = valueText
        }
        shiftText = attributesArray.getString(R.styleable.Key_keyValueTextShifted)
        if (isShiftable && shiftText == null) {
            shiftText = displayText!!.uppercase(Locale.getDefault())
        } else if (shiftText == null) {
            shiftText = ""
        }
        strokeText = attributesArray.getString(R.styleable.Key_stroke)
        if (strokeText == null) {
            strokeText = ""
        }
        cjText = attributesArray.getString(R.styleable.Key_cj)
        if (cjText == null) {
            cjText = ""
        }
        jiText = attributesArray.getString(R.styleable.Key_ji)
        if (jiText == null) {
            jiText = ""
        }
        upText = attributesArray.getString(R.styleable.Key_up)
        if (upText == null) {
            upText = ""
        }
        downText = attributesArray.getString(R.styleable.Key_down)
        if (downText == null) {
            downText = ""
        }
        leftText = attributesArray.getString(R.styleable.Key_left)
        if (leftText == null) {
            leftText = ""
        }
        rightText = attributesArray.getString(R.styleable.Key_right)
        if (rightText == null) {
            rightText = ""
        }
        width = getDimensionOrFraction(
            attributesArray,
            R.styleable.Key_keyWidth,
            grandparentKeyboard.screenWidth,
            parentRow.keyWidth
        )
        height = getDimensionOrFraction(
            attributesArray,
            R.styleable.Key_keyHeight,
            grandparentKeyboard.screenHeight,
            parentRow.keyHeight
        )
        fillColour =
            attributesArray.getColor(R.styleable.Key_keyFillColour, parentRow.keyFillColour)
        borderColour =
            attributesArray.getColor(R.styleable.Key_keyBorderColour, parentRow.keyBorderColour)
        borderThickness = attributesArray.getDimensionPixelSize(
            R.styleable.Key_keyBorderThickness,
            parentRow.keyBorderThickness
        )
        textColour =
            attributesArray.getColor(R.styleable.Key_keyTextColour, parentRow.keyTextColour)
        otherColour =
            attributesArray.getColor(R.styleable.Key_keyOtherColour, parentRow.keyOtherColour)
        textSwipeColour = attributesArray.getColor(
            R.styleable.Key_keyTextSwipeColour,
            parentRow.keyTextSwipeColour
        )
        textSize = attributesArray.getDimensionPixelSize(
            R.styleable.Key_keyTextSize,
            parentRow.keyTextSize
        )
        textOffsetX = attributesArray.getDimensionPixelSize(
            R.styleable.Key_keyTextOffsetX,
            parentRow.keyTextOffsetX
        )
        textOffsetY = attributesArray.getDimensionPixelSize(
            R.styleable.Key_keyTextOffsetY,
            parentRow.keyTextOffsetY
        )
        previewMagnification = attributesArray.getFloat(
            R.styleable.Key_keyPreviewMagnification,
            parentRow.keyPreviewMagnification
        )
        previewMarginY = getDimensionOrFraction(
            attributesArray,
            R.styleable.Key_keyPreviewMarginY,
            grandparentKeyboard.screenHeight,
            parentRow.keyPreviewMarginY
        )
        attributesArray.recycle()
    }

    fun containsPoint(x: Int, y: Int): Boolean {
        return ((isExtendedLeft || this.x <= x)
                &&
                (isExtendedRight || x <= this.x + width) && this.y <= y) && y <= this.y + height
    }

    fun shiftAwareDisplayText(shiftMode: Int): String? {
        return if (shiftMode == KeyboardView.SHIFT_DISABLED) {
            displayText
        } else {
            shiftText
        }
    }
}