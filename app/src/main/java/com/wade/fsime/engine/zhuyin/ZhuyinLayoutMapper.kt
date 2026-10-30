package com.wade.fsime.engine.zhuyin

object ZhuyinLayoutMapper {
    val INITIALS = setOf(
        "ㄅ", "ㄆ", "ㄇ", "ㄈ",
        "ㄉ", "ㄊ", "ㄋ", "ㄌ",
        "ㄍ", "ㄎ", "ㄏ",
        "ㄐ", "ㄑ", "ㄒ",
        "ㄓ", "ㄔ", "ㄕ", "ㄖ",
        "ㄗ", "ㄘ", "ㄙ"
    )

    val MEDIALS = setOf("ㄧ", "ㄨ", "ㄩ")

    val FINALS = setOf(
        "ㄚ", "ㄛ", "ㄜ", "ㄝ",
        "ㄞ", "ㄟ", "ㄠ", "ㄡ",
        "ㄢ", "ㄣ", "ㄤ", "ㄥ",
        "ㄦ"
    )

    val TONES = setOf("ˊ", "ˇ", "ˋ", "˙", " ")

    private val eten41KeyToSymbol = mapOf(
        "1" to "ㄅ", "q" to "ㄆ", "a" to "ㄇ", "z" to "ㄈ",
        "2" to "ㄉ", "w" to "ㄊ", "s" to "ㄋ", "x" to "ㄌ",
        "e" to "ㄍ", "d" to "ㄎ", "c" to "ㄏ",
        "r" to "ㄐ", "f" to "ㄑ", "v" to "ㄒ",
        "5" to "ㄓ", "t" to "ㄔ", "g" to "ㄕ", "b" to "ㄖ",
        "y" to "ㄗ", "h" to "ㄘ", "n" to "ㄙ",
        "u" to "ㄧ", "j" to "ㄨ", "m" to "ㄩ",
        "8" to "ㄚ", "i" to "ㄛ", "k" to "ㄜ", "," to "ㄝ",
        "9" to "ㄞ", "o" to "ㄟ", "l" to "ㄠ", "." to "ㄡ",
        "0" to "ㄢ", "p" to "ㄣ", ";" to "ㄤ", "/" to "ㄥ",
        "-" to "ㄦ",
        "3" to "ˇ", "4" to "ˋ", "6" to "ˊ", "7" to "˙", " " to " "
    )

    private val symbolToEten41Key = eten41KeyToSymbol.entries.associate { (k, v) -> v to k }

    fun isInitial(symbol: String): Boolean = INITIALS.contains(symbol)
    fun isMedial(symbol: String): Boolean = MEDIALS.contains(symbol)
    fun isFinal(symbol: String): Boolean = FINALS.contains(symbol)
    fun isToneSymbol(symbol: String): Boolean = TONES.contains(symbol)

    fun isZhuyinSymbol(symbol: String): Boolean =
        isInitial(symbol) || isMedial(symbol) || isFinal(symbol) || isToneSymbol(symbol)

    fun mapToSymbol(keyChar: String, layout: String = "eten41"): String? {
        if (isZhuyinSymbol(keyChar)) return keyChar
        val lower = keyChar.lowercase()
        return when (layout) {
            "eten41" -> eten41KeyToSymbol[lower] ?: eten41KeyToSymbol[keyChar]
            else -> eten41KeyToSymbol[lower] ?: eten41KeyToSymbol[keyChar]
        }
    }

    fun mapToKey(symbol: String, layout: String = "eten41"): String? {
        return when (layout) {
            "eten41" -> symbolToEten41Key[symbol]
            else -> symbolToEten41Key[symbol]
        }
    }
}
