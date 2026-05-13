/*
  Copyright 2021--2022 Conway
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

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Color
import android.util.Xml
import com.wade.utilities.Valuey.getDimensionOrFraction
import com.wade.utilities.Valuey.pxFromDp
import com.wade.utilities.Valuey.pxFromSp

/*
  A keyboard that holds rows of keys, to be declared in a layout XML.
*/
class Keyboard(private val context: Context, layoutResourceId: Int, name: String?) {
    private val defaultKeyHeightPx: Int
    private val defaultKeyBorderThicknessPx: Int
    private val defaultKeyTextSizePx: Int
    private val defaultKeyPreviewMarginYPx: Int

    // Keyboard properties
    var width = 0
        private set
    var height = 0
        private set
    private val keyList: MutableList<Key>
    @JvmField
    var fillColour = 0

    // Key properties
    var keysAreShiftable = false
    var keyWidth = 0
    var keyHeight = 0
    var keyFillColour = 0
    var keyBorderColour = 0
    var keyBorderThickness = 0
    var keyTextColour = 0
    var keyOtherColour = 0
    var keyTextSwipeColour = 0
    var keyTextSize = 0
    var keyTextOffsetX = 0
    var keyTextOffsetY = 0
    var keyPreviewMagnification = 0f
    var keyPreviewMarginY = 0

    // Screen properties
    val screenWidth: Int
    val screenHeight: Int
    @JvmField
    var name: String? = null
    @JvmField
    var shiftMode = 0
    @JvmField
    var ctrlMode = 0
    @JvmField
    var swipeDir = 0 // 0:None 1:右 2:左 3:上 4:下
    fun setName(name: String?) {
        this.name = name
    }

    init {
        setName(name)
        val displayMetrics = context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        defaultKeyHeightPx = pxFromDp(DEFAULT_KEY_HEIGHT_DP.toFloat(), displayMetrics).toInt()
        defaultKeyBorderThicknessPx =
            pxFromDp(DEFAULT_KEY_BORDER_THICKNESS_DP.toFloat(), displayMetrics).toInt()
        defaultKeyTextSizePx = pxFromSp(DEFAULT_KEY_TEXT_SIZE_SP.toFloat(), displayMetrics).toInt()
        defaultKeyPreviewMarginYPx =
            pxFromDp(DEFAULT_KEY_PREVIEW_MARGIN_Y_DP.toFloat(), displayMetrics).toInt()
        keyList = ArrayList()
        makeKeyboard(context, context.resources.getXml(layoutResourceId))
        capKeyboardHeight()
    }

    fun getKeyList(): List<Key> {
        return keyList
    }

    fun setShiftText(vt: String, st: String?) {
        for (key in keyList) {
            if (key.valueText == vt) {
                key.shiftText = st
                break
            }
        }
    }

    private fun makeKeyboard(context: Context, xmlResourceParser: XmlResourceParser) {
        try {
            var inKey = false
            var inRow = false
            var x = 0
            var y = KEYBOARD_GUTTER_HEIGHT_PX
            var key: Key? = null
            var row: Row2? = null
            var maximumX = x
            var maximumY = y
            val resources = context.resources
            var event: Int
            while (xmlResourceParser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
                when (event) {
                    XmlResourceParser.START_TAG -> {
                        val xmlTag = xmlResourceParser.name
                        when (xmlTag) {
                            KEYBOARD_TAG -> parseKeyboardAttributes(resources, xmlResourceParser)
                            ROW_TAG -> {
                                inRow = true
                                row = Row2(this, resources, xmlResourceParser)
                                x = row.offsetX
                            }

                            KEY_TAG -> {
                                inKey = true
                                key = Key(row!!, x, y, resources, xmlResourceParser)
                                keyList.add(key)
                            }
                        }
                    }

                    XmlResourceParser.END_TAG -> if (inKey) {
                        inKey = false
                        x += key!!.width
                        maximumX = Math.max(x, maximumX)
                    } else if (inRow) {
                        inRow = false
                        y += row!!.keyHeight
                        maximumY = Math.max(y, maximumY)
                    }
                }
            }
            width = maximumX
            height = maximumY
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun capKeyboardHeight() {
        val keyboardHeightCorrectionFactor =
            Math.min(1f, KEYBOARD_HEIGHT_MAX_FRACTION * screenHeight / height)
        for (key in keyList) {
            key.y *= keyboardHeightCorrectionFactor.toInt()
            key.height *= keyboardHeightCorrectionFactor.toInt()
            key.textOffsetY *= keyboardHeightCorrectionFactor.toInt()
            key.previewMarginY *= keyboardHeightCorrectionFactor.toInt()
        }
        height *= keyboardHeightCorrectionFactor.toInt()
    }

    private fun parseKeyboardAttributes(
        resources: Resources,
        xmlResourceParser: XmlResourceParser
    ) {
        val attributesArray =
            resources.obtainAttributes(Xml.asAttributeSet(xmlResourceParser), R.styleable.Keyboard)
        fillColour = attributesArray.getColor(
            R.styleable.Keyboard_keyboardFillColour,
            DEFAULT_KEYBOARD_FILL_COLOUR
        )
        keysAreShiftable = attributesArray.getBoolean(R.styleable.Keyboard_keysAreShiftable, false)
        keyWidth = getDimensionOrFraction(
            attributesArray,
            R.styleable.Keyboard_keyWidth,
            screenWidth, (DEFAULT_KEY_WIDTH_FRACTION * screenWidth).toInt()
        )
        keyHeight = getDimensionOrFraction(
            attributesArray,
            R.styleable.Keyboard_keyHeight,
            screenHeight,
            defaultKeyHeightPx
        )
        keyFillColour =
            attributesArray.getColor(R.styleable.Keyboard_keyFillColour, DEFAULT_KEY_FILL_COLOUR)
        keyBorderColour = attributesArray.getColor(
            R.styleable.Keyboard_keyBorderColour,
            DEFAULT_KEY_BORDER_COLOUR
        )
        keyBorderThickness = attributesArray.getDimensionPixelSize(
            R.styleable.Keyboard_keyBorderThickness,
            defaultKeyBorderThicknessPx
        )
        keyTextColour =
            attributesArray.getColor(R.styleable.Keyboard_keyTextColour, DEFAULT_KEY_TEXT_COLOUR)
        keyOtherColour =
            attributesArray.getColor(R.styleable.Keyboard_keyOtherColour, DEFAULT_KEY_TEXT_COLOUR)
        keyTextSwipeColour = attributesArray.getColor(
            R.styleable.Keyboard_keyTextSwipeColour,
            DEFAULT_KEY_TEXT_SWIPE_COLOUR
        )
        keyTextSize = attributesArray.getDimensionPixelSize(
            R.styleable.Keyboard_keyTextSize,
            defaultKeyTextSizePx
        )
        keyTextOffsetX =
            attributesArray.getDimensionPixelSize(R.styleable.Keyboard_keyTextOffsetX, 0)
        keyTextOffsetY =
            attributesArray.getDimensionPixelSize(R.styleable.Keyboard_keyTextOffsetY, 0)
        keyPreviewMagnification = attributesArray.getFloat(
            R.styleable.Keyboard_keyPreviewMagnification,
            DEFAULT_KEY_PREVIEW_MAGNIFICATION
        )
        keyPreviewMarginY = getDimensionOrFraction(
            attributesArray,
            R.styleable.Keyboard_keyPreviewMarginY,
            screenHeight,
            defaultKeyPreviewMarginYPx
        )
        attributesArray.recycle()
    }

    companion object {
        private const val KEYBOARD_TAG = "Keyboard"
        private const val ROW_TAG = "Row"
        private const val KEY_TAG = "Key"
        private const val KEYBOARD_GUTTER_HEIGHT_PX = 1
        private const val DEFAULT_KEYBOARD_FILL_COLOUR = Color.BLACK
        private const val KEYBOARD_HEIGHT_MAX_FRACTION = 0.5f
        private const val DEFAULT_KEY_WIDTH_FRACTION = 0.1f
        private const val DEFAULT_KEY_HEIGHT_DP = 64
        private const val DEFAULT_KEY_FILL_COLOUR = Color.BLACK
        private const val DEFAULT_KEY_BORDER_COLOUR = Color.GRAY
        private const val DEFAULT_KEY_BORDER_THICKNESS_DP = 2
        private const val DEFAULT_KEY_TEXT_COLOUR = Color.WHITE
        private const val DEFAULT_KEY_TEXT_SWIPE_COLOUR = Color.RED
        private const val DEFAULT_KEY_TEXT_SIZE_SP = 32
        private const val DEFAULT_KEY_PREVIEW_MAGNIFICATION = 1.2f
        private const val DEFAULT_KEY_PREVIEW_MARGIN_Y_DP = 24
    }
}