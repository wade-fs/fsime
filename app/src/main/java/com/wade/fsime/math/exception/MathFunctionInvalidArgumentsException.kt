package com.wade.fsime.math.exception

import com.wade.fsime.math.MathFunction

class MathFunctionInvalidArgumentsException(
    src: String?,
    index: Int,
    val function: MathFunction,
    count: Int
) : MathParserException(
    src,
    index,
    function.name() + "() Expected " + function.getParameterCount() + " arguments but found " + count
)