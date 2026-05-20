package com.wade.fsime.math.custom

import com.wade.fsime.math.MathParser
import com.wade.fsime.math.MathParser.MathVariable
import com.wade.fsime.math.exception.MathParserException

class FunctionWrapper(val parser: MathParser, val exp: String, val `var`: MathVariable) {
    var cache = Double.NaN
    var answer = 0.0
    var old = 0.0
    @Throws(MathParserException::class)
    fun apply(a: Double): Double {
        if (!java.lang.Double.isNaN(cache) && cache == old) return answer
        old = a
        `var`.updateAnswer(a)
        return parser.parse(exp).also { answer = it }
    }
}