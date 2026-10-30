package com.wade.fsime.data.repository

enum class CodeType {
    BOSHIAMY,
    ZHUYIN
}

data class CandidatePhrase(
    val phrase: String,
    val boshiamyCode: String? = null,
    val zhuyinCode: String? = null,
    val freq: Double = 1.0,
    val selectedCount: Int = 1,
    val source: String = "local",
    val confidence: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis()
)

interface PhraseRepository {
    suspend fun search(code: String, type: CodeType, limit: Int = 30): List<CandidatePhrase>
    suspend fun recordUsage(phrase: String, code: String, type: CodeType)
}
