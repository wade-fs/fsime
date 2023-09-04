package com.wade.MathParser.exception;

public class BalancedParenthesesException extends MathParserException {

    public BalancedParenthesesException(String src, int index) {
        super(src, index, "unexpected parentheses" + (index != -1 ? " at " + index : ""));
    }

}
