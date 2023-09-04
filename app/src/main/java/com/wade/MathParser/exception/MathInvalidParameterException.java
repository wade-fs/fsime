package com.wade.MathParser.exception;

public class MathInvalidParameterException extends MathParserException {

    public MathInvalidParameterException(String message) {
        super(null, -1, message);
    }
}
