package com.wade.MathParser.exception

class MathVariableNotFoundException : MathParserException {
    val variableName: String
    val guess: String?

    constructor(src: String?, index: Int, variableName: String) : super(
        src,
        index,
        "$variableName not found!"
    ) {
        this.variableName = variableName
        guess = null
    }

    constructor(src: String?, index: Int, variableName: String, guess: String) : super(
        src,
        index,
        "$variableName not found, did you mean $guess?"
    ) {
        this.variableName = variableName
        this.guess = guess
    }
}