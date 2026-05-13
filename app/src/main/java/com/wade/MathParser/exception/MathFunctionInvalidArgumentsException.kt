package com.wade.MathParser.exception

import com.wade.MathParser.MathFunction

class MathFunctionInvalidArgumentsException(
    src: String?,
    index: Int,
    val function: MathFunction,
    count: Int
) : MathParserException(
    src,
    index,
    function.name() + "() Expected " + function.parameterCount + " arguments but found " + count
)