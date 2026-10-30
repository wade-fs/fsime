package com.wade.fsime.engine

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class InputFieldClassifierTest {

    @Test
    fun testNullEditorInfo() {
        assertEquals(InputFieldClassifier.DirectMode.COMPOSING, InputFieldClassifier.classify(null))
    }

    @Test
    fun testNumberFieldDirectCommit() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        assertEquals(InputFieldClassifier.DirectMode.DIRECT_COMMIT, InputFieldClassifier.classify(info))
    }

    @Test
    fun testPhoneFieldDirectCommit() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_PHONE
        }
        assertEquals(InputFieldClassifier.DirectMode.DIRECT_COMMIT, InputFieldClassifier.classify(info))
    }

    @Test
    fun testNumberPasswordDirectCommit() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        assertEquals(InputFieldClassifier.DirectMode.DIRECT_COMMIT, InputFieldClassifier.classify(info))
    }

    @Test
    fun testNormalTextFieldComposing() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }
        assertEquals(InputFieldClassifier.DirectMode.COMPOSING, InputFieldClassifier.classify(info))
    }

    @Test
    fun testTextPasswordComposing() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertEquals(InputFieldClassifier.DirectMode.COMPOSING, InputFieldClassifier.classify(info))
    }
}
