package com.wade.fsime.data.repository

class CompositePhraseRepository(
    private val userDb: PhraseRepository,
    private val localDb: PhraseRepository,
    private val cloudApi: PhraseRepository? = null
) : PhraseRepository {
    override suspend fun search(code: String, type: CodeType, limit: Int): List<CandidatePhrase> {
        val userResults = userDb.search(code, type, limit)
        val remaining = limit - userResults.size
        val localResults = if (remaining > 0) localDb.search(code, type, remaining) else emptyList()
        val combined = (userResults + localResults).distinctBy { it.phrase }
        if (combined.size < limit && cloudApi != null) {
            val cloudResults = cloudApi.search(code, type, limit - combined.size)
            return (combined + cloudResults).distinctBy { it.phrase }
        }
        return combined
    }

    override suspend fun recordUsage(phrase: String, code: String, type: CodeType) {
        userDb.recordUsage(phrase, code, type)
    }
}
