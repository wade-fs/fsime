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

/**
 * Thrown when the expression can't be compiled, because it's either not
 * well-formed (e.g. "1+"), or because some simbols aren't defined (e.g. "foo+2").
 */
class SyntaxException : Exception() {
    /**
     * The expression which caused the error.
     */
    @JvmField
    var expression: String? = null

    /**
     * Explicative message (cause of error).
     */
    override var message: String? = null

    /**
     * The position inside expression where the error occured.
     */
    @JvmField
    var position = 0
    override fun toString(): String {
        return ("SyntaxException: " + message
                + " in '" + expression
                + "' at position " + position)
    }

    operator fun set(str: String?, pos: Int): SyntaxException {
        message = str
        position = pos
        fillInStackTrace()
        return this
    }
}