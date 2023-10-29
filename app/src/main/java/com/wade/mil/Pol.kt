package com.wade.mil

import com.wade.libs.Tools.Deg2DmsStr2
import com.wade.libs.Tools.Deg2Mil
import com.wade.libs.Tools.POLd
import java.util.Locale

class Pol {
    var app = arrayOfNulls<Field>(2)
    var step = 0
    var angleMode = Const.MODE_AUTO
    var d = 0.0
    var a = 0.0

    internal constructor() {
        step = 0
        angleMode = Const.MODE_AUTO
        app[0] = Field("x", "橫坐標", false)
        app[1] = Field("y", "縱坐標", false)
        a = 0.0
        d = a
    }

    internal constructor(m: Int) : super() {
        angleMode = m
    }

    fun setMode(m: Int) {
        angleMode = m
        for (i in app.indices) {
            app[i]!!.setMode(m)
        }
    }

    val desc: String?
        get() = app[step]!!.desc
    var value: String?
        get() = app[step]!!.value
        set(v) {
            app[step]!!.setValue(v)
        }

    //    double getDegree() {
    //        return app[step].degree;
    //    }
    operator fun next(): Boolean {
        if (step < app.size - 1) {
            step = step + 1
            return true
        }
        return false
    }

    fun back(): Boolean {
        if (step > 0) {
            step = step - 1
            return true
        }
        return false
    }

    fun calc(): DoubleArray? {
        for (i in app.indices) {
            if (app[i]!!.value!!.length == 0) {
                return null
            }
        }
        val res = POLd(app[1]!!.getValue(), app[0]!!.getValue())
        d = res[0]
        a = res[1]
        return res
    }

    fun string(): String {
        return when (angleMode) {
            Const.MODE_DEGREE, Const.MODE_AUTO -> {
                String.format(
                    Locale.TAIWAN,
                    "距離%.2f公尺 方位角%.2f度",
                    d,
                    Deg2Mil(a)
                )
            }

            Const.MODE_DMS -> {
                String.format(
                    Locale.TAIWAN,
                    "距離%.2f公尺 方位角%s",
                    d,
                    Deg2DmsStr2(a)
                )
            }

            else -> {
                String.format(
                    Locale.TAIWAN,
                    "距離%.2f公尺 方位角%.2f密位",
                    d,
                    Deg2Mil(a)
                )
            }
        }
    }
}