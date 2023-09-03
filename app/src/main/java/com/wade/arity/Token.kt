/*
 * Copyright (C) 2007-2008 Mihai Preda.
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

internal class Token(@JvmField val id: Int, @JvmField val priority: Int, @JvmField val assoc: Int, vmop: Int) {
    @JvmField
    val vmop: Byte
    @JvmField
    var value = 0.0 //for NUMBER only
    @JvmField
    var name: String? = null //for CONST & CALL
    @JvmField
    var arity: Int
    @JvmField
    var position = 0 //pos inside expression

    init {
        this.vmop = vmop.toByte()
        arity = if (id == Lexer.CALL) 1 else Symbol.CONST_ARITY
    }

    fun setPos(pos: Int): Token {
        position = pos
        return this
    }

    fun setValue(value: Double): Token {
        this.value = value
        return this
    }

    fun setAlpha(alpha: String?): Token {
        name = alpha
        return this
    }

    val isDerivative: Boolean
        get() {
            var len: Int = 0
            return (name != null) && (name!!.length.also {
                    len = it
                } > 0) && (name!![len - 1] == '\'')
        }

    override fun toString(): String {
        when (id) {
            Lexer.NUMBER -> return "" + value
            Lexer.CALL -> return "$name($arity)"
            Lexer.CONST -> return name!!
        }
        return "" + id
    }

    companion object {
        //kind
        const val PREFIX = 1
        const val LEFT = 2
        const val RIGHT = 3
        const val SUFIX = 4
    }
}