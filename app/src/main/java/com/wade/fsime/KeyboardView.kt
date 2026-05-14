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
package com.wade.fsime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils

/*
  A view that holds a keyboard.
  Touch logic is implemented here.
*/
class KeyboardView(context: Context, attributes: AttributeSet?) : View(context, attributes), View.OnClickListener {
    private var keyboardListener: KeyboardListener? = null
    var keyboard: Keyboard? = null
        set(value) {
            field = value
            value?.let {
                keyboardListener?.saveKeyboard(it)
                keyList = it.getKeyList()
                keyboardFillPaint.color = it.fillColour
                if (it.shiftState != Keyboard.ModifierState.PERSISTENT) {
                    it.shiftState = Keyboard.ModifierState.DISABLED
                }
                it.ctrlState = Keyboard.ModifierState.DISABLED
            }
            requestLayout()
        }
    private var keyList: List<Key>? = null

    // Active key
    private var activeKey: Key? = null
    private var activePointerId = NONEXISTENT_POINTER_ID

    // Long presses and key repeats
    private lateinit var extendedPressHandler: Handler
    private var keyRepeatIntervalMilliseconds = 0

    // Horizontal swipes
    private var pointerDownX = 0
    private var pointerDownY = 0
    val SWIPE_NONE = 0
    val SWIPE_RU = 1
    val SWIPE_LD = 2
    val SWIPE_LU = 4
    val SWIPE_RD = 8
    private var swipeDir = SWIPE_NONE // 0: false, 1: RU, 2: LD, 4: LU, 8: RD, 9: R, 6: L

    // Shift key
    private var shiftPointerId = NONEXISTENT_POINTER_ID
    private var ctrlPointerId = NONEXISTENT_POINTER_ID

    // Global gesture tracking
    private var isGlobalGesture = false
    private var pointerStartX = 0
    private var pointerStartY = 0

    // Keyboard drawing
    private lateinit var keyboardRectangle: Rect
    private lateinit var keyboardFillPaint: Paint

    // Key drawing
    private lateinit var keyRectangle: Rect
    private lateinit var keyFillPaint: Paint
    private lateinit var keyBorderPaint: Paint
    private lateinit var keyTextPaint: Paint
    private lateinit var keyTextShiftPaint: Paint
    private lateinit var keyTextStrokePaint: Paint
    private lateinit var keyTextUpPaint: Paint
    private lateinit var keyTextDownPaint: Paint
    private lateinit var keyTextLeftPaint: Paint
    private lateinit var keyTextRightPaint: Paint

    init {
        initialiseExtendedPressHandler()
        initialiseDrawing(context)
    }

    private fun initialiseExtendedPressHandler() {
        resetKeyRepeatIntervalMilliseconds()
        extendedPressHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                activeKey?.let {
                    when (message.what) {
                        MESSAGE_KEY_REPEAT -> {
                            keyboardListener?.onKey(it)
                            sendExtendedPressHandlerMessage(MESSAGE_KEY_REPEAT, keyRepeatIntervalMilliseconds.toLong())
                        }
                        MESSAGE_LONG_PRESS -> {
                            keyboardListener?.onLongPress(it)
                            activeKey = null
                            activePointerId = NONEXISTENT_POINTER_ID
                            invalidate()
                        }
                    }
                }
            }
        }
    }

    private fun initialiseDrawing(context: Context) {
        setBackgroundColor(Color.TRANSPARENT)
        keyboardRectangle = Rect()
        keyboardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        keyRectangle = Rect()
        keyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        keyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        val typeface = Typeface.createFromAsset(context.assets, KEYBOARD_FONT_FILE_NAME)
        keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        keyTextShiftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.RIGHT
        }
        keyTextStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.RIGHT
        }
        keyTextUpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.LEFT
        }
        keyTextDownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.LEFT
        }
        keyTextLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.LEFT
        }
        keyTextRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.LEFT
        }
    }

    interface KeyboardListener {
        fun onKey(key: Key)
        fun onLongPress(key: Key)
        fun onSwipe(key: Key, swipeDir: Int)
        fun onGlobalSwipe(direction: Int)
        fun saveKeyboard(keyboard: Keyboard)
    }

    fun setKeyboardListener(keyboardListener: KeyboardListener?) {
        this.keyboardListener = keyboardListener
    }

    fun setMainInputPlane(mainInputPlane: LinearLayout?) {}

    fun resetKeyRepeatIntervalMilliseconds() {
        keyRepeatIntervalMilliseconds = DEFAULT_KEY_REPEAT_INTERVAL_MILLISECONDS
    }

    fun setKeyRepeatIntervalMilliseconds(milliseconds: Int) {
        keyRepeatIntervalMilliseconds = milliseconds
    }

    override fun onClick(view: View) {
        // Touch logic implemented in onTouchEvent
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val keyboardWidth = keyboard?.width ?: 0
        val keyboardHeight = keyboard?.height ?: 0
        keyboardRectangle.set(0, 0, keyboardWidth, keyboardHeight)
        setMeasuredDimension(keyboardWidth, keyboardHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val kb = keyboard ?: return
        val list = keyList ?: return

        canvas.drawRect(keyboardRectangle, keyboardFillPaint)

        for (key in list) {
            drawKey(canvas, key, kb)
        }
    }

    private fun drawKey(canvas: Canvas, key: Key, kb: Keyboard) {
        keyRectangle.set(0, 0, key.width, key.height)
        var keyFillColour = key.fillColour
        if (key === activeKey ||
            (key.isShiftKey && (shiftPointerId != NONEXISTENT_POINTER_ID || kb.shiftState != Keyboard.ModifierState.DISABLED)) ||
            (key.isCtrlKey && kb.ctrlState != Keyboard.ModifierState.DISABLED)
        ) {
            keyFillColour = toPressedColour(keyFillColour)
        }
        keyFillPaint.color = keyFillColour
        keyBorderPaint.color = key.borderColour
        keyBorderPaint.strokeWidth = key.borderThickness.toFloat()

        val keyOtherColour = if (key === activeKey && swipeDir != SWIPE_NONE) key.textSwipeColour else key.otherColour
        val keyTextColour = if (key === activeKey && swipeDir != SWIPE_NONE) key.textSwipeColour else if (!key.isPreviewable) key.textColour else keyOtherColour

        keyTextPaint.apply {
            textSize = key.textSize.toFloat()
            color = keyTextColour
        }
        keyTextShiftPaint.apply {
            textSize = key.textSize * 6.0f / 10.0f
            color = keyOtherColour
        }
        keyTextStrokePaint.apply {
            textSize = key.textSize * 6.0f / 10.0f
            color = keyOtherColour
        }
        keyTextUpPaint.apply {
            textSize = key.textSize * 6.0f / 10.0f
            color = keyOtherColour
        }
        keyTextDownPaint.apply {
            textSize = key.textSize * 6.0f / 10.0f
            color = keyOtherColour
        }
        keyTextLeftPaint.apply {
            textSize = key.textSize * 6.0f / 10.0f
            color = keyOtherColour
        }
        keyTextRightPaint.apply {
            textSize = key.textSize * 6.0f / 10.0f
            color = keyOtherColour
        }

        if (kb.shiftState != Keyboard.ModifierState.DISABLED) {
            keyTextShiftPaint.color = key.textColour
        } else {
            when (kb.name) {
                "mix", "pure", "full", "digit" -> if (key.isPreviewable) keyTextPaint.color = key.textColour
                "stroke" -> keyTextStrokePaint.color = key.textColour
            }
        }

        val isPreviewable = key.isPreviewable
        val keyDisplayText = key.displayText
        val keyShiftText = key.shiftText ?: ""
        val keyStrokeText = key.strokeText ?: ""
        val keyJiText = key.jiText ?: ""
        val keyCjText = key.cjText ?: ""
        val keyUpText = if (key.upText?.isNotEmpty() == true) key.upText else ""
        val keyDownText = if (key.downText?.isNotEmpty() == true) key.downText else ""
        val keyLeftText = if (key.leftText?.isNotEmpty() == true) key.leftText else ""
        val keyRightText = if (key.rightText?.isNotEmpty() == true) key.rightText else ""
        
        val keyTextX = key.width / 2f + key.textOffsetX
        val keyTextY = (key.height - keyTextPaint.ascent() - keyTextPaint.descent()) / 2f + key.textOffsetY

        canvas.translate(key.x.toFloat(), key.y.toFloat())
        canvas.drawRect(keyRectangle, keyFillPaint)
        canvas.drawRect(keyRectangle, keyBorderPaint)
        
        // Draw the main label, taking shift state into account
        val labelToDraw = key.shiftAwareDisplayText(kb.shiftMode) ?: ""
        canvas.drawText(labelToDraw, keyTextX, keyTextY, keyTextPaint)

        val keyLeftTextX = key.width / 2f + key.textOffsetX - 14f
        val keyRightTextX = key.width / 2f + key.textOffsetX + 34.0f
        val keyUpTextY = (key.height - keyTextPaint.ascent() - keyTextPaint.descent()) / 2f + key.textOffsetY - 40f
        val keyDownTextY = (key.height - keyTextPaint.ascent() - keyTextPaint.descent()) / 2f + key.textOffsetY + 30f

        if (keyUpText.isNotEmpty()) canvas.drawText(keyUpText, keyLeftTextX - 20, keyUpTextY, keyTextUpPaint)
        else if (keyShiftText.isNotEmpty() && keyShiftText != labelToDraw) {
            // Only draw shiftText in the corner if it's not a single letter (uppercase A-Z)
            if (!(keyShiftText.length == 1 && keyShiftText[0] in 'A'..'Z')) {
                canvas.drawText(keyShiftText, keyLeftTextX - 20, keyUpTextY, keyTextUpPaint)
            }
        }
        
        if (keyDownText.isNotEmpty()) canvas.drawText(keyDownText, keyRightTextX - 20, keyDownTextY - 5, keyTextDownPaint)
        else if (keyCjText.isNotEmpty()) canvas.drawText(keyCjText, keyRightTextX - 20, keyDownTextY - 5, keyTextDownPaint)
        
        if (keyLeftText.isNotEmpty()) canvas.drawText(keyLeftText, keyLeftTextX - 20, keyDownTextY - 5, keyTextLeftPaint)
        else if (keyJiText.isNotEmpty() && kb.name != "pure") canvas.drawText(keyJiText, keyLeftTextX - 20, keyDownTextY - 5, keyTextLeftPaint)
        
        if (keyRightText.isNotEmpty()) canvas.drawText(keyRightText, keyRightTextX - 20, keyUpTextY, keyTextRightPaint)
        else if (keyStrokeText.isNotEmpty()) canvas.drawText(keyStrokeText, keyRightTextX - 20, keyUpTextY, keyTextRightPaint)

        canvas.translate((-key.x).toFloat(), (-key.y).toFloat())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val kb = keyboard ?: return super.onTouchEvent(event)
        val eventPointerCount = event.pointerCount
        if (eventPointerCount > 2) {
            sendCancelEvent()
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val downPointerIndex = event.actionIndex
                val downPointerId = event.getPointerId(downPointerIndex)
                val downPointerX = event.getX(downPointerIndex).toInt()
                val downPointerY = event.getY(downPointerIndex).toInt()
                
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    pointerStartX = downPointerX
                    pointerStartY = downPointerY
                    isGlobalGesture = false
                }

                val downKey = getKeyAtPoint(downPointerX, downPointerY)
                if (downKey?.isShiftKey == true) {
                    sendShiftDownEvent(downPointerId)
                    return true
                }
                if (downKey?.isCtrlKey == true) {
                    sendCtrlDownEvent(downPointerId)
                    return true
                }
                if (activePointerId != NONEXISTENT_POINTER_ID) {
                    sendUpEvent(activeKey, false)
                }
                sendDownEvent(downKey, downPointerId, downPointerX, downPointerY)
            }
            MotionEvent.ACTION_MOVE -> {
                for (index in 0 until eventPointerCount) {
                    val movePointerId = event.getPointerId(index)
                    val movePointerX = event.getX(index).toInt()
                    val movePointerY = event.getY(index).toInt()

                    if (!isGlobalGesture && movePointerId == activePointerId) {
                        val dx = movePointerX - pointerStartX
                        val dy = movePointerY - pointerStartY
                        if (Math.abs(dx) > GLOBAL_SWIPE_THRESHOLD && Math.abs(dy) < GLOBAL_SWIPE_THRESHOLD / 2) {
                            isGlobalGesture = true
                            activeKey = null
                            activePointerId = NONEXISTENT_POINTER_ID
                            removeAllExtendedPressHandlerMessages()
                            swipeDir = if (dx > 0) SWIPE_RU else SWIPE_LD // Using existing RU/LD as R/L placeholders for now
                            invalidate()
                            return true
                        }
                        if (Math.abs(dy) > GLOBAL_SWIPE_THRESHOLD && Math.abs(dx) < GLOBAL_SWIPE_THRESHOLD / 2) {
                            isGlobalGesture = true
                            activeKey = null
                            activePointerId = NONEXISTENT_POINTER_ID
                            removeAllExtendedPressHandlerMessages()
                            swipeDir = if (dy > 0) SWIPE_RD else SWIPE_LU // Using existing RD/LU as D/U placeholders for now
                            invalidate()
                            return true
                        }
                    }

                    val moveKey = getKeyAtPoint(movePointerX, movePointerY)
                    if (movePointerId == activePointerId) {
                        activeKey?.let { ak ->
                            if (moveKey?.isShiftKey == true && !isSwipeableKey(ak)) {
                                sendShiftMoveToEvent(movePointerId)
                                return true
                            }
                            if (moveKey?.isCtrlKey == true && !isSwipeableKey(ak)) {
                                sendCtrlMoveToEvent(movePointerId)
                                return true
                            }
                            if (moveKey != ak || isSwipeableKey(ak)) {
                                if (isSwipeableKey(ak)) {
                                    sendMoveEvent(ak, movePointerId, movePointerX, movePointerY)
                                } else {
                                    sendMoveEvent(moveKey, movePointerId, movePointerX, movePointerY)
                                }
                                return true
                            }
                        }
                        return true
                    }
                    if (movePointerId == shiftPointerId) {
                        if (moveKey?.isShiftKey == false) {
                            sendShiftMoveFromEvent(moveKey, movePointerId)
                            return true
                        }
                    }
                    if (movePointerId == ctrlPointerId) {
                        if (moveKey?.isCtrlKey == false) {
                            sendCtrlMoveFromEvent(moveKey, movePointerId)
                            return true
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val upPointerIndex = event.actionIndex
                val upPointerId = event.getPointerId(upPointerIndex)
                val upPointerX = event.getX(upPointerIndex).toInt()
                val upPointerY = event.getY(upPointerIndex).toInt()
                val upKey = getKeyAtPoint(upPointerX, upPointerY)

                if (isGlobalGesture) {
                    keyboardListener?.onGlobalSwipe(swipeDir)
                    isGlobalGesture = false
                    swipeDir = SWIPE_NONE
                    invalidate()
                    return true
                }

                if ((upPointerId == shiftPointerId || upKey?.isShiftKey == true) && !isSwipeableKey(activeKey)) {
                    sendShiftUpEvent(true)
                    return true
                }
                if ((upPointerId == ctrlPointerId || upKey?.isCtrlKey == true) && !isSwipeableKey(activeKey)) {
                    sendCtrlUpEvent(true)
                    return true
                }
                if (upPointerId == activePointerId) {
                    sendUpEvent(upKey, true)
                }
            }
            MotionEvent.ACTION_CANCEL -> sendCancelEvent()
        }
        return true
    }

    private fun sendCancelEvent() {
        shiftPointerId = NONEXISTENT_POINTER_ID
        ctrlPointerId = NONEXISTENT_POINTER_ID
        keyboard?.resetModifiers()
        activeKey = null
        activePointerId = NONEXISTENT_POINTER_ID
        invalidate()
    }

    private fun sendDownEvent(key: Key?, pointerId: Int, x: Int, y: Int) {
        if (isSwipeableKey(key)) {
            pointerDownX = x
            pointerDownY = y
        }
        swipeDir = SWIPE_NONE
        keyboard?.swipeDir = swipeDir
        if (shiftPointerId != NONEXISTENT_POINTER_ID) {
            keyboard?.shiftState = Keyboard.ModifierState.HELD
        }
        if (ctrlPointerId != NONEXISTENT_POINTER_ID) {
            keyboard?.ctrlState = Keyboard.ModifierState.HELD
        }
        activeKey = key
        activePointerId = pointerId
        sendAppropriateExtendedPressHandlerMessage(key)
        invalidate()
    }

    private fun sendMoveEvent(key: Key?, pointerId: Int, x: Int, y: Int) {
        var shouldRedrawKeyboard = false
        val dx = Math.abs(x - pointerDownX)
        val dy = Math.abs(y - pointerDownY)
        if (swipeDir != SWIPE_NONE) {
            if (dx < SWIPE_MX && dy < SWIPE_MY) {
                swipeDir = SWIPE_NONE
                keyboard?.swipeDir = swipeDir
                shouldRedrawKeyboard = true
            }
        } else if (key != null && key === activeKey && isSwipeableKey(key)) {
            if (dx >= SWIPE_MX || dy >= SWIPE_MY) {
                if (dx >= dy) {
                    swipeDir = if (x >= pointerDownX) SWIPE_RU else SWIPE_LD
                } else {
                    swipeDir = if (y >= pointerDownY) SWIPE_RD else SWIPE_LU
                }
                keyboard?.swipeDir = swipeDir
                removeAllExtendedPressHandlerMessages()
                shouldRedrawKeyboard = true
            }
        } else {
            activeKey = key
            removeAllExtendedPressHandlerMessages()
            sendAppropriateExtendedPressHandlerMessage(key)
            resetKeyRepeatIntervalMilliseconds()
            shouldRedrawKeyboard = true
        }
        activePointerId = pointerId
        if (shouldRedrawKeyboard) {
            invalidate()
        }
    }

    private fun sendUpEvent(key: Key?, shouldRedrawKeyboard: Boolean) {
        if (swipeDir != SWIPE_NONE) {
            activeKey?.let { keyboardListener?.onSwipe(it, swipeDir) }
        } else if (key != null) {
            keyboardListener?.onKey(key)
            keyboard?.onKeyUp(key)
        }
        activeKey = null
        activePointerId = NONEXISTENT_POINTER_ID
        removeAllExtendedPressHandlerMessages()
        resetKeyRepeatIntervalMilliseconds()
        if (shouldRedrawKeyboard) {
            invalidate()
        }
    }

    private fun sendShiftDownEvent(pointerId: Int) {
        keyboard?.onModifierDown(true, activeKey != null)
        shiftPointerId = pointerId
        invalidate()
    }

    private fun sendCtrlDownEvent(pointerId: Int) {
        keyboard?.onModifierDown(false, activeKey != null)
        ctrlPointerId = pointerId
        invalidate()
    }

    private fun sendShiftMoveToEvent(pointerId: Int) {
        keyboard?.onModifierMoveTo(true)
        shiftPointerId = pointerId
        activeKey = null
        activePointerId = NONEXISTENT_POINTER_ID
        removeAllExtendedPressHandlerMessages()
        invalidate()
    }

    private fun sendShiftMoveFromEvent(key: Key?, pointerId: Int) {
        sendShiftUpEvent(false)
        activeKey = key
        activePointerId = pointerId
        removeAllExtendedPressHandlerMessages()
        sendAppropriateExtendedPressHandlerMessage(key)
        resetKeyRepeatIntervalMilliseconds()
        invalidate()
    }

    private fun sendCtrlMoveToEvent(pointerId: Int) {
        keyboard?.onModifierMoveTo(false)
        ctrlPointerId = pointerId
        activeKey = null
        activePointerId = NONEXISTENT_POINTER_ID
        removeAllExtendedPressHandlerMessages()
        invalidate()
    }

    private fun sendCtrlMoveFromEvent(key: Key?, pointerId: Int) {
        sendCtrlUpEvent(false)
        ctrlPointerId = NONEXISTENT_POINTER_ID
        activeKey = key
        activePointerId = pointerId
        removeAllExtendedPressHandlerMessages()
        sendAppropriateExtendedPressHandlerMessage(key)
        resetKeyRepeatIntervalMilliseconds()
        invalidate()
    }

    private fun sendShiftUpEvent(shouldRedrawKeyboard: Boolean) {
        keyboard?.onModifierUp(true)
        shiftPointerId = NONEXISTENT_POINTER_ID
        if (shouldRedrawKeyboard) {
            invalidate()
        }
    }

    private fun sendCtrlUpEvent(shouldRedrawKeyboard: Boolean) {
        keyboard?.onModifierUp(false)
        ctrlPointerId = NONEXISTENT_POINTER_ID
        if (shouldRedrawKeyboard) {
            invalidate()
        }
    }

    private fun getKeyAtPoint(x: Int, y: Int): Key? {
        return keyList?.firstOrNull { it.containsPoint(x, y) }
    }

    private fun isSwipeableKey(key: Key?): Boolean {
        if (key == null) return false
        return key.isSwipeable || (!key.isModifier && !key.isRepeatable)
    }

    private fun sendAppropriateExtendedPressHandlerMessage(key: Key?) {
        key?.let {
            if (it.isRepeatable) {
                sendExtendedPressHandlerMessage(MESSAGE_KEY_REPEAT, KEY_REPEAT_START_MILLISECONDS.toLong())
            } else if (it.isLongPressable) {
                sendExtendedPressHandlerMessage(MESSAGE_LONG_PRESS, KEY_LONG_PRESS_MILLISECONDS.toLong())
            }
        }
    }

    private fun sendExtendedPressHandlerMessage(messageWhat: Int, delayMilliseconds: Long) {
        val message = extendedPressHandler.obtainMessage(messageWhat)
        extendedPressHandler.sendMessageDelayed(message, delayMilliseconds)
    }

    private fun removeAllExtendedPressHandlerMessages() {
        extendedPressHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val NONEXISTENT_POINTER_ID = -1
        private const val MESSAGE_KEY_REPEAT = 1
        private const val MESSAGE_LONG_PRESS = 2
        private const val DEFAULT_KEY_REPEAT_INTERVAL_MILLISECONDS = 75
        private const val KEY_REPEAT_START_MILLISECONDS = 500
        private const val KEY_LONG_PRESS_MILLISECONDS = 750
        private const val SWIPE_MX = 40
        private const val SWIPE_MY = 40
        private const val GLOBAL_SWIPE_THRESHOLD = 150
        const val KEYBOARD_FONT_FILE_NAME = "StrokeInputFont.ttf"
        private const val COLOUR_LIGHTNESS_CUTOFF = 0.7f

        @JvmStatic
        fun toPressedColour(colour: Int): Int {
            val colourHSLArray = FloatArray(3)
            ColorUtils.colorToHSL(colour, colourHSLArray)
            var colourLightness = colourHSLArray[2]
            colourLightness = if (colourLightness < COLOUR_LIGHTNESS_CUTOFF) {
                (2 * colourLightness + 1) / 3
            } else {
                (2 * colourLightness) / 3
            }
            colourHSLArray[2] = colourLightness
            return ColorUtils.HSLToColor(colourHSLArray)
        }
    }
}
