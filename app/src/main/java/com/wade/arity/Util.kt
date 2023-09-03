/*
 * Copyright (C) 2007-2008 Mihai Preda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wade.arity

/**
 * Contains static helper methods for formatting double values.
 */
object Util {
    const val LEN_UNLIMITED = 100
    const val FLOAT_PRECISION = -1

    /** Returns a number which is an approximation of v (within maxError)
     * and which has fewer digits in base-10).
     * @param value the value to be approximated
     * @param maxError the maximum deviation from value
     * @return an approximation with a more compact base-10 representation.
     */
    fun shortApprox(value: Double, maxError: Double): Double {
        val v = Math.abs(value)
        val tail = MoreMath.intExp10(MoreMath.intLog10(Math.abs(maxError)))
        val ret = Math.floor(v / tail + .5) * tail
        return if (value < 0) -ret else ret
    }

    /**
     * Returns an approximation with no more than maxLen chars.
     *
     * This method is not public, it is called through doubleToString,
     * that's why we can make some assumptions about the format of the string,
     * such as assuming that the exponent 'E' is upper-case.
     *
     * @param str the value to truncate (e.g. "-2.898983455E20")
     * @param maxLen the maximum number of characters in the returned string
     * @return a truncation no longer then maxLen (e.g. "-2.8E20" for maxLen=7).
     */
    fun sizeTruncate(str: String, maxLen: Int): String {
        if (maxLen == LEN_UNLIMITED) {
            return str
        }
        val ePos = str.lastIndexOf('E')
        val tail = if (ePos != -1) str.substring(ePos) else ""
        val tailLen = tail.length
        val headLen = str.length - tailLen
        val maxHeadLen = maxLen - tailLen
        val keepLen = Math.min(headLen, maxHeadLen)
        if (keepLen < 1 || keepLen < 2 && str.length > 0 && str[0] == '-') {
            return str // impossible to truncate
        }
        var dotPos = str.indexOf('.')
        if (dotPos == -1) {
            dotPos = headLen
        }
        if (dotPos > keepLen) {
            var exponent = if (ePos != -1) str.substring(ePos + 1).toInt() else 0
            val start = if (str[0] == '-') 1 else 0
            exponent += dotPos - start - 1
            val newStr = str.substring(0, start + 1) + '.' + str.substring(
                start + 1,
                headLen
            ) + 'E' + exponent
            return sizeTruncate(newStr, maxLen)
        }
        return str.substring(0, keepLen) + tail
    }

    /**
     * Rounds by dropping roundingDigits of double precision
     * (similar to 'hidden precision digits' on calculators),
     * and formats to String.
     * @param v the value to be converted to String
     * @param roundingDigits the number of 'hidden precision' digits (e.g. 2).
     * @return a String representation of v
     */
    fun doubleToString(v: Double, roundingDigits: Int): String {
        val absv = Math.abs(v)
        val str =
            if (roundingDigits == FLOAT_PRECISION) java.lang.Float.toString(absv.toFloat()) else java.lang.Double.toString(
                absv
            )
        val buf = StringBuffer(str)
        var roundingStart =
            if (roundingDigits <= 0 || roundingDigits > 13) 17 else 16 - roundingDigits
        val ePos = str.lastIndexOf('E')
        var exp = if (ePos != -1) str.substring(ePos + 1).toInt() else 0
        if (ePos != -1) {
            buf.setLength(ePos)
        }
        var len = buf.length

        //remove dot
        var dotPos: Int
        dotPos = 0
        while (dotPos < len && buf[dotPos] != '.') {
            ++dotPos
        }
        exp += dotPos
        if (dotPos < len) {
            buf.deleteCharAt(dotPos)
            --len
        }

        //round
        var p = 0
        while (p < len && buf[p] == '0') {
            ++roundingStart
            ++p
        }
        if (roundingStart < len) {
            if (buf[roundingStart] >= '5') {
                var p: Int
                p = roundingStart - 1
                while (p >= 0 && buf[p] == '9') {
                    buf.setCharAt(p, '0')
                    --p
                }
                if (p >= 0) {
                    buf.setCharAt(p, (buf[p].code + 1).toChar())
                } else {
                    buf.insert(0, '1')
                    ++roundingStart
                    ++exp
                }
            }
            buf.setLength(roundingStart)
        }

        //re-insert dot
        if (exp < -5 || exp > 10) {
            buf.insert(1, '.')
            --exp
        } else {
            for (i in len until exp) {
                buf.append('0')
            }
            for (i in exp..0) {
                buf.insert(0, '0')
            }
            buf.insert(if (exp <= 0) 1 else exp, '.')
            exp = 0
        }
        len = buf.length

        //remove trailing dot and 0s.
        var tail: Int
        tail = len - 1
        while (tail >= 0 && buf[tail] == '0') {
            buf.deleteCharAt(tail)
            --tail
        }
        if (tail >= 0 && buf[tail] == '.') {
            buf.deleteCharAt(tail)
        }
        if (exp != 0) {
            buf.append('E').append(exp)
        }
        if (v < 0) {
            buf.insert(0, '-')
        }
        return buf.toString()
    }

    /**
     * Renders a real number to a String (for user display).
     * @param maxLen the maximum total length of the resulting string
     * @param rounding the number of final digits to round
     */
    fun doubleToString(x: Double, maxLen: Int, rounding: Int): String {
        return sizeTruncate(doubleToString(x, rounding), maxLen)
    }

    /**
     * Renders a complex number to a String (for user display).
     * @param maxLen the maximum total length of the resulting string
     * @param rounding the number of final digits to round
     */
    fun complexToString(x: Complex, maxLen: Int, rounding: Int): String {
        //System.out.println("" + x.re + ' ' + x.im);
        var maxLen = maxLen
        if (x.im == 0.0) {
            return doubleToString(x.re, maxLen, rounding)
        }
        if (x.isNaN) {
            return "NaN"
        }
        var xre = x.re
        var xim = x.im
        if (x.isInfinite) {
            if (!java.lang.Double.isInfinite(xre)) {
                xre = 0.0
            } else if (!java.lang.Double.isInfinite(xim)) {
                xim = 0.0
            }
        }
        if (xim == 0.0) {
            return doubleToString(xre, maxLen, rounding)
        }

        // insert plus between re & im
        val addPlus = xre != 0.0 && xim >= 0
        var sre = if (xre == 0.0) "" else doubleToString(xre, rounding)
        var sim = doubleToString(xim, rounding)
        val finalMultiply = if (java.lang.Double.isInfinite(xim)) "*" else ""
        if (sim == "1") {
            sim = ""
        }
        if (sim == "-1") {
            sim = "-"
        }
        if (maxLen != LEN_UNLIMITED) {
            --maxLen // for final "i"
            if (addPlus) {
                --maxLen
            }
            maxLen -= finalMultiply.length
            val sreLen = sre.length
            val simLen = sim.length
            val reduce = sreLen + simLen - maxLen
            if (reduce > 0) {
                val diff = Math.abs(sreLen - simLen)
                val rShort = if (reduce > diff) (reduce - diff) / 2 else 0
                val rLong = rShort + Math.min(reduce, diff)
                var sreTarget = sreLen
                var simTarget = simLen
                if (sreLen > simLen) {
                    sreTarget -= rLong
                    simTarget -= rShort
                } else {
                    sreTarget -= rShort
                    simTarget -= rLong
                }
                if (sreTarget + simTarget > maxLen) {
                    --simTarget
                }
                sre = sizeTruncate(sre, sreTarget)
                sim = sizeTruncate(sim, simTarget)
            }
        }
        return sre + (if (addPlus) "+" else "") + sim + finalMultiply + 'i'
    }
}