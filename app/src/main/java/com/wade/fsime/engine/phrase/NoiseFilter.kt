package com.wade.fsime.engine.phrase

import kotlin.math.ln

data class MinedPhrase(
    val phrase: String,
    val count: Int,
    val pmi: Double,
    val boundaryEntropy: Double,
    val confidence: Double
)

class NoiseFilter(
    private val minCount: Int = 3,
    private val minPmi: Double = 0.5,
    private val minEntropy: Double = 0.5
) {
    private val stopParticles = setOf('的', '了', '麼', '呢', '吧', '啊', '在', '是', '有', '和')

    private fun log2(x: Double): Double = if (x <= 0.0) 0.0 else ln(x) / ln(2.0)

    fun calculateEntropy(distribution: Map<Char, Int>): Double {
        val total = distribution.values.sum().toDouble()
        if (total <= 1.0) return 0.0
        var entropy = 0.0
        for (count in distribution.values) {
            val p = count / total
            if (p > 0.0) {
                entropy -= p * log2(p)
            }
        }
        return entropy
    }

    /**
     * Calculates Pointwise Mutual Information for a phrase.
     */
    fun calculatePmi(
        phrase: String,
        phraseCount: Int,
        subCounts: (String) -> Int,
        totalObservations: Long
    ): Double {
        if (phrase.length < 2 || phraseCount <= 0 || totalObservations <= 0) return 0.0
        val n = totalObservations.toDouble()
        var minSplitPmi = Double.MAX_VALUE

        for (k in 1 until phrase.length) {
            val leftPart = phrase.substring(0, k)
            val rightPart = phrase.substring(k)
            val leftCount = subCounts(leftPart).coerceAtLeast(1)
            val rightCount = subCounts(rightPart).coerceAtLeast(1)

            val pmi = log2((phraseCount.toDouble() * n) / (leftCount.toDouble() * rightCount.toDouble()))
            if (pmi < minSplitPmi) {
                minSplitPmi = pmi
            }
        }
        return if (minSplitPmi == Double.MAX_VALUE) 0.0 else minSplitPmi
    }

    /**
     * Evaluates whether an observed N-gram qualifies as a new valid phrase.
     */
    fun evaluate(
        phrase: String,
        count: Int,
        leftNeighbors: Map<Char, Int>,
        rightNeighbors: Map<Char, Int>,
        subCounts: (String) -> Int,
        totalObservations: Long
    ): MinedPhrase? {
        if (count < minCount) return null
        if (phrase.length < 2) return null

        // Penalize phrases starting or ending with stop particles
        if (stopParticles.contains(phrase.first()) || stopParticles.contains(phrase.last())) {
            return null
        }

        val pmi = calculatePmi(phrase, count, subCounts, totalObservations)
        if (pmi < minPmi) return null

        val leftEntropy = calculateEntropy(leftNeighbors)
        val rightEntropy = calculateEntropy(rightNeighbors)
        val boundaryEntropy = leftEntropy.coerceAtMost(rightEntropy)

        if (boundaryEntropy < minEntropy) return null

        // Confidence normalized based on count, pmi, and entropy
        val confCount = (count.toDouble() / 10.0).coerceAtMost(1.0)
        val confPmi = (pmi / 5.0).coerceIn(0.1, 1.0)
        val confEntropy = (boundaryEntropy / 2.0).coerceIn(0.1, 1.0)
        val confidence = (confCount * 0.4 + confPmi * 0.3 + confEntropy * 0.3).coerceIn(0.1, 1.0)

        return MinedPhrase(
            phrase = phrase,
            count = count,
            pmi = pmi,
            boundaryEntropy = boundaryEntropy,
            confidence = confidence
        )
    }
}
