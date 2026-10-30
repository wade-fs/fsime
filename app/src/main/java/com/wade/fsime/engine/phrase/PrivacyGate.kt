package com.wade.fsime.engine.phrase

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.wade.fsime.engine.KeyboardPreferences

object PrivacyGate {
    fun shouldLearn(editorInfo: EditorInfo?, prefs: KeyboardPreferences): Boolean {
        if (!prefs.isPersonalizedLearningEnabled()) return false
        if (editorInfo == null) return false

        // 1. System privacy flag
        if ((editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) {
            return false
        }

        // 2. Password field protection
        val inputType = editorInfo.inputType
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) {
            return false
        }

        return true
    }
}
