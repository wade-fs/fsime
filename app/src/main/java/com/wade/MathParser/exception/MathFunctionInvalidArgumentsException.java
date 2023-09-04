package com.wade.MathParser.exception;

import com.wade.MathParser.MathFunction;

public class MathFunctionInvalidArgumentsException extends MathParserException {

    private final MathFunction function;

    public MathFunctionInvalidArgumentsException(String src, int index, MathFunction function, int count) {
        super(src, index, function.name() + "() Expected " + function.getParameterCount() + " arguments but found " + count);
        this.function = function;
    }

    public MathFunction getFunction() {
        return function;
    }
}
