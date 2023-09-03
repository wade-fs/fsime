package com.wade.kparser

enum class NormalOperators(val sign: String, val precedence: Int) {
    PLUS("+", 2),
    MINUS("-", 2),
    MULTIPLY("*", 3),
    DIVISION("/", 4),
    POWER("^", 5),
    EXPONENTIAL("E", 5),
    UNARY("u", 6);
}

enum class FunctionalOperators(val func: String) {
    sin("SIN("),
    cos("COS("),
    tan("TAN("),
    asin("ASIN("),
    acos("ACOS("),
    atan("ATAN("),
    sinh("SINH("),
    cosh("COSH("),
    tanh("TANH("),
    log2("LOG2("),
    log10("LOG10("),
    ln("LN("),
    logx("LOG"),
    sqrt("SQRT("),
    exp("EXP(")

}

infix fun <T> String.isIn(operators: Array<T>): Boolean {

    for (operator in operators) {
        if (operator is NormalOperators) {
            if (this == operator.sign) {
                return true
            }
        } else if (operator is FunctionalOperators) {
            if (this.contains(operator.func)) {
                return true
            } else if (this.contains(FunctionalOperators.logx.func)) {
                return true
            }
        }
    }
    return false
}

infix fun <T> String.notIn(operators: Array<T>): Boolean {
    return !(this isIn operators)
}



