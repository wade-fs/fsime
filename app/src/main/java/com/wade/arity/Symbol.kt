/*
 * Copyright (C) 2008 Mihai Preda.
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

class Symbol {
    var name: String? = null
        private set
    private var arity = 0
    @JvmField
    var op: Byte = 0
    @JvmField
    var `fun`: Function? = null
    @JvmField
    var valueRe = 0.0
    @JvmField
    var valueIm = 0.0
    @JvmField
    var isConst = false

    private constructor(name: String?, arity: Int, op: Byte, isConst: Boolean, dummy: Int) {
        setKey(name, arity)
        this.op = op
        this.isConst = isConst
    }

    internal constructor(name: String?, `fun`: Function) {
        setKey(name, `fun`.arity())
        this.`fun` = `fun`
        // this.comment = fun.comment;
    }

    internal constructor(name: String?, re: Double, isConst: Boolean) : this(name, re, 0.0, isConst)
    internal constructor(name: String?, re: Double, im: Double, isConst: Boolean) {
        setKey(name, CONST_ARITY)
        valueRe = re
        valueIm = im
        this.isConst = isConst
    }

    override fun toString(): String {
        return "Symbol '$name' arity $arity val $valueRe op $op"
    }

    /*
    public String getComment() {
	return comment;
    }
    */
    fun getArity(): Int {
        return if (arity == CONST_ARITY) 0 else arity
    }

    val isEmpty: Boolean
        get() = op.toInt() == 0 && `fun` == null && valueRe == 0.0 && valueIm == 0.0

    fun setKey(name: String?, arity: Int): Symbol {
        this.name = name
        this.arity = arity
        return this
    }

    override fun equals(other: Any?): Boolean {
        val symbol = other as Symbol?
        return name == symbol!!.name && arity == symbol.arity
    }

    override fun hashCode(): Int {
        return name.hashCode() + arity
    }

    companion object {
        const val CONST_ARITY = -3
        @JvmStatic
        fun makeArg(name: String?, order: Int): Symbol {
            return Symbol(name, CONST_ARITY, (VM.LOAD0 + order).toByte(), false, 0)
        }

        @JvmStatic
        fun makeVmOp(name: String?, op: Int): Symbol {
            return Symbol(name, VM.arity[op].toInt(), op.toByte(), true, 0)
        }

        @JvmStatic
        fun newEmpty(s: Symbol): Symbol {
            return Symbol(s.name, s.arity, 0.toByte(), false, 0)
        }
    }
}