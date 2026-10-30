package com.wade.fsime.engine.phrase

import android.view.inputmethod.EditorInfo
import com.wade.fsime.data.BDatabase
import com.wade.fsime.data.repository.CandidatePhrase
import com.wade.fsime.engine.KeyboardPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex

class PhraseLearner(
    private val database: BDatabase,
    private val prefs: KeyboardPreferences
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    private val stagingBuffer = StagingBuffer()
    private val noiseFilter = NoiseFilter()

    /**
     * 第一層：使用者主動選字確認 (立即加權學習)
     */
    fun onCandidateSelected(
        phrase: String,
        editorInfo: EditorInfo?,
        boshiamyCode: String? = null,
        zhuyinCode: String? = null
    ) {
        if (!PrivacyGate.shouldLearn(editorInfo, prefs)) return
        if (phrase.length < 2) return

        scope.launch {
            withContext(Dispatchers.IO) {
                database.incrementUserPhraseUsage(
                    phrase = phrase,
                    boshiamyCode = boshiamyCode,
                    zhuyinCode = zhuyinCode,
                    source = "selected",
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * 第二層：整段文字送出 (無監督 N-gram 累積觀察)
     */
    fun onSentenceCommitted(sentence: String, editorInfo: EditorInfo?) {
        if (!PrivacyGate.shouldLearn(editorInfo, prefs)) return
        if (sentence.length < 2) return

        scope.launch {
            val ngrams = WordSegmenter.extractNGrams(sentence, minLen = 2, maxLen = 4)
            if (ngrams.isEmpty()) return@launch

            stagingBuffer.recordAll(ngrams)
            val qualified = stagingBuffer.evaluateQualified(noiseFilter)
            if (qualified.isNotEmpty()) {
                val candidatePhrases = qualified.map { mined ->
                    CandidatePhrase(
                        phrase = mined.phrase,
                        freq = 1.0 + mined.count * 0.1,
                        selectedCount = 0,
                        source = "mined",
                        confidence = mined.confidence,
                        createdAt = System.currentTimeMillis(),
                        lastUsed = System.currentTimeMillis()
                    )
                }
                withContext(Dispatchers.IO) {
                    database.batchInsertMinedPhrases(candidatePhrases)
                }
            }
        }
    }

    /**
     * 清理過期低頻冷詞
     */
    fun decayAndPrune() {
        scope.launch(Dispatchers.IO) {
            database.decayAndPruneMinedPhrases()
        }
    }
}
