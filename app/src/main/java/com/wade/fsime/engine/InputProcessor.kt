package com.wade.fsime.engine

import android.content.Context
import android.view.inputmethod.EditorInfo
import com.wade.fsime.data.BDatabase
import com.wade.fsime.engine.phrase.PhraseLearner
import com.wade.fsime.engine.zhuyin.ZhuyinStateMachine
import com.wade.fsime.math.MathParser
import com.wade.fsime.util.Stringy.removeSuffixRegex

/**
 * Handles the logic of input processing, candidate generation, and state management.
 * This acts as a "ViewModel" or "Controller" for the IME.
 */
class InputProcessor(
    private val context: Context,
    private val bdatabase: BDatabase,
    private val sharedPreferences: KeyboardPreferences
) {
    var state = KeyboardState()
        private set(value) {
            field = value
            onStateChanged?.invoke(value)
        }

    var onStateChanged: ((KeyboardState) -> Unit)? = null

    private var usePhrase = sharedPreferences.getUseKb("ck_phrase")
    private val zhuyinStateMachine = ZhuyinStateMachine()
    val phraseLearner = PhraseLearner(bdatabase, sharedPreferences)

    fun isZhuyinMode(): Boolean = state.activeKeyboardName in listOf("zhuyin", "ji")

    fun setKeyboard(name: String) {
        state = state.copy(activeKeyboardName = name)
        if (isZhuyinMode()) {
            zhuyinStateMachine.reset()
        }
    }

    fun setTs(t: Int) {
        bdatabase.setTs(t)
    }

    fun setCandidates(candidates: List<String>) {
        state = state.copy(candidates = candidates)
    }

    fun updateHandwritingVisibility(visible: Boolean) {
        state = state.copy(isHandwritingVisible = visible)
    }

    fun appendStroke(stroke: String) {
        if (isZhuyinMode()) {
            val stepResult = zhuyinStateMachine.processKey(stroke)
            val (newComposing, newDisplay) = when (stepResult) {
                is ZhuyinStateMachine.ZhuyinStepResult.Updated -> stepResult.searchCode to stepResult.displayText
                is ZhuyinStateMachine.ZhuyinStepResult.SyllableClosed -> stepResult.searchCode to stepResult.displayText
                is ZhuyinStateMachine.ZhuyinStepResult.AutoSegmented -> stepResult.searchCode to stepResult.displayText
                is ZhuyinStateMachine.ZhuyinStepResult.Ignored -> (state.composingText + stroke) to (state.displayComposing + stroke)
            }
            val newCandidates = computeCandidateList(newComposing)
            state = state.copy(
                composingText = newComposing,
                composingDisplay = newDisplay,
                candidates = newCandidates,
                shortestCode = null
            )
        } else {
            val newComposing = state.composingText + stroke
            val newCandidates = computeCandidateList(newComposing)
            val newShortestCode = if (newCandidates.isNotEmpty()) {
                bdatabase.reverseLookup(newCandidates[0]).firstOrNull()
            } else {
                null
            }
            state = state.copy(
                composingText = newComposing,
                composingDisplay = newComposing,
                candidates = newCandidates,
                shortestCode = newShortestCode
            )
        }
    }

    fun backspace() {
        if (isZhuyinMode()) {
            val removed = zhuyinStateMachine.backspace()
            if (removed && !zhuyinStateMachine.isEmpty()) {
                val newComposing = zhuyinStateMachine.currentSearchCode()
                val newDisplay = zhuyinStateMachine.currentDisplay()
                val newCandidates = computeCandidateList(newComposing)
                state = state.copy(
                    composingText = newComposing,
                    composingDisplay = newDisplay,
                    candidates = newCandidates,
                    shortestCode = null
                )
            } else {
                clearComposition()
            }
        } else if (state.composingText.isNotEmpty()) {
            val newComposing = removeSuffixRegex("(?s).", state.composingText)
            val newCandidates = computeCandidateList(newComposing)
            val newShortestCode = if (newCandidates.isNotEmpty()) {
                bdatabase.reverseLookup(newCandidates[0]).firstOrNull()
            } else {
                null
            }
            state = state.copy(
                composingText = newComposing,
                composingDisplay = newComposing,
                candidates = newCandidates,
                shortestCode = newShortestCode
            )
        }
    }

    fun setZhuyinTone1(): Boolean {
        if (!isZhuyinMode()) return false
        if (zhuyinStateMachine.currentSyllableHasNucleus() && !zhuyinStateMachine.currentSyllableHasTone()) {
            val res = zhuyinStateMachine.closeWithTone1()
            if (res is ZhuyinStateMachine.ZhuyinStepResult.SyllableClosed) {
                val newComposing = res.searchCode
                val newDisplay = res.displayText
                val newCandidates = computeCandidateList(newComposing)
                state = state.copy(
                    composingText = newComposing,
                    composingDisplay = newDisplay,
                    candidates = newCandidates,
                    shortestCode = null
                )
                return true
            }
        }
        return false
    }

    fun clearComposition() {
        if (isZhuyinMode()) {
            zhuyinStateMachine.reset()
        }
        state = state.copy(composingText = "", composingDisplay = "", candidates = emptyList())
    }

    fun commitText() {
        if (isZhuyinMode()) {
            zhuyinStateMachine.reset()
        }
        state = state.copy(composingText = "", composingDisplay = "", candidates = emptyList())
    }

    fun computeCandidateList(composing: String): List<String> {
        if (composing.isEmpty()) return emptyList()

        val listFromDb = bdatabase.getWord(
            composing,
            0,
            30,
            state.activeKeyboardName
        )
        
        val resultList: MutableList<String> = listFromDb.toMutableList()

        // Handle math parsing for digit keyboard
        if (state.activeKeyboardName == "digit" && (composing.endsWith("!") || composing.any { it in "0123456789+-*/^√" })) {
            try {
                val parser = MathParser.create()
                val isRadian = sharedPreferences.angleUnit() == "radian"
                android.util.Log.d("FSIME_MATH", "Digit parse: mode=$isRadian, expr=$composing")
                parser.isRadianMode = isRadian
                val res = parser.parse(composing)
                if (resultList.isEmpty() || resultList[0] != composing) {
                    resultList.add(0, composing)
                }
                resultList.add(1, res.toString())
            } catch (e: Exception) {
                android.util.Log.e("FSIME_MATH", "Digit parse failed: ${e.message}", e)
            }
        }

        // Handle complex math expressions (separated by ;)
        if (resultList.size == 1) {
            try {
                val parser = MathParser.create()
                parser.isRadianMode = sharedPreferences.angleUnit() == "radian"
                val exp = resultList[0]
                android.util.Log.d("FSIME_MATH", "Attempting to parse: $exp")
                val exps = exp.split(";").filter { it.isNotEmpty() }
                for (i in 0 until exps.size - 1) {
                    parser.addExpression(exps[i])
                }
                val res = parser.parse(exps.last())
                android.util.Log.d("FSIME_MATH", "Parse result: $res")
                resultList.add(res.toString())
            } catch (e: Exception) {
                android.util.Log.e("FSIME_MATH", "Parse failed: ${e.message}", e)
            }
        }

        return resultList
    }

    fun getAssociatedPhrases(contextString: String): List<String> {
        val tb = if (sharedPreferences.getUseKb("ck_phrase")) "phrase" else "vocabulary"
        return bdatabase.getPhrase(tb, contextString, 0, 30)
    }

    fun recordSelection(prevChar: String, selection: String, editorInfo: EditorInfo? = null) {
        val code = state.composingText
        val isZhuyin = isZhuyinMode()
        val boshiamyCode = if (!isZhuyin) code else null
        val zhuyinCode = if (isZhuyin) code else null

        bdatabase.updateUsage(prevChar, code, selection)
        phraseLearner.onCandidateSelected(selection, editorInfo, boshiamyCode, zhuyinCode)

        // Update shortestCode for the selected candidate
        val newShortestCode = bdatabase.reverseLookup(selection).firstOrNull()
        state = state.copy(shortestCode = newShortestCode)
    }

    fun onSentenceCommitted(sentence: String, editorInfo: EditorInfo? = null) {
        phraseLearner.onSentenceCommitted(sentence, editorInfo)
    }

    fun pickCandidate(index: Int): String? {
        val candidate = state.candidates.getOrNull(index)
        if (candidate != null) {
            clearComposition()
        }
        return candidate
    }
}
