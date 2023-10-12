package com.wade.MathParser.exception

class MathFunctionNotFoundException : MathParserException {
    val function: String?

    constructor(src: String?, index: Int, function: String?) : super(
        src,
        index,
        if (function == null) "couldn't find function" else "$function() not found"
    ) {
        this.function = function
    }

    constructor(src: String?, index: Int, function: String?, message: String) : super(
        src,
        index,
        "couldn't find function: $message"
    ) {
        this.function = function
    }
}