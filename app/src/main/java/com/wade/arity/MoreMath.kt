/*
 * Copyright (C) 2006-2009 Mihai Preda.
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
 *
 */
package com.wade.arity

internal object MoreMath {
    val TAG = "Calculator"
    private val LOG2E = 1.4426950408889634074
    private val PI2 = Math.PI + Math.PI
    private val PI23 = (Math.PI + Math.PI) / 3
    private val PI32 = Math.PI * 3 / 2
    private val HALF_PI = Math.PI / 2
    private val NINTY = HALF_PI
    private val SIXTY = Math.PI / 3
    private val FORTY_FIVE = Math.PI / 4
    private val THIRTY = Math.PI / 6
    fun trunc(x: Double): Double {
        return if (x >= 0) Math.floor(x) else Math.ceil(x)
    }

    @JvmStatic
    fun gcd(x: Double, y: Double): Double {
        var x = x
        var y = y
        if (java.lang.Double.isNaN(x) || java.lang.Double.isNaN(y) ||
            java.lang.Double.isInfinite(x) || java.lang.Double.isInfinite(y)
        ) {
            return Double.NaN
        }
        x = Math.abs(x)
        y = Math.abs(y)
        while (x < y * 1e15) {
            val save = y
            y = x % y
            x = save
        }
        return x
    }

    @JvmField
    val GAMMA = doubleArrayOf(
        57.156235665862923517,
        -59.597960355475491248,
        14.136097974741747174,
        -0.49191381609762019978,
        .33994649984811888699e-4,
        .46523628927048575665e-4,
        -.98374475304879564677e-4,
        .15808870322491248884e-3,
        -.21026444172410488319e-3,
        .21743961811521264320e-3,
        -.16431810653676389022e-3,
        .84418223983852743293e-4,
        -.26190838401581408670e-4,
        .36899182659531622704e-5
    )

    fun lgamma(x: Double): Double {
        var x = x
        val tmp = x + 5.2421875 //== 607/128. + .5;
        var sum = 0.99999999999999709182
        for (i in GAMMA.indices) {
            sum += GAMMA[i] / ++x
        }
        return (0.9189385332046727418 //LN_SQRT2PI, ln(sqrt(2*pi))
                + Math.log(sum)
                + ((tmp - 4.7421875) * Math.log(tmp))) - tmp
    }

    val FACT = doubleArrayOf(
        1.0,
        40320.0,
        2.0922789888E13,
        6.204484017332394E23,
        2.631308369336935E35,
        8.159152832478977E47,
        1.2413915592536073E61,
        7.109985878048635E74,
        1.2688693218588417E89,
        6.1234458376886085E103,
        7.156945704626381E118,
        1.8548264225739844E134,
        9.916779348709496E149,
        1.0299016745145628E166,
        1.974506857221074E182,
        6.689502913449127E198,
        3.856204823625804E215,
        3.659042881952549E232,
        5.5502938327393044E249,
        1.3113358856834524E267,
        4.7147236359920616E284,
        2.5260757449731984E302
    )

    @JvmStatic
    fun factorial(x: Double): Double {
        var x = x
        if (x < 0.0) { // x <= -1 ?
            return Double.NaN
        }
        if (x <= 170.0) {
            if (Math.floor(x) == x) {
                val n = x.toInt()
                var extra = x
                when (n and 7) {
                    7 -> {
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        return FACT[n shr 3] * extra
                    }

                    6 -> {
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        return FACT[n shr 3] * extra
                    }

                    5 -> {
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        return FACT[n shr 3] * extra
                    }

                    4 -> {
                        extra *= --x
                        extra *= --x
                        extra *= --x
                        return FACT[n shr 3] * extra
                    }

                    3 -> {
                        extra *= --x
                        extra *= --x
                        return FACT[n shr 3] * extra
                    }

                    2 -> {
                        extra *= --x
                        return FACT[n shr 3] * extra
                    }

                    1 -> return FACT[n shr 3] * extra
                    0 -> return FACT[n shr 3]
                }
            }
        }
        return Math.exp(lgamma(x))
    }

    @JvmStatic
    fun combinations(n: Double, k: Double): Double {
        var k = k
        if (n < 0.0 || k < 0.0) {
            return Double.NaN
        }
        if (n < k) {
            return 0.0
        }
        if (Math.floor(n) == n && Math.floor(k) == k) {
            k = Math.min(k, n - k)
            if ((n <= 170.0) && (12.0 < k) && (k <= 170.0)) {
                return factorial(n) / factorial(k) / factorial(n - k)
            } else {
                var r = 1.0
                val diff = n - k
                var i = k
                while (i > .5 && r < Double.POSITIVE_INFINITY) {
                    r *= (diff + i) / i
                    --i
                }
                return r
            }
        } else {
            return Math.exp(lgamma(n) - lgamma(k) - lgamma(n - k))
        }
    }

    @JvmStatic
    fun permutations(n: Double, k: Double): Double {
        if (n < 0.0 || k < 0.0) {
            return Double.NaN
        }
        if (n < k) {
            return 0.0
        }
        if (Math.floor(n) == n && Math.floor(k) == k) {
            if ((n <= 170.0) && (10.0 < k) && (k <= 170.0)) {
                return factorial(n) / factorial(n - k)
            } else {
                var r = 1.0
                val limit = n - k + .5
                var i = n
                while (i > limit && r < Double.POSITIVE_INFINITY) {
                    r *= i
                    --i
                }
                return r
            }
        } else {
            return Math.exp(lgamma(n) - lgamma(n - k))
        }
    }

    fun log2(x: Double): Double {
        return Math.log(x) * LOG2E
    }

    private fun isPiMultiple(x: Double): Boolean {
        // x % y == 0
        val d = x / 180.0
        return d == Math.floor(d)
    }

    @JvmStatic
    fun sin(x: Double): Double {
        return if (isPiMultiple(x)) 0.0 else Math.sin(x * Math.PI / 180.0)
    }

    @JvmStatic
    fun cos(x: Double): Double {
        return if (isPiMultiple(x + 270.0)) 0.0 else Math.cos(x * Math.PI / 180.0)
    }

    @JvmStatic
    fun tan(x: Double): Double {
        return if (isPiMultiple(x)) 0.0 else Math.tan(x * Math.PI / 180.0)
    }

    @JvmStatic
    fun sec(x: Double): Double {
        var x = x
        if (x < 0.0) return sec(-x)
        x = (x + 360.0) % 360.0
        if (x == 90.0) return Double.NaN else if (x == 0.0) return 1.0 else return if (x == 180.0) -1.0 else 1.0 / Math.cos(
            x * Math.PI / 180.0
        )
    }

    @JvmStatic
    fun csc(x: Double): Double {
        var x = x
        if (x < 0.0) return -csc(-x)
        x = (x + 360.0) % 360.0
        if (x == 0.0 || x == 180.0) return Double.NaN else if (x == 90.0) return 1.0 else return if (x == 270.0) -1.0 else 1.0 / Math.sin(
            x * Math.PI / 180.0
        )
    }

    @JvmStatic
    fun cot(x: Double): Double {
        var x = x
        x = (x + 180.0) % 180.0
        if (x == 0.0 || x == 180.0) return Double.NaN else return if (x == 90.0) 0.0 else 1.0 / Math.tan(
            x * Math.PI / 180.0
        )
    }

    fun asin(x: Double): Double {
        if (Math.abs(x) > 1.0) return Double.NaN else if (x < 0.0) return -asin(-x) else if (x == 0.0) return 0.0 else if (x == 0.5) return 30.0 else return if (x == 1.0) 90.0 else Math.asin(
            x
        ) * 180.0 / Math.PI
    }

    fun acos(x: Double): Double {
        if (Math.abs(x) > 1.0) return Double.NaN else if (x == -1.0) return 180.0 else if (x == -0.5) return 120.0 else if (x == 0.0) return 90.0 else if (x == 1.0) return 0.0 else return if (x == 0.5) 60.0 else 90.0 - asin(
            x
        )
    }

    @JvmStatic
    fun atan(x: Double): Double {
        if (x == 0.0) return 0.0
        if (x < 0.0) return -atan(-x) else return if (x == 1.0) 45.0 else Math.atan(x) * 180.0 / Math.PI
    }

    @JvmStatic
    fun asec(x: Double): Double {
        if (Math.abs(x) < 1.0) return Double.NaN else if (x < 0.0) return 180.0 - asec(-x) else return if (x == 1.0) 0.0 else acos(
            1.0 / x
        )
    }

    @JvmStatic
    fun acsc(x: Double): Double {
        if (Math.abs(x) < 1.0) return Double.NaN else if (x < 0.0) return -acsc(-x) else return if (x == 1.0) 90.0 else asin(
            1.0 / x
        )
    }

    @JvmStatic
    fun acot(x: Double): Double {
        if (x == 0.0) return 90.0 else if (x == 1.0) return 45.0 else return if (x == 0.0) 90.0 else 90.0 - atan(
            x
        )
    }

    fun sinh(x: Double): Double {
        return if (x == 0.0) 0.0 else Math.sinh(x) // (Math.exp(x)-Math.exp(-x)) / 2.0;
    }

    fun cosh(x: Double): Double {
        return if (x == 0.0) 1.0 else Math.cosh(x) // return (Math.exp(x)+Math.exp(-x)) / 2.0;
    }

    fun tanh(x: Double): Double {
        return if (x == 0.0) 0.0 else Math.tanh(x) // 1.0-2.0/ (Math.exp(2*x)+1);
    }

    @JvmStatic
    fun sech(x: Double): Double {
        return if (cos(x) == 0.0) 1.0 else 1.0 / Math.cosh(x)
    }

    @JvmStatic
    fun csch(x: Double): Double {
        return if (sin(x) == 0.0) Double.NaN else 1.0 / Math.sinh(x)
    }

    @JvmStatic
    fun coth(x: Double): Double {
        return if (tan(x) == 0.0) Double.NaN else 1.0 / Math.tanh(x)
    }

    @JvmStatic
    fun asinh(x: Double): Double {
        if (x < 0.0) return -asinh(-x) else return if (x == 0.0) 0.0 else Math.log(x + Math.sqrt(x * x + 1.0))
    }

    @JvmStatic
    fun acosh(x: Double): Double {
        if (x < 1.0) return Double.NaN else return if (x == 1.0) 0.0 else Math.log(x + Math.sqrt(x * x - 1.0))
    }

    @JvmStatic
    fun atanh(x: Double): Double {
        if (Math.abs(x) >= 1.0) return Double.NaN else return if (x == 0.0) 0.0 else 0.5 * Math.log(
            (1.0 + x) / (1.0 - x)
        )
    }

    @JvmStatic
    fun asech(x: Double): Double {
        return if (x > 1.0 || x <= 0.0) Double.NaN else Math.log(
            (1.0 + Math.sqrt(
                1.0 - x * x
            )) / x
        )
    }

    @JvmStatic
    fun acsch(x: Double): Double {
        return if (x == 0.0) Double.NaN else if ((x < 0)) -acsch(-x) else Math.log(
            (1.0 + Math.sqrt(1.0 + x * x)) / x
        )
    }

    @JvmStatic
    fun acoth(x: Double): Double {
        return if (Math.abs(x) <= 1.0) Double.NaN else 0.5 * Math.log(
            (x + 1.0) / (x - 1.0)
        )
    }

    fun intLog10(x: Double): Int {
        //an alternative implem is using a for loop.
        return Math.floor(Math.log10(x)).toInt()
        //return (int)log10(x);
    }

    fun intExp10(exp: Int): Double {
        return java.lang.Double.valueOf("1E$exp")
    }
}