package com.wade.fsime.engine

/**
 * Represents the current state of the IME that the UI should reflect.
 */
data class KeyboardState(
    val composingText: String = "",
    val candidates: List<String> = emptyList(),
    val activeKeyboardName: String = "full",
    val isHandwritingVisible: Boolean = false
)
