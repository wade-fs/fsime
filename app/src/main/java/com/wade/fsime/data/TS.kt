package com.wade.fsime.data

import android.util.Log

/**
 * Created by npes87184 on 2015/4/10.
 * Modified to load mappings from database and handle supplementary characters.
 */
object TS {
    private val T2S = HashMap<Int, Int>()
    private val S2T = HashMap<Int, Int>()
    private var initialized = false

    fun initMapping(utf8t: String, utf8s: String) {
        if (initialized) return
        
        val tCodePoints = utf8t.codePoints().toArray()
        val sCodePoints = utf8s.codePoints().toArray()
        
        val size = minOf(tCodePoints.size, sCodePoints.size)
        for (i in 0 until size) {
            val cT = tCodePoints[i]
            val cS = sCodePoints[i]
            T2S[cT] = cS
            S2T[cS] = cT
        }
        initialized = true
        Log.i("TS", "Initialized mapping with $size code points")
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    @JvmStatic
    fun StoT(text: String): String {
        if (!initialized || text.isEmpty()) return text
        val result = StringBuilder()
        text.codePoints().forEach { cp ->
            val found = S2T[cp]
            if (found != null) {
                result.appendCodePoint(found)
            } else {
                result.appendCodePoint(cp)
            }
        }
        return result.toString()
    }

    @JvmStatic
    fun TtoS(text: String): String {
        if (!initialized || text.isEmpty()) return text
        val result = StringBuilder()
        text.codePoints().forEach { cp ->
            val found = T2S[cp]
            if (found != null) {
                result.appendCodePoint(found)
            } else {
                result.appendCodePoint(cp)
            }
        }
        return result.toString()
    }
}
