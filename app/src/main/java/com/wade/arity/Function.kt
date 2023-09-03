/*
 * Copyright (C) 2008 Mihai Preda.
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

import com.wade.arity.ArityException

/**
 * Base class for functions.
 *
 *
 * A function has an arity (the number of arguments), and a way for evaluation
 * given the values of the arguments.
 *
 *
 * To create user-defined functions,
 * derive from this class and override one of the eval() methods.
 *
 *
 *
 * If the user subclasses Function, he is responsible for the thread-safety of
 * his user-defined Functions.
 */
abstract class Function {
    private var cachedDerivate: Function? = null
    var comment: String? = null

    /**
     * Gives the arity of this function.
     * @return the arity (the number of arguments). Arity >= 0.
     */
    abstract fun arity(): Int
    var derivative: Function?
        get() {
            if (cachedDerivate == null) {
                cachedDerivate = Derivative(this)
            }
            return cachedDerivate
        }
        set(deriv) {
            cachedDerivate = deriv
        }

    /**
     * Evaluates an arity-0 function (a function with no arguments).
     * @return the value of the function
     */
    open fun eval(): Double {
        throw ArityException(0)
    }

    /**
     * Evaluates a function with a single argument (arity == 1).
     */
    open fun eval(x: Double): Double {
        throw ArityException(1)
    }

    /**
     * Evaluates a function with two arguments (arity == 2).
     */
    open fun eval(x: Double, y: Double): Double {
        throw ArityException(2)
    }

    /**
     * Evaluates the function given the argument values.
     * @param args array containing the arguments.
     * @return the value of the function
     * @throws ArityException if args.length != arity.
     */
    open fun eval(args: DoubleArray): Double {
        when (args.size) {
            0 -> return eval()
            1 -> return eval(args[0])
            2 -> return eval(args[0], args[1])
        }
        throw ArityException(args.size)
    }

    /** By default complex forwards to real eval is the arguments are real,
     * otherwise returns NaN.
     * This allow calling any real functions as a (restricted) complex one.
     */
    open fun evalComplex(): Complex? {
        checkArity(0)
        return Complex(eval(), 0.0)
    }

    /**
     * Complex evaluates a function with a single argument.
     */
    open fun eval(x: Complex): Complex? {
        checkArity(1)
        return Complex(if (x.im == 0.0) eval(x.re) else Double.NaN, 0.0)
    }

    /**
     * Complex evaluates a function with two arguments.
     */
    open fun eval(x: Complex, y: Complex): Complex? {
        checkArity(2)
        return Complex(if (x.im == 0.0 && y.im == 0.0) eval(x.re, y.re) else Double.NaN, 0.0)
    }

    /**
     * Complex evaluates a function with an arbitrary number of arguments.
     */
    open fun eval(args: Array<Complex>): Complex? {
        return when (args.size) {
            0 -> evalComplex()
            1 -> eval(args[0])
            2 -> eval(args[0], args[1])
            else -> {
                val len = args.size
                checkArity(len)
                val reArgs = DoubleArray(len)
                var i = args.size - 1
                while (i >= 0) {
                    if (args[i].im != 0.0) {
                        return Complex(Double.NaN, 0.0)
                    }
                    reArgs[i] = args[i].re
                    --i
                }
                Complex(eval(reArgs), 0.0)
            }
        }
    }

    @Throws(ArityException::class)
    fun checkArity(nArgs: Int) {
        if (arity() != nArgs) {
            throw ArityException("Expected " + arity() + " arguments, got " + nArgs)
        }
    }
}