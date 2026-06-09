/*
 * Copyright (C) 2022 - Amir Hossein Aghajari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.wade.fsime.math

import com.wade.fsime.math.custom.*
import com.wade.fsime.math.exception.MathFunctionInvalidArgumentsException
import com.wade.fsime.math.exception.MathInvalidParameterException
import com.wade.fsime.math.exception.MathParserException
import java.lang.reflect.Method
import java.util.*
import kotlin.math.*

object Functions {

    /**
     * All static methods in [Math] and [Functions]
     * that returns double and matches arguments will wrap a [MathFunction]
     * by [MathFunction.wrap] and import here so
     * the MathParser can recognize the functions as a built-in function.
     */
    val functions: MutableList<MathFunction> = ArrayList()

    init {
        functions.add(LogFunction())
        functions.add(RadicalFunction())
        try {
            functions.add(MathFunction.wrap(Functions::class.java.getMethod("unitDegree", MathParser::class.java, Double::class.javaPrimitiveType), "unitDegree"))
            functions.add(MathFunction.wrap(Functions::class.java.getMethod("unitMinute", MathParser::class.java, Double::class.javaPrimitiveType), "unitMinute"))
            functions.add(MathFunction.wrap(Functions::class.java.getMethod("unitSecond", MathParser::class.java, Double::class.javaPrimitiveType), "unitSecond"))
            
            functions.add(MathFunction.wrap(Functions::class.java.getMethod("sin", MathParser::class.java, Double::class.javaPrimitiveType), "sin"))
            functions.add(MathFunction.wrap(Functions::class.java.getMethod("cos", MathParser::class.java, Double::class.javaPrimitiveType), "cos"))
            functions.add(MathFunction.wrap(Functions::class.java.getMethod("tan", MathParser::class.java, Double::class.javaPrimitiveType), "tan"))
            functions.add(MathFunction.wrap(Functions::class.java.getMethod("asin", MathParser::class.java, Double::class.javaPrimitiveType), "asin"))
            functions.add(MathFunction.wrap(Functions::class.java.getMethod("acos", MathParser::class.java, Double::class.javaPrimitiveType), "acos"))
            functions.add(MathFunction.wrap(Functions::class.java.getMethod("atan", MathParser::class.java, Double::class.javaPrimitiveType), "atan"))

            functions.add(
                MathFunction.wrap(
                    Functions::class.java.getMethod(
                        "integral",
                        MathParser::class.java,
                        String::class.java,
                        String::class.java,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType
                    ), "∫"
                )
            )
            functions.add(
                MathFunction.wrap(
                    Functions::class.java.getMethod(
                        "integral",
                        MathParser::class.java,
                        String::class.java,
                        String::class.java,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType
                    ), "∫"
                )
            )
            functions.add(
                MathFunction.wrap(
                    Functions::class.java.getMethod(
                        "radical",
                        Double::class.javaPrimitiveType
                    ), "√"
                )
            )
            functions.add(
                MathFunction.wrap(
                    Functions::class.java.getMethod(
                        "radical",
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType
                    ), "√"
                )
            )
            functions.add(
                MathFunction.wrap(
                    Functions::class.java.getMethod(
                        "sigma",
                        MathParser::class.java,
                        String::class.java,
                        String::class.java,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType
                    ), "Σ"
                )
            )
            functions.add(
                MathFunction.wrap(
                    Functions::class.java.getMethod(
                        "sigma",
                        MathParser::class.java,
                        String::class.java,
                        String::class.java,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType
                    ), "Σ"
                )
            )
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        }

        val methods = ArrayList<Method>()
        methods.addAll(listOf(*Functions::class.java.methods))
        methods.addAll(listOf(*Math::class.java.methods))
        addFunctions(methods, functions)
    }

    @JvmStatic
    fun addFunctions(methods: Collection<Method>, functions: MutableList<MathFunction>) {
        methodLoop@ for (method in methods) {
            if (method.returnType == Double::class.javaPrimitiveType) {
                if (method.parameterTypes.size != 1 ||
                    (method.parameterTypes[0] != Array<Any>::class.java && method.parameterTypes[0] != Array<Double>::class.java)
                ) {
                    var index = 0
                    for (cls in method.parameterTypes) {
                        if (cls != Double::class.javaPrimitiveType && cls != String::class.java && !(index == 0 && cls == MathParser::class.java)) continue@methodLoop
                        index++
                    }
                }
                
                val wrapped = MathFunction.wrap(method)
                // Avoid adding duplicates if a function with same name and parameter count already exists
                if (functions.none { it.compareNames(wrapped.name()) && it.getParameterCount() == wrapped.getParameterCount() }) {
                    functions.add(wrapped)
                }
            }
        }
    }

    @JvmStatic
    @Throws(MathFunctionInvalidArgumentsException::class)
    fun getFunction(
        src: String,
        index: Int,
        name: String,
        count: Int,
        innerFunctions: List<MathFunction>?
    ): MathFunction? {
        var function: MathFunction? = null
        if (innerFunctions != null) {
            for (func in innerFunctions) {
                if (func.compareNames(name)) {
                    function = func
                    if (func.getParameterCount() == count || func.getParameterCount() == -1) return func
                }
            }
        }

        for (func in functions) {
            if (func.compareNames(name)) {
                function = func
                if (func.getParameterCount() == count || func.getParameterCount() == -1) return func
            }
        }

        if (function != null) throw MathFunctionInvalidArgumentsException(src, index, function, count)
        return null
    }

    /* Built-in functions */

    @JvmStatic
    fun log(a: Double, b: Double): Double {
        return ln(a) / ln(b)
    }

    @JvmStatic
    fun ln(a: Double): Double {
        return kotlin.math.ln(a)
    }

    @JvmStatic
    fun percentage(a: Double): Double {
        return a / 100.0
    }

    @JvmStatic
    fun radical(a: Double): Double {
        return sqrt(a)
    }

    @JvmStatic
    fun radical(a: Double, b: Double): Double {
        return when {
            b <= 2 -> sqrt(a)
            b == 3.0 -> cbrt(a)
            else -> a.pow(1.0 / b)
        }
    }

    @JvmStatic
    fun max(vararg a: Double?): Double {
        var out = a[0]!!
        for (b in a) out = max(out, b!!)
        return out
    }

    @JvmStatic
    fun min(vararg a: Double?): Double {
        var out = a[0]!!
        for (b in a) out = min(out, b!!)
        return out
    }

    @JvmStatic
    fun sum(vararg a: Double?): Double {
        var out = 0.0
        for (b in a) out += b!!
        return out
    }

    @JvmStatic
    fun average(vararg a: Double?): Double {
        return avg(*a)
    }

    @JvmStatic
    fun avg(vararg a: Double?): Double {
        return sum(*a) / a.size
    }

    @JvmStatic
    @Throws(MathParserException::class)
    fun sigma(parser: MathParser, variableName: String, exp: String, from: Double, to: Double): Double {
        return sigma(parser, variableName, exp, from, to, 1.0)
    }

    @JvmStatic
    @Throws(MathParserException::class)
    fun sigma(
        parser: MathParser,
        variableName: String,
        exp: String,
        fromValue: Double,
        toValue: Double,
        stepValue: Double
    ): Double {
        var from = fromValue
        var to = toValue
        var step = stepValue
        if (!Utils.isIdentifier(variableName)) throw MathInvalidParameterException("sigma(): invalid variable name ($variableName)")
        if (step == 0.0) throw MathInvalidParameterException("sigma(): step can not be 0")

        val newParser = parser.clone()
        newParser.addVariable(variableName, from, 0)
        val variable = newParser.getVariable(variableName)
        val ans = arrayOf<Any>(from)
        var out = 0.0
        if (step < 0) {
            val tmp = from
            from = to
            to = tmp
            step *= -1.0
        }

        var i = from
        while (i <= to) {
            ans[0] = i
            variable!!.updateAnswer(*ans)
            out += newParser.parse(exp)
            i += step
        }
        return out
    }

    @JvmStatic
    @Throws(MathParserException::class)
    fun lim(parser: MathParser, variable: String, exp: String): Double {
        return limit(parser, variable, exp)
    }

    @JvmStatic
    @Throws(MathParserException::class)
    fun limit(parser: MathParser, variableValue: String, exp: String): Double {
        var variable = variableValue
        val newParser = parser.clone()
        variable = variable.replace("->", "=")
        if (!variable.contains("=")) throw MathInvalidParameterException("limit(): invalid variable ($variable), must be something like x->2")

        val varSplit = variable.split("=").toTypedArray()
        val variableName = varSplit[0]
        if (!Utils.isIdentifier(variableName)) throw MathInvalidParameterException("limit(): invalid variable name ($variableName)")

        val a: Double
        varSplit[1] = Utils.realTrim(varSplit[1])
        a = if (varSplit[1].equals("+inf", ignoreCase = true) || varSplit[1].equals("inf", ignoreCase = true)) {
            Double.POSITIVE_INFINITY
        } else if (varSplit[1].equals("-inf", ignoreCase = true)) {
            Double.NEGATIVE_INFINITY
        } else {
            newParser.parse(varSplit[1])
        }

        //newParser.setRoundEnabled(false);
        newParser.addVariable(variableName, 0.0, 0)
        return LimitFunction.limit(newParser, newParser.getVariable(variableName)!!, exp, a)
    }

    @JvmStatic
    @Throws(MathParserException::class)
    fun derivative(parser: MathParser, variableName: String, exp: String, x: Double): Double {
        if (!Utils.isIdentifier(variableName)) throw MathInvalidParameterException("derivative(): invalid variable name ($variableName)")

        val newParser = parser.clone()
        newParser.isRoundEnabled = false
        newParser.addVariable(variableName, 0.0, 0)
        val variable = newParser.getVariable(variableName)
        return Derivative.getDerivative(FunctionWrapper(newParser, exp, variable!!), x)
    }

    @JvmStatic
    @Throws(MathParserException::class)
    fun intg(parser: MathParser, variableName: String, exp: String, lowerLimit: Double, upperLimit: Double): Double {
        return integral(parser, variableName, exp, lowerLimit, upperLimit, 20.0)
    }

    @JvmStatic
    @Throws(MathParserException::class)
    fun integral(parser: MathParser, variableName: String, exp: String, lowerLimit: Double, upperLimit: Double): Double {
        return integral(parser, variableName, exp, lowerLimit, upperLimit, 20.0)
    }

    @JvmStatic
    @Throws(MathParserException::class)
    fun integral(
        parser: MathParser,
        variableName: String,
        exp: String,
        lowerLimit: Double,
        upperLimit: Double,
        glPoints: Double
    ): Double {
        if (!Utils.isIdentifier(variableName)) throw MathInvalidParameterException("integral(): invalid variable name ($variableName)")

        val newParser = parser.clone()
        newParser.isRoundEnabled = false
        newParser.addVariable(variableName, 0.0, 0)
        val variable = newParser.getVariable(variableName)
        val integration = Integration(FunctionWrapper(newParser, exp, variable!!), lowerLimit, upperLimit)
        integration.gaussQuad(abs(glPoints).toInt())
        return integration.integralSum
    }

    @JvmStatic
    fun lcm(vararg x: Double?): Double {
        var result = 1.0
        for (value in x) result = lcm(value!!, result)
        return result
    }

    @JvmStatic
    fun lcm(a: Double, b: Double): Double {
        return a * b / gcd(a, b)
    }

    @JvmStatic
    fun gcd(vararg x: Double?): Double {
        var result = 0.0
        for (value in x) result = gcd(value!!, result)
        return result
    }

    private fun gcd(a: Double, b: Double): Double {
        var x = abs(a)
        var y = abs(b)
        while (y != 0.0) {
            val z = x % y
            x = y
            y = z
        }
        return x
    }

    @JvmStatic
    fun factorial(x: Double): Double {
        val number = x.toInt()
        var result: Long = 1
        for (factor in 2..number) {
            result *= factor.toLong()
        }
        return result.toDouble()
    }

    // ignore Math.log as base e
    @JvmStatic
    fun log(a: Double): Double {
        return log10(a)
    }

    @JvmStatic
    fun mod(a: Double, b: Double): Double {
        return a % b
    }

    @JvmStatic
    fun nor(a: Double, b: Double): Double {
        return not(or(a, b))
    }

    @JvmStatic
    fun not(a: Double): Double {
        return a.toLong().inv().toDouble()
    }

    @JvmStatic
    fun or(a: Double, b: Double): Double {
        return (a.toLong() or b.toLong()).toDouble()
    }

    @JvmStatic
    fun and(a: Double, b: Double): Double {
        return (a.toLong() and b.toLong()).toDouble()
    }

    @JvmStatic
    fun xor(a: Double, b: Double): Double {
        return (a.toLong() xor b.toLong()).toDouble()
    }

    @JvmStatic
    fun shiftLeft(a: Double, b: Double): Double {
        return (a.toLong() shl b.toInt()).toDouble()
    }

    @JvmStatic
    fun shiftRight(a: Double, b: Double): Double {
        return (a.toLong() shr b.toInt()).toDouble()
    }

    @JvmStatic
    fun unsignedShiftRight(a: Double, b: Double): Double {
        return (a.toLong() ushr b.toInt()).toDouble()
    }

    @JvmStatic
    fun sign(a: Double): Double {
        return a.sign
    }

    @JvmStatic
    @Throws(MathParserException::class)
    fun IF(parser: MathParser, conditionValue: String, a: String, b: String): Double {
        var condition = conditionValue
        condition = Utils.realTrim(condition)
        val matcher = Utils.splitIf.matcher(condition)
        val ca: Double
        var cb = 0.0
        var type = "!="

        if (matcher.find()) {
            ca = parser.parse(matcher.group(1)!!.trim())
            cb = parser.parse(matcher.group(3)!!.trim())
            type = matcher.group(2)!!.trim()
        } else {
            ca = parser.parse(condition)
        }

        val c = when (type) {
            "==", "=" -> ca == cb
            ">=" -> ca >= cb
            "<=" -> ca <= cb
            ">" -> ca > cb
            "<" -> ca < cb
            "<>", "!=" -> ca != cb
            else -> ca != cb
        }
        return parser.parse(if (c) a else b)
    }

    @JvmStatic
    fun unitDegree(parser: MathParser, x: Double): Double {
        return Math.toRadians(x)
    }

    @JvmStatic
    fun unitMinute(parser: MathParser, x: Double): Double {
        return Math.toRadians(x / 60.0)
    }

    @JvmStatic
    fun unitSecond(parser: MathParser, x: Double): Double {
        return Math.toRadians(x / 3600.0)
    }

    private fun isExplicitRadian(parser: MathParser): Boolean {
        return parser.hasExplicitAngleUnits
    }

    @JvmStatic
    fun sin(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        android.util.Log.d("FSIME_MATH", "sin: x=$x, mode=${parser.isRadianMode}, explicit=${parser.hasExplicitAngleUnits} -> isRadian=$isRadian")
        return kotlin.math.sin(if (isRadian) x else Math.toRadians(x))
    }

    @JvmStatic
    fun cos(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        android.util.Log.d("FSIME_MATH", "cos: x=$x, mode=${parser.isRadianMode}, explicit=${parser.hasExplicitAngleUnits} -> isRadian=$isRadian")
        return kotlin.math.cos(if (isRadian) x else Math.toRadians(x))
    }

    @JvmStatic
    fun tan(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        android.util.Log.d("FSIME_MATH", "tan: x=$x, mode=${parser.isRadianMode}, explicit=${parser.hasExplicitAngleUnits} -> isRadian=$isRadian")
        return kotlin.math.tan(if (isRadian) x else Math.toRadians(x))
    }

    @JvmStatic
    fun asin(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = kotlin.math.asin(x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun acos(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = kotlin.math.acos(x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun atan(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = kotlin.math.atan(x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun atan2(parser: MathParser, y: Double, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = kotlin.math.atan2(y, x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun cot(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        return 1.0 / kotlin.math.tan(if (isRadian) x else Math.toRadians(x))
    }

    @JvmStatic
    fun arccos(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = kotlin.math.acos(x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun acosh(x: Double): Double {
        return arccosh(x)
    }

    @JvmStatic
    fun arccosh(x: Double): Double {
        return kotlin.math.ln(x + kotlin.math.sqrt(x * x - 1))
    }

    @JvmStatic
    fun arcsin(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = kotlin.math.asin(x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun asinh(x: Double): Double {
        return arcsinh(x)
    }

    @JvmStatic
    fun arcsinh(x: Double): Double {
        return kotlin.math.ln(x + kotlin.math.sqrt(x * x + 1))
    }

    @JvmStatic
    fun sec(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        return 1.0 / kotlin.math.cos(if (isRadian) x else Math.toRadians(x))
    }

    @JvmStatic
    fun asec(parser: MathParser, x: Double): Double {
        return arcsec(parser, x)
    }

    @JvmStatic
    fun arcsec(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = kotlin.math.acos(1.0 / x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun sech(x: Double): Double {
        return 1.0 / cosh(x)
    }

    @JvmStatic
    fun asech(parser: MathParser, x: Double): Double {
        return arcsech(parser, x)
    }

    @JvmStatic
    fun arcsech(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = arccosh(1.0 / x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun csc(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        return 1.0 / kotlin.math.sin(if (isRadian) x else Math.toRadians(x))
    }

    @JvmStatic
    fun acsc(parser: MathParser, x: Double): Double {
        return arccsc(parser, x)
    }

    @JvmStatic
    fun arccsc(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = kotlin.math.asin(1.0 / x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun csch(x: Double): Double {
        return 1.0 / sinh(x)
    }

    @JvmStatic
    fun acsch(parser: MathParser, x: Double): Double {
        return arccsch(parser, x)
    }

    @JvmStatic
    fun arccsch(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = arcsinh(1.0 / x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun arctan(parser: MathParser, x: Double): Double {
        return atan(parser, x)
    }

    @JvmStatic
    fun atanh(x: Double): Double {
        return arctanh(x)
    }

    @JvmStatic
    fun arctanh(x: Double): Double {
        return 0.5 * kotlin.math.ln((1 + x) / (1 - x))
    }

    @JvmStatic
    fun coth(x: Double): Double {
        return 1.0 / tanh(x)
    }

    @JvmStatic
    fun acot(parser: MathParser, x: Double): Double {
        return arccot(parser, x)
    }

    @JvmStatic
    fun arccot(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = kotlin.math.atan(1.0 / x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun acoth(parser: MathParser, x: Double): Double {
        return arccoth(parser, x)
    }

    @JvmStatic
    fun arccoth(parser: MathParser, x: Double): Double {
        val isRadian = parser.isRadianMode || isExplicitRadian(parser)
        val res = arctanh(1.0 / x)
        return if (isRadian) res else Math.toDegrees(res)
    }

    @JvmStatic
    fun c(x: Double, y: Double): Double {
        return factorial(x) / (factorial(y) * factorial(x - y))
    }
}
