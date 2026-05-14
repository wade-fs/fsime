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
class Key(
    private val grandparentKeyboard: Keyboard,
    val isLongPressable: Boolean,
    val isRepeatable: Boolean,
    val isSwipeable: Boolean,
    val isShiftable: Boolean,
    val isExtendedLeft: Boolean,
    val isExtendedRight: Boolean,
    val isPreviewable: Boolean,
    val keyCode: Int,
    val valueText: String?,
    var displayText: String?,
    var shiftText: String?,
    val strokeText: String?,
    val upText: String?,
    val downText: String?,
    val leftText: String?,
    val rightText: String?,
    var width: Int,
    var height: Int,
    val fillColour: Int,
    val borderColour: Int,
    val borderThickness: Int,
    val textColour: Int,
    val otherColour: Int,
    val textSwipeColour: Int,
    val textSize: Int,
    val textOffsetX: Int,
    var textOffsetY: Int,
    val previewMagnification: Float,
    var previewMarginY: Int,
    var x: Int,
    var y: Int
) {
    fun containsPoint(x: Int, y: Int): Boolean {
        return ((isExtendedLeft || this.x <= x)
                &&
                (isExtendedRight || x <= this.x + width) && this.y <= y) && y <= this.y + height
    }

    fun shiftAwareDisplayText(shiftMode: Int): String? {
        return if (shiftMode == 0 || !isShiftable) { // 0: SHIFT_DISABLED
            displayText
        } else {
            shiftText
        }
    }

    val isShiftKey: Boolean
        get() = valueText == FsimeService.SHIFT_KEY_VALUE_TEXT

    val isCtrlKey: Boolean
        get() = valueText == FsimeService.CTRL_KEY_VALUE_TEXT

    val isModifier: Boolean
        get() = isShiftKey || isCtrlKey

    val isControlKey: Boolean
        get() {
            val v = valueText ?: return false
            return isModifier ||
                    v == "BACKSPACE" || v == "⌫" ||
                    v == "ENTER" ||
                    v == "SPACE" ||
                    v == "TAB" || v == "↹" ||
                    v == "ESC" ||
                    v == "←" || v == "↑" || v == "↓" || v == "→" ||
                    v == "⇱" || v == "⇲" || v == "⇞" || v == "⇟" ||
                    v == "⎆"
        }

    companion object {
        fun fromXml(
            parentRow: Row2,
            x: Int,
            y: Int,
            resources: Resources,
            xmlResourceParser: XmlResourceParser?
        ): Key {
            val grandparentKeyboard = parentRow.parentKeyboard
            val attributesArray =
                resources.obtainAttributes(Xml.asAttributeSet(xmlResourceParser), R.styleable.Key)
            
            val isLongPressable = attributesArray.getBoolean(R.styleable.Key_keyIsLongPressable, true)
            val isRepeatable = attributesArray.getBoolean(R.styleable.Key_keyIsRepeatable, false)
            val isSwipeable = attributesArray.getBoolean(R.styleable.Key_keyIsSwipeable, false)
            val isShiftable = attributesArray.getBoolean(R.styleable.Key_keyIsShiftable, parentRow.keysAreShiftable)
            val isExtendedLeft = attributesArray.getBoolean(R.styleable.Key_keyIsExtendedLeft, false)
            val isExtendedRight = attributesArray.getBoolean(R.styleable.Key_keyIsExtendedRight, false)
            val isPreviewable = attributesArray.getBoolean(R.styleable.Key_keyIsPreviewable, true)
            val keyCode = attributesArray.getInt(R.styleable.Key_keyCode, 0)
            val valueText = attributesArray.getString(R.styleable.Key_keyValueText)
            
            var displayText = attributesArray.getString(R.styleable.Key_keyDisplayText)
            if (displayText == null) {
                displayText = valueText
            }
            
            var shiftText = attributesArray.getString(R.styleable.Key_keyValueTextShifted)
            if (isShiftable && shiftText == null) {
                shiftText = displayText?.uppercase(Locale.getDefault())
            } else if (shiftText == null) {
                shiftText = ""
            }
            
            val strokeText = attributesArray.getString(R.styleable.Key_stroke) ?: ""
            val upText = attributesArray.getString(R.styleable.Key_up) ?: ""
            val downText = attributesArray.getString(R.styleable.Key_down) ?: ""
            val leftText = attributesArray.getString(R.styleable.Key_left) ?: ""
            val rightText = attributesArray.getString(R.styleable.Key_right) ?: ""

            val width = getDimensionOrFraction(
                attributesArray,
                R.styleable.Key_keyWidth,
                grandparentKeyboard.screenWidth,
                parentRow.keyWidth
            )
            val height = getDimensionOrFraction(
                attributesArray,
                R.styleable.Key_keyHeight,
                grandparentKeyboard.screenHeight,
                parentRow.keyHeight
            )
            val fillColour = attributesArray.getColor(R.styleable.Key_keyFillColour, parentRow.keyFillColour)
            val borderColour = attributesArray.getColor(R.styleable.Key_keyBorderColour, parentRow.keyBorderColour)
            val borderThickness = attributesArray.getDimensionPixelSize(R.styleable.Key_keyBorderThickness, parentRow.keyBorderThickness)
            val textColour = attributesArray.getColor(R.styleable.Key_keyTextColour, parentRow.keyTextColour)
            val otherColour = attributesArray.getColor(R.styleable.Key_keyOtherColour, parentRow.keyOtherColour)
            val textSwipeColour = attributesArray.getColor(R.styleable.Key_keyTextSwipeColour, parentRow.keyTextSwipeColour)
            val textSize = attributesArray.getDimensionPixelSize(R.styleable.Key_keyTextSize, parentRow.keyTextSize)
            val textOffsetX = attributesArray.getDimensionPixelSize(R.styleable.Key_keyTextOffsetX, parentRow.keyTextOffsetX)
            val textOffsetY = attributesArray.getDimensionPixelSize(R.styleable.Key_keyTextOffsetY, parentRow.keyTextOffsetY)
            val previewMagnification = attributesArray.getFloat(R.styleable.Key_keyPreviewMagnification, parentRow.keyPreviewMagnification)
            val previewMarginY = getDimensionOrFraction(attributesArray, R.styleable.Key_keyPreviewMarginY, grandparentKeyboard.screenHeight, parentRow.keyPreviewMarginY)
            
            attributesArray.recycle()

            return Key(
                grandparentKeyboard, isLongPressable, isRepeatable, isSwipeable, isShiftable,
                isExtendedLeft, isExtendedRight, isPreviewable, keyCode, valueText, displayText,
                shiftText, strokeText, upText, downText, leftText, rightText,
                width, height, fillColour, borderColour, borderThickness, textColour, otherColour,
                textSwipeColour, textSize, textOffsetX, textOffsetY, previewMagnification,
                previewMarginY, x, y
            )
        }
    }
}
