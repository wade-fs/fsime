package com.wade.fsime.engine.phrase

data class NGramOccurrence(
    val phrase: String,
    val leftChar: Char?,
    val rightChar: Char?
)

object WordSegmenter {
    /**
     * Checks if a character is a CJK ideograph.
     */
    fun isChineseChar(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
    }

    /**
     * Extracts all N-grams of length [minLen] to [maxLen] from a sentence,
     * including boundary neighbor characters for entropy computation.
     */
    fun extractNGrams(sentence: String, minLen: Int = 2, maxLen: Int = 4): List<NGramOccurrence> {
        val results = mutableListOf<NGramOccurrence>()
        if (sentence.length < minLen) return results

        // Split text into continuous Chinese chunks
        var currentChunk = StringBuilder()
        val chunks = mutableListOf<String>()
        for (char in sentence) {
            if (isChineseChar(char)) {
                currentChunk.append(char)
            } else {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                }
            }
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }

        for (chunk in chunks) {
            val len = chunk.length
            if (len < minLen) continue
            for (wLen in minLen..maxLen.coerceAtMost(len)) {
                for (i in 0..(len - wLen)) {
                    val phrase = chunk.substring(i, i + wLen)
                    val leftChar = if (i > 0) chunk[i - 1] else null
                    val rightChar = if (i + wLen < len) chunk[i + wLen] else null
                    results.add(NGramOccurrence(phrase, leftChar, rightChar))
                }
            }
        }

        return results
    }
}
