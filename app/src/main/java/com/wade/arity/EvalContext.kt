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
 * To evaluate CompiledFunctions from multiple threads in parallel,
 * you need to create one EvalContext instance per thread,
 * and pass it to the eval() methods of CompiledFunction.
 */
class EvalContext {
    @JvmField
    var stackRe = DoubleArray(MAX_STACK_SIZE)
    @JvmField
    val stackComplex = arrayOfNulls<Complex>(MAX_STACK_SIZE)
    @JvmField
    var stackBase = 0
    @JvmField
    var args1 = DoubleArray(1)
    @JvmField
    var args2 = DoubleArray(2)
    @JvmField
    var args1c: Array<Complex>
    @JvmField
    var args2c: Array<Complex>

    /** Constructs a new EvalContext, ready to be used with CompiledFunction.eval().
     */
    init {
        for (i in 0 until MAX_STACK_SIZE) {
            stackComplex[i] = Complex()
        }
        args1c = arrayOf(Complex())
        args2c = arrayOf(Complex(), Complex())
    }

    companion object {
        const val MAX_STACK_SIZE = 128 //if stack ever grows above this likely something is wrong
    }
}