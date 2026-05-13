package com.wade.MathParser.exception

class BalancedParenthesesException(src: String?, index: Int) : MathParserException(
    src,
    index,
    "unexpected parentheses" + if (index != -1) " at $index" else ""
)