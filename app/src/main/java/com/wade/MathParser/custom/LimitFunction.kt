package com.wade.MathParser.custom

import com.wade.MathParser.MathParser
import com.wade.MathParser.MathParser.MathVariable
import com.wade.MathParser.exception.MathParserException

/**
 * TO-DO: Write a new function for limit.
 */
object LimitFunction {
    @JvmStatic
    @Throws(MathParserException::class)
    fun limit(parser: MathParser?, `var`: MathVariable?, exp: String?, approach: Double): Double {
        val func = FunctionWrapper(parser!!, exp!!, `var`!!)
        val below = limitFromBelow(func, approach)
        val above = limitFromAbove(func, approach)
        //System.out.println(below + " : " + above);
        return if (below == above) below else Double.NaN
    }

    @Throws(MathParserException::class)
    fun limitFromBelow(function: FunctionWrapper, approach: Double): Double {
        var d = approach - 10
        while (d <= approach) {
            if (function.apply(d) == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY
            } else if (function.apply(d) == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY
            } else if (java.lang.Double.isNaN(function.apply(d))) {
                return function.apply(approach + (approach - d) * 10)
            } else {
                if (d == approach) {
                    return function.apply(d)
                } else if (approach - d < 0.00000000001) d = approach
            }
            d = approach - (approach - d) / 10
        }
        return Double.NaN
    }

    @Throws(MathParserException::class)
    fun limitFromAbove(function: FunctionWrapper, approach: Double): Double {
        var d = approach + 10
        while (d >= approach) {
            if (function.apply(d) == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY
            } else if (function.apply(d) == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY
            } else if (java.lang.Double.isNaN(function.apply(d))) {
                return function.apply(approach + (approach - d) * 10)
            } else {
                if (d == approach) {
                    return function.apply(d)
                } else if (d - approach < 0.00000000001) d = approach
            }
            d = approach - (approach - d) / 10
        }
        return Double.NaN
    }
}