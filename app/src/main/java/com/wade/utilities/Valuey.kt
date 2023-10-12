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
package com.wade.utilities

import android.content.res.TypedArray
import android.util.DisplayMetrics
import android.util.TypedValue

object Valuey {
    fun clipValueToRange(value: Float, rangeMin: Float, rangeMax: Float): Float {
        return Math.max(rangeMin, Math.min(rangeMax, value))
    }

    @JvmStatic
    fun pxFromDp(dp: Float, displayMetrics: DisplayMetrics): Float {
        return dp * displayMetrics.density
    }

    @JvmStatic
    fun pxFromSp(sp: Float, displayMetrics: DisplayMetrics): Float {
        return sp * displayMetrics.scaledDensity
    }

    @JvmStatic
    fun getDimensionOrFraction(
        array: TypedArray,
        attributeIndex: Int,
        baseValue: Int,
        defaultValue: Int
    ): Int {
        val value = array.peekValue(attributeIndex) ?: return defaultValue
        return when (value.type) {
            TypedValue.TYPE_DIMENSION -> array.getDimensionPixelOffset(attributeIndex, defaultValue)
            TypedValue.TYPE_FRACTION -> Math.round(
                array.getFraction(
                    attributeIndex,
                    baseValue,
                    baseValue,
                    defaultValue.toFloat()
                )
            )

            else -> defaultValue
        }
    }
}