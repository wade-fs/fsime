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

import com.wade.fsime.math.exception.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

/**
 * A simple but powerful math parser for java.
 * <pre>`
 *     MathParser parser = new MathParser();                            // start
 *     parser.addExpression("f(x, y) = 2(x + y)");                      // addFunction
 *     parser.addExpression("x0 = 1 + 2 ^ 2");                          // addVariable
 *     parser.addExpression("y0 = 2x0");                                // addVariable
 *     System.out.println(parser.parse("1 + 2f(x0, y0)/3"));            // 21.0
 * `</pre>
 *
 * Supports all [Math] functions:
 * <pre>`
 *     System.out.println(parser.parse("cos(45°) ^ (2 * sin(pi/2))"));  // 0.5
 * `</pre>
 *
 * Supports integral, derivative, limit and sigma:
 * <pre>`
 *     System.out.println(parser.parse("2 ∫(x, (x^3)/(x+1), 5, 10)"));  // 517.121062
 *     System.out.println(parser.parse("derivative(x, x^3, 2)"));       // 12.0
 *     System.out.println(parser.parse("lim(x->2, x^(x + 2)) / 2"));    // 8.0
 *     System.out.println(parser.parse("Σ(i, 2i^2, 1, 5)"));            // 220.0
 * `</pre>
 *
 * Supports factorial, binary, hexadecimal and octal:
 * <pre>`
 *     System.out.println(parser.parse("5!/4"));                        // 30.0
 *     System.out.println(parser.parse("(0b100)!"));                    // 4! = 24.0
 *     System.out.println(parser.parse("log2((0xFF) + 1)"));            // log2(256) = 8.0
 *     System.out.println(parser.parse("(0o777)"));                     // 511.0
 * `</pre>
 * Supports IF conditions:
 * <pre>`
 *     System.out.println(parser.parse("2 + if(2^5 >= 5!, 1, 0)"));     // 2.0
 *     parser.addExpression("gcd(x, y) = if(y == 0, x, gcd(y, x%y))");  // GCD Recursive
 *     System.out.println(parser.parse("gcd(8, 20)"));                  // 4.0
 * `</pre>
 * Supports array arguments:
 * <pre>`
 *     System.out.println(parser.parse("sum(10, 20, 30, 40)"));         // 100.0
 *     System.out.println(parser.parse("gcd(8, 20, 150)"));             // 2.0
 * `</pre>
 *
 * Let's see how does `MathParser` work with an example:
 * exp = cos(x) ^ 2 + (1 + x * sin(x)) / 2
 * <pre>
 * - let tmp1 be cos(x) -> exp = tmp1 ^ 2 + (1 + x * sin(x)) / 2
 *  + tmp1 is ready                                                     // tmp1 = cos(x)
 * - let tmp2 be sin(x) -> exp = tmp1 ^ 2 + (1 + x * tmp2) / 2
 *  + tmp2 is ready                                                     // tmp2 = sin(x)
 * - let tmp3 be (1 + x * tmp2) -> exp = tmp1 ^ 2 + tmp3 / 2
 *  + tmp3 = 1 + x * tmp2
 *  + order tmp3 operations -> tmp3 = 1 + (x * tmp2)
 *      - let tmp4 be (x * tmp2) -> tmp3 = 1 + tmp4
 *          + tmp4 is ready                                             // tmp4 = x * tmp2
 *  + tmp3 = 1 + tmp4
 *  + tmp3 is ready                                                     // tmp3 = 1 + tmp4
 * - exp = tmp1 ^ 2 + tmp3 / 2
 *  + order exp operations -> exp = (tmp1 ^ 2) + tmp3 / 2
 *      - let tmp5 be (tmp1 ^ 2) -> exp = tmp5 + tmp3 / 2
 *          + tmp5 is ready                                             // tmp5 = tmp1 ^ 2
 *  + exp = tmp5 + tmp3 / 2
 *  + order exp operations -> exp = tmp5 + (tmp3 / 2)
 *      - let tmp6 be (tmp3 / 2) -> exp = tmp5 + tmp6
 *          + tmp6 is ready                                             // tmp6 = tmp3 / 2
 *  + exp = tmp5 + tmp6
 *  + exp is ready
 * </pre>
 *
 * Here is the list of inner variables after simplification:
 * tmp1 = cos(x)
 * tmp2 = sin(x)
 * tmp4 = x * tmp2
 * tmp3 = 1 + tmp4
 * tmp5 = tmp1 ^ 2
 * tmp6 = tmp3 / 2
 * exp  = tmp5 + tmp6
 *
 * As you can see, all variables contain only a very small part of
 * the original expression and all the operations in variables
 * have the same priority, So makes the calculation very easy.
 * [SimpleParser], In order of the above list, starts calculating
 * the variables separately to reach the exp which is the final answer.
 *
 * @author AmirHossein Aghajari
 * @version 1.0.0
 */
class MathParser private constructor() : Cloneable {

    /**
     * [isRoundEnabled]
     */
    var isRoundEnabled = true
    var roundScale = 6

    val variables = ArrayList<MathVariable>()
    val functions = ArrayList<MathFunction>()
    val innerVariables = ArrayList<MathVariable>()
    private val tmpGenerator = AtomicInteger(0)

    companion object {
        /* The order of operations */
        internal val order = charArrayOf('%', '^', '*', '/', '+', '-')

        /* The priority of operations connected to order[] */
        internal val orderPriority = intArrayOf(3, 2, 1, 1, 0, 0)

        /* Special characters will end name of variables or functions */
        internal val special = charArrayOf('%', '^', '*', '/', '+', '-', ',', '(', ')', '!', '=', '<', '>')

        /* Basic math operations that parser supports */
        internal val operations = HashMap<Char, (Double, Double) -> Double>()

        init {
            operations['^'] = { a, b -> a.pow(b) }
            operations['*'] = { a, b -> a * b }
            operations['/'] = { a, b -> a / b }
            operations['+'] = { a, b -> a + b }
            operations['-'] = { a, b -> a - b }
            operations['%'] = { a, b -> a % b }
        }

        @JvmStatic
        fun create(): MathParser {
            return MathParser()
        }

        /**
         * @return True if src exists on supported operations, False otherwise
         */
        @JvmStatic
        fun isMathSign(src: String): Boolean {
            val s = src.trim()
            if (s.isEmpty()) return false
            return isMathSign(s[0])
        }

        /**
         * @return True if src exists on supported operations, False otherwise
         */
        @JvmStatic
        fun isMathSign(src: Char): Boolean {
            return operations.containsKey(src)
        }

        /**
         * @return True if src is an special characters, False otherwise
         */
        private fun isSpecialSign(src: Char): Boolean {
            for (c in special) if (c == src) return true
            return false
        }
    }

    /**
     * Parses and calculates the expression
     *
     * @param expression the expression to parse and calculate
     * @throws MathParserException                   If something went wrong
     * @throws BalancedParenthesesException          If parentheses aren't balanced
     * @throws MathInvalidParameterException         If parameter of the function is invalid
     * @throws MathFunctionInvalidArgumentsException If the number of arguments is unexpected
     * @throws MathFunctionNotFoundException         If couldn't find the function
     * @throws MathVariableNotFoundException         If couldn't find the variable
     */
    @Throws(MathParserException::class)
    fun parse(expression: String): Double {
        var exp = expression
        val org = exp
        validate(exp)
        return try {
            initDefaultVariables()
            exp = firstSimplify(exp)
            calculateVariables()
            round(calculate(exp, org))
        } catch (e: Exception) {
            when {
                e is MathParserException -> throw e
                e.cause is MathParserException -> throw e.cause as MathParserException
                else -> throw MathParserException(org, e.message ?: "", e)
            }
        }
    }

    /**
     * validate syntax
     */
    @Throws(MathParserException::class)
    private fun validate(src: String) {
        Utils.validateBalancedParentheses(src)
    }

    /**
     * Simplify syntax for common functions
     */
    private fun firstSimplify(expression: String): String {
        var exp = Utils.realTrim(expression)
        exp = exp.replace("²", "^2")
        exp = exp.replace("³", "^3")
        exp = exp.replace("×", "*")
        exp = exp.replace("÷", "/")
        exp = exp.replace("π", "pi")
        exp = exp.replace("Π", "pi")
        exp = fixDegrees(exp)
        exp = fixFactorial(exp)
        exp = fixPercentage(exp)
        exp = fixDoubleType(exp)
        exp = fixBinary(exp)
        exp = fixHexadecimal(exp)
        exp = fixOctal(exp)
        return exp
    }

    /**
     * Makes percentage readable
     * x% => (x/100)
     */
    private fun fixPercentage(src: String): String {
        return fix(src, "percentage", '%')
    }

    /**
     * Makes degrees readable for Math trigonometry functions
     * x° => toRadians(x)
     */
    private fun fixDegrees(src: String): String {
        var s = src
        if (getVariable("degrees") == null) s = s.replace("(?<=\\d)degrees(?=[^\\w]|$)".toRegex(), "")
        if (getVariable("deg") == null) s = s.replace("(?<=\\d)deg(?=[^\\w]|$)".toRegex(), "")
        if (getVariable("radians") == null) s = s.replace("(?<=\\d)radians(?=[^\\w]|$)".toRegex(), "")
        if (getVariable("radian") == null) s = s.replace("(?<=\\d)radian(?=[^\\w]|$)".toRegex(), "")
        if (getVariable("rad") == null) s = s.replace("(?<=\\d)rad(?=[^\\w]|$)".toRegex(), "")

        s = s.replace("°", "")
        return s
    }

    /**
     * Makes factorial readable
     * x! => factorial(x)
     */
    private fun fixFactorial(src: String): String {
        return fix(src, "factorial", '!')
    }

    private fun fix(srcValue: String, function: String, c: Char): String {
        var src = srcValue
        var index: Int
        while (src.indexOf(c).also { index = it } != -1) {
            var applyToFirst = true
            var ph = false
            var count = 0

            for (i in index - 1 downTo 0) {
                if (i == index - 1 && src[i] == ')') {
                    ph = true
                    count++
                    continue
                }
                if (ph) {
                    if (src[i] == ')') {
                        count++
                    } else if (src[i] == '(') {
                        count--
                    }
                    if (count != 0) continue
                    ph = false
                }
                if (!isSpecialSign(src[i])) continue

                val sign = if (isSpecialSign(src[i])) "" else "*"
                val insertIndex = i + 1
                src = src.substring(0, insertIndex) + sign + function + "(" + src.substring(insertIndex, index) + ")" + src.substring(index + 1)
                applyToFirst = false
                break
            }
            if (applyToFirst) src = "$function(" + src.substring(0, index) + ")" + src.substring(index + 1)
        }
        return src
    }

    /**
     * (2e+2) -> (200.0)
     */
    private fun fixDoubleType(src: String): String {
        val matcher = Utils.doubleType.matcher(src)
        if (matcher.find()) {
            val a = matcher.group(1)
            var b = a
            val e = matcher.group(2)
            var ignoreE = a.endsWith("-") || a.endsWith("+")
            if (!ignoreE) {
                try {
                    b = a.toDouble().toString()
                } catch (ignore: Exception) {
                    ignoreE = true
                }
            }
            if (ignoreE) {
                b = a.substring(0, a.indexOf(e)) + " " + a.substring(a.indexOf(e))
            }
            val newSrc = src.substring(0, matcher.start() + 1) + b + src.substring(matcher.end() - 1)
            return fixDoubleType(newSrc.trim { it <= ' ' })
        }
        return src.trim { it <= ' ' }
    }

    /**
     * (0b010) -> (2)
     */
    private fun fixBinary(src: String): String {
        val matcher = Utils.binary.matcher(src)
        if (matcher.find()) {
            val a = matcher.group(0)
            val value = java.lang.Long.parseLong(a.substring(3, a.length - 1), 2)
            val newSrc = src.substring(0, matcher.start() + 1) + value + src.substring(matcher.end() - 1)
            return fixBinary(newSrc)
        }
        return src
    }

    /**
     * (0x0FF) -> (255)
     */
    private fun fixHexadecimal(src: String): String {
        val matcher = Utils.hexadecimal.matcher(src)
        if (matcher.find()) {
            val a = matcher.group(0)
            val value = java.lang.Long.parseLong(a.substring(3, a.length - 1), 16)
            val newSrc = src.substring(0, matcher.start() + 1) + value + src.substring(matcher.end() - 1)
            return fixHexadecimal(newSrc)
        }
        return src
    }

    /**
     * (0o027) -> (23)
     */
    private fun fixOctal(src: String): String {
        val matcher = Utils.octal.matcher(src)
        if (matcher.find()) {
            val a = matcher.group(0)
            val value = java.lang.Long.parseLong(a.substring(3, a.length - 1), 8)
            val newSrc = src.substring(0, matcher.start() + 1) + value + src.substring(matcher.end() - 1)
            return fixOctal(newSrc)
        }
        return src
    }

    /**
     * Adds default constants {pi, e}
     */
    private fun initDefaultVariables() {
        addConst("e", Math.E)
        addConst("Π", Math.PI)
        addConst("π", Math.PI)
        addConst("pi", Math.PI)
    }

    /**
     * Calculate the answer of variables
     */
    @Throws(MathParserException::class)
    private fun calculateVariables() {
        for (variable in variables) {
            if (!variable.hasFound) {
                validate(variable.expression)
                variable.answer = arrayOf<Any>(round(calculate(firstSimplify(variable.expression), variable.original)))
                variable.hasFound = true
            }
        }
    }

    /**
     * Rounds the value if [isRoundEnabled] is true.
     *
     * @see isRoundEnabled
     */
    private fun round(a: Double): Double {
        if (!isRoundEnabled || java.lang.Double.isInfinite(a) || java.lang.Double.isNaN(a)) return a
        return BigDecimal.valueOf(a).setScale(roundScale, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Calculates and returns the value of an expression
     *
     * @param exp  the simplified expression
     * @param main the original expression
     */
    @Throws(MathParserException::class)
    private fun calculate(exp: String, main: String): Double {
        return calculate(exp, main, false)
    }

    /**
     * Calculates and returns the value of an expression.
     *
     * This function is not going to check the priorities of [operations],
     * but tries to simplify the expression as much as possible and calculate
     * the parentheses from the innermost order and put them in [innerVariables],
     * If the parentheses are related to function calls, adds a [MathVariable.function]
     * to the temp variable to identify the function. So eventually a simple expression without
     * parentheses and functions will be created.
     *
     * For example:
     * 2 + (2x + abs(x)) / 2
     *
     * - let tmp1 be abs(x) -> 2 + (2x + tmp1) / 2
     * - let tmp2 be (2x + tmp1) -> 2 + tmp2 / 2
     * - calls [orderAgain] to order operations
     *
     * @param src          the simplified expression
     * @param main         the user-entered expression
     * @param fromExpValue [ExpValue] is an expression that contained an unknown variable in
     * the first step that was calculated. If fromExpValue is true, it means that
     * it is trying to calculate it again in the hope that the unknown variable is
     * found. If it is found, returns the final answer, otherwise will throw
     * [MathVariableNotFoundException]. Basically, functions that will add a
     * dynamic variable, such as sigma and integral, need this property to update
     * and apply the variable in the second step.
     */
    @Throws(MathParserException::class)
    private fun calculate(srcValue: String, main: String, fromExpValue: Boolean): Double {
        var src = Utils.realTrim(srcValue)
        if (src.startsWith("(") && src.endsWith(")") && !src.substring(1).contains("(")) src = src.substring(1, src.length - 1)

        while (src.contains("(") || src.contains(")")) {
            val matcher = Utils.innermostParentheses.matcher(src)
            if (matcher.find()) {
                val name = generateTmpName()
                var exp = matcher.group(0).trim { it <= ' ' }
                exp = exp.substring(1, exp.length - 1)

                val matcher2 = Utils.splitParameters.matcher(exp)
                val answers = ArrayList<String>()
                var startParameter = 0
                while (matcher2.find()) {
                    answers.add(exp.substring(startParameter, matcher2.end() - 1).also { startParameter = matcher2.end() })
                }
                if (answers.isEmpty()) {
                    answers.add(exp)
                } else {
                    answers.add(exp.substring(startParameter))
                }

                var function: MathFunction? = null
                var start = matcher.start()
                var signBefore = if (start == 0 || isSpecialSign(Utils.findCharBefore(src, matcher.start()))) "" else "*"
                val signAfter = if (matcher.end() == src.length || isSpecialSign(Utils.findCharAfter(src, matcher.end()))) "" else "*"

                if (start > 0) {
                    val before = src.substring(0, start)
                    var wordBefore = before.substring(Utils.findBestIndex(before, true)).trim { it <= ' ' }
                    while (wordBefore.isNotEmpty() && Character.isDigit(wordBefore[0])) wordBefore = wordBefore.substring(1)

                    if (wordBefore.isNotEmpty()) {
                        function = Functions.getFunction(main, main.indexOf(wordBefore), wordBefore, answers.size, functions)
                        if (function != null) {
                            signBefore = ""
                            start -= wordBefore.length
                        } else if (answers.size > 1) throw MathFunctionNotFoundException(main, main.indexOf(wordBefore), wordBefore)
                    }
                }
                if (answers.size > 1 && function == null) throw MathFunctionNotFoundException(main, -1, null, matcher.group(0).trim { it <= ' ' })

                val answers2 = ArrayList<Any>()
                if (function == null) {
                    if (!fromExpValue) try {
                        answers2.add(calculate(answers[0], main))
                    } catch (e: Exception) {
                        answers2.add(ExpValue(answers[0], main))
                    } else answers2.add(calculate(answers[0], main))
                } else {
                    for (i in answers.indices) {
                        if (function.isSpecialParameter(i)) answers2.add(Utils.realTrim(answers[i])) else {
                            if (!fromExpValue) try {
                                answers2.add(calculate(answers[i], main))
                            } catch (e: Exception) {
                                answers2.add(ExpValue(answers[i], main))
                            } else answers2.add(calculate(answers[i], main))
                        }
                    }
                }
                val v = MathVariable(name, answers2, exp, main, function)
                function?.attachToParser(this)
                innerVariables.add(v)

                src = src.substring(0, start) + signBefore + name + signAfter + src.substring(matcher.end())
            } else break
        }

        return orderAgain(src, main)
    }

    /**
     * [calculate] has simplified the expression
     * as much as it's possible, It is time to set priorities of [operations].
     * This function first checks if all operations are in the same priority.
     * If yes, the phrase is the simplest case possible and the final calculations
     * should be done by the [SimpleParser]. If not, it parentheses the higher
     * priority operation and sends it back to the [calculate]
     * for simplification. This is done so that in the end a linear expression remains which
     * all operations have the same priority.
     *
     * For example:
     * 2 + tmp2 / 2
     *
     * <pre>
     * + parentheses the division operation -> 2 + (tmp2 / 2)
     * + sends it back to [calculate]
     *  - let tmp3 be (tmp2 / 2) -> 2 + tmp3
     * + final calculation by [SimpleParser]
     * </pre>
     *
     * This cycle will also apply to all variables
     */
    @Throws(MathParserException::class)
    private fun orderAgain(srcValue: String, main: String): Double {
        var src = srcValue
        var allInSamePriority = true
        var highestPriority = -1
        for (i in order.indices) {
            if (src.contains(order[i].toString())) {
                if (highestPriority != -1 && orderPriority[i] != highestPriority) {
                    allInSamePriority = false
                    break // the first ones always have higher priority
                }
                highestPriority = highestPriority.coerceAtLeast(orderPriority[i])
            }
        }

        if (!allInSamePriority) {
            var ind = -1
            var op = '+'
            for (i in order.indices) {
                if (orderPriority[i] == highestPriority) {
                    val ind2 = src.indexOf(order[i])
                    if (ind2 != -1 && (ind == -1 || ind > ind2)) {
                        ind = ind2
                        op = order[i]
                    }
                }
            }

            if (ind != -1) {
                val index: Int
                val wordAfter = src.substring(ind + 1, ind + 1 + Utils.findBestIndex(src.substring(ind + 1), false))
                val wordBefore = src.substring(Utils.findBestIndex(src.substring(0, ind), true).also { index = it }, ind)

                src = src.substring(0, index) + "(" + wordBefore + op + wordAfter + ")" + src.substring(ind + wordAfter.length + 1)
            }
        }

        return if (src.contains("(") || src.contains(")")) calculate(src, main) else SimpleParser(src, main).parse()
    }

    /**
     * Generates a new unique name for temp variables in format of __tmp{index}
     */
    private fun generateTmpName(): String {
        var name: String
        do {
            name = "__tmp" + tmpGenerator.incrementAndGet()
        } while (getVariable(name) != null)
        return name
    }

    /**
     * Adds support of an expression to this [MathParser],
     * the expression must contains `=`,
     * There are two types of phrases. Variable and Function
     * Variables are in the form of `name = expression`
     * Functions are in the form of `name(x, y, ..) = expression`
     *
     * For example:
     * <pre>`
     *      MathParser parser = MathParser.create();
     *      parser.addExpression("f(x, y) = x ^ y");
     *      parser.addExpression("x0 = 2");
     *      parser.addExpression("y0 = 4");
     *      System.out.println(parser.parse("f(x0, y0)")); // 2^4 = 16.0
     * `</pre>
     */
    fun addExpression(exp: String) {
        val varSplit = arrayOf(exp.substring(0, exp.indexOf('=')), exp.substring(exp.indexOf('=') + 1))
        if (varSplit[0].contains("(")) addFunction(MathFunction.wrap(exp)) else addVariable(varSplit[0].trim { it <= ' ' }, varSplit[1].trim { it <= ' ' })
    }

    /**
     * Adds a new variable to this [MathParser]
     *
     * @param name       variable name
     * @param expression expression of variable
     * @see addVariable
     * @see addExpression
     */
    fun addVariable(name: String, expression: String) {
        removeVariable(name)
        variables.add(MathVariable(name, expression))
    }

    /**
     * Adds a new variable at specific index to this [MathParser]
     *
     * @param name       variable name
     * @param expression expression of variable
     * @param index      index of variable in [getVariables] list
     * @see addVariable
     * @see addExpression
     */
    fun addVariable(name: String, expression: String, index: Int) {
        removeVariable(name)
        variables.add(index, MathVariable(name, expression))
    }

    /**
     * Adds a new variable to this [MathParser]
     *
     * @param name  variable name
     * @param value value of variable
     * @see addVariable
     */
    fun addVariable(name: String, value: Double) {
        removeVariable(name)
        variables.add(MathVariable(name, value))
    }

    /**
     * Adds a new variable at specific index to this [MathParser]
     *
     * @param name  variable name
     * @param value value of variable
     * @param index index of variable in [getVariables] list
     * @see addVariable
     */
    fun addVariable(name: String, value: Double, index: Int) {
        removeVariable(name)
        variables.add(index, MathVariable(name, value))
    }

    /**
     * Adds a new function to this [MathParser]
     *
     * @see addExpression
     */
    fun addFunction(function: MathFunction) {
        functions.add(function)
    }

    /**
     * Adds set of functions to this [MathParser]
     * based on [Method]
     * Only the methods that return `double` are supported.
     * The method can get [MathParser] only as the first argument.
     * The method can get String only if [MathFunction.isSpecialParameter]
     * returns true for the index of argument.
     * The method can get array of double in form of `Object...`
     * The method must be static and have at least one argument
     *
     * @see addExpression
     */
    fun addFunctions(methods: Collection<java.lang.reflect.Method>) {
        Functions.addFunctions(methods, functions)
    }

    /**
     * @see addFunctions
     */
    fun addFunctions(cls: Class<*>) {
        Functions.addFunctions(Arrays.asList(*cls.methods), functions)
    }

    /**
     * Removes the specified variable from [getVariables]
     */
    fun removeVariable(nameValue: String) {
        val name = nameValue.trim { it <= ' ' }.lowercase(Locale.getDefault())
        val iterator = variables.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().name == name) {
                iterator.remove()
                return
            }
        }
    }

    /**
     * Clears all imported variables
     */
    fun clearVariables() {
        variables.clear()
    }

    /**
     * Clears all imported functions
     */
    fun clearFunctions() {
        functions.clear()
    }

    /**
     * Returns `true` if [getVariables] contains the specified variable.
     */
    fun containsVariable(nameValue: String): Boolean {
        val name = nameValue.trim { it <= ' ' }.lowercase(Locale.getDefault())
        for (variable in variables) if (variable.name == name) return true
        return false
    }

    /**
     * Adds a const variable to [innerVariables];
     */
    private fun addConst(name: String, value: Double) {
        innerVariables.add(MathVariable(name, value))
    }

    /**
     * Returns the instance of [MathVariable] if the variable exists
     * on [getVariables] or [innerVariables], Null otherwise.
     */
    internal fun getVariable(nameValue: String): MathVariable? {
        val name = nameValue.trim { it <= ' ' }.lowercase(Locale.getDefault())
        for (variable in variables) if (variable.name == name) return variable
        for (variable in innerVariables) if (variable.name == name) return variable
        return null
    }

    /**
     * Creates a new copy of this [MathParser]
     *
     * @return a shallow copy of this [MathParser]
     */
    public override fun clone(): MathParser {
        val newParser = create()
        newParser.variables.addAll(variables)
        newParser.innerVariables.addAll(innerVariables)
        newParser.functions.addAll(functions)
        newParser.tmpGenerator.set(tmpGenerator.get())
        newParser.isRoundEnabled = isRoundEnabled
        newParser.roundScale = roundScale
        return newParser
    }

    /**
     * Resets temp variables
     *
     * @param deep True to clear all imported functions and variables, False otherwise
     */
    fun reset(deep: Boolean) {
        if (deep) {
            variables.clear()
            functions.clear()
        }
        innerVariables.clear()
        tmpGenerator.set(0)
    }

    class MathVariable {
        val name: String
        val expression: String
        val original: String
        var answer: Array<Any> = arrayOf(0.0)
        var hasFound = false
        var function: MathFunction? = null

        constructor(name: String, expression: String) {
            this.name = name.trim { it <= ' ' }.lowercase(Locale.getDefault())
            this.expression = expression.trim { it <= ' ' }.lowercase(Locale.getDefault())
            original = this.name + " = " + this.expression
        }

        constructor(name: String, value: Double) {
            this.name = name.trim { it <= ' ' }.lowercase(Locale.getDefault())
            expression = value.toString()
            original = this.name + " = " + expression
            answer = arrayOf(value)
            hasFound = true
        }

        constructor(name: String, value: List<Any>, expression: String, original: String, function: MathFunction?) {
            this.name = name.trim { it <= ' ' }.lowercase(Locale.getDefault())
            this.expression = expression
            this.original = original
            answer = value.toTypedArray()
            hasFound = true
            this.function = function
        }

        @Throws(MathParserException::class)
        fun getAnswer(): Double {
            return if (function == null) answer[0] as Double else function!!.calculate(answer)
        }

        fun updateAnswer(vararg answers: Any?) {
            this.answer = answers as Array<Any>
        }

        @Throws(MathParserException::class)
        fun getAnswer(parser: MathParser): Double {
            return if (function == null) {
                if (answer[0] is ExpValue) (answer[0] as ExpValue).calculate(parser) else answer[0] as Double
            } else {
                function!!.attachToParser(parser)
                var allAreDouble = true
                val ans = ArrayList<Any>()
                for (o in answer) {
                    if (o is ExpValue) {
                        ans.add(o.calculate(parser))
                    } else {
                        ans.add(o)
                        if (o !is Double) allAreDouble = false
                    }
                }
                if (allAreDouble) function!!.calculate(ans.toArray(arrayOf<Double>())) else function!!.calculate(ans.toTypedArray())
            }
        }

        override fun toString(): String {
            return name + " = " + (if (function != null) function!!.name() + "(" else "") + expression + (if (function != null) ")" else "")
        }
    }

    /**
     * A new ExpValue means parser once tried to calculate [ExpValue.expression]
     * but something went wrong! (usually a variable is missed), So
     * Parser tries to store the expression in a [ExpValue] and calculate it again
     * when needed by [MathParser.calculate] and will set fromExpValue true,
     * if something went wrong again it will throw an exception this time so
     * it won't go on a loop, and if everything was ok, it will use the calculated value.
     *
     * Some functions like integral or sigma will add a dynamic variable during parsing,
     * this type of variables will be unknown for parser at the first time.
     * that's why we use [ExpValue], to make sure there won't be any dynamic variable.
     */
    private class ExpValue(val expression: String, val original: String) {
        @Throws(MathParserException::class)
        fun calculate(parser: MathParser): Double {
            return parser.calculate(expression, original, true)
        }
    }

    /**
     * A simple parser to parse a linear expression,
     * Doesn't need to check priority of operations
     */
    inner class SimpleParser(var src: String, val original: String) {
        var currentOperation = operations['+']
        var a = 0.0
        var b: Double? = null

        @Throws(MathParserException::class)
        fun parse(): Double {
            trim()

            if (b != null) {
                a = currentOperation!!(a, b!!)
                b = null
            }

            if (src.isEmpty()) return a

            if (isMathSign(src)) {
                currentOperation = operations[get()]
            } else {
                val word = nextWord()
                b = try {
                    word.toDouble()
                } catch (e: Exception) {
                    tryParseWord(word)
                }
            }
            return parse()
        }

        /**
         * so there is a string which isn't a number,
         * this function will try to parse this word and check for variables
         * "xy" => can be two variables (x or y) or just a variable named xy,
         * figures out what would xy mean. (int this case, being one variable
         * is the higher priority)
         */
        @Throws(MathParserException::class)
        private fun tryParseWord(wordValue: String): Double {
            var word = wordValue
            var variable = getVariable(word)
            return if (variable == null) {
                if (Character.isDigit(word[0])) {
                    val numberFirst = StringBuilder()
                    for (i in 0 until word.length) {
                        val c = word[i]
                        if (Character.isDigit(c) || c == '.') numberFirst.append(c) else break
                    }

                    val coefficient = numberFirst.toString().toDouble()
                    word = word.substring(numberFirst.length)
                    variable = getVariable(word)
                    if (variable == null) trySplitVariables(word, coefficient) else coefficient * variable.getAnswer(this@MathParser)
                } else {
                    trySplitVariables(word, 1.0)
                }
            } else variable.getAnswer(this@MathParser)
        }

        @Throws(MathParserException::class)
        private fun trySplitVariables(word: String, coefficientValue: Double): Double {
            var coefficient = coefficientValue
            var name = StringBuilder()
            var indexOfStart = 0
            for (i in 0 until word.length) {
                val c = word[i]
                if (i + 1 < word.length && Character.isDigit(word[i + 1])) {
                    name.append(c)
                    continue
                }

                val variable = getVariable(name.toString() + c)
                if (variable == null) {
                    name.append(c)
                } else {
                    indexOfStart = i + 1
                    name = StringBuilder()
                    coefficient *= variable.getAnswer(this@MathParser)
                }
            }
            if (name.isNotEmpty()) couldNotFindVariables(original.indexOf(word) + 1 + indexOfStart, name.toString())
            return coefficient
        }

        /**
         * Couldn't find the variable but let's try to guess what could it be,
         * may help to debug the expression
         */
        @Throws(MathParserException::class)
        private fun couldNotFindVariables(index: Int, nameOut: String) {
            var guess = 0.0
            var guessName = ""
            for (variable in variables) {
                if (!variable.hasFound) continue

                val sim = Utils.similarity(nameOut, variable.name)
                if (sim > guess) {
                    guessName = variable.name
                    guess = sim
                }
            }
            if (guess == 0.0) throw MathVariableNotFoundException(original, index, nameOut) else throw MathVariableNotFoundException(original, index, nameOut, guessName)
        }

        private fun trim() {
            src = src.trim { it <= ' ' }
        }

        private fun get(): Char {
            val c = src[0]
            src = src.substring(1)
            return c
        }

        private fun nextWord(): String {
            return try {
                val index = Utils.findBestIndex(src, false)
                val word = src.substring(0, index).trim { it <= ' ' }
                src = src.substring(index)
                word
            } catch (ignore: Exception) {
                ""
            }
        }
    }
}
