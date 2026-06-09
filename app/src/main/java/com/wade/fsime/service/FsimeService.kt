/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.fsime.service

import com.wade.fsime.R
import com.wade.fsime.engine.Keyboard
import com.wade.fsime.engine.Key
import com.wade.fsime.engine.KeyboardPreferences
import com.wade.fsime.ui.view.InputContainer
import com.wade.fsime.ui.view.HandwritingView
import com.wade.fsime.ui.view.KeyboardView
import com.wade.fsime.ui.adapter.CandidatesViewAdapter

import com.wade.fsime.engine.InputProcessor
import com.wade.fsime.engine.KeyboardState
import android.net.Uri
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import com.wade.fsime.activity.OCRActivity
import com.wade.fsime.activity.OCRResultHolder
import com.wade.fsime.activity.BarcodeActivity
import com.wade.fsime.activity.QRCodeActivity
import com.wade.fsime.activity.BarcodeResultHolder
import androidx.core.content.ContextCompat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.graphics.Color
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
import com.wade.fsime.math.MathParser
import com.wade.fsime.ui.adapter.CandidatesViewAdapter.CandidateListener
import com.wade.fsime.ui.view.KeyboardView.KeyboardListener
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.*
import com.wade.fsime.data.BDatabase
import com.wade.fsime.util.Contexty.loadPreferenceString
import com.wade.fsime.util.Contexty.savePreferenceString
import com.wade.fsime.util.Contexty.showSystemKeyboardChanger
import com.wade.fsime.util.Stringy.isAscii
import com.wade.fsime.util.Stringy.removeSuffixRegex
import java.util.*

/*
  An InputMethodService for the FS Input Method (混瞎輸入法).
*/
class FsimeService : InputMethodService(), CandidateListener, KeyboardListener, HandwritingView.HandwritingListener {
    var fullKB: Keyboard? = null
    var fsimeKB: Keyboard? = null
    var digitKB: Keyboard? = null
    var symbolKB: Keyboard? = null
    private var inputContainer: InputContainer? = null
    
    private lateinit var inputProcessor: InputProcessor
    
    private var inputOptionsBits = 0
    private var enterKeyHasAction = false
    private var inputIsPassword = false
    var bdatabase: BDatabase? = null
    var sharedPreferences: KeyboardPreferences? = null
    var codeMaps: MutableMap<Int, String> = HashMap()
    private var speechRecognizer: SpeechRecognizer? = null
    
    private var recognizer: DigitalInkRecognizer? = null
    private var model: DigitalInkRecognitionModel? = null
    private var pendingOcrResult: String? = null

    private val ocrResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == OCRActivity.ACTION_OCR_RESULT) {
                val text = intent.getStringExtra(OCRActivity.EXTRA_OCR_TEXT)
                if (!text.isNullOrEmpty()) {
                    pendingOcrResult = text
                    commitPendingOcrResult()
                }
            }
        }
    }

    private fun checkOCRResultHolder() {
        val ocrText = OCRResultHolder.pendingResult
        if (!ocrText.isNullOrEmpty()) {
            Log.i(LOG_TAG, "Found result in OCRResultHolder: '$ocrText'")
            pendingOcrResult = ocrText
            OCRResultHolder.pendingResult = null
            commitPendingOcrResult(sendEnter = false)
        }

        val barcodeText = BarcodeResultHolder.pendingResult
        if (!barcodeText.isNullOrEmpty()) {
            Log.i(LOG_TAG, "Found result in BarcodeResultHolder: '$barcodeText'")
            pendingOcrResult = barcodeText
            BarcodeResultHolder.pendingResult = null
            commitPendingOcrResult(sendEnter = true)
        }
    }

    private fun commitPendingOcrResult(sendEnter: Boolean = false) {
        val text = pendingOcrResult ?: return
        val ic = currentInputConnection
        if (ic != null) {
            Log.i(LOG_TAG, "Committing OCR text: '$text'")
            ic.commitText(text, 1)
            if (sendEnter) {
                Log.i(LOG_TAG, "Sending Enter action after barcode")
                effectEnterKey(ic)
            }
            pendingOcrResult = null
        } else {
            Log.w(LOG_TAG, "Cannot commit OCR text, InputConnection is null")
        }
    }

    val SWIPE_NONE = 0
    val SWIPE_RU = 1
    val SWIPE_LD = 2
    val SWIPE_LU = 4
    val SWIPE_RD = 8
    private var usePhrase = false

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        reinitializeKeyboards()
    }

    private fun reinitializeKeyboards() {
        fullKB = Keyboard(this, R.xml.keyboard_full, KEYBOARD_NAME_FULL)
        fsimeKB = Keyboard(this, R.xml.keyboard_fsime, KEYBOARD_NAME_FSIME)
        digitKB = Keyboard(this, R.xml.keyboard_digit, KEYBOARD_NAME_DIGIT)
        symbolKB = Keyboard(this, R.xml.keyboard_symbol, KEYBOARD_NAME_SYMBOL)
        
        inputContainer?.let {
            val savedKeyboardName = loadPreferenceString(
                applicationContext,
                PREFERENCES_FILE_NAME,
                KEYBOARD_NAME_PREFERENCE_KEY
            )
            val keyboardSet = arrayOf(fullKB!!, fsimeKB!!, digitKB!!, symbolKB!!)
            var targetKeyboard = fullKB
            for (k in keyboardSet) {
                if (k.name == savedKeyboardName) {
                    targetKeyboard = k
                    break
                }
            }
            it.keyboard = targetKeyboard
            inputProcessor.setKeyboard(targetKeyboard!!.name!!)
            it.redrawKeyboard()
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = KeyboardPreferences(this)
        bdatabase = BDatabase(applicationContext)
        inputProcessor = InputProcessor(this, bdatabase!!, sharedPreferences!!)
        
        inputProcessor.onStateChanged = { state ->
            inputContainer?.let {
                it.setCandidateList(state.candidates)
                it.showHandwriting(state.isHandwritingVisible)
                it.redrawKeyboard() // Redraw to update Space key shortest code
                // If you had a composing text view, you'd update it here.
            }
        }

        val filter = IntentFilter(OCRActivity.ACTION_OCR_RESULT)
        registerReceiver(ocrResultReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

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

        reinitializeKeyboards()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        unregisterReceiver(ocrResultReceiver)
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(applicationContext, "需開啟錄音權限才能使用語音輸入，請在設定中開啟", Toast.LENGTH_LONG).show()
            openAppSettings()
            return
        }

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

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun initHandwritingRecognizer() {
        val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zh-Hani-TW")
        if (modelIdentifier == null) {
            Log.e(LOG_TAG, "Model identifier not found for zh-Hani-TW")
            return
        }
        val builder = DigitalInkRecognitionModel.builder(modelIdentifier)
        model = builder.build()
        val currentModel = model
        if (currentModel == null) {
            Log.e(LOG_TAG, "Failed to build DigitalInkRecognitionModel")
            return
        }
        val remoteModelManager = RemoteModelManager.getInstance()
        
        remoteModelManager.isModelDownloaded(currentModel)
            .addOnSuccessListener { isDownloaded ->
                if (isDownloaded) {
                    val options = DigitalInkRecognizerOptions.builder(currentModel).build()
                    recognizer = DigitalInkRecognition.getClient(options)
                } else {
                    Toast.makeText(this, "手寫辨識模型下載中...", Toast.LENGTH_SHORT).show()
                    remoteModelManager.download(currentModel, DownloadConditions.Builder().build())
                        .addOnSuccessListener {
                            val options = DigitalInkRecognizerOptions.builder(currentModel).build()
                            recognizer = DigitalInkRecognition.getClient(options)
                            Toast.makeText(this, "手寫辨識就緒", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e(LOG_TAG, "Error downloading model", e)
                            Toast.makeText(this, "下載失敗：請確認網路連線", Toast.LENGTH_SHORT).show()
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
                    inputProcessor.setCandidates(candidates)
                    inputContainer?.clearHandwriting()
                }
            }
            ?.addOnFailureListener { e ->
                Log.e(LOG_TAG, "Error during recognition", e)
            }
    }

    @SuppressLint("InflateParams")
    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd,
            newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )

        // If selection is not empty (i.e., text is selected)
        if (newSelStart != newSelEnd) {
            val ic = currentInputConnection
            if (ic != null) {
                val selectedText = ic.getSelectedText(0)?.toString()
                if (!selectedText.isNullOrEmpty()) {
                    val db = bdatabase
                    if (db != null) {
                        // Only perform reverse lookup for the FIRST character of the selection
                        val firstChar = selectedText[0].toString()
                        val codes = db.reverseLookup(firstChar)
                        
                        if (codes.isNotEmpty()) {
                            inputProcessor.setCandidates(codes)
                        }
                    }
                }
            }
        } else if (inputProcessor.state.composingText.isEmpty()) {
            // Clear candidates only if we are not currently composing
            // inputProcessor.setCandidates(emptyList())
        }
    }

    override fun onCreateInputView(): View {
        bdatabase = BDatabase(applicationContext)
        inputContainer = layoutInflater.inflate(R.layout.input_container, null) as InputContainer
        inputContainer!!.initialiseCandidatesView(this)
        inputContainer!!.initialiseKeyboardView(this, loadSavedKeyboard())
        inputContainer!!.setHandwritingListener(this)

        // Fix for the white shading layer on the bottom row (navigation bar area)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window?.window?.let { win ->
                win.navigationBarColor = Color.BLACK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    win.navigationBarDividerColor = Color.TRANSPARENT
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    @Suppress("DEPRECATION")
                    win.decorView.systemUiVisibility = win.decorView.systemUiVisibility and
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
            }
        }

        return inputContainer!!
    }

    private fun setCandidateOrder() {
        val candidateOrder: String = sharedPreferences!!.candidateOrder()
        val tsMode = when (candidateOrder) {
            "TraditionalOnly" -> 1
            "SimplifiedOnly" -> 2
            else -> 0
        }
        bdatabase!!.setTs(tsMode)
        inputProcessor.setTs(tsMode)
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
        keyboardSet += digitKB!!
        keyboardSet += symbolKB!!

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
        Log.d(LOG_TAG, "onStartInputView, checking OCR holder...")
        updateFullscreenMode() // needed in API level 31+ so that fullscreen works after rotate whilst keyboard showing
        val isFullscreen = isFullscreenMode
        inputContainer!!.setBackground(isFullscreen)
        inputContainer!!.setCandidateList(inputProcessor.state.candidates)
        setEnterKeyDisplayText()
        setCandidateOrder()
        
        checkOCRResultHolder() // Check for results whenever keyboard appears
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
        val context = getTextBeforeCursor(inputConnection, 1)
        inputConnection.commitText(candidate, 1)
        
        if (candidate != null) {
            inputProcessor.recordSelection(context, candidate)
        }
        
        inputProcessor.commitText()
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
        inputProcessor.clearComposition()
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
                        inputProcessor.clearComposition()
                        inputProcessor.appendStroke(hk)
                        return
                    }
                }
                keyDownUp(keyCode, inputContainer!!.keyboard!!.metaState)
                return
            }
        }

        if (inputContainer!!.keyboard!!.name == KEYBOARD_NAME_SYMBOL && !key.isControlKey) {
            inputConnection.commitText(valueText, 1)
            return
        }

        when (valueText) {
            "VOICE" -> startVoiceInput()
            "HANDWRITING" -> inputProcessor.updateHandwritingVisibility(true)
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
            else -> inputProcessor.appendStroke(valueText)
        }
    }

    override fun onLongPress(key: Key) {
        val valueText = key.valueText ?: ""
        val shiftText = key.shiftText ?: ""
        when (valueText) {
            SPACE_BAR_VALUE_TEXT -> showSystemKeyboardChanger(this)
            ESC_KEY_VALUE_TEXT -> {
                inputProcessor.updateHandwritingVisibility(true)
            }
            SHIFT_KEY_VALUE_TEXT -> {
                startVoiceInput()
            }
            "⇱" -> {
                val intent = Intent(this, BarcodeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            "⇲" -> {
                val intent = Intent(this, QRCodeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            "VOICE" -> {
                val intent = Intent(this, OCRActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            else -> {
                if (shiftText.isNotEmpty() && !key.isControlKey) {
                    inputProcessor.appendStroke(shiftText)
                } else if (inputContainer!!.keyboard!!.name == KEYBOARD_NAME_DIGIT && shiftText.isNotEmpty()) {
                    inputProcessor.appendStroke(shiftText)
                } else if (inputContainer!!.keyboard!!.name == KEYBOARD_NAME_SYMBOL) {
                    currentInputConnection?.commitText(valueText, 1)
                } else {
                    inputProcessor.appendStroke(valueText)
                }
            }
        }
    }

    override fun onSwipe(key: Key, swipeDir: Int) {
        if (key.valueText == SPACE_BAR_VALUE_TEXT) {
            if (swipeDir and (SWIPE_RU or SWIPE_RD) > 0) {
                switchKeyboard(true)
            } else if (swipeDir and (SWIPE_LD or SWIPE_LU) > 0) {
                switchKeyboard(false)
            }
            return
        } else {
            var swipeText = when (swipeDir) {
                SWIPE_LU -> if (key.upText?.isNotEmpty() == true) key.upText else key.shiftText
                SWIPE_RD -> if (key.downText?.isNotEmpty() == true) key.downText else ""
                SWIPE_LD -> if (key.leftText?.isNotEmpty() == true) key.leftText else key.jiText
                SWIPE_RU -> if (key.rightText?.isNotEmpty() == true) key.rightText else key.strokeText
                else -> key.valueText
            }
            // Map stroke symbols to their database codes
            swipeText = when (swipeText) {
                "一" -> "z"
                "丨" -> "x"
                "丿" -> "c"
                "丶" -> "v"
                "乛" -> "b"
                else -> swipeText
            }
            if (inputContainer!!.keyboard!!.name == KEYBOARD_NAME_SYMBOL) {
                currentInputConnection?.commitText(swipeText, 1)
            } else {
                inputProcessor.appendStroke(swipeText ?: "")
            }
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

            SWIPE_LU -> { // Up swipe
                switchKeyboard(false)
            }

            SWIPE_RD -> { // Down swipe
                switchKeyboard(true)
            }
        }
    }

    private fun switchKeyboard(next: Boolean) {
        val keyboardSet = initKeyboardSet()
        if (keyboardSet.isEmpty()) {
            return
        }
        val keyboard = inputContainer!!.keyboard
        if (keyboard!!.name == null) {
            return
        }
        val currentIndex = keyboardSet.indexOf(keyboard)
        if (currentIndex == -1) return

        val nextIndex = if (next) {
            (currentIndex + 1) % keyboardSet.size
        } else {
            (currentIndex + keyboardSet.size - 1) % keyboardSet.size
        }
        val nextKeyboard = keyboardSet[nextIndex]
        inputContainer!!.keyboard = nextKeyboard
        inputProcessor.setKeyboard(nextKeyboard.name!!)
        inputContainer!!.redrawKeyboard()
        saveKeyboard(nextKeyboard)
    }

    private fun effectBackspace(inputConnection: InputConnection) {
        if (inputProcessor.state.composingText.isNotEmpty()) {
            inputProcessor.backspace()
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
        val inputConnection = currentInputConnection ?: return
        val context = getTextBeforeCursor(inputConnection, 2)
        val list = inputProcessor.getAssociatedPhrases(context)
        inputProcessor.setCandidates(list)
    }

    private fun effectSpaceKey(inputConnection: InputConnection) {
        if (inputProcessor.state.composingText.isNotEmpty()) {
            val sel = if (inputProcessor.state.candidates.size > 1) getCandidate(1) else getCandidate(0)
            onCandidate(sel)
        } else {
            inputConnection.commitText(" ", 1)
        }
    }

    private fun effectEnterKey(inputConnection: InputConnection) {
        if (inputProcessor.state.composingText.isNotEmpty()) {
            onCandidate(getCandidate(0))
        } else if (enterKeyHasAction) {
            inputConnection.performEditorAction(inputOptionsBits)
        } else {
            inputConnection.commitText("\n", 1)
        }
        inputProcessor.commitText()
    }

    override fun getShortestCode(): String? {
        val showShortestCode = sharedPreferences?.getUseKb("ck_show_shortest_code") ?: true
        if (!showShortestCode) return null
        return inputProcessor.state.shortestCode
    }

    override fun getHintFontSize(): String {
        return sharedPreferences?.read("hintFontSize", "Small") ?: "Small"
    }

    override fun saveKeyboard(keyboard: Keyboard) {
        savePreferenceString(
            applicationContext,
            PREFERENCES_FILE_NAME,
            KEYBOARD_NAME_PREFERENCE_KEY,
            keyboard.name!!
        )
    }

    private fun getCandidate(idx: Int): String {
        return try {
            val candidates = inputProcessor.state.candidates
            if (candidates.size > idx) candidates[idx] else ""
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
        private const val KEYBOARD_NAME_DIGIT = "digit"
        private const val KEYBOARD_NAME_SYMBOL = "symbol"
        const val PREFERENCES_FILE_NAME = "preferences.txt"
        private const val KEYBOARD_NAME_PREFERENCE_KEY = "keyboardName"
        private const val BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_ASCII = 50
        private const val BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8 = 100
    }
}

