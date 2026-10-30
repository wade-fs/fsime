package com.wade.fsime.data.repository

import com.wade.fsime.data.BDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserPhraseRepository(private val database: BDatabase) : PhraseRepository {
    override suspend fun search(code: String, type: CodeType, limit: Int): List<CandidatePhrase> =
        withContext(Dispatchers.IO) {
            database.searchUserPhrases(code, type, limit)
        }

    override suspend fun recordUsage(phrase: String, code: String, type: CodeType) =
        withContext(Dispatchers.IO) {
            val bCode = if (type == CodeType.BOSHIAMY) code else null
            val zCode = if (type == CodeType.ZHUYIN) code else null
            database.incrementUserPhraseUsage(
                phrase = phrase,
                boshiamyCode = bCode,
                zhuyinCode = zCode,
                source = "selected",
                timestamp = System.currentTimeMillis()
            )
        }
}
