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

/**
 * A constant presented as a function, always evaluates to the same value.
 */
class Constant(o: Complex?) : Function() {
    private val value: Complex

    init {
        value = Complex(o)
    }
    //@Override
    /** Returns the complex constant.  */
    override fun evalComplex(): Complex {
        return value
    }
    //@Override
    /** Returns the complex constant as a real value.
     * @see Complex.asReas
     */
    override fun eval(): Double {
        return value.asReal()
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun arity(): Int {
        return 0
    }
}