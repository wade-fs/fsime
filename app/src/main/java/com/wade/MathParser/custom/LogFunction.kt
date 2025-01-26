package com.wade.MathParser.custom

import com.wade.MathParser.Functions
import com.wade.MathParser.MathFunction
import com.wade.MathParser.MathParser
import com.wade.MathParser.Utils.isUnsignedInteger
import java.util.Locale

/**
 * A [MathFunction] for [Math.log]
 * log2(x)
 * log3(x)
 * ...
 * log[BASE](x)
 */
class LogFunction : MathFunction {
    var base = 0
    override fun name(): String {
        return "log"
    }

    override fun compareNames(name: String): Boolean {
        var name = name
        if (name.trim { it <= ' ' }.lowercase(Locale.getDefault()).startsWith("log")) {
            name = name.substring(3)
            if (isUnsignedInteger(name)) {
                base = name.toInt()
                return true
            }
        }
        return false
    }

    override fun calculate(vararg parameters: Any): Double {
        return Functions.log(parameters[0] as Double, base.toDouble())
    }

    override fun getParameterCount(): Int {
        return 1
    }

    override fun isSpecialParameter(index: Int): Boolean {
        return false
    }

    override fun attachToParser(parser: MathParser) {}
}