/*
 * Copyright (C) 2007-2009 Mihai Preda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wade.arity

/**
 * Compiles a textual arithmetic expression to a [Function].
 *
 *
 */
internal class Compiler {
    private val exception = SyntaxException()
    private val lexer = Lexer(exception)
    private val rpn = RPN(exception)
    private val declParser: DeclarationParser = DeclarationParser(exception)
    private val codeGen = OptCodeGen(exception)
    private val simpleCodeGen = SimpleCodeGen(exception)
    private val decl = Declaration()
    @Throws(SyntaxException::class)
    fun compileSimple(symbols: Symbols?, expression: String?): Function {
        rpn.setConsumer(simpleCodeGen.setSymbols(symbols))
        lexer.scan(expression, rpn)
        return simpleCodeGen.getFun()
    }

    @Throws(SyntaxException::class)
    fun compile(symbols: Symbols, source: String?): Function {
        var `fun`: Function? = null
        decl.parse(source, lexer, declParser)
        if (decl.arity == DeclarationParser.UNKNOWN_ARITY) {
            try {
                `fun` = Constant(compileSimple(symbols, decl.expression).evalComplex())
            } catch (e: SyntaxException) {
                if (e !== SimpleCodeGen.HAS_ARGUMENTS) {
                    throw e
                }
                // fall-through (see below)
            }
        }
        if (`fun` == null) {
            symbols.pushFrame()
            symbols.addArguments(decl.args)
            try {
                rpn.setConsumer(codeGen.setSymbols(symbols))
                lexer.scan(decl.expression, rpn)
            } finally {
                symbols.popFrame()
            }
            var arity = decl.arity
            if (arity == DeclarationParser.UNKNOWN_ARITY) {
                arity = codeGen.intrinsicArity
            }
            `fun` = codeGen.getFun(arity)
        }
        `fun`!!.comment = source
        return `fun`
    }

    @Throws(SyntaxException::class)
    fun compileWithName(symbols: Symbols, source: String?): FunctionAndName {
        return FunctionAndName(compile(symbols, source), decl.name)
    }
}