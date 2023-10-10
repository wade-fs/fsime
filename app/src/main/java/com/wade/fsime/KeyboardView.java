/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
/*
  This file contains bytes copied from the deprecated `KeyboardView` class,
  i.e. `core/java/android/inputmethodservice/KeyboardView.java`
  from <https://android.googlesource.com/platform/frameworks/base>,
  which is licensed under the Apache License 2.0,
  see <https://www.apache.org/licenses/LICENSE-2.0.html>.
  ---
  Take your pick from the following out-of-date notices:
  In `core/java/android/inputmethodservice/KeyboardView.java`:
    Copyright (C) 2008-2009 Google Inc.
  In `NOTICE`:
    Copyright 2005-2008 The Android Open Source Project
*/

package com.wade.fsime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import java.util.List;
import com.wade.mil.Mil;

/*
  A view that holds a keyboard.
  Touch logic is implemented here.
*/
public class KeyboardView
        extends View
        implements View.OnClickListener {
    private static final int NONEXISTENT_POINTER_ID = -1;

    private static final int MESSAGE_KEY_REPEAT = 1;
    private static final int MESSAGE_LONG_PRESS = 2;
    private static final int DEFAULT_KEY_REPEAT_INTERVAL_MILLISECONDS = 75;
    private static final int KEY_REPEAT_START_MILLISECONDS = 500;
    private static final int KEY_LONG_PRESS_MILLISECONDS = 750;

    private static final int SWIPE_MX = 40;
    private static final int SWIPE_MY = 10;

    public static final int SHIFT_DISABLED = 0;
    private static final int SHIFT_SINGLE = 1;
    private static final int SHIFT_PERSISTENT = 2;
    private static final int SHIFT_INITIATED = 3;
    private static final int SHIFT_HELD = 4;
    private static final int CTRL_DISABLED = 0;
    private static final int CTRL_SINGLE = 1;
    private static final int CTRL_INITIATED = 3;
    private static final int CTRL_HELD = 4;

    public static final String KEYBOARD_FONT_FILE_NAME = "StrokeInputFont.ttf";
    private static final float COLOUR_LIGHTNESS_CUTOFF = 0.7f;

    // View properties
    private KeyboardListener keyboardListener;
    private Keyboard keyboard;
    private List<Key> keyList;

    // Active key
    private Key activeKey;
    private int activePointerId = NONEXISTENT_POINTER_ID;

    // Long presses and key repeats
    private Handler extendedPressHandler;
    private int keyRepeatIntervalMilliseconds;

    // Horizontal swipes
    private int pointerDownX, pointerDownY;
    final int SWIPE_NONE=0, SWIPE_RIGHT=1, SWIPE_LEFT=2, SWIPE_UP=3, SWIPE_DOWN=4;
    private int swipeDir = SWIPE_NONE; // 0: false, 1: right, 2: left, 3: up, 4: down

    // Shift key
    private int shiftPointerId = NONEXISTENT_POINTER_ID, ctrlPointerId = NONEXISTENT_POINTER_ID;
    private int shiftMode, ctrlMode;

    // Keyboard drawing
    private Rect keyboardRectangle;
    private Paint keyboardFillPaint;

    // Key drawing
    private Rect keyRectangle;
    private Paint keyFillPaint;
    private Paint keyBorderPaint;
    private Paint keyTextPaint;
    private Paint keyTextShiftPaint;
    private Paint keyTextStrokePaint;
    private Paint keyTextCjPaint, keyTextJiPaint, keyTextMilPaint;
    private Paint keyTextUpPaint, keyTextDownPaint, keyTextLeftPaint, keyTextRightPaint;

    public KeyboardView(final Context context, final AttributeSet attributes) {
        super(context, attributes);

        initialiseExtendedPressHandler();
        initialiseDrawing(context);
    }

    private void initialiseExtendedPressHandler() {
        resetKeyRepeatIntervalMilliseconds();

        extendedPressHandler =
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message message) {
                        if (activeKey != null) {
                            switch (message.what) {
                                case MESSAGE_KEY_REPEAT -> {
                                    keyboardListener.onKey(activeKey.valueText);
                                    sendExtendedPressHandlerMessage(MESSAGE_KEY_REPEAT, keyRepeatIntervalMilliseconds);
                                }
                                case MESSAGE_LONG_PRESS -> {
                                    keyboardListener.onLongPress(activeKey.valueText + activeKey.shiftText);
                                    activeKey = null;
                                    activePointerId = NONEXISTENT_POINTER_ID;
                                    invalidate();
                                }
                            }
                        }
                    }
                };

    }

    private void initialiseDrawing(final Context context) {
        this.setBackgroundColor(Color.TRANSPARENT);

        keyboardRectangle = new Rect();
        keyboardFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        keyRectangle = new Rect();

        keyFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyFillPaint.setStyle(Paint.Style.FILL);

        keyBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyBorderPaint.setStyle(Paint.Style.STROKE);

        keyTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextPaint.setTextAlign(Paint.Align.CENTER);

        keyTextShiftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextShiftPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextShiftPaint.setTextAlign(Paint.Align.RIGHT);

        keyTextStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextStrokePaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextStrokePaint.setTextAlign(Paint.Align.RIGHT);

        keyTextCjPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextCjPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextCjPaint.setTextAlign(Paint.Align.RIGHT);

        keyTextJiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextJiPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextJiPaint.setTextAlign(Paint.Align.LEFT);

        keyTextMilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextMilPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextMilPaint.setTextAlign(Paint.Align.LEFT);

        keyTextUpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextUpPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextUpPaint.setTextAlign(Paint.Align.LEFT);

        keyTextDownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextDownPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextDownPaint.setTextAlign(Paint.Align.LEFT);

        keyTextLeftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextLeftPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextLeftPaint.setTextAlign(Paint.Align.LEFT);

        keyTextRightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        keyTextRightPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), KEYBOARD_FONT_FILE_NAME));
        keyTextRightPaint.setTextAlign(Paint.Align.LEFT);
    }

    /*
      A listener for keyboard events.
    */
    public interface KeyboardListener {
        void onKey(String valueText);

        void onLongPress(String valueText);

        void onSwipe(String valueText);

        void saveKeyboard(Keyboard keyboard);
    }

    public void setKeyboardListener(final KeyboardListener keyboardListener) {
        this.keyboardListener = keyboardListener;
    }

    public void setMainInputPlane(final LinearLayout mainInputPlane) {
    }

    public Keyboard getKeyboard() {
        return keyboard;
    }

    public void setKeyboard(final Keyboard keyboard) {
        keyboardListener.saveKeyboard(keyboard);
        this.keyboard = keyboard;
        keyList = keyboard.getKeyList();
        keyboardFillPaint.setColor(keyboard.fillColour);
        if (shiftMode != SHIFT_PERSISTENT) {
            shiftMode = SHIFT_DISABLED;
        }
        if (shiftMode != SHIFT_DISABLED) {
            keyboard.shiftMode = keyboard.shiftMode | KeyEvent.META_SHIFT_MASK;
        } else {
            keyboard.shiftMode = keyboard.shiftMode & ~KeyEvent.META_SHIFT_MASK;
        }
        ctrlMode = CTRL_DISABLED;
        keyboard.ctrlMode = ctrlMode;
        requestLayout();
    }

    public void resetKeyRepeatIntervalMilliseconds() {
        keyRepeatIntervalMilliseconds = DEFAULT_KEY_REPEAT_INTERVAL_MILLISECONDS;
    }

    public void setKeyRepeatIntervalMilliseconds(final int milliseconds) {
        keyRepeatIntervalMilliseconds = milliseconds;
    }

    @Override
    public void onClick(final View view) {
        // Touch logic implemented in onTouchEvent
    }

    @Override
    public void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int keyboardWidth;
        final int keyboardHeight;
        if (keyboard != null) {
            keyboardWidth = keyboard.getWidth();
            keyboardHeight = keyboard.getHeight();
        } else {
            keyboardWidth = 0;
            keyboardHeight = 0;
        }
        keyboardRectangle.set(0, 0, keyboardWidth, keyboardHeight);

        setMeasuredDimension(keyboardWidth, keyboardHeight);
    }

    @Override
    public void onDraw(final Canvas canvas) {
        if (keyboard == null) {
            return;
        }

        canvas.drawRect(keyboardRectangle, keyboardFillPaint);

        for (final Key key : keyList) {
            keyRectangle.set(0, 0, key.width, key.height);

            int keyFillColour = key.fillColour;
            if (
                    key == activeKey
                            ||
                            key.valueText.equals(FsimeService.SHIFT_KEY_VALUE_TEXT) && (
                                    shiftPointerId != NONEXISTENT_POINTER_ID
                                            ||
                                            shiftMode == SHIFT_PERSISTENT
                                            ||
                                            shiftMode == SHIFT_INITIATED
                                            ||
                                            shiftMode == SHIFT_HELD
                            ) ||
                            key.valueText.equals(FsimeService.CTRL_KEY_VALUE_TEXT) && ctrlMode != CTRL_DISABLED
            ) { // 畫黃底
                keyFillColour = toPressedColour(keyFillColour);
            }
            if (keyboard.name.equals("mil") && key.valueText == Mil.appName) { // 畫黃底
                keyFillColour = toPressedColour(keyFillColour);
            }
            keyFillPaint.setColor(keyFillColour);
            keyBorderPaint.setColor(key.borderColour);
            keyBorderPaint.setStrokeWidth(key.borderThickness);

            final int keyOtherColour;
            if (key == activeKey && swipeDir != SWIPE_NONE) {
                keyOtherColour = key.textSwipeColour;
            } else {
                keyOtherColour = key.otherColour;
            }
            final int keyTextColour;
            if (key == activeKey && swipeDir != SWIPE_NONE) { keyTextColour = key.textSwipeColour;
            } else if (!key.isPreviewable) { keyTextColour = key.textColour;
            } else { keyTextColour = keyOtherColour; }

            keyTextPaint.setTextSize(key.textSize);
            keyTextPaint.setColor(keyTextColour);

            keyTextShiftPaint.setTextSize(key.textSize * 6.0f / 10.0f);
            keyTextShiftPaint.setColor(keyOtherColour);

            keyTextStrokePaint.setTextSize(key.textSize * 6.0f / 10.0f);
            keyTextStrokePaint.setColor(keyOtherColour);

            keyTextCjPaint.setTextSize(key.textSize * 5.0f / 10.0f);
            keyTextCjPaint.setColor(keyOtherColour);

            keyTextJiPaint.setTextSize(key.textSize * 5.0f / 10.0f);
            keyTextJiPaint.setColor(keyOtherColour);

            keyTextUpPaint.setTextSize(key.textSize * 6.0f / 10.0f);
            keyTextUpPaint.setColor(keyOtherColour);

            keyTextDownPaint.setTextSize(key.textSize * 6.0f / 10.0f);
            keyTextDownPaint.setColor(keyOtherColour);

            keyTextLeftPaint.setTextSize(key.textSize * 6.0f / 10.0f);
            keyTextLeftPaint.setColor(keyOtherColour);

            keyTextRightPaint.setTextSize(key.textSize * 6.0f / 10.0f);
            keyTextRightPaint.setColor(keyOtherColour);

            if (shiftMode > 0) {
                keyTextShiftPaint.setColor(key.textColour);
            } else {
                switch (keyboard.name) {
                    case "mix","pure", "full", "digit" -> {
                        if (key.isPreviewable) {
                            keyTextPaint.setColor(key.textColour);
                        }
                    }
                    case "ji" -> keyTextJiPaint.setColor(key.textColour);
                    case "cj" -> keyTextCjPaint.setColor(key.textColour);
                    case "stroke" -> keyTextStrokePaint.setColor(key.textColour);
                    case "mil" -> keyTextMilPaint.setColor(key.textColour);
                }
            }

            final boolean isPreviewable = key.isPreviewable;
            final String keyDisplayText = key.displayText;
            final String keyShiftText = key.shiftText;
            final String keyStrokeText = key.strokeText;
            final String keyCjText = key.cjText;
            final String keyJiText = key.jiText;
            final String keyUpText = key.upText;
            final String keyDownText = key.downText;
            final String keyLeftText = key.leftText;
            final String keyRightText = key.rightText;
            final float keyTextX = key.width / 2f + key.textOffsetX;
            final float keyTextY = (key.height - keyTextPaint.ascent() - keyTextPaint.descent()) / 2f + key.textOffsetY;

            canvas.translate(key.x, key.y);
            canvas.drawRect(keyRectangle, keyFillPaint);
            canvas.drawRect(keyRectangle, keyBorderPaint);
            canvas.drawText(keyDisplayText, keyTextX, keyTextY, keyTextPaint);

            float keyLeftTextX = key.width / 2f + key.textOffsetX - 14f;
            if (keyboard.name.equals("mil")) { keyLeftTextX = key.width*0.8f + 14.0f; }
            float keyRightTextX = key.width / 2f + key.textOffsetX + 34.0f;
            float keyUpTextY = (key.height - keyTextPaint.ascent() - keyTextPaint.descent()) / 2f + key.textOffsetY - 40f;
            float keyDownTextY = (key.height - keyTextPaint.ascent() - keyTextPaint.descent()) / 2f + key.textOffsetY + 30f;

            if (keyShiftText.length() > 0 && isPreviewable) { canvas.drawText(keyShiftText, keyLeftTextX, keyUpTextY, keyTextShiftPaint); }
            if (keyStrokeText.length() > 0) { canvas.drawText(keyStrokeText, keyRightTextX, keyUpTextY, keyTextStrokePaint); }
            if (keyCjText.length() > 0) { canvas.drawText(keyCjText, keyLeftTextX, keyDownTextY, keyTextCjPaint); }

            float keyJiTextYv = (key.height - keyTextPaint.ascent() - keyTextPaint.descent()) / 2f + key.textOffsetY + 80f;
            if (keyJiText.length() > 0) {
                if ("ˊˇˋ˙".contains(keyJiText)) {
                    keyTextJiPaint.setTextSize(key.textSize * 5.0f / 3.0f);
                    canvas.drawText(keyJiText, keyRightTextX, keyJiTextYv, keyTextJiPaint);
                } else {
                    keyTextJiPaint.setTextSize(key.textSize * 6.0f / 10.0f);
                    canvas.drawText(keyJiText, keyRightTextX-20, keyDownTextY, keyTextJiPaint);
                }
            }

			// Shift
            if (keyUpText.length() > 0) { canvas.drawText(keyUpText, keyLeftTextX-20, keyUpTextY, keyTextUpPaint); }

			// Ji
            if (keyDownText.length() > 0) { canvas.drawText(keyDownText, keyRightTextX-20, keyDownTextY-5, keyTextDownPaint); }

			// Cj
            if (keyLeftText.length() > 0) { canvas.drawText(keyLeftText, keyLeftTextX-20, keyDownTextY-5, keyTextLeftPaint); }

			// Stroke
            if (keyRightText.length() > 0) { canvas.drawText(keyRightText, keyRightTextX-20, keyUpTextY, keyTextRightPaint); }

            canvas.translate(-key.x, -key.y);
        }
    }

    /*
      Lighten a dark colour and darken a light colour.
      Used for key press colour changes.
    */
    public static int toPressedColour(final int colour) {
        final float[] colourHSLArray = new float[3];
        ColorUtils.colorToHSL(colour, colourHSLArray);

        float colourLightness = colourHSLArray[2];

        if (colourLightness < COLOUR_LIGHTNESS_CUTOFF) {
            colourLightness = (2 * colourLightness + 1) / 3;
        } else {
            colourLightness = (2 * colourLightness) / 3;
        }

        colourHSLArray[2] = colourLightness;

        return ColorUtils.HSLToColor(colourHSLArray);
    }

    /*
      Handle logic for multiple pointers (e.g. two-thumb typing).
      The correct handling of a pointer moving outside the keyboard
      is ensured by including a 1-pixel gutter at the top of the keyboard
      (so that the pointer must move through a key-free row of pixels).
      The correct handling of merging pointers has not been implemented.
    */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int eventPointerCount = event.getPointerCount();

        if (eventPointerCount > 2) {
            sendCancelEvent();
            return true;
        }

        touchLogic:
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                final int downPointerIndex = event.getActionIndex();
                final int downPointerId = event.getPointerId(downPointerIndex);
                final int downPointerX = (int) event.getX(downPointerIndex);
                final int downPointerY = (int) event.getY(downPointerIndex);
                final Key downKey = getKeyAtPoint(downPointerX, downPointerY);
                if (isShiftKey(downKey)) {
                    sendShiftDownEvent(downPointerId);
                    if (shiftMode != SHIFT_DISABLED && shiftMode != SHIFT_PERSISTENT) {
                        downKey.displayText = "⬆";
                    } else {
                        downKey.displayText = "⇧";
                    }
                    break;
                }
                if (isCtrlKey(downKey)) {
                    sendCtrlDownEvent(downPointerId);
                    break;
                }
                if (activePointerId != NONEXISTENT_POINTER_ID) {
                    sendUpEvent(activeKey, false);
                }
                sendDownEvent(downKey, downPointerId, downPointerX, downPointerY);
            }
            case MotionEvent.ACTION_MOVE -> {
                for (int index = 0; index < eventPointerCount; index++) {
                    final int movePointerId = event.getPointerId(index);
                    final int movePointerX = (int) event.getX(index);
                    final int movePointerY = (int) event.getY(index);
                    final Key moveKey = getKeyAtPoint(movePointerX, movePointerY);

                    if (movePointerId == activePointerId) {
                        if (isShiftKey(moveKey) && !isSwipeableKey(activeKey)) {
                            sendShiftMoveToEvent(movePointerId);
                            break touchLogic;
                        }
                        if (isCtrlKey(moveKey) && !isSwipeableKey(activeKey)) {
                            sendCtrlMoveToEvent(movePointerId);
                            break touchLogic;
                        }

                        if (moveKey != activeKey || isSwipeableKey(activeKey)) {
                            sendMoveEvent(moveKey, movePointerId, movePointerX, movePointerY);
                            break touchLogic;
                        }

                        break touchLogic;
                    }

                    if (movePointerId == shiftPointerId) {
                        if (!isShiftKey(moveKey)) {
                            sendShiftMoveFromEvent(moveKey, movePointerId);
                            break touchLogic;
                        }
                    }
                    if (movePointerId == ctrlPointerId) {
                        if (!isCtrlKey(moveKey)) {
                            sendCtrlMoveFromEvent(moveKey, movePointerId);
                            break touchLogic;
                        }
                    }
                }
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                final int upPointerIndex = event.getActionIndex();
                final int upPointerId = event.getPointerId(upPointerIndex);
                final int upPointerX = (int) event.getX(upPointerIndex);
                final int upPointerY = (int) event.getY(upPointerIndex);
                final Key upKey = getKeyAtPoint(upPointerX, upPointerY);
                if ((upPointerId == shiftPointerId || isShiftKey(upKey)) && !isSwipeableKey(activeKey)) {
                    sendShiftUpEvent(true);
                    break;
                }
                if ((upPointerId == ctrlPointerId || isCtrlKey(upKey)) && !isSwipeableKey(activeKey)) {
                    sendCtrlUpEvent(true);
                    break;
                }
                if (upPointerId == activePointerId) {
                    sendUpEvent(upKey, true);
                }
            }
            case MotionEvent.ACTION_CANCEL -> sendCancelEvent();
        }

        return true;
    }

    private void sendCancelEvent() {
        shiftPointerId = NONEXISTENT_POINTER_ID;
        ctrlPointerId = NONEXISTENT_POINTER_ID;
        activeKey = null;
        activePointerId = NONEXISTENT_POINTER_ID;
        invalidate();
    }

    private void sendDownEvent(final Key key, final int pointerId, final int x, final int y) {
        if (isSwipeableKey(key)) {
            pointerDownX = x;
            pointerDownY = y;
        }
        swipeDir = SWIPE_NONE;
        keyboard.swipeDir = swipeDir;

        if (shiftPointerId != NONEXISTENT_POINTER_ID) {
            shiftMode = SHIFT_HELD;
        }

        if (ctrlPointerId != NONEXISTENT_POINTER_ID) {
            ctrlMode = CTRL_HELD;
        }
        keyboard.ctrlMode = ctrlMode;
        activeKey = key;
        activePointerId = pointerId;

        sendAppropriateExtendedPressHandlerMessage(key);
        invalidate();
    }

    private String swipe_dir(int sd) {
        return switch (sd) {
            case SWIPE_NONE -> "NONE";
            case SWIPE_UP -> "UP";
            case SWIPE_DOWN -> "DOWN";
            case SWIPE_LEFT -> "LEFT";
            case SWIPE_RIGHT -> "RIGHT";
            default -> "UNKNOWN";
        };
    }
    private void sendMoveEvent(final Key key, final int pointerId, final int x, final int y) {
        boolean shouldRedrawKeyboard = false;
        int dx = Math.abs(x - pointerDownX);
        int dy = Math.abs(y - pointerDownY);
        Log.d("KB", "swipeDir from "+swipe_dir(swipeDir)+" ("+dx+", "+dy+")");
        if (swipeDir != SWIPE_NONE) {
            if (dx < SWIPE_MX && dy < SWIPE_MY) {
                swipeDir = SWIPE_NONE;
                keyboard.swipeDir = swipeDir;
                shouldRedrawKeyboard = true;
            }
        } else if (key == activeKey && isSwipeableKey(key)) {
            if (dx >= SWIPE_MX || dy >= SWIPE_MY) {
                if (x >= pointerDownX) {
                    if (y >= pointerDownY) {
                        swipeDir = SWIPE_DOWN;
                    } else {
                        swipeDir = SWIPE_RIGHT;
                    }
                } else {
                    if (y >= pointerDownY) {
                        swipeDir = SWIPE_LEFT;
                    } else {
                        swipeDir = SWIPE_UP;
                    }
                }
                keyboard.swipeDir = swipeDir;
                removeAllExtendedPressHandlerMessages();
                shouldRedrawKeyboard = true;
                Log.d("KB", "swipeDir to "+swipe_dir(swipeDir));
            }
        } else { // move is a key change
            activeKey = key;
            removeAllExtendedPressHandlerMessages();
            sendAppropriateExtendedPressHandlerMessage(key);
            resetKeyRepeatIntervalMilliseconds();
            shouldRedrawKeyboard = true;
        }

        activePointerId = pointerId;

        if (shouldRedrawKeyboard) {
            invalidate();
        }
    }

    private void sendUpEvent(final Key key, final boolean shouldRedrawKeyboard) {
        if (swipeDir != SWIPE_NONE) {
            if (activeKey.upText.length() > 0) {
                switch (swipeDir) {
                    case SWIPE_UP:
                        keyboardListener.onSwipe(activeKey.upText);
                        break;
                    case SWIPE_DOWN:
                        keyboardListener.onSwipe(activeKey.downText);
                        break;
                    case SWIPE_LEFT:
                        keyboardListener.onSwipe(activeKey.leftText);
                        break;
                    case SWIPE_RIGHT:
                        keyboardListener.onSwipe(activeKey.rightText);
                        break;
                }
            } else {
                keyboardListener.onSwipe(activeKey.valueText);
            }
        } else if (key != null) {
            if (shiftMode != SHIFT_DISABLED && key.isShiftable) {
                keyboardListener.onKey(key.shiftText);
            } else {
                keyboardListener.onKey(key.valueText);
            }

            if (shiftMode == SHIFT_SINGLE) {
                shiftMode = SHIFT_DISABLED;
            }

            if (ctrlMode == CTRL_SINGLE) {
                ctrlMode = CTRL_DISABLED;
                keyboard.ctrlMode = ctrlMode;
                ctrlPointerId = NONEXISTENT_POINTER_ID;
            }
        }

        activeKey = null;
        activePointerId = NONEXISTENT_POINTER_ID;

        removeAllExtendedPressHandlerMessages();
        resetKeyRepeatIntervalMilliseconds();
        if (shouldRedrawKeyboard) {
            invalidate();
        }
    }

    private void sendShiftDownEvent(final int pointerId) {
        if (shiftMode == SHIFT_DISABLED) {
            shiftMode = (activeKey == null)
                    ? SHIFT_INITIATED
                    : SHIFT_HELD;
        }
        shiftPointerId = pointerId;
        invalidate();
    }

    private void sendCtrlDownEvent(final int pointerId) {
        if (ctrlMode == SHIFT_DISABLED) {
            ctrlMode =
                    (activeKey == null)
                            ? CTRL_INITIATED
                            : CTRL_HELD;
        }
        ctrlPointerId = pointerId;
        invalidate();
    }

    private void sendShiftMoveToEvent(final int pointerId) {
        shiftMode = SHIFT_HELD;
        shiftPointerId = pointerId;

        activeKey = null;
        activePointerId = NONEXISTENT_POINTER_ID;

        removeAllExtendedPressHandlerMessages();
        invalidate();
    }

    private void sendShiftMoveFromEvent(final Key key, final int pointerId) {
        sendShiftUpEvent(false);

        activeKey = key;
        activePointerId = pointerId;

        removeAllExtendedPressHandlerMessages();
        sendAppropriateExtendedPressHandlerMessage(key);
        resetKeyRepeatIntervalMilliseconds();
        invalidate();
    }

    private void sendCtrlMoveToEvent(final int pointerId) {
        ctrlMode = CTRL_HELD;
        ctrlPointerId = pointerId;

        activeKey = null;
        activePointerId = NONEXISTENT_POINTER_ID;

        removeAllExtendedPressHandlerMessages();
        invalidate();
    }

    private void sendCtrlMoveFromEvent(final Key key, final int pointerId) {
        sendCtrlUpEvent(false);
        ctrlPointerId = NONEXISTENT_POINTER_ID;
        activeKey = key;
        activePointerId = pointerId;

        removeAllExtendedPressHandlerMessages();
        sendAppropriateExtendedPressHandlerMessage(key);
        resetKeyRepeatIntervalMilliseconds();
        invalidate();
    }

    private void sendShiftUpEvent(boolean shouldRedrawKeyboard) {
        switch (shiftMode) {
            case SHIFT_SINGLE -> {
                shiftMode = SHIFT_PERSISTENT;
                keyboard.shiftMode = keyboard.shiftMode | KeyEvent.META_SHIFT_MASK;
            }
            case SHIFT_INITIATED -> {
                shiftMode = SHIFT_SINGLE;
                keyboard.shiftMode = keyboard.shiftMode | KeyEvent.META_SHIFT_MASK;
            }
            case SHIFT_PERSISTENT, SHIFT_HELD -> {
                shiftMode = SHIFT_DISABLED;
                keyboard.shiftMode = keyboard.shiftMode & ~KeyEvent.META_SHIFT_MASK;
            }
        }
        shiftPointerId = NONEXISTENT_POINTER_ID;
        if (shouldRedrawKeyboard) {
            invalidate();
        }
    }

    private void sendCtrlUpEvent(boolean shouldRedrawKeyboard) {
        switch (ctrlMode) {
            case CTRL_SINGLE, CTRL_HELD -> {
                ctrlMode = CTRL_DISABLED;
                keyboard.ctrlMode = keyboard.ctrlMode & ~KeyEvent.META_CTRL_MASK;
            }
            case CTRL_DISABLED, CTRL_INITIATED -> {
                ctrlMode = CTRL_SINGLE;
                keyboard.ctrlMode = keyboard.ctrlMode | KeyEvent.META_CTRL_MASK;
            }
        }
        ctrlPointerId = NONEXISTENT_POINTER_ID;
        if (shouldRedrawKeyboard) {
            invalidate();
        }
    }

    private Key getKeyAtPoint(final int x, final int y) {
        for (final Key key : keyList) {
            if (key.containsPoint(x, y)) {
                return key;
            }
        }

        return null;
    }

    private boolean isShiftKey(final Key key) {
        return key != null && key.valueText.equals(FsimeService.SHIFT_KEY_VALUE_TEXT);
    }

    private boolean isCtrlKey(final Key key) {
        return key != null && key.valueText.equals(FsimeService.CTRL_KEY_VALUE_TEXT);
    }

    private boolean isSwipeableKey(final Key key) {
        return key != null && key.isSwipeable;
    }

    private void sendAppropriateExtendedPressHandlerMessage(final Key key) {
        if (key == null) {
            return;
        }
        if (key.isRepeatable) {
            sendExtendedPressHandlerMessage(MESSAGE_KEY_REPEAT, KEY_REPEAT_START_MILLISECONDS);
        } else if (key.isLongPressable) {
            sendExtendedPressHandlerMessage(MESSAGE_LONG_PRESS, KEY_LONG_PRESS_MILLISECONDS);
        }
    }

    private void sendExtendedPressHandlerMessage(final int messageWhat, final long delayMilliseconds) {
        final Message message = extendedPressHandler.obtainMessage(messageWhat);
        extendedPressHandler.sendMessageDelayed(message, delayMilliseconds);
    }

    private void removeAllExtendedPressHandlerMessages() {
        extendedPressHandler.removeCallbacksAndMessages(null);
    }
}
