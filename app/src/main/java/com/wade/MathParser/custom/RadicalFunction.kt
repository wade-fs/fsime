package com.wade.MathParser.custom

import com.wade.MathParser.Functions
import com.wade.MathParser.MathFunction
import com.wade.MathParser.MathParser
import com.wade.MathParser.Utils.isUnsignedInteger
import java.util.Locale

/**
 * A [MathFunction] for radical, [)][Math.sqrt] & [Math.cbrt]
 * radical2(x) OR √2(x)
 * radical3(x) OR √3(x)
 * ...
 * radical[BASE](x) OR √[Root](x)
 */
class RadicalFunction : MathFunction {
    var root = 0
    override fun name(): String {
        return "radical"
    }

    override fun compareNames(name: String): Boolean {
        var name = name
        name = if (name.trim { it <= ' ' }.lowercase(Locale.getDefault())
                .startsWith("radical")
        ) {
            name.substring(7)
        } else if (name.startsWith("√")) name.substring("√".length) else return false
        if (isUnsignedInteger(name)) {
            root = name.toInt()
            return true
        }
        return false
    }

    override fun calculate(vararg parameters: Any): Double {
        return Functions.radical(parameters[0] as Double, root.toDouble())
    }

    override fun getParameterCount(): Int {
        return 1
    }

    override fun isSpecialParameter(index: Int): Boolean {
        return false
    }

    override fun attachToParser(parser: MathParser) {}
}