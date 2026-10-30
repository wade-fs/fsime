package com.wade.fsime.engine.zhuyin

class ZhuyinStateMachine(private var layout: String = "eten41") {
    private var currentSyllable = ZhuyinSyllable()
    private val completedSyllables = mutableListOf<ZhuyinSyllable>()

    fun setLayout(newLayout: String) {
        this.layout = newLayout
        reset()
    }

    fun reset() {
        currentSyllable = ZhuyinSyllable()
        completedSyllables.clear()
    }

    fun isEmpty(): Boolean = !currentSyllable.isValidPartial() && completedSyllables.isEmpty()
    fun hasCurrentSyllable(): Boolean = currentSyllable.isValidPartial()
    fun currentSyllableHasNucleus(): Boolean = currentSyllable.hasNucleus()
    fun currentSyllableHasTone(): Boolean = currentSyllable.tone != null

    fun currentDisplay(): String {
        val completed = completedSyllables.joinToString("") { it.toDisplay() }
        return completed + currentSyllable.toDisplay()
    }

    fun currentSearchCode(): String {
        return if (currentSyllable.isValidPartial()) {
            ZhuyinCodeEncoder.encodeSyllable(currentSyllable)
        } else if (completedSyllables.isNotEmpty()) {
            ZhuyinCodeEncoder.encodeSyllable(completedSyllables.last())
        } else {
            ""
        }
    }

    fun currentFullCode(): String {
        val all = completedSyllables.toMutableList()
        if (currentSyllable.isValidPartial()) {
            all.add(currentSyllable)
        }
        return ZhuyinCodeEncoder.encodeSyllables(all)
    }

    /**
     * Explicitly closes the current syllable with Tone 1 (Space key decision tree).
     */
    fun closeWithTone1(): ZhuyinStepResult {
        if (currentSyllable.hasNucleus() && currentSyllable.tone == null) {
            val closed = currentSyllable.copy(tone = " ", isClosed = true)
            completedSyllables.add(closed)
            currentSyllable = ZhuyinSyllable()
            return ZhuyinStepResult.SyllableClosed(
                closed,
                currentDisplay(),
                ZhuyinCodeEncoder.encodeSyllable(closed)
            )
        }
        return ZhuyinStepResult.Ignored
    }

    /**
     * Removes the last entered component.
     */
    fun backspace(): Boolean {
        when {
            currentSyllable.tone != null -> {
                currentSyllable = currentSyllable.copy(tone = null, isClosed = false)
                return true
            }
            currentSyllable.`final` != null -> {
                currentSyllable = currentSyllable.copy(`final` = null)
                return true
            }
            currentSyllable.medial != null -> {
                currentSyllable = currentSyllable.copy(medial = null)
                return true
            }
            currentSyllable.initial != null -> {
                currentSyllable = currentSyllable.copy(initial = null)
                return true
            }
            completedSyllables.isNotEmpty() -> {
                currentSyllable = completedSyllables.removeAt(completedSyllables.size - 1).copy(isClosed = false)
                return true
            }
            else -> return false
        }
    }

    /**
     * Processes a single input key/symbol.
     */
    fun processKey(keyChar: String): ZhuyinStepResult {
        val symbol = ZhuyinLayoutMapper.mapToSymbol(keyChar, layout) ?: return ZhuyinStepResult.Ignored

        when {
            ZhuyinLayoutMapper.isToneSymbol(symbol) -> {
                if (symbol == " ") {
                    return closeWithTone1()
                }
                if (currentSyllable.isValidPartial()) {
                    val closed = currentSyllable.copy(tone = symbol, isClosed = true)
                    completedSyllables.add(closed)
                    currentSyllable = ZhuyinSyllable()
                    return ZhuyinStepResult.SyllableClosed(
                        closed,
                        currentDisplay(),
                        ZhuyinCodeEncoder.encodeSyllable(closed)
                    )
                } else if (completedSyllables.isNotEmpty()) {
                    val last = completedSyllables.removeAt(completedSyllables.size - 1)
                    val updated = last.copy(tone = symbol)
                    completedSyllables.add(updated)
                    return ZhuyinStepResult.SyllableClosed(
                        updated,
                        currentDisplay(),
                        ZhuyinCodeEncoder.encodeSyllable(updated)
                    )
                }
            }
            ZhuyinLayoutMapper.isInitial(symbol) -> {
                if (currentSyllable.hasNucleus()) {
                    val previous = currentSyllable.copy(tone = currentSyllable.tone ?: " ", isClosed = true)
                    completedSyllables.add(previous)
                    currentSyllable = ZhuyinSyllable(initial = symbol)
                    return ZhuyinStepResult.AutoSegmented(
                        previous,
                        currentSyllable,
                        currentDisplay(),
                        ZhuyinCodeEncoder.encodeSyllable(currentSyllable)
                    )
                } else {
                    currentSyllable = currentSyllable.copy(initial = symbol)
                    return ZhuyinStepResult.Updated(currentDisplay(), currentSearchCode())
                }
            }
            ZhuyinLayoutMapper.isMedial(symbol) -> {
                if (currentSyllable.`final` != null) {
                    val previous = currentSyllable.copy(tone = currentSyllable.tone ?: " ", isClosed = true)
                    completedSyllables.add(previous)
                    currentSyllable = ZhuyinSyllable(medial = symbol)
                    return ZhuyinStepResult.AutoSegmented(
                        previous,
                        currentSyllable,
                        currentDisplay(),
                        ZhuyinCodeEncoder.encodeSyllable(currentSyllable)
                    )
                } else {
                    currentSyllable = currentSyllable.copy(medial = symbol)
                    return ZhuyinStepResult.Updated(currentDisplay(), currentSearchCode())
                }
            }
            ZhuyinLayoutMapper.isFinal(symbol) -> {
                if (currentSyllable.`final` != null) {
                    val previous = currentSyllable.copy(tone = currentSyllable.tone ?: " ", isClosed = true)
                    completedSyllables.add(previous)
                    currentSyllable = ZhuyinSyllable(`final` = symbol)
                    return ZhuyinStepResult.AutoSegmented(
                        previous,
                        currentSyllable,
                        currentDisplay(),
                        ZhuyinCodeEncoder.encodeSyllable(currentSyllable)
                    )
                } else {
                    currentSyllable = currentSyllable.copy(`final` = symbol)
                    return ZhuyinStepResult.Updated(currentDisplay(), currentSearchCode())
                }
            }
        }
        return ZhuyinStepResult.Updated(currentDisplay(), currentSearchCode())
    }

    sealed class ZhuyinStepResult {
        object Ignored : ZhuyinStepResult()
        data class Updated(val displayText: String, val searchCode: String) : ZhuyinStepResult()
        data class SyllableClosed(val syllable: ZhuyinSyllable, val displayText: String, val searchCode: String) : ZhuyinStepResult()
        data class AutoSegmented(val completed: ZhuyinSyllable, val newSyllable: ZhuyinSyllable, val displayText: String, val searchCode: String) : ZhuyinStepResult()
    }
}
