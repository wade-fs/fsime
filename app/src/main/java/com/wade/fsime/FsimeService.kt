/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.fsime

import android.annotation.SuppressLint
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import com.wade.MathParser.MathParser
import com.wade.fsime.CandidatesViewAdapter.CandidateListener
import com.wade.fsime.KeyboardView.KeyboardListener
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*
import com.wade.libs.BDatabase
import com.wade.utilities.Contexty.loadPreferenceString
import com.wade.utilities.Contexty.savePreferenceString
import com.wade.utilities.Contexty.showSystemKeyboardChanger
import com.wade.utilities.Stringy.isAscii
import com.wade.utilities.Stringy.removeSuffixRegex
import java.util.*

/*
  An InputMethodService for the FS Input Method (混瞎輸入法).
*/
class FsimeService : InputMethodService(), CandidateListener, KeyboardListener, HandwritingView.HandwritingListener {
    var fullKB: Keyboard? = null
    var fsimeKB: Keyboard? = null
    var pureKB: Keyboard? = null
    var digitKB: Keyboard? = null
    var jiKB: Keyboard? = null
    var cjKB: Keyboard? = null
    var strokeKB: Keyboard? = null
    private var inputContainer: InputContainer? = null
    private var mComposing = ""
    private var candidateList: List<String> = ArrayList()
    private var inputOptionsBits = 0
    private var enterKeyHasAction = false
    private var inputIsPassword = false
    var bdatabase: BDatabase? = null
    var sharedPreferences: KeyboardPreferences? = null
    var codeMaps: MutableMap<Int, String> = HashMap()
    private var speechRecognizer: SpeechRecognizer? = null
    
    private var recognizer: DigitalInkRecognizer? = null
    private var model: DigitalInkRecognitionModel? = null

    val SWIPE_NONE = 0
    val SWIPE_RU = 1
    val SWIPE_LD = 2
    val SWIPE_LU = 4
    val SWIPE_RD = 8
    private var usePhrase = false

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = KeyboardPreferences(this)
        initSpeechRecognizer()
        initHandwritingRecognizer()

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
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(LOG_TAG, "Speech recognition not available")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(applicationContext, "🎤 請開始說話...", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "錄音失敗"
                    SpeechRecognizer.ERROR_CLIENT -> "客戶端錯誤"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "權限不足，請開啟錄音權限"
                    SpeechRecognizer.ERROR_NETWORK -> "網路連接錯誤"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "網路逾時"
                    SpeechRecognizer.ERROR_NO_MATCH -> "聽不清楚，請再試一次"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "語音服務忙碌中"
                    SpeechRecognizer.ERROR_SERVER -> "伺服器錯誤"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "無語音輸入"
                    else -> "語音辨識錯誤: $error"
                }
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    currentInputConnection?.commitText(matches[0], 1)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startVoiceInput() {
        if (speechRecognizer == null) {
            Toast.makeText(applicationContext, "此裝置不支援語音辨識", Toast.LENGTH_LONG).show()
            initSpeechRecognizer() // 嘗試再次初始化
            return
        }
        
        Toast.makeText(applicationContext, "語音啟動中...", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "請說話...")
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "無法啟動語音：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initHandwritingRecognizer() {
        val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zh-Hant")
        if (modelIdentifier == null) {
            Log.e(LOG_TAG, "Model identifier not found for zh-Hant")
            return
        }
        model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        val remoteModelManager = RemoteModelManager.getInstance()
        
        remoteModelManager.isModelDownloaded(model!!)
            .addOnSuccessListener { isDownloaded ->
                if (isDownloaded) {
                    recognizer = DigitalInkRecognition.getClient(
                        DigitalInkRecognizerOptions.builder(model!!).build()
                    )
                } else {
                    Toast.makeText(this, "正在下載手寫辨識模型...", Toast.LENGTH_SHORT).show()
                    remoteModelManager.download(model!!, DownloadConditions.Builder().build())
                        .addOnSuccessListener {
                            recognizer = DigitalInkRecognition.getClient(
                                DigitalInkRecognizerOptions.builder(model!!).build()
                            )
                            Toast.makeText(this, "手寫辨識模型下載完成", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e(LOG_TAG, "Error downloading model", e)
                            Toast.makeText(this, "手寫辨識模型下載失敗", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    override fun onInkFinished(ink: Ink) {
        if (recognizer == null) {
            Toast.makeText(this, "手寫辨識尚未就緒", Toast.LENGTH_SHORT).show()
            return
        }

        recognizer?.recognize(ink)
            ?.addOnSuccessListener { result ->
                val candidates = result.candidates.map { it.text }
                if (candidates.isNotEmpty()) {
                    setCandidateList(candidates)
                    inputContainer?.clearHandwriting()
                }
            }
            ?.addOnFailureListener { e ->
                Log.e(LOG_TAG, "Error during recognition", e)
            }
    }

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        bdatabase = BDatabase(applicationContext)
        inputContainer = layoutInflater.inflate(R.layout.input_container, null) as InputContainer
        inputContainer!!.initialiseCandidatesView(this)
        inputContainer!!.initialiseKeyboardView(this, loadSavedKeyboard())
        inputContainer!!.setHandwritingListener(this)
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

    private fun initKeyboardSet(): Array<Keyboard> {
        var keyboardSet = arrayOf<Keyboard>()
        keyboardSet += fullKB!!
        keyboardSet += fsimeKB!!
        keyboardSet += pureKB!!
        keyboardSet += digitKB!!

        // "ck_use_cj", "ck_use_ji", "ck_use_stroke"
        if (sharedPreferences!!.getUseKb("ck_use_cj"))
            keyboardSet += cjKB!!
        if (sharedPreferences!!.getUseKb("ck_use_ji"))
            keyboardSet += jiKB!!
        if (sharedPreferences!!.getUseKb("ck_use_stroke"))
            keyboardSet += strokeKB!!
        if (sharedPreferences!!.getUseKb("ck_phrase"))
            usePhrase = true
        else usePhrase = false
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

    override fun onKey(key: Key) {
        val inputConnection = currentInputConnection ?: return
        val valueText = if (inputContainer!!.keyboard!!.shiftState != Keyboard.ModifierState.DISABLED && key.isShiftable && !key.isControlKey)
            key.shiftText ?: ""
        else
            key.valueText ?: ""

        Log.i(LOG_TAG, "onKey: $valueText")

        if (inputContainer!!.keyboard!!.ctrlMode != 0) {
            val keyCode = if (key.keyCode != 0) key.keyCode else {
                // Fallback for keys without keyCode defined in XML
                val mKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
                val events = mKeyCharacterMap.getEvents(key.valueText?.toCharArray())
                events?.firstOrNull { it.action == KeyEvent.ACTION_DOWN && it.keyCode != KeyEvent.KEYCODE_SHIFT_LEFT }?.keyCode ?: 0
            }

            if (keyCode != 0) {
                if (codeMaps.containsKey(keyCode)) {
                    val hk = sharedPreferences!!.getHotkey(codeMaps[keyCode]!!)
                    if (hk.isNotEmpty()) {
                        mComposing = ""
                        effectStrokeAppend(hk)
                        return
                    }
                }
                keyDownUp(keyCode, inputContainer!!.keyboard!!.metaState)
                return
            }
        }

        when (valueText) {
            "VOICE" -> startVoiceInput()
            "HANDWRITING" -> inputContainer?.showHandwriting(true)
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
            BACKSPACE_VALUE_TEXT, "⌫" -> effectBackspace(inputConnection)
            TAB_KEY_VALUE_TEXT, TAB_SHIFT_KEY_VALUE_TEXT -> keyDownUp(
                KeyEvent.KEYCODE_TAB,
                inputContainer!!.keyboard!!.shiftMode
            )

            ESC_KEY_VALUE_TEXT -> turnCandidateOff()
            SPACE_BAR_VALUE_TEXT -> effectSpaceKey(inputConnection)
            ENTER_KEY_VALUE_TEXT -> effectEnterKey(inputConnection)
            else -> effectStrokeAppend(valueText)
        }
    }

    override fun onLongPress(key: Key) {
        val valueText = key.valueText ?: ""
        val shiftText = key.shiftText ?: ""
        when (valueText) {
            SPACE_BAR_VALUE_TEXT -> showSystemKeyboardChanger(this)
            ESC_KEY_VALUE_TEXT -> {
                inputContainer?.showHandwriting(true)
            }
            "VOICE" -> {
                inputContainer?.showHandwriting(true)
            }
            else -> {
                if (shiftText.isNotEmpty()) {
                    effectStrokeAppend(shiftText)
                } else {
                    effectStrokeAppend(valueText)
                }
            }
        }
    }

    override fun onSwipe(key: Key, swipeDir: Int) {
        if (key.valueText == SPACE_BAR_VALUE_TEXT) {
            val keyboardSet = initKeyboardSet()
            if (keyboardSet.isEmpty()) {
                return
            }
            val keyboard = inputContainer!!.keyboard
            if (keyboard!!.name == null) {
                return
            }
            if (swipeDir and (SWIPE_RU or SWIPE_RD) > 0) {
                val next = (keyboardSet.indexOf(keyboard) + 1) % keyboardSet.size
                inputContainer!!.keyboard = keyboardSet[next]
            } else if (swipeDir and (SWIPE_LD or SWIPE_LU) > 0) {
                val next = (keyboardSet.indexOf(keyboard) + keyboardSet.size - 1) % keyboardSet.size
                inputContainer!!.keyboard = keyboardSet[next]
            }
            inputContainer!!.redrawKeyboard()
            return
        } else {
            val swipeText = when (swipeDir) {
                SWIPE_LU -> key.upText
                SWIPE_RD -> key.downText
                SWIPE_LD -> key.leftText
                SWIPE_RU -> key.rightText
                else -> key.valueText
            }
            effectStrokeAppend(swipeText ?: "")
        }
    }

    override fun onGlobalSwipe(direction: Int) {
        val inputConnection = currentInputConnection ?: return
        when (direction) {
            SWIPE_LD -> { // Left swipe
                effectBackspace(inputConnection)
            }

            SWIPE_RU -> { // Right swipe
                inputConnection.commitText(" ", 1)
            }
        }
    }

    private fun computeCandidateList(mComposing: String): List<String> {
        return if (mComposing.isEmpty()) {
            emptyList()
        } else bdatabase!!.getWord(
            mComposing,
            0,
            30,
            inputContainer!!.keyboard!!.name!!
        )
    }

    private fun effectStrokeAppend(key: String) {
        val newInputSequence = mComposing + key
        var list = computeCandidateList(newInputSequence)
        if (list.size == 1) {
            val parser = MathParser.create()
            try {
                val exp = list[0]
                val exps = exp.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in 0 until exps.size - 1) {
                    parser.addExpression(exps[i])
                }
                val res = parser.parse(exps[exps.size - 1])
                list = list.toMutableList().apply { add(res.toString()) }
            } catch (e: Exception) {
            }
        }
        mComposing = newInputSequence
        setCandidateList(list)
    }

    private fun effectBackspace(inputConnection: InputConnection) {
        if (mComposing.isNotEmpty()) {
            mComposing = removeSuffixRegex("(?s).", mComposing)
            val newCandidateList = computeCandidateList(mComposing)
            setCandidateList(newCandidateList)
            inputContainer!!.setKeyRepeatIntervalMilliseconds(
                BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8
            )
        } else {
            val upToOneCharacterBeforeCursor = getTextBeforeCursor(inputConnection, 1)
            if (upToOneCharacterBeforeCursor.isNotEmpty()) {
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
        val tb = if (usePhrase) "phrase" else "vocabulary"
        val list = bdatabase!!.getPhrase(
            tb,
            sel,
            0,
            30
        )
        setCandidateList(list)
    }

    private fun effectSpaceKey(inputConnection: InputConnection) {
        if (mComposing.isNotEmpty()) {
            val sel = if (candidateList.size > 1) getCandidate(1) else getCandidate(0)
            onCandidate(sel)
        } else {
            inputConnection.commitText(" ", 1)
        }
    }

    private fun effectEnterKey(inputConnection: InputConnection) {
        if (mComposing.isNotEmpty()) {
            onCandidate(getCandidate(0))
        } else if (enterKeyHasAction) {
            inputConnection.performEditorAction(inputOptionsBits)
        } else {
            inputConnection.commitText("\n", 1)
        }
        setCandidateList(emptyList())
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
            if (candidateList.size > idx) candidateList[idx] else ""
        } catch (exception: IndexOutOfBoundsException) {
            ""
        }
    }

    private fun getTextBeforeCursor(inputConnection: InputConnection, characterCount: Int): String {
        if (inputIsPassword) return ""
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
        const val PREFERENCES_FILE_NAME = "preferences.txt"
        private const val KEYBOARD_NAME_PREFERENCE_KEY = "keyboardName"
        private const val BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_ASCII = 50
        private const val BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8 = 100
    }
}
