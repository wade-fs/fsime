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
package com.wade.MathParser

import com.wade.MathParser.exception.BalancedParenthesesException
import java.util.regex.Pattern

object Utils {
    /* used to check if parentheses are balanced */ //public final static Pattern balancedParentheses = Pattern.compile("\\((?:[^)(]+|\\((?:[^)(]+|\\([^)(]*\\))*\\))*\\)");
    /* used to find the innermost parentheses */
    @JvmField
    val innermostParentheses = Pattern.compile("(\\([^\\(]*?\\))")

    /* used to split function arguments by comma */
    @JvmField
    val splitParameters = Pattern.compile(",(?=(?:[^()]*\\([^()]*\\))*[^\\()]*$)")

    /* used to split if condition to two comparable part */
    @JvmField
    val splitIf = Pattern.compile("(.*?)(!=|<>|>=|<=|==|>|=|<)(.*?$)")

    /* used to simplify double type values */
    @JvmField
    val doubleType = Pattern.compile("\\(([\\d.]+([eE])[\\d+-]+)\\)")

    /* used to simplify binary values */
    @JvmField
    val binary = Pattern.compile("\\(0b[01]+\\)")

    /* used to simplify octal values */
    @JvmField
    val octal = Pattern.compile("\\(0o[0-7]+\\)")

    /* used to simplify hexadecimal values */
    @JvmField
    val hexadecimal = Pattern.compile("\\(0x[0-9a-fA-F]+\\)")

    /**
     * @param src the expression to check
     * @throws BalancedParenthesesException If parentheses aren't balanced
     */
    @JvmStatic
    @Throws(BalancedParenthesesException::class)
    fun validateBalancedParentheses(src: String) {
        /*String dest = src.replaceAll(balancedParentheses.pattern(), "");
        if (dest.contains(")"))
            throw new BalancedParenthesesException(src, src.indexOf(dest.substring(dest.indexOf(")"))) + 1);
        else if (dest.contains("("))
            throw new BalancedParenthesesException(src, src.indexOf(dest.substring(dest.indexOf("("))) + 1);*/
        if (realTrim(src).contains("()")) throw BalancedParenthesesException(null, -1)
        var opened = 0
        for (i in 0 until src.length) if (src[i] == '(') opened++ else if (src[i] == ')') {
            opened--
            if (opened < 0) throw BalancedParenthesesException(src, i + 1)
        }
        if (opened != 0) throw BalancedParenthesesException(src, src.length)
    }

    /**
     * @see jdk.internal.joptsimple.internal.Strings.repeat
     */
    fun repeat(ch: Char, count: Int): String {
        val buffer = StringBuilder()
        for (i in 0 until count) buffer.append(ch)
        return buffer.toString()
    }

    @JvmStatic
    fun findCharBefore(src: String, start: Int): Char {
        var src = src
        return try {
            src = src.substring(0, start).trim { it <= ' ' }
            if (src.isEmpty()) '\u0000' else src[src.length - 1]
        } catch (ignore: Exception) {
            '\u0000'
        }
    }

    @JvmStatic
    fun findCharAfter(src: String, start: Int): Char {
        var src = src
        return try {
            src = src.substring(start).trim { it <= ' ' }
            if (src.isEmpty()) '\u0000' else src[0]
        } catch (ignore: Exception) {
            ignore.printStackTrace()
            '\u0000'
        }
    }

    @JvmStatic
    fun findBestIndex(src: String, before: Boolean): Int {
        var index = if (before) src.lastIndexOf(' ') else src.indexOf(' ')
        if (index == -1) index = if (before) 0 else src.length else if (before) index++
        for (c in MathParser.special) {
            val id = if (before) src.lastIndexOf(c) else src.indexOf(c)
            if (id != -1) index = if (before) Math.max(index, id + 1) else Math.min(index, id)
        }
        return index
    }

    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     * https://stackoverflow.com/a/16018452/9187189
     */
    @JvmStatic
    fun similarity(s1: String, s2: String): Double {
        var longer = s1
        var shorter = s2
        if (s1.length < s2.length) { // longer should always have greater length
            longer = s2
            shorter = s1
        }
        val longerLength = longer.length
        return if (longerLength == 0) {
            1.0 /* both strings are zero length */
        } else (longerLength - getLevenshteinDistance(
            longer,
            shorter
        )) / longerLength.toDouble()
    }

    /**
     * java.org.apache.commons.lang3.StringUtils#getLevenshteinDistance(CharSequence, CharSequence)
     */
    private fun getLevenshteinDistance(s: CharSequence, t: CharSequence): Int {
        var s = s
        var t = t
        var n = s.length
        var m = t.length
        if (n == 0) {
            return m
        }
        if (m == 0) {
            return n
        }
        if (n > m) {
            // swap the input strings to consume less memory
            val tmp = s
            s = t
            t = tmp
            n = m
            m = t.length
        }
        val p = IntArray(n + 1)
        // indexes into strings s and t
        var i: Int // iterates through s
        var j: Int // iterates through t
        var upper_left: Int
        var upper: Int
        var t_j: Char // jth character of t
        var cost: Int
        i = 0
        while (i <= n) {
            p[i] = i
            i++
        }
        j = 1
        while (j <= m) {
            upper_left = p[0]
            t_j = t[j - 1]
            p[0] = j
            i = 1
            while (i <= n) {
                upper = p[i]
                cost = if (s[i - 1] == t_j) 0 else 1
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                p[i] = Math.min(Math.min(p[i - 1] + 1, p[i] + 1), upper_left + cost)
                upper_left = upper
                i++
            }
            j++
        }
        return p[n]
    }

    @JvmStatic
    fun isUnsignedInteger(s: String): Boolean {
        if (s.isEmpty()) return false
        for (i in 0 until s.length) {
            if (!Character.isDigit(s[i])) return false
        }
        return true
    }

    @JvmStatic
    fun realTrim(src: String): String {
        return src.trim { it <= ' ' }.replace("\\s+".toRegex(), "")
    }

    @JvmStatic
    fun isIdentifier(text: String?): Boolean {
        if (text == null || text.isEmpty()) return false
        if (!Character.isLetter(text[0]) && text[0] != '_') return false
        for (ix in 1 until text.length) if (!Character.isLetterOrDigit(text[ix]) && text[ix] != '_') return false
        return true
    }
}