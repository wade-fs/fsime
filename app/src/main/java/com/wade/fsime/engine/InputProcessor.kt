package com.wade.fsime.engine

import android.content.Context
import com.wade.fsime.data.BDatabase
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

    fun setKeyboard(name: String) {
        state = state.copy(activeKeyboardName = name)
    }

    fun updateHandwritingVisibility(visible: Boolean) {
        state = state.copy(isHandwritingVisible = visible)
    }

    fun appendStroke(stroke: String) {
        val newComposing = state.composingText + stroke
        val newCandidates = computeCandidateList(newComposing)
        state = state.copy(
            composingText = newComposing,
            candidates = newCandidates
        )
    }

    fun backspace() {
        if (state.composingText.isNotEmpty()) {
            val newComposing = removeSuffixRegex("(?s).", state.composingText)
            val newCandidates = computeCandidateList(newComposing)
            state = state.copy(
                composingText = newComposing,
                candidates = newCandidates
            )
        }
    }

    fun clearComposition() {
        state = state.copy(composingText = "", candidates = emptyList())
    }

    fun setCandidates(candidates: List<String>) {
        state = state.copy(candidates = candidates)
    }

    fun computeCandidateList(composing: String): List<String> {
        if (composing.isEmpty()) return emptyList()

        val listFromDb = bdatabase.getWord(
            composing,
            0,
            30,
            state.activeKeyboardName
        )
        
        var resultList: MutableList<String> = listFromDb.toMutableList()

        // Handle math parsing for digit keyboard
        if (state.activeKeyboardName == "digit" && composing.endsWith("!")) {
            try {
                val parser = MathParser.create()
                val res = parser.parse(composing)
                resultList.add(0, composing)
                resultList.add(1, res.toString())
            } catch (ignore: Exception) {}
        }

        // Handle complex math expressions (separated by ;)
        if (resultList.size == 1) {
            try {
                val parser = MathParser.create()
                val exp = resultList[0]
                val exps = exp.split(";").filter { it.isNotEmpty() }
                for (i in 0 until exps.size - 1) {
                    parser.addExpression(exps[i])
                }
                val res = parser.parse(exps.last())
                resultList.add(res.toString())
            } catch (ignore: Exception) {}
        }

        return resultList
    }

    fun updateRelative(lastCommitted: String) {
        // Logic for phrase/vocabulary association
        val tb = if (sharedPreferences.getUseKb("ck_phrase")) "phrase" else "vocabulary"
        // This normally needs context from the input connection which we don't have here.
        // We might need to pass the context string into this method.
    }
    
    fun getAssociatedPhrases(contextString: String): List<String> {
        val tb = if (sharedPreferences.getUseKb("ck_phrase")) "phrase" else "vocabulary"
        return bdatabase.getPhrase(tb, contextString, 0, 30)
    }

    fun recordSelection(prevChar: String, selection: String) {
        val code = state.composingText
        bdatabase.updateUsage(prevChar, code, selection)
    }

    fun pickCandidate(index: Int): String? {
        val candidate = state.candidates.getOrNull(index)
        if (candidate != null) {
            clearComposition()
        }
        return candidate
    }
}
