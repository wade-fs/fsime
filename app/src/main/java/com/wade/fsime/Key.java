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

package com.wade.fsime;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.Log;
import android.util.Xml;

import com.wade.utilities.Valuey;

/*
  An individual key.
*/
public class Key {
    // Key behaviour
    public boolean isLongPressable;
    public boolean isRepeatable; // overrides isLongPressable
    public boolean isSwipeable;
    public boolean isShiftable;
    public boolean isExtendedLeft;
    public boolean isExtendedRight;
    public boolean isPreviewable;
    public String valueText;
    public String displayText; // overrides valueText drawn
//    public String valueTextShifted; // overrides displayText drawn when shifted
    public String shiftText, cjText, jiText, strokeText;

    // Key dimensions
    public int width;
    public int height;

    // Key styles
    public int fillColour;
    public int borderColour;
    public int borderThickness;
    public int textColour;
    public int textShiftColour;
    public int textCjColour;
    public int textJiColour;
    public int textSwipeColour;
    public int textSize;
    public int textOffsetX;
    public int textOffsetY;
    public float previewMagnification;
    public int previewMarginY;

    // Key position
    public int x;
    public int y;

    // Key meta-properties
    private final Keyboard grandparentKeyboard;

    public Key(final Row parentRow) {
        grandparentKeyboard = parentRow.parentKeyboard;
        width = parentRow.keyWidth;
        height = parentRow.keyHeight;
    }

    public Key(final Row parentRow,
               final int x,
               final int y,
               final Resources resources,
               final XmlResourceParser xmlResourceParser
    ) {
        this(parentRow);

        this.x = x;
        this.y = y;

        final TypedArray attributesArray =
                resources.obtainAttributes(Xml.asAttributeSet(xmlResourceParser), R.styleable.Key);

        isLongPressable = attributesArray.getBoolean(R.styleable.Key_keyIsLongPressable, false);
        isRepeatable = attributesArray.getBoolean(R.styleable.Key_keyIsRepeatable, false);
        isSwipeable = attributesArray.getBoolean(R.styleable.Key_keyIsSwipeable, false);
        isShiftable = attributesArray.getBoolean(R.styleable.Key_keyIsShiftable, parentRow.keysAreShiftable);
        isExtendedLeft = attributesArray.getBoolean(R.styleable.Key_keyIsExtendedLeft, false);
        isExtendedRight = attributesArray.getBoolean(R.styleable.Key_keyIsExtendedRight, false);
        isPreviewable = attributesArray.getBoolean(R.styleable.Key_keyIsPreviewable, true);

        valueText = attributesArray.getString(R.styleable.Key_keyValueText);
        displayText = attributesArray.getString(R.styleable.Key_keyDisplayText);
        if (displayText == null) {
            displayText = valueText;
        }

        cjText = attributesArray.getString(R.styleable.Key_cj);
        if (cjText == null) {
            cjText = "";
        }

        jiText = attributesArray.getString(R.styleable.Key_ji);
        if (jiText == null) {
            jiText = "";
        }

        strokeText = attributesArray.getString(R.styleable.Key_stroke);
        if (strokeText == null) {
            strokeText = "";
        }

        shiftText = attributesArray.getString(R.styleable.Key_keyValueTextShifted);
        if (isShiftable && shiftText == null) {
            shiftText = displayText.toUpperCase();
        } else if (shiftText == null) {
            shiftText = "";
        }

        width = Valuey.getDimensionOrFraction(
                attributesArray,
                R.styleable.Key_keyWidth,
                grandparentKeyboard.getScreenWidth(),
                parentRow.keyWidth
        );
        height = Valuey.getDimensionOrFraction(
                attributesArray,
                R.styleable.Key_keyHeight,
                grandparentKeyboard.getScreenHeight(),
                parentRow.keyHeight
        );

        fillColour =
                attributesArray.getColor(R.styleable.Key_keyFillColour, parentRow.keyFillColour);
        borderColour =
                attributesArray.getColor(R.styleable.Key_keyBorderColour, parentRow.keyBorderColour);
        borderThickness =
                attributesArray.getDimensionPixelSize(R.styleable.Key_keyBorderThickness, parentRow.keyBorderThickness);

        textColour = attributesArray.getColor(R.styleable.Key_keyTextColour, parentRow.keyTextColour);
        textShiftColour = attributesArray.getColor(R.styleable.Key_keyTextShiftColour, parentRow.keyTextShiftColour);
        textCjColour = attributesArray.getColor(R.styleable.Key_keyTextCjColour, parentRow.keyTextCjColour);
        textJiColour = attributesArray.getColor(R.styleable.Key_keyTextJiColour, parentRow.keyTextJiColour);
        textSwipeColour = attributesArray.getColor(R.styleable.Key_keyTextSwipeColour, parentRow.keyTextSwipeColour);
        textSize = attributesArray.getDimensionPixelSize(R.styleable.Key_keyTextSize, parentRow.keyTextSize);
        textOffsetX = attributesArray.getDimensionPixelSize(R.styleable.Key_keyTextOffsetX, parentRow.keyTextOffsetX);
        textOffsetY = attributesArray.getDimensionPixelSize(R.styleable.Key_keyTextOffsetY, parentRow.keyTextOffsetY);

        previewMagnification =
                attributesArray.getFloat(
                        R.styleable.Key_keyPreviewMagnification,
                        parentRow.keyPreviewMagnification
                );
        previewMarginY =
                Valuey.getDimensionOrFraction(
                        attributesArray,
                        R.styleable.Key_keyPreviewMarginY,
                        grandparentKeyboard.getScreenHeight(),
                        parentRow.keyPreviewMarginY
                );

        attributesArray.recycle();
    }

    public boolean containsPoint(final int x, final int y) {
        return (
                (this.isExtendedLeft || this.x <= x)
                        &&
                        (this.isExtendedRight || x <= this.x + this.width)
                        &&
                        this.y <= y && y <= this.y + this.height
        );
    }

    public String shiftAwareDisplayText(final int shiftMode) {
        if (shiftMode == KeyboardView.SHIFT_DISABLED) {
            return displayText;
        } else {
            return shiftText;
        }
    }
}
