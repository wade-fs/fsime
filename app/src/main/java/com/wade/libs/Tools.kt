package com.wade.libs

import android.graphics.PointF
import android.util.Log
import android.widget.EditText
import java.util.Locale

/**
 * Created by wade on 2017/2/20.
 */
object Tools {
    const val TAG = "MyLog"

    /* 底下一堆 Casio FX880P 提供的 Basic 裡的函數 */
    fun INT(d: Double): Double {
        return if (d > 0.0) FIX(d)
        else if (d == 0.0) 0.0
        else FIX(d)-1
    }

    /// FIX 只取整數部份，跟 INT() 在正數時很像
    fun FIX(f: Double): Double {
        return if (f < 0) -Math.floor(-f) else Math.floor(f)
    }

    fun FIX(s: String): String {
        var s = s
        if (s.indexOf('-') == 0) s = s.replace("-", "")
        return if (s.indexOf('.') >= 0) s.substring(0, s.indexOf('.')) else s
    }

    fun FRAC(d: Double): Double {
        return d - FIX(d)
    }

    fun FRAC(s: String): String {
        var s = s
        if (s.indexOf('-') == 0) s = s.replace("-", "")
        val idx = s.indexOf('.')
        return if (idx >= 0 && idx < s.length - 1) {
            s.substring(idx + 1, s.length)
        } else ""
    }

    fun SGN(s: String): Int {
        return if (s.indexOf('-') == 0) -1 else 1
    }

    fun SGN(d: Double): Int {
        return if (d > 0) 1 else if (d == 0.0) 0 else -1
    }

    // POL(X, Y) 轉成極座標的 Distance, Angle
    // C: d = POL(dy, dx, x, y) ==> return [d, y]
    fun POL(dy: Double, dx: Double): DoubleArray {
        val ret = DoubleArray(2)
        val v = PointF(dx.toFloat(), dy.toFloat())
        ret[0] = lengthOfVector(v)
        ret[1] = azimuth(v)
        while (ret[1] < 0) ret[1] += Math.PI * 2
        return ret
    }

    @JvmStatic
    fun POLd(dy: Double, dx: Double): DoubleArray {
        val res = POL(dy, dx)
        res[1] = res[1] * 180 / Math.PI
        return res
    }

    fun len(dx: Double, dy: Double): Double {
        return Math.sqrt(dx * dx + dy * dy)
    }

    // REC(Distance, Angle) 轉成直角座標 X,Y, 跟 POL() 相反
    // C: x = REC(d, a, x, y) ==> return [x, y];
    fun REC(d: Double, a: Double): DoubleArray {
        val ret = DoubleArray(2)
        ret[0] = d * COS(a)
        ret[1] = d * SIN(a)
        return ret
    }

    @JvmOverloads
    fun DEG(d: Double, m: Double, s: Double = 0.0): Double {
        return d + m / 60.0 + s / 3600.0
    }

    fun LEFTS(s: String, n: Int): String {
        return if (s.length >= n) s.substring(0, n) else s
    }

    fun RIGHTS(s: String, n: Int): String {
        return if (s.length >= n) s.substring(s.length - n) else s
    }

    // MIDS() 因為 BASIC 的索引是從 1 開始，所以在使用時要 -1
    fun MIDS(s: String, start: Int, n: Int): String {
        var start = start
        --start
        return if (s.length + start >= n) s.substring(start, n) else s.substring(start)
    }

    fun innerProduct(v1: PointF, v2: PointF): Double { // v1, v2 視為向量
        return (v1.x * v2.x + v1.y * v2.y).toDouble()
    }

    fun lengthOfVector(v1: PointF): Double {
        return Math.sqrt((v1.x * v1.x + v1.y * v1.y).toDouble())
    }

    fun lengthOfXY(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
    }

    fun intersectionAngle(v1: PointF, v2: PointF): Double {
        val l1 = lengthOfVector(v1)
        val l2 = lengthOfVector(v2)
        return if (l1 > 0 && l2 > 0) {
            Math.acos(innerProduct(v1, v2) / (l1 * l2))
        } else -100.0 // 夾角介於 -PI .. PI, 所以不可能是 -100
    }

    fun azimuth(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return (Rad2Deg(azimuth(PointF((x2 - x1).toFloat(), (y2 - y1).toFloat()))) + 360.0) % 360.0
    }

    fun azimuth(v1: PointF, v2: PointF): Double { // 方位角，而不是迪卡爾座標的方向角
        return azimuth(PointF((v2.x - v1.x), (v2.y - v1.y)))
    }

    fun azimuth(v: PointF): Double { // 方位角，而不是迪卡爾座標的方向角
        return Math.PI / 2 - vectorAngle(v)
    }

    fun vectorAngle(v1: PointF, v2: PointF): Double { // 迪卡爾座標的方向角
        return vectorAngle(PointF((v2.x - v1.x), (v2.y - v1.y)))
    }

    fun vectorAngle(v: PointF): Double { // 迪卡爾座標的方向角
        var rad: Double
        rad =
            if (Math.abs(v.x - 0.00000001) <= 0.00000001) Math.PI / 2 else if (Math.abs(
                    v.y - 0.00000001
                ) <= 0.00000001
            ) 0.0 else Math.atan((v.y / v.x).toDouble())
        if (v.x < 0) {
            rad = Math.PI + rad // rad > 0
        } else {
            if (v.y < 0) rad = 2 * Math.PI + rad // rad < 0
        }
        return rad
    }

    var parseDoubleException = false
    @JvmStatic
    fun parseDouble(s: String): Double {
        var s = s
        val sign = SGN(s)
        s = s.replace("[^\\d.]".toRegex(), "")
        if (s.length == 0) return 0.0
        var r = 0.0
        try {
            parseDoubleException = false
            r = sign * s.toDouble()
        } catch (e: NumberFormatException) {
            r = 0.0
            parseDoubleException = true
        }
        return r
    }

    fun parseDouble(et: EditText?): Double {
        return if (et == null) 0.0 else parseDouble(et.text.toString().trim { it <= ' ' })
    }

    fun Deg2Rad(a: Double): Double {
        return Math.PI * a / 180.0
    }

    @JvmStatic
    fun Deg2Mil(a: Double): Double {
        return a * 160.0 / 9.0
    }

    fun Deg2DmsStr(a: Double): String { // 45^00'0.000"
        val res = Deg2Dms3(a)
        return String.format(
            "%s%d°%02d'%d\"",
            if (res[0] == -1.0) "-" else "",
            res[1].toInt(),
            res[2].toInt(),
            Math.round(
                res[3]
            ).toInt()
        )
    }

    @JvmStatic
    fun Deg2DmsStr2(a: Double): String { // 45^00'0.000"
        val res = Deg2Dms3(a)
        return String.format(
            "%s%d°%02d'%.3f\"",
            if (res[0] == -1.0) "-" else "",
            res[1].toInt(),
            res[2].toInt(),
            res[3]
        )
    }

    @JvmStatic
    fun Deg2Dms3(a: Double): DoubleArray { // 45^00'0.000"
        var a = a
        val SGN = if (a > 0) 1 else -1
        a = Math.abs(a)
        var d = FIX(a)
        val frac = FRAC(a)
        var m = frac * 60
        var s = FRAC(m) * 60.0
        if (Math.abs(s - 60) < 0.01) {
            s = 0.0
            m++
        }
        if (Math.floor(m) == 60.0) {
            m = 0.0
            d += 1.0
        }
        return doubleArrayOf(SGN.toDouble(), d, INT(m), s)
    }

    fun Deg2Dms(a: Double): Double {
        val res = Deg2Dms3(a)
        return res[0] * (res[1] + res[2] / 100 + res[3] / 10000)
    }

    fun Mil2Deg(m: Double): Double {
        return m * 9.0 / 160.0
    }

    fun Mil2Rad(m: Double): Double {
        return Deg2Rad(Mil2Deg(m))
    }

    fun Mil2Dms(m: Double): Double {
        return Deg2Dms(Mil2Deg(m))
    }

    fun Mil2DmsStr(r: Double): String {
        return Deg2DmsStr(Mil2Deg(r))
    }

    fun Rad2Deg(r: Double): Double {
        return 180.0 / Math.PI * r
    }

    fun Rad2Dms(r: Double): Double {
        return Deg2Dms(Rad2Deg(r))
    }

    fun Rad2Mil(r: Double): Double {
        return Deg2Mil(Rad2Deg(r))
    }

    fun Rad2DmsStr(r: Double): String {
        return Deg2DmsStr(Rad2Deg(r))
    }

    fun Dms2Rad(dms: Double): Double {
        return Deg2Rad(Dms2Deg(dms))
    }

    fun Dms2Mil(dms: Double): Double {
        return Deg2Mil(Dms2Deg(dms))
    }

    fun Dms2Deg(dms: Double): Double { // 度.分秒　轉成 ##.## 度
        val m: Double
        val s: Double
        m = FRAC(dms) * 100
        s = FRAC(dms * 100) * 100
        return FIX(dms) + FIX(m) / 60.0 + s / 3600.0
    }

    fun Dms2Rad(exp: String): Double {
        return Deg2Rad(Dms2Deg(exp))
    }

    fun Dms2Mil(exp: String): Double {
        return Deg2Mil(Dms2Deg(exp))
    }

    fun Dms2Deg(D: String, M: String, S: String): String {
//        Log.d(TAG, "Dms2Deg("+D+","+M+","+S+")");
        val d = parseDouble(D)
        val m = parseDouble(M)
        val s = parseDouble(S)
        return String.format("%f", d + m / 60.0 + s / 3600.0)
    }

    fun Dms2Deg(exp: String): Double { // 度.分秒　轉成 ##.## 度
//        Log.d(TAG, "Dms2Deg("+exp+")");
        return if (exp.indexOf('\'') >= 0 || exp.indexOf('"') >= 0 || exp.indexOf('°') >= 0 || exp.indexOf(
                '^'
            ) >= 0
        ) {
            lastAutoDegreeMode = 2
            val res = exp.split("['^\"°]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val d: Double
            val m: Double
            val s: Double
            if (res.size == 1) {
                d = parseDouble(res[0])
                s = 0.0
                m = s
            } else if (res.size == 2) {
                d = parseDouble(res[0])
                m = parseDouble(res[1]) / 60.0
                s = 0.0
            } else {
                d = parseDouble(res[0])
                m = parseDouble(res[1]) / 60.0
                s = parseDouble(res[2]) / 3600.0
            }
            d + m + s
        } else {
            var dms = parseDouble(exp)
            val SGN = if (dms > 0) 1 else -1
            dms = Math.abs(dms)
            var r = 0.0
            val m: Double
            val s: Double
            m = FRAC(dms) * 100
            s = FRAC(dms * 100) * 100
            r = FIX(dms) + INT(m) / 60.0 + s / 3600.0
            SGN * r
        }
    }

    // 底下三角函數，直接轉換成角度運算，符合原程式碼
    fun SIN(z: Double): Double {
        return Math.sin(z * Math.PI / 180.0)
    }

    fun COS(z: Double): Double {
        return Math.cos(z * Math.PI / 180.0)
    }

    fun TAN(z: Double): Double {
        return Math.tan(z * Math.PI / 180.0)
    }

    fun ASIN(z: Double): Double {
        return 180.0 / Math.PI * Math.asin(z)
    }

    fun ACOS(z: Double): Double {
        return 180.0 / Math.PI * Math.acos(z)
    }

    fun ATAN(z: Double): Double {
        return 180.0 / Math.PI * Math.atan(z)
    }

    fun SQR(z: Double): Double {
        return Math.sqrt(z)
    }

    fun parseInt(s: String): Int {
        return Integer.valueOf(s.trim { it <= ' ' })
    }

    var lastAutoDegreeMode = 0
        private set

    @JvmStatic
    fun parseDMS(exp: String): Double { // 保須保證4位數
        val deg = FIX(exp)
        val frac = FRAC(exp)
        val m = LEFTS(frac, 2)
        val s = RIGHTS(frac, 2)
        val M = parseDouble(m)
        val S = parseDouble(s)
        val d = Math.abs(parseDouble(deg)) + M / 60.0 + S / 3600.0
        return SGN(exp) * d
    }

    @JvmStatic
    fun parseDMS2(exp: String): Double {
        var exp = exp
        var d = 0.0
        var m = 0.0
        var s = 0.0
        val sgn = SGN(exp).toDouble()
        if (exp[0] == '+' || exp[0] == '-') exp = exp.substring(1, exp.length - 1)
        val D: String
        val M: String
        val S: String
        var dc = '\u0000'
        if (exp.indexOf('°') > 0) dc = '°' else if (exp.indexOf('^') > 0) dc =
            '^' else if (exp.indexOf('.') > 0) dc = '.'
        if (dc != '\u0000') {
            D = exp.substring(0, exp.indexOf(dc))
            d = parseDouble(D)
            exp =
                if (exp.indexOf(dc) < exp.length - 1) exp.substring(
                    exp.indexOf(dc) + 1,
                    exp.length
                ) else ""
        }
        dc = '\''
        if (exp.indexOf(dc) > 0) {
            M = exp.substring(0, exp.indexOf(dc))
            m = parseDouble(M)
            exp =
                if (exp.indexOf(dc) < exp.length - 1) exp.substring(
                    exp.indexOf(dc) + 1,
                    exp.length
                ) else ""
        }
        dc = '"'
        if (exp.indexOf(dc) > 0) {
            S = exp.substring(0, exp.indexOf(dc))
            s = parseDouble(S)
        }
        return sgn * (d + m / 60 + s / 3600)
    }

    private fun parseMil(exp: String): Double {
//        Log.d(TAG, "parseMil("+exp+")");
        var exp = exp
        exp = exp.replace("M".toRegex(), "")
        return parseDouble(exp) * 9 / 160
    }

    fun ang2Str(ang: Double): String {
        var ang = ang
        ang = (ang + 360) % 360
        return String.format("%.2f密位(%s)", Deg2Mil(ang), Deg2DmsStr2(ang))
    }

    private fun cleanString(exp: String): String? {
        var exp = exp ?: return ""
        exp = exp.trim { it <= ' ' }.replace(" ".toRegex(), "")
        if (exp.isEmpty()) return ""
        while (exp.length > 1 && (exp.lastIndexOf('+') == exp.length - 1 || exp.lastIndexOf('-') == exp.length - 1 || exp.lastIndexOf(
                '.'
            ) == exp.length - 1)
        ) {
            exp = exp.substring(0, exp.length - 1)
        }
        return if (exp == "+" || exp == "-" || exp == ".") "" else exp
    }

    @JvmStatic
    fun autoDeg(exp: String, degreeMode: Int): Double {
        var exp = exp
        return if (exp.indexOf('\'') > 0 || exp.indexOf('"') > 0 || exp.indexOf('^') > 0 || exp.indexOf(
                '°'
            ) > 0
        ) parseDMS2(exp) else if (degreeMode == 3 || exp.indexOf('M') == exp.length - 1 || exp.indexOf(
                'm'
            ) == exp.length - 1
        ) {
            parseMil(exp)
        } else if (degreeMode == 2) {
            if (exp.indexOf('.') == 0) exp =
                "$exp.0000" else while (FRAC(exp).length < 4) exp += "0"
            if (FRAC(exp).length > 4) {
                exp = exp.substring(0, exp.indexOf('.') + 5)
                Log.d(TAG, "\t" + exp)
            }
            parseDMS(exp)
        } else if (degreeMode == 1) parseDouble(exp) else if (degreeMode == 0) {
            val f = FRAC(exp).length
            if (f < 4) { // 密位
                parseMil(exp)
            } else if (f == 4) { // 度分秒
                parseDMS(exp)
            } else {
                parseDouble(exp)
            }
        } else 0.0
    }

    private fun get(): Char {
        return if (express!!.size > 0) express!!.removeAt(0) else '\u0000'
    }

    private fun peek(): Char {
        return if (express!!.size > 0) express!![0] else '\u0000'
    }

    private fun number(degreeMode: Int): Double {
        var exp = ""
        var p = peek()
        while (p >= '0' && p <= '9' || p == '.' || p == '^' || p == '\'' || p == '"' || p == 'm' || p == 'M' || p == '°') {
            exp += get()
            p = peek()
        }
        return autoDeg(exp, degreeMode)
    }

    private fun factor(degreeMode: Int): Double {
        val p = peek()
        if (p >= '0' && p <= '9') {
            return number(degreeMode)
        } else if (p == '(') {
            get()
            val result = expression(degreeMode)
            get()
            return result
        } else if (p == '-') {
            get()
            return -factor(degreeMode)
        }
        return 0.0 // error
    }

    private fun term(degreeMode: Int): Double {
        var result = factor(degreeMode)
        var p = peek()
        while (p == '*' || p == '/' || p == '%') {
            get()
            if (p == '*') result *= factor(1) else if (p == '/') result /= factor(1) else result %= factor(
                1
            )
            p = peek()
        }
        return result
    }

    private fun expression(degreeMode: Int): Double {
        var result = term(degreeMode)
        while (peek() == '+' || peek() == '-') {
            val c = get()
            val t = term(degreeMode)
            if (c == '+') result += t else result -= t
        }
        return result
    }

    /* degreeMode 0:自動, 1:角度, 2:度分秒, 3:密位
     * 其中如果 exp 含有 ^'" 的符號，則強迫採用 度分秒, degreeMode 失效
     */
    fun auto2Deg(exp: String, degreeMode: Int): Double {
        asList(exp)
        return expression(degreeMode)
    }

    private var express: ArrayList<Char>? = null
    private fun asList(string: String) {
        express = ArrayList()
        for (c in string.toCharArray()) {
            express!!.add(c)
        }
    }

    fun auto2Rad(exp: String, degreeMode: Int): Double {
        return Deg2Rad(auto2Deg(exp, degreeMode))
    }

    fun traverse3(
        x: Double,
        y: Double,
        h: Double,
        p: Double,
        d: Double,
        a: Double,
        v: Double
    ): DoubleArray {
        var x = x
        var y = y
        var h = h
        var p = p
        var d = d
        var a = a
        val res = len2Height(h, d, v, 0.0, 0.0, 1)
        h = res[0]
        d = res[1]
        a = (a + p) % 360.0
        x = x + d * SIN(a)
        y = y + d * COS(a)
        p = (a + 180.0) % 360.0
        return doubleArrayOf(x, y, h, a, p, d)
    }

    fun Pads(
        x1: Double, y1: Double, h1: Double, p1: Double, a1: Double, v1: Double,
        x2: Double, y2: Double, h2: Double, p2: Double, a2: Double, v2: Double
    ): DoubleArray? {
        val res = DoubleArray(5)
        res[3] = (a1 + p1 + 360) % 360
        res[4] = (a2 + p2 + 360) % 360

        // 先判斷左右
        val oo = POLd(y2 - y1, x2 - x1)
        var a = res[3] - oo[1]
        if (a < 0) a += 360.0
        if (a > 180) a -= 180.0
        var b = res[4] - oo[1]
        if (b < 0) b += 360.0
        if (b > 180) b -= 180.0
        val olx: Double
        val oly: Double
        val olh: Double
        var olv: Double
        val orx: Double
        val ory: Double
        val orh: Double
        var orv: Double
        if (a < b) { // O2 在左, O1 在右
            val xx = a
            a = b
            b = xx
            olx = x2
            oly = y2
            olh = h2
            olv = v2
            orx = x1
            ory = y1
            orh = h1
            orv = v1
        } else { // O1 左，O2 右
            oo[1] = (oo[1] + 180) % 360
            olx = x1
            oly = y1
            olh = h1
            olv = v1
            orx = x2
            ory = y2
            orh = h2
            orv = v2
        }
        var fl = olv != 0.0
        var fr = orv != 0.0
        if (!fl && !fr) {
            fr = true
            fl = fr
        }
        val e = oo[0] / SIN(a - b)
        val l = e * SIN(b)
        val r = e * SIN(a)
        // 這邊透過 PADS() 求目標的時候，因為兩邊長是算出來的，基本上是平距，因此垂直角應該換算成高低角
        if (45.0 < olv && olv <= 135) olv = (90.0 - olv + 360) % 360
        if (45.0 < orv && orv <= 135) orv = (90.0 - orv + 360) % 360
        if (l <= 0 || r <= 0) {
            Log.d(
                TAG,
                String.format(
                    Locale.CHINESE,
                    "請檢查各項數值，尤其與角度有關的部份, l=%.2f, r=%.2f",
                    l,
                    r
                )
            )
            return null
        }
        val resr: DoubleArray
        val resl: DoubleArray
        resr = traverse3(orx, ory, orh, oo[1], r, b, orv)
        resl = traverse3(olx, oly, olh, oo[1], l, a, olv)
        if (fl && fr) {
            res[0] = (resr[0] + resl[0]) / 2
            res[1] = (resr[1] + resl[1]) / 2
            res[2] = (resr[2] + resl[2]) / 2
        } else if (fr) {
            res[0] = resr[0]
            res[1] = resr[1]
            res[2] = resr[2]
        } else if (fl) {
            res[0] = resl[0]
            res[1] = resl[1]
            res[2] = resl[2]
        }
        return res
    }

    fun len2(d: Double, v: Double): DoubleArray {
        return if (45.0 < v && v < 135.0) {
            doubleArrayOf(d * Math.abs(SIN(v)), (90 - v + 360) % 360)
        } else doubleArrayOf(d, v)
    }

    fun len2Height(
        h: Double,
        d: Double,
        v: Double,
        e: Double,
        c: Double,
        invert: Int
    ): DoubleArray {
        var d = d
        var v = v
        val res = len2(d, v)
        d = res[0]
        v = res[1]
        return doubleArrayOf(h + invert * (d * TAN(v) + e - c) + 6.75039e-8 * d * d, d)
    }
}