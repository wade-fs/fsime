package com.wade.MathParser.custom

import com.wade.MathParser.exception.MathParserException

// https://github.com/gbenroscience/ParserNG/blob/master/src/main/java/math/numericalmethods/Integration.java
class Integration {
    private var function: FunctionWrapper? = null // Function to be integrated
    private var setFunction = false // = true when Function set
    private var lowerLimit = Double.NaN // Lower integration limit
    private var upperLimit = Double.NaN // Upper integration limit
    private var setLimits = false // = true when limits set
    private var glPoints = 0 // Number of points in the Gauss-Legendre integration
    private var setGLpoints = false // = true when glPoints set
    private var nIntervals = 0 // Number of intervals in the rectangular rule integrations
    private var setIntervals = false // = true when nIntervals set
    private var integralSum = 0.0 // Sum returned by the numerical integration method
    private var setIntegration = false // = true when integration performed

    // Iterative trapezium rule
    private var requiredAccuracy =
        0.0 // required accuracy at which iterative trapezium is terminated

    // Get the actual accuracy acheived when the iterative trapezium calls were terminated, using the instance method
    var trapeziumAccuracy =
        0.0 // actual accuracy at which iterative trapezium is terminated as instance variable
        private set
    private var maxIntervals = 0 // maximum number of intervals allowed in iterative trapezium

    // Get the number of intervals at which accuracy was last met in trapezium if using the instance trapezium call
    var trapeziumIntervals =
        1 // number of intervals in trapezium at which accuracy was satisfied as instance variable
        private set

    // CONSTRUCTORS
    // Constructor taking function to be integrated
    constructor(intFunc: FunctionWrapper?) {
        function = intFunc
        setFunction = true
    }

    // Constructor taking function to be integrated and the limits
    constructor(intFunc: FunctionWrapper?, lowerLimit: Double, upperLimit: Double) {
        function = intFunc
        setFunction = true
        this.lowerLimit = lowerLimit
        this.upperLimit = upperLimit
        setLimits = true
    }

    // SET METHODS
    // Set function to be integrated
    fun setFunction(intFunc: FunctionWrapper?) {
        function = intFunc
        setFunction = true
    }

    // Set limits
    fun setLimits(lowerLimit: Double, upperLimit: Double) {
        this.lowerLimit = lowerLimit
        this.upperLimit = upperLimit
        setLimits = true
    }

    // Set lower limit
    fun setLowerLimit(lowerLimit: Double) {
        this.lowerLimit = lowerLimit
        if (!java.lang.Double.isNaN(upperLimit)) setLimits = true
    }

    // Set upper limit
    fun setUpperLimit(upperLimit: Double) {
        this.upperLimit = upperLimit
        if (!java.lang.Double.isNaN(lowerLimit)) setLimits = true
    }

    // Set number of points in the Gaussian Legendre integration
    fun setGLpoints(nPoints: Int) {
        glPoints = nPoints
        setGLpoints = true
    }

    // Set number of intervals in trapezoidal, forward or backward rectangular integration
    fun setNintervals(nIntervals: Int) {
        this.nIntervals = nIntervals
        setIntervals = true
    }

    // GET METHODS
    // Get the sum returned by the numerical integration
    fun getIntegralSum(): Double {
        require(setIntegration) { "No integration has been performed" }
        return integralSum
    }

    // GAUSSIAN-LEGENDRE QUADRATURE
    // Numerical integration using n point Gaussian-Legendre quadrature (instance method)
    // All parameters preset
    @Throws(MathParserException::class)
    fun gaussQuad(): Double {
        require(setGLpoints) { "Number of points not set" }
        require(setLimits) { "One limit or both limits not set" }
        require(setFunction) { "No integral function has been set" }
        var gaussQuadDist = DoubleArray(glPoints)
        var gaussQuadWeight = DoubleArray(glPoints)
        var sum = 0.0
        val xplus = 0.5 * (upperLimit + lowerLimit)
        val xminus = 0.5 * (upperLimit - lowerLimit)
        var dx = 0.0
        var test = true
        var k = -1
        var kn = -1

        // Get Gauss-Legendre coefficients, i.e. the weights and scaled distances
        // Check if coefficients have been already calculated on an earlier call
        if (!gaussQuadIndex.isEmpty()) {
            k = 0
            while (k < gaussQuadIndex.size) {
                val ki = gaussQuadIndex[k]
                if (ki == glPoints) {
                    test = false
                    kn = k
                }
                k++
            }
        }
        if (test) {
            // Calculate and store coefficients
            gaussQuadCoeff(gaussQuadDist, gaussQuadWeight, glPoints)
            gaussQuadIndex.add(glPoints)
            gaussQuadDistArrayList.add(gaussQuadDist)
            gaussQuadWeightArrayList.add(gaussQuadWeight)
        } else {
            // Recover coefficients
            gaussQuadDist = gaussQuadDistArrayList[kn]
            gaussQuadWeight = gaussQuadWeightArrayList[kn]
        }

        // Perform summation
        for (i in 0 until glPoints) {
            dx = xminus * gaussQuadDist[i]
            sum += gaussQuadWeight[i] * function!!.apply(xplus + dx)
        }
        integralSum = sum * xminus // rescale
        setIntegration = true // integration performed
        return integralSum // return value
    }

    // Numerical integration using n point Gaussian-Legendre quadrature (instance method)
    // All parametes except the number of points in the Gauss-Legendre integration preset
    @Throws(MathParserException::class)
    fun gaussQuad(glPoints: Int): Double {
        this.glPoints = glPoints
        setGLpoints = true
        return this.gaussQuad()
    }

    // TRAPEZIUM METHODS
    // Numerical integration using the trapeziodal rule (instance method)
    // all parameters preset
    @Throws(MathParserException::class)
    fun trapezium(): Double {
        require(setIntervals) { "Number of intervals not set" }
        require(setLimits) { "One limit or both limits not set" }
        require(setFunction) { "No integral function has been set" }
        var y1 = 0.0
        var interval = (upperLimit - lowerLimit) / nIntervals
        var x0 = lowerLimit
        var x1 = lowerLimit + interval
        var y0 = function!!.apply(x0)
        integralSum = 0.0
        for (i in 0 until nIntervals) {
            // adjust last interval for rounding errors
            if (x1 > upperLimit) {
                x1 = upperLimit
                interval -= x1 - upperLimit
            }

            // perform summation
            y1 = function!!.apply(x1)
            integralSum += 0.5 * (y0 + y1) * interval
            x0 = x1
            y0 = y1
            x1 += interval
        }
        setIntegration = true
        return integralSum
    }

    // Numerical integration using the trapeziodal rule (instance method)
    // all parameters except the number of intervals preset
    @Throws(MathParserException::class)
    fun trapezium(nIntervals: Int): Double {
        this.nIntervals = nIntervals
        setIntervals = true
        return this.trapezium()
    }

    // Numerical integration using an iteration on the number of intervals in the trapeziodal rule
    // until two successive results differ by less than a predetermined accuracy times the penultimate result
    @Throws(MathParserException::class)
    fun trapezium(accuracy: Double, maxIntervals: Int): Double {
        requiredAccuracy = accuracy
        this.maxIntervals = maxIntervals
        trapeziumIntervals = 1
        var summ = trapezium(function, lowerLimit, upperLimit, 1)
        var oldSumm = summ
        var i = 2
        i = 2
        while (i <= this.maxIntervals) {
            summ = trapezium(function, lowerLimit, upperLimit, i)
            trapeziumAccuracy = Math.abs((summ - oldSumm) / oldSumm)
            if (trapeziumAccuracy <= requiredAccuracy) break
            oldSumm = summ
            i++
        }
        if (i > this.maxIntervals) {
            println("accuracy criterion was not met in Integration.trapezium - current sum was returned as result.")
            trapeziumIntervals = this.maxIntervals
        } else {
            trapeziumIntervals = i
        }
        trapIntervals = trapeziumIntervals
        trapAccuracy = trapeziumAccuracy
        return summ
    }

    // BACKWARD RECTANGULAR METHODS
    // Numerical integration using the backward rectangular rule (instance method)
    // All parameters preset
    @Throws(MathParserException::class)
    fun backward(): Double {
        require(setIntervals) { "Number of intervals not set" }
        require(setLimits) { "One limit or both limits not set" }
        require(setFunction) { "No integral function has been set" }
        var interval = (upperLimit - lowerLimit) / nIntervals
        var x = lowerLimit + interval
        var y: Double
        integralSum = 0.0
        for (i in 0 until nIntervals) {
            // adjust last interval for rounding errors
            if (x > upperLimit) {
                x = upperLimit
                interval -= x - upperLimit
            }

            // perform summation
            y = function!!.apply(x)
            integralSum += y * interval
            x += interval
        }
        setIntegration = true
        return integralSum
    }

    // Numerical integration using the backward rectangular rule (instance method)
    // all parameters except number of intervals preset
    @Throws(MathParserException::class)
    fun backward(nIntervals: Int): Double {
        this.nIntervals = nIntervals
        setIntervals = true
        return this.backward()
    }

    // FORWARD RECTANGULAR METHODS
    // Numerical integration using the forward rectangular rule
    // all parameters preset
    @Throws(MathParserException::class)
    fun forward(): Double {
        var interval = (upperLimit - lowerLimit) / nIntervals
        var x = lowerLimit
        var y: Double
        integralSum = 0.0
        for (i in 0 until nIntervals) {
            // adjust last interval for rounding errors
            if (x > upperLimit) {
                x = upperLimit
                interval -= x - upperLimit
            }

            // perform summation
            y = function!!.apply(x)
            integralSum += y * interval
            x += interval
        }
        setIntegration = true
        return integralSum
    }

    // Numerical integration using the forward rectangular rule
    // all parameters except number of intervals preset
    @Throws(MathParserException::class)
    fun forward(nIntervals: Int): Double {
        this.nIntervals = nIntervals
        setIntervals = true
        return this.forward()
    }

    companion object {
        // ArrayLists to hold Gauss-Legendre Coefficients saving repeated calculation
        private val gaussQuadIndex = ArrayList<Int>() // Gauss-Legendre indices
        private val gaussQuadDistArrayList = ArrayList<DoubleArray>() // Gauss-Legendre distances
        private val gaussQuadWeightArrayList = ArrayList<DoubleArray>() // Gauss-Legendre weights

        // Get the actual accuracy acheived when the iterative trapezium calls were terminated, using the static method
        var trapAccuracy =
            0.0 // actual accuracy at which iterative trapezium is terminated as class variable
            private set

        // Get the number of intervals at which accuracy was last met in trapezium if using static trapezium call
        var trapIntervals =
            1 // number of intervals in trapezium at which accuracy was satisfied as class variable
            private set

        // Numerical integration using n point Gaussian-Legendre quadrature (static method)
        // All parametes provided
        @Throws(MathParserException::class)
        fun gaussQuad(
            intFunc: FunctionWrapper?,
            lowerLimit: Double,
            upperLimit: Double,
            glPoints: Int
        ): Double {
            val intgrtn = Integration(intFunc, lowerLimit, upperLimit)
            return intgrtn.gaussQuad(glPoints)
        }

        // Returns the distance (gaussQuadDist) and weight coefficients (gaussQuadCoeff)
        // for an n point Gauss-Legendre Quadrature.
        // The Gauss-Legendre distances, gaussQuadDist, are scaled to -1 to 1
        // See Numerical Recipes for details
        fun gaussQuadCoeff(gaussQuadDist: DoubleArray, gaussQuadWeight: DoubleArray, n: Int) {
            var z = 0.0
            var z1 = 0.0
            var pp = 0.0
            var p1 = 0.0
            var p2 = 0.0
            var p3 = 0.0
            val eps = 3e-11 // set required precision
            val x1 = -1.0 // lower limit
            val x2 = 1.0 // upper limit

            //  Calculate roots
            // Roots are symmetrical - only half calculated
            val m = (n + 1) / 2
            val xm = 0.5 * (x2 + x1)
            val xl = 0.5 * (x2 - x1)

            // Loop for  each root
            for (i in 1..m) {
                // Approximation of ith root
                z = Math.cos(Math.PI * (i - 0.25) / (n + 0.5))

                // Refinement on above using Newton's method
                do {
                    p1 = 1.0
                    p2 = 0.0

                    // Legendre polynomial (p1, evaluated at z, p2 is polynomial of
                    //  one order lower) recurrence relationsip
                    for (j in 1..n) {
                        p3 = p2
                        p2 = p1
                        p1 = ((2.0 * j - 1.0) * z * p2 - (j - 1.0) * p3) / j
                    }
                    pp = n * (z * p1 - p2) / (z * z - 1.0) // Derivative of p1
                    z1 = z
                    z = z1 - p1 / pp // Newton's method
                } while (Math.abs(z - z1) > eps)
                gaussQuadDist[i - 1] = xm - xl * z // Scale root to desired interval
                gaussQuadDist[n - i] = xm + xl * z // Symmetric counterpart
                gaussQuadWeight[i - 1] = 2.0 * xl / ((1.0 - z * z) * pp * pp) // Compute weight
                gaussQuadWeight[n - i] = gaussQuadWeight[i - 1] // Symmetric counterpart
            }
        }

        // Numerical integration using the trapeziodal rule (static method)
        // all parameters to be provided
        @Throws(MathParserException::class)
        fun trapezium(
            intFunc: FunctionWrapper?,
            lowerLimit: Double,
            upperLimit: Double,
            nIntervals: Int
        ): Double {
            val intgrtn = Integration(intFunc, lowerLimit, upperLimit)
            return intgrtn.trapezium(nIntervals)
        }

        // Numerical integration using an iteration on the number of intervals in the trapeziodal rule (static method)
        // until two successive results differ by less than a predtermined accuracy times the penultimate result
        // All parameters to be provided
        @Throws(MathParserException::class)
        fun trapezium(
            intFunc: FunctionWrapper?,
            lowerLimit: Double,
            upperLimit: Double,
            accuracy: Double,
            maxIntervals: Int
        ): Double {
            val intgrtn = Integration(intFunc, lowerLimit, upperLimit)
            return intgrtn.trapezium(accuracy, maxIntervals)
        }

        // Numerical integration using the backward rectangular rule (static method)
        // all parameters must be provided
        @Throws(MathParserException::class)
        fun backward(
            intFunc: FunctionWrapper?,
            lowerLimit: Double,
            upperLimit: Double,
            nIntervals: Int
        ): Double {
            val intgrtn = Integration(intFunc, lowerLimit, upperLimit)
            return intgrtn.backward(nIntervals)
        }

        // Numerical integration using the forward rectangular rule (static method)
        // all parameters provided
        @Throws(MathParserException::class)
        fun forward(
            integralFunc: FunctionWrapper?,
            lowerLimit: Double,
            upperLimit: Double,
            nIntervals: Int
        ): Double {
            val intgrtn = Integration(integralFunc, lowerLimit, upperLimit)
            return intgrtn.forward(nIntervals)
        }
    }
}