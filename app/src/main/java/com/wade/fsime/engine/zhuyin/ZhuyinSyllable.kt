package com.wade.fsime.engine.zhuyin

/**
 * Represents a single Zhuyin syllable comprising initial, medial, final, and tone.
 */
data class ZhuyinSyllable(
    val initial: String? = null, // 聲母: ㄅ~ㄙ (可選)
    val medial: String? = null,  // 介音: ㄧ, ㄨ, ㄩ (可選)
    val `final`: String? = null, // 韻母: ㄚ~ㄦ (可選)
    val tone: String? = null,    // 聲調: 空白(一聲), ˊ, ˇ, ˋ, ˙ (可選)
    val isClosed: Boolean = false // 音節是否已定界閉合
) {
    // 檢查目前已輸入的部分是否構成合法音節片段
    fun isValidPartial(): Boolean = initial != null || medial != null || `final` != null

    // 是否已具備發音主體 (介音或韻母)
    fun hasNucleus(): Boolean = medial != null || `final` != null

    fun toDisplay(): String = "${initial ?: ""}${medial ?: ""}${`final` ?: ""}${if (tone != null && tone != " ") tone else ""}"
}
