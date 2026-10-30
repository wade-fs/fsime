package com.wade.fsime.engine

import android.text.InputType
import android.view.inputmethod.EditorInfo

object InputFieldClassifier {
    enum class DirectMode { DIRECT_COMMIT, COMPOSING }

    fun classify(editorInfo: EditorInfo?): DirectMode {
        if (editorInfo == null) return DirectMode.COMPOSING
        val inputType = editorInfo.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION

        // Direct commit conditions for OTP / digits / phone
        if (inputClass == InputType.TYPE_CLASS_NUMBER ||
            inputClass == InputType.TYPE_CLASS_PHONE ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) {
            return DirectMode.DIRECT_COMMIT
        }
        return DirectMode.COMPOSING
    }
}
