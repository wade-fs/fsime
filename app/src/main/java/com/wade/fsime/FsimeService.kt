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
    private var nameFromKeyboard: MutableMap<Keyboard, String?>? = null
    private var keyboardFromName: Map<String?, Keyboard>? = null
    private var keyboardSet: Set<Keyboard>? = null
    private var inputContainer: InputContainer? = null
    private var mComposing = ""
    private var candidateList: List<String?> = ArrayList()
    private val phraseCompletionFirstCodePointList: List<Int> = ArrayList()
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
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        bdatabase = BDatabase(applicationContext)
        fullKB = Keyboard(this, R.xml.keyboard_full, KEYBOARD_NAME_FULL)
        fsimeKB = Keyboard(this, R.xml.keyboard_fsime, KEYBOARD_NAME_FSIME)
        pureKB = Keyboard(this, R.xml.keyboard_pure, KEYBOARD_NAME_PURE)
        digitKB = Keyboard(this, R.xml.keyboard_digit, KEYBOARD_NAME_DIGIT)
        jiKB = Keyboard(this, R.xml.keyboard_ji, KEYBOARD_NAME_JI)
        cjKB = Keyboard(this, R.xml.keyboard_cj, KEYBOARD_NAME_CJ)
        strokeKB = Keyboard(this, R.xml.keyboard_stroke, KEYBOARD_NAME_STROKE)
        milKB = Keyboard(this, R.xml.keyboard_mil, KEYBOARD_NAME_MIL)
        nameFromKeyboard = HashMap()
        nameFromKeyboard[fullKB!!] = KEYBOARD_NAME_FULL
        nameFromKeyboard[fsimeKB!!] = KEYBOARD_NAME_FSIME
        nameFromKeyboard[pureKB!!] = KEYBOARD_NAME_PURE
        nameFromKeyboard[digitKB!!] = KEYBOARD_NAME_DIGIT
        nameFromKeyboard[jiKB!!] = KEYBOARD_NAME_JI
        nameFromKeyboard[cjKB!!] = KEYBOARD_NAME_CJ
        nameFromKeyboard[strokeKB!!] = KEYBOARD_NAME_STROKE
        nameFromKeyboard[milKB!!] = KEYBOARD_NAME_MIL
        keyboardFromName = invertMap(nameFromKeyboard)
        keyboardSet = nameFromKeyboard.keys
        inputContainer = layoutInflater.inflate(R.layout.input_container, null) as InputContainer
        inputContainer!!.initialiseCandidatesView(this)
        inputContainer!!.initialiseKeyboardView(this, loadSavedKeyboard())
        sharedPreferences = KeyboardPreferences(this)
        return inputContainer!!
    }

    private fun loadSavedKeyboard(): Keyboard? {
        val savedKeyboardName = loadPreferenceString(
            applicationContext,
            PREFERENCES_FILE_NAME,
            KEYBOARD_NAME_PREFERENCE_KEY
        )
        val savedKeyboard = keyboardFromName!![savedKeyboardName]
        return savedKeyboard ?: fullKB
    }

    private fun loadCharactersIntoCodePointSet(
        charactersFileName: String,
        codePointSet: MutableSet<Int>
    ) {
        val startMilliseconds = System.currentTimeMillis()
        try {
            val inputStream = assets.open(charactersFileName)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var line: String
            while (bufferedReader.readLine().also { line = it } != null) {
                if (!isCommentLine(line)) {
                    codePointSet.add(getFirstCodePoint(line))
                }
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
        val endMilliseconds = System.currentTimeMillis()
        sendLoadingTimeLog(charactersFileName, startMilliseconds, endMilliseconds)
    }

    private fun loadRankingData(
        rankingFileName: String,
        sortingRankFromCodePoint: MutableMap<Int, Int>,
        commonCodePointSet: MutableSet<Int>
    ) {
        val startMilliseconds = System.currentTimeMillis()
        try {
            val inputStream = assets.open(rankingFileName)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var currentRank = 0
            var line: String
            while (bufferedReader.readLine().also { line = it } != null) {
                if (!isCommentLine(line)) {
                    for (codePoint in toCodePointList(line)) {
                        currentRank++
                        if (!sortingRankFromCodePoint.containsKey(codePoint)) {
                            sortingRankFromCodePoint[codePoint] = currentRank
                        }
                        if (currentRank < LAG_PREVENTION_CODE_POINT_COUNT) {
                            commonCodePointSet.add(codePoint)
                        }
                    }
                }
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
        val endMilliseconds = System.currentTimeMillis()
        sendLoadingTimeLog(rankingFileName, startMilliseconds, endMilliseconds)
    }

    private fun loadPhrasesIntoSet(phrasesFileName: String, phraseSet: MutableSet<String>) {
        val startMilliseconds = System.currentTimeMillis()
        try {
            val inputStream = assets.open(phrasesFileName)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var line: String
            while (bufferedReader.readLine().also { line = it } != null) {
                if (!isCommentLine(line)) {
                    phraseSet.add(line)
                }
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
        val endMilliseconds = System.currentTimeMillis()
        sendLoadingTimeLog(phrasesFileName, startMilliseconds, endMilliseconds)
    }

    private fun sendLoadingTimeLog(
        fileName: String,
        startMilliseconds: Long,
        endMilliseconds: Long
    ) {
        if (BuildConfig.DEBUG) {
            val durationMilliseconds = endMilliseconds - startMilliseconds
            Log.d(LOG_TAG, String.format("Loaded %s in %d ms", fileName, durationMilliseconds))
        }
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
        for (keyboard in keyboardSet!!) {
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
            val list: List<String?> = listOf(btnMsg)
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
                        if (event2.action == 0) {
                            val keycode = event2.keyCode
                            val hk = sharedPreferences!!.getHotkey(codeMaps[keycode]!!)
                            if (hk.length > 0) {
                                effectStrokeAppend(hk)
                            } else {
                                keyDownUp(keycode, KeyEvent.META_CTRL_ON)
                            }
                        }
                        break
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
                    val comp: ArrayList<String?> = bdatabase!!.getCompose(w.substring(0, 1))
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
        var keyboard = inputContainer!!.keyboard
        val keyboardName = keyboard!!.name
        if (valueText == SPACE_BAR_VALUE_TEXT) {
            if (keyboardName == null) {
                return
            }
            if (keyboard.swipeDir and (SWIPE_RU or SWIPE_RD) > 0) { // right full > fsime > pure > digit > ji > cj > stroke > mil
                keyboard = when (keyboardName) {
                    KEYBOARD_NAME_FULL -> keyboardFromName!![KEYBOARD_NAME_FSIME]
                    KEYBOARD_NAME_FSIME -> keyboardFromName!![KEYBOARD_NAME_PURE]
                    KEYBOARD_NAME_PURE -> keyboardFromName!![KEYBOARD_NAME_DIGIT]
                    KEYBOARD_NAME_DIGIT -> keyboardFromName!![KEYBOARD_NAME_JI]
                    KEYBOARD_NAME_JI -> keyboardFromName!![KEYBOARD_NAME_CJ]
                    KEYBOARD_NAME_CJ -> keyboardFromName!![KEYBOARD_NAME_STROKE]
                    KEYBOARD_NAME_STROKE -> keyboardFromName!![KEYBOARD_NAME_MIL]
                    else -> keyboardFromName!![KEYBOARD_NAME_FULL]
                }
            } else if (keyboard.swipeDir and (SWIPE_LD or SWIPE_LU) > 0) { // left  mil > stroke > cj > ji > digit > pure > fsime > full
                keyboard = when (keyboardName) {
                    KEYBOARD_NAME_MIL -> keyboardFromName!![KEYBOARD_NAME_STROKE]
                    KEYBOARD_NAME_STROKE -> keyboardFromName!![KEYBOARD_NAME_CJ]
                    KEYBOARD_NAME_CJ -> keyboardFromName!![KEYBOARD_NAME_JI]
                    KEYBOARD_NAME_JI -> keyboardFromName!![KEYBOARD_NAME_DIGIT]
                    KEYBOARD_NAME_DIGIT -> keyboardFromName!![KEYBOARD_NAME_PURE]
                    KEYBOARD_NAME_PURE -> keyboardFromName!![KEYBOARD_NAME_FSIME]
                    KEYBOARD_NAME_FSIME -> keyboardFromName!![KEYBOARD_NAME_FULL]
                    else -> keyboardFromName!![KEYBOARD_NAME_MIL]
                }
            }
            inputContainer!!.keyboard = keyboard
            inputContainer!!.redrawKeyboard()
            return
        } else {
            effectStrokeAppend(valueText)
        }
    }

    private fun computeCandidateList(mComposing: String): List<String?> {
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
        val list: MutableList<String?> = ArrayList()
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
        val list = computeCandidateList(newInputSequence)
        if (list.size == 1) {
            val parser = MathParser.create()
            try {
                val exp = list[0]
                val exps = exp!!.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in 0 until exps.size - 1) {
                    parser.addExpression(exps[i])
                }
                val res = parser.parse(exps[exps.size - 1])
                list.add(java.lang.Double.toString(res))
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
            } else { // for apps like Termux
                keyDownUp(KeyEvent.KEYCODE_DEL, inputContainer!!.keyboard!!.shiftMode)
            }
            val nextBackspaceIntervalMilliseconds =
                if (isAscii(upToOneCharacterBeforeCursor)) BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_ASCII else BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8
            inputContainer!!.setKeyRepeatIntervalMilliseconds(nextBackspaceIntervalMilliseconds)
        }
    }

    private fun effectSpaceKey(inputConnection: InputConnection) {
        if (mComposing.length > 0 && candidateList.size > 1) {
            onCandidate(getCandidate(1))
        } else if (candidateList.size > 0) {
            onCandidate(getCandidate(0))
        } else {
            inputConnection.commitText(" ", 1)
        }
    }

    private fun effectEnterKey(inputConnection: InputConnection) {
        if (mComposing.length > 0) {
            onCandidate(getCandidate(0))
        } else if (enterKeyHasAction) {
            inputConnection.performEditorAction(inputOptionsBits)
        } else {
            inputConnection.commitText("\n", 1)
        }
    }

    override fun saveKeyboard(keyboard: Keyboard) {
        val keyboardName = nameFromKeyboard!![keyboard]
        savePreferenceString(
            applicationContext,
            PREFERENCES_FILE_NAME,
            KEYBOARD_NAME_PREFERENCE_KEY,
            keyboardName
        )
    }

    private fun setCandidateList(candidateList: List<String?>) {
        this.candidateList = candidateList
        inputContainer!!.setCandidateList(candidateList)
    }

    private fun getCandidate(idx: Int): String? {
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