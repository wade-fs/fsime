package com.wade.fsime.engine.phrase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StagingBuffer(private val maxCapacity: Int = 3000) {
    private val mutex = Mutex()
    private val phraseCounts = mutableMapOf<String, Int>()
    private val leftNeighbors = mutableMapOf<String, MutableMap<Char, Int>>()
    private val rightNeighbors = mutableMapOf<String, MutableMap<Char, Int>>()
    private val promotedPhrases = mutableSetOf<String>()
    private var totalObservations: Long = 0L

    suspend fun recordAll(ngrams: List<NGramOccurrence>) = mutex.withLock {
        for (item in ngrams) {
            totalObservations++
            val phrase = item.phrase
            phraseCounts[phrase] = (phraseCounts[phrase] ?: 0) + 1

            if (item.leftChar != null) {
                val leftMap = leftNeighbors.getOrPut(phrase) { mutableMapOf() }
                leftMap[item.leftChar] = (leftMap[item.leftChar] ?: 0) + 1
            }

            if (item.rightChar != null) {
                val rightMap = rightNeighbors.getOrPut(phrase) { mutableMapOf() }
                rightMap[item.rightChar] = (rightMap[item.rightChar] ?: 0) + 1
            }
        }

        // Bound memory size if exceeds capacity
        if (phraseCounts.size > maxCapacity) {
            pruneInfrequent()
        }
    }

    private fun pruneInfrequent() {
        val entries = phraseCounts.entries.sortedBy { it.value }
        val toRemove = entries.take(entries.size / 3)
        for (entry in toRemove) {
            phraseCounts.remove(entry.key)
            leftNeighbors.remove(entry.key)
            rightNeighbors.remove(entry.key)
        }
    }

    suspend fun evaluateQualified(filter: NoiseFilter): List<MinedPhrase> = mutex.withLock {
        val qualified = mutableListOf<MinedPhrase>()
        val subCounts: (String) -> Int = { phraseCounts[it] ?: 0 }

        for ((phrase, count) in phraseCounts) {
            if (promotedPhrases.contains(phrase)) continue

            val leftMap = leftNeighbors[phrase] ?: emptyMap()
            val rightMap = rightNeighbors[phrase] ?: emptyMap()

            val mined = filter.evaluate(
                phrase = phrase,
                count = count,
                leftNeighbors = leftMap,
                rightNeighbors = rightMap,
                subCounts = subCounts,
                totalObservations = totalObservations
            )

            if (mined != null) {
                promotedPhrases.add(phrase)
                qualified.add(mined)
            }
        }
        return@withLock qualified
    }

    suspend fun clear() = mutex.withLock {
        phraseCounts.clear()
        leftNeighbors.clear()
        rightNeighbors.clear()
        promotedPhrases.clear()
        totalObservations = 0L
    }
}
