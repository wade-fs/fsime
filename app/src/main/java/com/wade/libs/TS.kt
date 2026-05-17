package com.wade.libs

import android.util.Log

/**
 * Created by npes87184 on 2015/4/10.
 * Modified to load mappings from database.
 */
object TS {
    private val T2S = HashMap<Char, Char>()
    private val S2T = HashMap<Char, Char>()
    private var initialized = false

    fun initMapping(utf8t: String, utf8s: String) {
        if (initialized) return
        
        val tChars = utf8t.toCharArray()
        val sChars = utf8s.toCharArray()
        
        val size = minOf(tChars.size, sChars.size)
        for (i in 0 until size) {
            val cT = tChars[i]
            val cS = sChars[i]
            T2S[cT] = cS
            S2T[cS] = cT
        }
        initialized = true
        Log.i("TS", "Initialized mapping with $size characters")
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    @JvmStatic
    fun StoT(text: String): String {
        if (!initialized) return text
        val chars = text.toCharArray()
        var i = 0
        val n = chars.size
        while (i < n) {
            val found = S2T[chars[i]]
            if (null != found) chars[i] = found
            ++i
        }
        return String(chars)
    }

    @JvmStatic
    fun TtoS(text: String): String {
        if (!initialized) return text
        val chars = text.toCharArray()
        var i = 0
        val n = chars.size
        while (i < n) {
            val found = T2S[chars[i]]
            if (null != found) chars[i] = found
            ++i
        }
        return String(chars)
    }
}
