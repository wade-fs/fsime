package com.wade.MathParser.custom

import com.wade.MathParser.exception.MathParserException

// https://github.com/allusai/calculus-solver/blob/master/Calculus.java
object Derivative {
    /*
     *These constants can modified to change the accuracy of approximation
     *A smaller epsilon/step size uses more memory but yields a more
     *accurate approximation of the derivative/integral respectively
     */
    private const val EPSILON = 0.0000001

    /**
     * Calculates the derivative around a certain point using
     * a numerical approximation.
     *
     * @param x the x-coordinate at which to approximate the derivative
     * @return double  the derivative at the specified point
     */
    @JvmStatic
    @Throws(MathParserException::class)
    fun getDerivative(function: FunctionWrapper, x: Double): Double {
        //The y-coordinates of the points close to the specified x-coordinates
        val yOne = function.apply(x - EPSILON)
        val yTwo = function.apply(x + EPSILON)
        return (yTwo - yOne) / (2 * EPSILON)
    }
}