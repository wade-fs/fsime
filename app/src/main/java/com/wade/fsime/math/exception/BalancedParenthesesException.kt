package com.wade.fsime.math.exception

class BalancedParenthesesException(src: String?, index: Int) : MathParserException(
    src,
    index,
    "unexpected parentheses" + if (index != -1) " at $index" else ""
)