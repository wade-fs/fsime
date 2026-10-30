package com.wade.fsime.data.repository

import com.wade.fsime.data.BDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalPhraseRepository(private val database: BDatabase) : PhraseRepository {
    override suspend fun search(code: String, type: CodeType, limit: Int): List<CandidatePhrase> =
        withContext(Dispatchers.IO) {
            val table = if (type == CodeType.BOSHIAMY) "boshiamy" else "ji"
            val words = database.getWord(code, 0, limit, table)
            words.filter { it != code }.map { word ->
                CandidatePhrase(
                    phrase = word,
                    boshiamyCode = if (type == CodeType.BOSHIAMY) code else null,
                    zhuyinCode = if (type == CodeType.ZHUYIN) code else null,
                    source = "local"
                )
            }
        }

    override suspend fun recordUsage(phrase: String, code: String, type: CodeType) {
        // Local read-only database does not record usage directly
    }
}
