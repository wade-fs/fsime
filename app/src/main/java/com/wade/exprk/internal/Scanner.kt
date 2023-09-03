package com.wade.exprk.internal

import com.wade.exprk.ExpressionException
import com.wade.exprk.internal.TokenType.AMP_AMP
import com.wade.exprk.internal.TokenType.ASSIGN
import com.wade.exprk.internal.TokenType.BAR_BAR
import com.wade.exprk.internal.TokenType.COMMA
import com.wade.exprk.internal.TokenType.EOF
import com.wade.exprk.internal.TokenType.EQUAL_EQUAL
import com.wade.exprk.internal.TokenType.EXPONENT
import com.wade.exprk.internal.TokenType.GREATER
import com.wade.exprk.internal.TokenType.GREATER_EQUAL
import com.wade.exprk.internal.TokenType.IDENTIFIER
import com.wade.exprk.internal.TokenType.LEFT_PAREN
import com.wade.exprk.internal.TokenType.LESS
import com.wade.exprk.internal.TokenType.LESS_EQUAL
import com.wade.exprk.internal.TokenType.MINUS
import com.wade.exprk.internal.TokenType.MODULO
import com.wade.exprk.internal.TokenType.NOT_EQUAL
import com.wade.exprk.internal.TokenType.NUMBER
import com.wade.exprk.internal.TokenType.PLUS
import com.wade.exprk.internal.TokenType.RIGHT_PAREN
import com.wade.exprk.internal.TokenType.SLASH
import com.wade.exprk.internal.TokenType.SQUARE_ROOT
import com.wade.exprk.internal.TokenType.STAR
import java.math.MathContext

private fun invalidToken(c: Char) {
    throw ExpressionException("Invalid token '$c'")
}

internal class Scanner(private val source: String,
                       private val mathContext: MathContext) {

    private val tokens: MutableList<Token> = mutableListOf()
    private var start = 0
    private var current = 0

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            scanToken()
        }

        tokens.add(Token(EOF, "", null))
        return tokens
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun scanToken() {
        start = current
        val c = advance()

        when (c) {
            ' ',
            '\r',
            '\t' -> {
                // Ignore whitespace.
            }
            '+' -> addToken(PLUS)
            '-' -> addToken(MINUS)
            '*' -> addToken(STAR)
            '/' -> addToken(SLASH)
            '%' -> addToken(MODULO)
            '^' -> addToken(EXPONENT)
            '√' -> addToken(SQUARE_ROOT)
            '=' -> if (match('=')) addToken(EQUAL_EQUAL) else addToken(ASSIGN)
            '!' -> if (match('=')) addToken(NOT_EQUAL) else invalidToken(c)
            '>' -> if (match('=')) addToken(GREATER_EQUAL) else addToken(GREATER)
            '<' -> if (match('=')) addToken(LESS_EQUAL) else addToken(LESS)
            '|' -> if (match('|')) addToken(BAR_BAR) else invalidToken(c)
            '&' -> if (match('&')) addToken(AMP_AMP) else invalidToken(c)
            ',' -> addToken(COMMA)
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            else -> {
                when {
                    c.isDigit() -> number()
                    c.isAlpha() -> identifier()
                    else -> invalidToken(c)
                }
            }
        }
    }

    private fun isDigit(char: Char,
                        previousChar: Char = '\u0000',
                        nextChar: Char = '\u0000'): Boolean {
        return char.isDigit() || when (char) {
            '.'      -> true
            'e', 'E' -> previousChar.isDigit() && (nextChar.isDigit() || nextChar == '+' || nextChar == '-')
            '+', '-' -> (previousChar == 'e' || previousChar == 'E') && nextChar.isDigit()
            else     -> false
        }
    }

    private fun number() {
        while (peek().isDigit()) advance()

        if (isDigit(peek(), peekPrevious(), peekNext())) {
            advance()
            while (isDigit(peek(), peekPrevious(), peekNext())) advance()
        }

        val value = source
                .substring(start, current)
                .toBigDecimal(mathContext)

        addToken(NUMBER, value)
    }

    private fun identifier() {
        while (peek().isAlphaNumeric()) advance()

        addToken(IDENTIFIER)
    }

    private fun advance() = source[current++]

    private fun peek(): Char {
        return if (isAtEnd()) {
            '\u0000'
        } else {
            source[current]
        }
    }

    private fun peekPrevious(): Char = if (current > 0) source[current - 1] else '\u0000'

    private fun peekNext(): Char {
        return if (current + 1 >= source.length) {
            '\u0000'
        } else {
            source[current + 1]
        }
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        return true
    }

    private fun addToken(type: TokenType) = addToken(type, null)

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal))
    }

    private fun Char.isAlphaNumeric() = isAlpha() || isDigit()

    private fun Char.isAlpha() = this in 'a'..'z'
            || this in 'A'..'Z'
            || this == '_'

    private fun Char.isDigit() = this == '.' || this in '0'..'9'

}
