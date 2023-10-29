package com.wade.mil

import com.wade.libs.Tools.Deg2Dms3
import com.wade.libs.Tools.autoDeg
import com.wade.libs.Tools.parseDMS
import com.wade.libs.Tools.parseDMS2
import com.wade.libs.Tools.parseDouble

class Field @JvmOverloads constructor(
    var name: String?,
    @JvmField var desc: String?,
    var isAngle: Boolean = false,
    v: String? = "",
    m: Int = 0
) {
    @JvmField
    var value: String? = null
    var degree = 0.0

    // 取得欄位角度值
    fun toDegree(): Double {
        return degree
    }

    // 取得欄位密位值
    fun toMil(): Double {
        return degree * 6400 / 360.0
    }

    // 取得欄位度分秒值
    fun toDms(): DoubleArray {
        return Deg2Dms3(degree)
    }

    // 取得欄位徑度值
    fun toRad(): Double {
        return degree * Math.PI / 180.0
    }

    private fun cantainDMS(v: String?): Boolean {
        if (v!!.indexOf('°') >= 0) return true
        if (v.indexOf('\'') >= 0) return true
        return if (v.indexOf('"') >= 0) true else false
    }

    constructor(n: String?, d: String?, a: Boolean, m: Int) : this(n, d, a, "", m)

    init {
        setValue(v, m)
    }

    fun getValue(): Double {
        return if (isAngle) {
            degree
        } else parseDouble(value!!)
    }

    fun setValue(v: String?) {
        value = v
    }

    fun setValue(v: String?, m: Int) {
        value = v
        setMode(m)
    }

    fun setMode(m: Int) {
        if (!isAngle) return
        degree = if (cantainDMS(value)) {
            parseDMS2(value!!)
        } else {
            when (m) {
                Const.MODE_DEGREE ->  // degree
                    value!!.toDouble()

                Const.MODE_DMS ->  // dms
                    parseDMS(value!!)

                Const.MODE_MIL ->  // mil
                    parseDouble(value!!) * 9.0 / 160.0

                else ->  // auto
                    autoDeg(value!!, 0)
            }
        }
    }
}