package com.wade.fsime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.digitalink.Ink

class HandwritingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val strokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f
        isAntiAlias = true
    }

    private var inkBuilder = Ink.builder()
    private var strokeBuilder = Ink.Stroke.builder()
    private val currentPath = Path()
    private var callback: HandwritingListener? = null

    interface HandwritingListener {
        fun onInkFinished(ink: Ink)
    }

    fun setHandwritingListener(listener: HandwritingListener) {
        this.callback = listener
    }

    fun clear() {
        inkBuilder = Ink.builder()
        strokeBuilder = Ink.Stroke.builder()
        currentPath.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(currentPath, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val t = System.currentTimeMillis()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                strokeBuilder = Ink.Stroke.builder()
                strokeBuilder.addPoint(Ink.Point.create(x, y, t))
                currentPath.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                strokeBuilder.addPoint(Ink.Point.create(x, y, t))
                currentPath.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                strokeBuilder.addPoint(Ink.Point.create(x, y, t))
                currentPath.lineTo(x, y)
                inkBuilder.addStroke(strokeBuilder.build())
                callback?.onInkFinished(inkBuilder.build())
            }
        }
        invalidate()
        return true
    }
}
