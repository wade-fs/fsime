package com.wade.fsime.engine

/**
 * Represents the current state of the IME that the UI should reflect.
 */
data class KeyboardState(
    val composingText: String = "",
    val composingDisplay: String = "",
    val candidates: List<String> = emptyList(),
    val shortestCode: String? = null,
    val activeKeyboardName: String = "full",
    val isHandwritingVisible: Boolean = false
) {
    val displayComposing: String
        get() = if (composingDisplay.isNotEmpty()) composingDisplay else composingText
}
