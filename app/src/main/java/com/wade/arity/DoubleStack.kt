/*
 * Copyright (C) 2008-2009 Mihai Preda.
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

internal class DoubleStack {
    private var re = DoubleArray(8)
    private var im = DoubleArray(8)
    private var size = 0
    fun clear() {
        size = 0
    }

    fun push(a: Double, b: Double) {
        if (size >= re.size) {
            val newSize = re.size shl 1
            val newRe = DoubleArray(newSize)
            val newIm = DoubleArray(newSize)
            System.arraycopy(re, 0, newRe, 0, re.size)
            System.arraycopy(im, 0, newIm, 0, re.size)
            re = newRe
            im = newIm
        }
        re[size] = a
        im[size] = b
        ++size
    }

    fun pop(cnt: Int) {
        if (cnt > size) {
            throw Error("pop $cnt from $size")
        }
        size -= cnt
    }

    fun pop() {
        --size
    }

    fun getRe(): DoubleArray {
        val trimmed = DoubleArray(size)
        System.arraycopy(re, 0, trimmed, 0, size)
        return trimmed
    }

    fun getIm(): DoubleArray? {
        var allZero = true
        for (i in 0 until size) {
            if (im[i] != 0.0) {
                allZero = false
                break
            }
        }
        if (allZero) {
            return null
        }
        val trimmed = DoubleArray(size)
        System.arraycopy(im, 0, trimmed, 0, size)
        return trimmed
    }
}