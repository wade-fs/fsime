package com.wade.MathParser

import com.wade.MathParser.exception.MathFunctionInvalidArgumentsException
import com.wade.MathParser.exception.MathParserException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

interface MathFunction {

    /**
     * @return the name of function
     */
    fun name(): String

    /**
     * @return True if name mentions this function
     */
    fun compareNames(name: String): Boolean

    /**
     * Calculates and returns the value
     * Parameters are usually double values
     */
    @Throws(MathParserException::class)
    fun calculate(vararg parameters: Any): Double

    fun getParameterCount(): Int

    /**
     * @return True if the parameter at specified index won't be a double value (String otherwise)
     */
    fun isSpecialParameter(index: Int): Boolean

    /**
     * Calls when this function just attached to a parser or it's parent variable called to calculate
     */
    fun attachToParser(parser: MathParser)

    companion object {

        @JvmStatic
        fun wrap(method: Method): MathFunction {
            return wrap(method, method.name)
        }

        @JvmStatic
        fun wrap(method: Method, name: String): MathFunction {
            return object : MathFunction {

                var parser: MathParser? = null

                override fun name(): String {
                    return name
                }

                override fun compareNames(name: String): Boolean {
                    return this.name().trim { it <= ' ' }.equals(name.trim { it <= ' ' }, ignoreCase = true)
                }

                @Throws(MathParserException::class)
                override fun calculate(vararg parameters: Any): Double {
                    try {
                        val pars = ArrayList<Any?>()
                        if (method.parameterTypes.isNotEmpty() && method.parameterTypes[0] == MathParser::class.java) {
                            pars.add(parser)
                        }
                        if (parameterCount == -1) {
                            pars.add(parameters)
                        } else {
                            pars.addAll(listOf(*parameters))
                        }
                        return method.invoke(null, *pars.toTypedArray()) as Double
                    } catch (e: IllegalAccessException) {
                        if (e.cause is MathParserException) throw (e.cause as MathParserException)
                    } catch (e: InvocationTargetException) {
                        if (e.cause is MathParserException) throw (e.cause as MathParserException)
                    }
                    return parameters[0] as Double
                }

                override fun getParameterCount(): Int {
                    var count = method.parameterCount
                    var first = 0
                    if (count >= 1 && method.parameterTypes[0] == MathParser::class.java) {
                        count--
                        first++
                    }
                    if (count == 1 && method.parameterTypes[first].isArray) return -1

                    return count
                }

                override fun isSpecialParameter(index: Int): Boolean {
                    var first = 0
                    if (method.parameterCount >= 1 && method.parameterTypes[0] == MathParser::class.java) {
                        first++
                    }

                    if (method.parameterCount <= index + first) return false

                    return method.parameterTypes[index + first] == String::class.java
                }

                override fun attachToParser(parser: MathParser) {
                    this.parser = parser
                }
            }
        }

        @JvmStatic
        fun wrap(exp: String): MathFunction {
            val trimmedExp = Utils.realTrim(exp)
            val v = arrayOf(trimmedExp.substring(0, trimmedExp.indexOf('=')), trimmedExp.substring(trimmedExp.indexOf('=') + 1))
            val name = v[0].substring(0, v[0].indexOf('('))
            val vars = v[0].substring(v[0].indexOf('(') + 1, v[0].indexOf(')'))
            return wrap(name, vars.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), v[1])
        }

        @JvmStatic
        fun wrap(functionName: String, variables: Array<String>, exp: String): MathFunction {
            val name = functionName.trim { it <= ' ' }

            return object : MathFunction {

                var parser: MathParser? = null
                var mVariables: Array<MathParser.MathVariable>? = null

                override fun name(): String {
                    return name
                }

                override fun compareNames(name: String): Boolean {
                    return this.name().trim { it <= ' ' }.equals(name.trim { it <= ' ' }, ignoreCase = true)
                }

                @Throws(MathParserException::class)
                override fun calculate(vararg parameters: Any): Double {
                    if (parameters.size != mVariables!!.size) {
                        throw MathFunctionInvalidArgumentsException(null, -1, this, parameters.size)
                    }

                    for (i in parameters.indices) {
                        mVariables!![i].updateAnswer(parameters[i])
                    }

                    return parser!!.parse(exp)
                }

                override fun getParameterCount(): Int {
                    return variables.size
                }

                override fun isSpecialParameter(index: Int): Boolean {
                    return false
                }

                override fun attachToParser(parser: MathParser) {
                    this.parser = parser.clone()
                    mVariables = Array(variables.size) { i ->
                        this.parser!!.addVariable(variables[i].trim { it <= ' ' }, 0.0, 0)
                        this.parser!!.getVariable(variables[i].trim { it <= ' ' })!!
                    }
                }
            }
        }
    }
}