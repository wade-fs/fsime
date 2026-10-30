package com.wade.fsime.engine.phrase

import com.wade.fsime.data.repository.CandidatePhrase
import com.wade.fsime.data.repository.CodeType
import com.wade.fsime.data.repository.CompositePhraseRepository
import com.wade.fsime.data.repository.PhraseRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseMiningTest {

    @Test
    fun testWordSegmenter() {
        val sentence = "我們今天在台北吃牛肉麵！"
        val ngrams = WordSegmenter.extractNGrams(sentence, minLen = 2, maxLen = 4)
        assertTrue(ngrams.isNotEmpty())

        val phrases = ngrams.map { it.phrase }
        assertTrue(phrases.contains("今天"))
        assertTrue(phrases.contains("台北"))
        assertTrue(phrases.contains("牛肉麵"))

        // Check context neighbors for "牛肉麵"
        val beefNoodles = ngrams.find { it.phrase == "牛肉麵" }
        assertNotNull(beefNoodles)
        assertEquals('吃', beefNoodles?.leftChar)
        assertNull(beefNoodles?.rightChar) // Exclamation mark is stripped
    }

    @Test
    fun testNoiseFilterStopParticles() {
        val filter = NoiseFilter(minCount = 2, minPmi = 0.1, minEntropy = 0.1)
        val leftMap = mapOf('我' to 5, '你' to 5)
        val rightMap = mapOf('好' to 5, '棒' to 5)

        // Phrase ending with '的' should be rejected
        val badPhrase = filter.evaluate(
            phrase = "很好的",
            count = 10,
            leftNeighbors = leftMap,
            rightNeighbors = rightMap,
            subCounts = { 5 },
            totalObservations = 100
        )
        assertNull(badPhrase)
    }

    @Test
    fun testNoiseFilterValidPhrase() {
        val filter = NoiseFilter(minCount = 3, minPmi = 0.5, minEntropy = 0.5)
        val leftMap = mapOf('去' to 3, '到' to 3, '在' to 4)
        val rightMap = mapOf('吃' to 3, '玩' to 4, '買' to 3)

        val result = filter.evaluate(
            phrase = "台北車站",
            count = 10,
            leftNeighbors = leftMap,
            rightNeighbors = rightMap,
            subCounts = { 10 },
            totalObservations = 200
        )
        assertNotNull(result)
        assertEquals("台北車站", result?.phrase)
        assertTrue((result?.confidence ?: 0.0) > 0.0)
    }

    @Test
    fun testStagingBufferPromotion() = runBlocking {
        val buffer = StagingBuffer()
        val filter = NoiseFilter(minCount = 2, minPmi = 0.1, minEntropy = 0.1)

        val ngrams = listOf(
            NGramOccurrence("人工智慧", '做', '的'),
            NGramOccurrence("人工智慧", '學', '在'),
            NGramOccurrence("人工智慧", '用', '是')
        )

        buffer.recordAll(ngrams)
        val qualified = buffer.evaluateQualified(filter)
        assertTrue(qualified.any { it.phrase == "人工智慧" })

        // Second evaluation should not re-promote
        val qualifiedSecond = buffer.evaluateQualified(filter)
        assertTrue(qualifiedSecond.none { it.phrase == "人工智慧" })
    }

    @Test
    fun testCompositePhraseRepository() = runBlocking {
        val userRepo = object : PhraseRepository {
            override suspend fun search(code: String, type: CodeType, limit: Int): List<CandidatePhrase> {
                return listOf(CandidatePhrase(phrase = "混蝦輸入法", freq = 10.0, source = "selected"))
            }
            override suspend fun recordUsage(phrase: String, code: String, type: CodeType) {}
        }

        val localRepo = object : PhraseRepository {
            override suspend fun search(code: String, type: CodeType, limit: Int): List<CandidatePhrase> {
                return listOf(
                    CandidatePhrase(phrase = "混淆", freq = 2.0, source = "local"),
                    CandidatePhrase(phrase = "混蝦輸入法", freq = 1.0, source = "local")
                )
            }
            override suspend fun recordUsage(phrase: String, code: String, type: CodeType) {}
        }

        val composite = CompositePhraseRepository(
            userDb = userRepo,
            localDb = localRepo
        )

        val res = composite.search("test", CodeType.BOSHIAMY, 10)
        assertEquals(2, res.size)
        // User phrase comes first
        assertEquals("混蝦輸入法", res[0].phrase)
        assertEquals("selected", res[0].source)
        assertEquals("混淆", res[1].phrase)
    }
}
