/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.fsime

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.readystatesoftware.android.sqliteassethelper.BuildConfig
import com.wade.MathParser.MathParser
import com.wade.fsime.CandidatesViewAdapter.CandidateListener
import com.wade.fsime.KeyboardView.KeyboardListener
import com.wade.libs.BDatabase
import com.wade.mil.Mil
import com.wade.utilities.Contexty.loadPreferenceString
import com.wade.utilities.Contexty.savePreferenceString
import com.wade.utilities.Contexty.showSystemKeyboardChanger
import com.wade.utilities.Stringy.getFirstCodePoint
import com.wade.utilities.Stringy.isAscii
import com.wade.utilities.Stringy.removeSuffixRegex
import com.wade.utilities.Stringy.toCodePointList
import com.wade.utilities.invertMap
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Arrays

/*
  An InputMethodService for the FS Input Method (混瞎輸入法).
*/
class FsimeService : InputMethodService(), CandidateListener, KeyboardListener {
    var fullKB: Keyboard? = null
    var fsimeKB: Keyboard? = null
    var pureKB: Keyboard? = null
    var digitKB: Keyboard? = null
    var jiKB: Keyboard? = null
    var cjKB: Keyboard? = null
    var strokeKB: Keyboard? = null
    var milKB: Keyboard? = null
    private var inputContainer: InputContainer? = null
    private var mComposing = ""
    private var candidateList: List<String> = ArrayList()
    private var inputOptionsBits = 0
    private var enterKeyHasAction = false
    private var inputIsPassword = false
    var bdatabase: BDatabase? = null
    private var mil: Mil? = null
    var sharedPreferences: KeyboardPreferences? = null
    var codeMaps: MutableMap<Int, String> = HashMap()
    val SWIPE_NONE = 0
    val SWIPE_RU = 1
    val SWIPE_LD = 2
    val SWIPE_LU = 4
    val SWIPE_RD = 8
    override fun onCreate() {
        super.onCreate()
        sharedPreferences = KeyboardPreferences(this)

        mil = Mil()
        codeMaps[KeyEvent.KEYCODE_0] = "Ctrl0"
        codeMaps[KeyEvent.KEYCODE_1] = "Ctrl1"
        codeMaps[KeyEvent.KEYCODE_2] = "Ctrl2"
        codeMaps[KeyEvent.KEYCODE_3] = "Ctrl3"
        codeMaps[KeyEvent.KEYCODE_4] = "Ctrl4"
        codeMaps[KeyEvent.KEYCODE_5] = "Ctrl5"
        codeMaps[KeyEvent.KEYCODE_6] = "Ctrl6"
        codeMaps[KeyEvent.KEYCODE_7] = "Ctrl7"
        codeMaps[KeyEvent.KEYCODE_8] = "Ctrl8"
        codeMaps[KeyEvent.KEYCODE_9] = "Ctrl9"
        codeMaps[KeyEvent.KEYCODE_Q] = "CtrlQ"
        codeMaps[KeyEvent.KEYCODE_W] = "CtrlW"
        codeMaps[KeyEvent.KEYCODE_E] = "CtrlE"
        codeMaps[KeyEvent.KEYCODE_R] = "CtrlR"
        codeMaps[KeyEvent.KEYCODE_T] = "CtrlT"
        codeMaps[KeyEvent.KEYCODE_Y] = "CtrlY"
        codeMaps[KeyEvent.KEYCODE_U] = "CtrlU"
        codeMaps[KeyEvent.KEYCODE_I] = "CtrlI"
        codeMaps[KeyEvent.KEYCODE_O] = "CtrlO"
        codeMaps[KeyEvent.KEYCODE_P] = "CtrlP"


        fullKB = Keyboard(this, R.xml.keyboard_full, KEYBOARD_NAME_FULL)
        fsimeKB = Keyboard(this, R.xml.keyboard_fsime, KEYBOARD_NAME_FSIME)
        pureKB = Keyboard(this, R.xml.keyboard_pure, KEYBOARD_NAME_PURE)
        digitKB = Keyboard(this, R.xml.keyboard_digit, KEYBOARD_NAME_DIGIT)
        jiKB = Keyboard(this, R.xml.keyboard_ji, KEYBOARD_NAME_JI)
        cjKB = Keyboard(this, R.xml.keyboard_cj, KEYBOARD_NAME_CJ)
        strokeKB = Keyboard(this, R.xml.keyboard_stroke, KEYBOARD_NAME_STROKE)
        milKB = Keyboard(this, R.xml.keyboard_mil, KEYBOARD_NAME_MIL)
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        bdatabase = BDatabase(applicationContext)
        inputContainer = layoutInflater.inflate(R.layout.input_container, null) as InputContainer
        inputContainer!!.initialiseCandidatesView(this)
        inputContainer!!.initialiseKeyboardView(this, loadSavedKeyboard())
        return inputContainer!!
    }
    private fun setCandidateOrder() {
        val candidateOrder: String = sharedPreferences!!.candidateOrder()
        bdatabase!!.setTs(
            when (candidateOrder) {
                "TraditionalOnly" -> 1
                "SimplifiedOnly" -> 2
                else -> 0
            }
        )
    }
    private fun loadSavedKeyboard(): Keyboard? {
        val savedKeyboardName = loadPreferenceString(
            applicationContext,
            PREFERENCES_FILE_NAME,
            KEYBOARD_NAME_PREFERENCE_KEY
        )
        val keyboardSet = initKeyboardSet()
        keyboardSet.forEach { k ->
            if (k.name == savedKeyboardName) return k
        }
        return fullKB
    }

    private fun initKeyboardSet() : Array<Keyboard> {
        var keyboardSet = arrayOf<Keyboard>()
        keyboardSet += fullKB!!
        keyboardSet += fsimeKB!!
        keyboardSet += pureKB!!
        keyboardSet += digitKB!!

        // "ck_use_cj", "ck_use_ji", "ck_use_stroke", "ck_use_mil"
        if (sharedPreferences!!.getUseKb("ck_use_cj"))
            keyboardSet += cjKB!!
        if (sharedPreferences!!.getUseKb("ck_use_ji"))
            keyboardSet += jiKB!!
        if (sharedPreferences!!.getUseKb("ck_use_stroke"))
            keyboardSet += strokeKB!!
        if (sharedPreferences!!.getUseKb("ck_use_mil"))
            keyboardSet += milKB!!
        return keyboardSet.clone()
    }
    override fun onStartInput(editorInfo: EditorInfo, isRestarting: Boolean) {
        super.onStartInput(editorInfo, isRestarting)

        inputOptionsBits = editorInfo.imeOptions
        enterKeyHasAction = inputOptionsBits and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0
        val inputTypeBits = editorInfo.inputType
        val inputClassBits = inputTypeBits and InputType.TYPE_MASK_CLASS
        val inputVariationBits = inputTypeBits and InputType.TYPE_MASK_VARIATION
        inputIsPassword = when (inputClassBits) {
            InputType.TYPE_CLASS_NUMBER -> inputVariationBits == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            InputType.TYPE_CLASS_TEXT -> {
                when (inputVariationBits) {
                    InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> true
                    else -> false
                }
            }

            else -> false
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo, isRestarting: Boolean) {
        super.onStartInputView(editorInfo, isRestarting)
        updateFullscreenMode() // needed in API level 31+ so that fullscreen works after rotate whilst keyboard showing
        val isFullscreen = isFullscreenMode
        inputContainer!!.setBackground(isFullscreen)
        inputContainer!!.setCandidateList(candidateList)
        setEnterKeyDisplayText()
        setCandidateOrder()
    }

    private fun setEnterKeyDisplayText() {
        var enterKeyDisplayText = when (inputOptionsBits and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_DONE -> getString(R.string.display_text__done)
            EditorInfo.IME_ACTION_GO -> getString(R.string.display_text__go)
            EditorInfo.IME_ACTION_NEXT -> getString(R.string.display_text__next)
            EditorInfo.IME_ACTION_PREVIOUS -> getString(R.string.display_text__previous)
            EditorInfo.IME_ACTION_SEARCH -> getString(R.string.display_text__search)
            EditorInfo.IME_ACTION_SEND -> getString(R.string.display_text__send)
            else -> null
        }
        if (!enterKeyHasAction || enterKeyDisplayText == null) {
            enterKeyDisplayText = getString(R.string.display_text__return)
        }
        val keyboardSet = initKeyboardSet()
        for (keyboard in keyboardSet) {
            for (key in keyboard.getKeyList()) {
                if (key.valueText == ENTER_KEY_VALUE_TEXT) {
                    key.displayText = enterKeyDisplayText
                }
            }
        }
        inputContainer!!.redrawKeyboard()
    }

    override fun onComputeInsets(insets: Insets) {
        super.onComputeInsets(insets)
        if (inputContainer != null) // check needed in API level 30
        {
            val candidatesViewTop = inputContainer!!.candidatesViewTop
            insets.visibleTopInsets = candidatesViewTop
            insets.contentTopInsets = candidatesViewTop
        }
    }

    override fun onCandidate(candidate: String?) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(candidate, 1)
        mComposing = ""
        updateRelative(candidate!!)
    }

    private fun keyDownUp(keyEventCode: Int, meta: Int) {
        val ic = currentInputConnection
        ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEventCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEventCode, 0, meta))
    }

    private fun keyDownUps(keyEventCodes: IntArray, meta: Int) {
        val ic = currentInputConnection
        for (i in keyEventCodes.indices) {
            ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEventCodes[i], 0, meta))
        }
        for (i in keyEventCodes.indices.reversed()) {
            ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEventCodes[i], 0, meta))
        }
    }

    private fun turnCandidateOff() {
        mComposing = ""
        inputContainer!!.setCandidateList(ArrayList())
    }

    private fun showMilMessage(inputText: String) {
        val btnMsg = mil!!.getBtnMsg(inputText)
        if (btnMsg.length > 0) {
            val list: List<String> = listOf(btnMsg)
            setCandidateList(list)
        }
    }

    fun onKeyMil(valueText: String) {
        val inputConnection = currentInputConnection ?: return
        if (valueText.matches("[0123456789\\+\\-\\*/°'\"()\\.]".toRegex())) {
            effectStrokeAppendMil(valueText)
            return
        }
        when (valueText) {
            BACKSPACE_VALUE_TEXT -> effectBackspace(inputConnection)
            TAB_KEY_VALUE_TEXT, TAB_SHIFT_KEY_VALUE_TEXT -> {}
            ESC_KEY_VALUE_TEXT -> turnCandidateOff()
            SPACE_BAR_VALUE_TEXT -> effectSpaceKey(inputConnection)
            ENTER_KEY_VALUE_TEXT -> effectEnterKey(inputConnection)
            ANGLE_KEY_VALUE_TEXT -> inputContainer!!.keyboard!!.setShiftText(
                ANGLE_KEY_VALUE_TEXT,
                mil!!.nextMode()
            )

            else -> mil!!.setApp(valueText)
        }
    }

    fun onCtrlKey(valueText: String?) {}
    override fun onKey(valueText: String) {
        if (inputContainer!!.keyboard!!.name == KEYBOARD_NAME_MIL) {
            onKeyMil(valueText)
            return
        }
        val inputConnection = currentInputConnection ?: return
        when (valueText) {
            "⎆" -> keyDownUps(
                intArrayOf(
                    KeyEvent.KEYCODE_CTRL_LEFT,
                    KeyEvent.KEYCODE_ALT_LEFT,
                    KeyEvent.KEYCODE_DEL
                ), 0
            )

            "⇱" -> keyDownUp(KeyEvent.KEYCODE_MOVE_HOME, 0)
            "⇲" -> keyDownUp(KeyEvent.KEYCODE_MOVE_END, 0)
            "⇞" -> keyDownUp(KeyEvent.KEYCODE_PAGE_UP, 0)
            "⇟" -> keyDownUp(KeyEvent.KEYCODE_PAGE_DOWN, 0)
            "←" -> keyDownUp(KeyEvent.KEYCODE_DPAD_LEFT, 0)
            "↑" -> keyDownUp(KeyEvent.KEYCODE_DPAD_UP, 0)
            "↓" -> keyDownUp(KeyEvent.KEYCODE_DPAD_DOWN, 0)
            "→" -> keyDownUp(KeyEvent.KEYCODE_DPAD_RIGHT, 0)
            BACKSPACE_VALUE_TEXT -> effectBackspace(inputConnection)
            TAB_KEY_VALUE_TEXT, TAB_SHIFT_KEY_VALUE_TEXT -> keyDownUp(
                KeyEvent.KEYCODE_TAB,
                inputContainer!!.keyboard!!.shiftMode
            )

            ESC_KEY_VALUE_TEXT -> turnCandidateOff()
            SPACE_BAR_VALUE_TEXT -> effectSpaceKey(inputConnection)
            ENTER_KEY_VALUE_TEXT -> effectEnterKey(inputConnection)
            else -> {
                if (inputContainer!!.keyboard!!.ctrlMode != 0) {
                    val mKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
                    val events = mKeyCharacterMap.getEvents(valueText.toCharArray())
                    for (event2 in events) {
                        // 其實只做一次
                        if (event2.action == 0 && event2.keyCode != KeyEvent.KEYCODE_SHIFT_LEFT) {
                            val keycode = event2.keyCode
                            if (codeMaps.containsKey(keycode)) {
                                val hk = sharedPreferences!!.getHotkey(codeMaps.get(keycode)!!)
                                if (hk.length > 0) {
                                    mComposing = ""
                                    effectStrokeAppend(hk)
                                    break
                                } else {
                                    if (inputContainer!!.keyboard!!.shiftMode != 0) {
                                        keyDownUp(
                                            keycode,
                                            KeyEvent.META_CTRL_ON + KeyEvent.META_SHIFT_ON
                                        )
                                        break
                                    }
                                }
                            }
                            keyDownUp(keycode, KeyEvent.META_CTRL_ON)
                            break
                        }
                    }
                } else {
                    effectStrokeAppend(valueText)
                }
            }
        }
    }

    override fun onLongPress(inputText: String) {
        if (inputContainer!!.keyboard!!.name == KEYBOARD_NAME_MIL) {
            showMilMessage(inputText)
            return
        }
        val valueText: String
        val shiftText: String
        if (inputText.length == 2) {
            valueText = inputText.substring(0, 1)
            shiftText = inputText.substring(1, 2)
        } else {
            valueText = inputText
            shiftText = ""
        }
        when (valueText) {
            SPACE_BAR_VALUE_TEXT -> showSystemKeyboardChanger(this)
            ESC_KEY_VALUE_TEXT -> {
                val inputConnection = currentInputConnection
                val w = getTextBeforeCursor(inputConnection, 1)
                if (w.length > 0) {
                    val comp: ArrayList<String> = bdatabase!!.getCompose(w.substring(0, 1))
                    comp.add(0, w)
                    setCandidateList(comp)
                }
            }

            else -> {
                if (shiftText.length > 0) {
                    effectStrokeAppend(shiftText)
                } else {
                    effectStrokeAppend(valueText)
                }
            }
        }
    }

    override fun onSwipe(valueText: String) {
        if (valueText == SPACE_BAR_VALUE_TEXT) {
            val keyboardSet = initKeyboardSet()
            if (keyboardSet.isEmpty()) {
                return
            }
            var keyboard = inputContainer!!.keyboard
            if (keyboard!!.name == null) {
                return
            }
            if (keyboard.swipeDir and (SWIPE_RU or SWIPE_RD) > 0) { // right full > fsime > pure > digit > ji > cj > stroke > mil
                var next = (keyboardSet.indexOf(keyboard)+1) % keyboardSet.size
                inputContainer!!.keyboard  = keyboardSet.get(next)
            } else if (keyboard.swipeDir and (SWIPE_LD or SWIPE_LU) > 0) { // left  mil > stroke > cj > ji > digit > pure > fsime > full
                var next = (keyboardSet.indexOf(keyboard)+keyboardSet.size-1) % keyboardSet.size
                inputContainer!!.keyboard  = keyboardSet.get(next)
            }
            inputContainer!!.redrawKeyboard()
            return
        } else {
            effectStrokeAppend(valueText)
        }
    }

    private fun computeCandidateList(mComposing: String): List<String> {
        return if (inputContainer!!.keyboard!!.name == KEYBOARD_NAME_MIL || mComposing.length == 0) {
            emptyList<String>()
        } else bdatabase!!.getWord(
            mComposing,
            0,
            30,
            inputContainer!!.keyboard!!.name!!
        )
    }

    private fun effectStrokeAppendMil(key: String) {
        val exp = mComposing + key
        val parser = MathParser.create()
        val list: MutableList<String> = ArrayList()
        list.add(exp)
        val exps = exp.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        try {
            for (i in 0 until exps.size - 1) {
                parser.addExpression(exps[i])
            }
            val res = parser.parse(exps[exps.size - 1])
            list.add(java.lang.Double.toString(res))
        } catch (e: Exception) {
        }
        mComposing = exp
        setCandidateList(list)
    }

    private fun effectStrokeAppend(key: String) {
        val newInputSequence = mComposing + key
        var list = computeCandidateList(newInputSequence)
        if (list.size == 1) {
            val parser = MathParser.create()
            try {
                val exp = list[0]
                val exps = exp!!.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in 0 until exps.size - 1) {
                    parser.addExpression(exps[i])
                }
                val res = parser.parse(exps[exps.size - 1])
                list += java.lang.Double.toString(res)
                // list.add(java.lang.Double.toString(res))
            } catch (e: Exception) {
            }
        }
        mComposing = newInputSequence
        setCandidateList(list)
    }

    private fun effectBackspace(inputConnection: InputConnection) {
        if (mComposing.length > 0) {
            mComposing = removeSuffixRegex("(?s).", mComposing)
            val newCandidateList = computeCandidateList(mComposing)
            setCandidateList(newCandidateList)
            inputContainer!!.setKeyRepeatIntervalMilliseconds(
                BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8
            )
        } else {
            val upToOneCharacterBeforeCursor = getTextBeforeCursor(inputConnection, 1)
            if (upToOneCharacterBeforeCursor.length > 0) {
                val selection = inputConnection.getSelectedText(0)
                if (TextUtils.isEmpty(selection)) {
                    inputConnection.deleteSurroundingTextInCodePoints(1, 0)
                } else {
                    inputConnection.commitText("", 1)
                }
            } else if (inputContainer!!.keyboard!!.shiftMode != 0) {
                keyDownUp(KeyEvent.KEYCODE_DEL, KeyEvent.META_SHIFT_ON)
            } else {
                keyDownUp(KeyEvent.KEYCODE_DEL, 0)
            }
            val nextBackspaceIntervalMilliseconds =
                if (isAscii(upToOneCharacterBeforeCursor)) BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_ASCII else BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8
            inputContainer!!.setKeyRepeatIntervalMilliseconds(nextBackspaceIntervalMilliseconds)
        }
    }

    private fun updateRelative(sel: String) {
        val list = bdatabase!!.getVocabulary(
            sel,
            0,
            30
        )
        setCandidateList(list)
    }
    private fun effectSpaceKey(inputConnection: InputConnection) {
        if (mComposing.length > 0) {
            var sel = ""
            if (candidateList.size > 1) {
                sel = getCandidate(1)
            } else if (candidateList.size > 0) {
                sel = getCandidate(0)
            }
            onCandidate(sel)
        } else {
            inputConnection.commitText(" ", 1)
        }
    }

    private fun effectEnterKey(inputConnection: InputConnection) {
        if (enterKeyHasAction) {
            inputConnection.performEditorAction(inputOptionsBits)
        } else if (mComposing.length > 0) {
            onCandidate(getCandidate(0))
        } else {
            inputConnection.commitText("\n", 1)
        }
        setCandidateList(emptyList<String>())
    }

    override fun saveKeyboard(keyboard: Keyboard) {
        savePreferenceString(
            applicationContext,
            PREFERENCES_FILE_NAME,
            KEYBOARD_NAME_PREFERENCE_KEY,
            keyboard.name!!
        )
    }

    private fun setCandidateList(candidateList: List<String>) {
        this.candidateList = candidateList
        inputContainer!!.setCandidateList(candidateList)
    }

    private fun getCandidate(idx: Int): String {
        return try {
            if (candidateList.size > idx) {
                candidateList[idx]
            } else {
                ""
            }
        } catch (exception: IndexOutOfBoundsException) {
            ""
        }
    }

    private fun getTextBeforeCursor(inputConnection: InputConnection, characterCount: Int): String {
        if (inputIsPassword) {
            return "" // don't read passwords
        }
        val textBeforeCursor = inputConnection.getTextBeforeCursor(characterCount, 0) as String?
        return textBeforeCursor ?: ""
    }

    companion object {
        private const val LOG_TAG = "FsimeService"
        const val ANGLE_KEY_VALUE_TEXT = "∠"
        const val ENTER_KEY_VALUE_TEXT = "ENTER"
        const val SHIFT_KEY_VALUE_TEXT = "SHIFT"
        const val CTRL_KEY_VALUE_TEXT = "CTRL"
        private const val TAB_KEY_VALUE_TEXT = "TAB"
        private const val TAB_SHIFT_KEY_VALUE_TEXT = "↹"
        private const val ESC_KEY_VALUE_TEXT = "ESC"
        private const val BACKSPACE_VALUE_TEXT = "BACKSPACE"
        private const val SPACE_BAR_VALUE_TEXT = "SPACE"
        private const val KEYBOARD_NAME_FULL = "full"
        private const val KEYBOARD_NAME_FSIME = "mix"
        private const val KEYBOARD_NAME_PURE = "pure"
        private const val KEYBOARD_NAME_DIGIT = "digit"
        private const val KEYBOARD_NAME_CJ = "cj"
        private const val KEYBOARD_NAME_JI = "ji"
        private const val KEYBOARD_NAME_STROKE = "stroke"
        private const val KEYBOARD_NAME_MIL = "mil"
        private const val BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_ASCII = 50
        private const val BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8 = 100
        const val PREFERENCES_FILE_NAME = "preferences.txt"
        private const val KEYBOARD_NAME_PREFERENCE_KEY = "keyboardName"
        private const val LAG_PREVENTION_CODE_POINT_COUNT = 1400
        private const val LARGISH_SORTING_RANK = 3000
        private fun isCommentLine(line: String): Boolean {
            return line.startsWith("#") || line.length == 0
        }
    }
}
