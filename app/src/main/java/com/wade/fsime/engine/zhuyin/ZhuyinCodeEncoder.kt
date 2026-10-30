package com.wade.fsime.engine.zhuyin

object ZhuyinCodeEncoder {
    private val initialToCode = mapOf(
        "ㄅ" to "1", "ㄆ" to "q", "ㄇ" to "a", "ㄈ" to "z",
        "ㄉ" to "2", "ㄊ" to "w", "ㄋ" to "s", "ㄌ" to "x",
        "ㄍ" to "e", "ㄎ" to "d", "ㄏ" to "c",
        "ㄐ" to "r", "ㄑ" to "f", "ㄒ" to "v",
        "ㄓ" to "5", "ㄔ" to "t", "ㄕ" to "g", "ㄖ" to "b",
        "ㄗ" to "y", "ㄘ" to "h", "ㄙ" to "n"
    )

    private val medialToCode = mapOf(
        "ㄧ" to "u", "ㄨ" to "j", "ㄩ" to "m"
    )

    private val finalToCode = mapOf(
        "ㄚ" to "8", "ㄛ" to "i", "ㄜ" to "k", "ㄝ" to ",",
        "ㄞ" to "9", "ㄟ" to "o", "ㄠ" to "l", "ㄡ" to ".",
        "ㄢ" to "0", "ㄣ" to "p", "ㄤ" to ";", "ㄥ" to "/",
        "ㄦ" to "-"
    )

    private val toneToCode = mapOf(
        "ˊ" to "6", // 二聲
        "ˇ" to "3", // 三聲
        "ˋ" to "4", // 四聲
        "˙" to "7", // 輕聲
        " " to "",  // 一聲無後綴
        "" to ""
    )

    /**
     * Encodes a single [ZhuyinSyllable] into the ASCII code format used by the `ji` table in b.db.
     * Zero-initial syllables have no initial prefix.
     * Tone 1 has no suffix.
     */
    fun encodeSyllable(syllable: ZhuyinSyllable): String {
        val sb = java.lang.StringBuilder()
        syllable.initial?.let { initialToCode[it]?.let { c -> sb.append(c) } }
        syllable.medial?.let { medialToCode[it]?.let { c -> sb.append(c) } }
        syllable.`final`?.let { finalToCode[it]?.let { c -> sb.append(c) } }
        syllable.tone?.let { toneToCode[it]?.let { c -> sb.append(c) } }
        return sb.toString()
    }

    /**
     * Encodes a list of syllables delimited by space, as specified for user_phrases.
     */
    fun encodeSyllables(syllables: List<ZhuyinSyllable>): String {
        return syllables.joinToString(" ") { encodeSyllable(it) }
    }
}
