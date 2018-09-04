/*
 * Copyright (C) 2008-2009 Google Inc.
 * Copyright (C) 2009 Alejandro Grijalba
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.wade.fsime;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.preference.PreferenceManager;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;


import java.util.ArrayList;
import java.util.List;

import com.wade.fsime.LatinKeyboard.LatinKey;
import com.wade.fsime.R;


/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SlideTypeKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {
    static final boolean DEBUG = false;
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     * <p>
     * I could enable this to enable predictive text with hard keyboard.
     * But it seems other keybards dont do it.
     */

    static final boolean PROCESS_HARD_KEYS = false;
    static int pressedCode;
    static int keyLayout;
    BDatabase bdatabase;
    private ArrayList<B> b;

    /*
    @Override
    public boolean onEvaluateFullscreenMode() {
    	return true;
    }
    */
    static int slideThreshold;
    private static LatinKeyboard mCurKeyboard;
    private static SlideTypeKeyboard mInstance;

    EditorInfo sEditorInfo;
    private AlertDialog mOptionsDialog;
    private LatinKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;
    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    //    private long mLastShiftTime;
    private long mMetaState;
    private LatinKeyboard keyboardQwerty;
    private LatinKeyboard keyboardTwo;
    private LatinKeyboard keyboardJuin;
    private String mWordSeparators;

    static int getCharFromKey(int primaryCode, int direction) {
        if (direction == -1 || primaryCode == '\n')
            return primaryCode; // hardware key

        int dir = direction;
        if (primaryCode != -100) {
            if (dir == 1) dir = 2;
            else if (dir == 2) dir = 1;
        }
        List<Key> listKeys = mInstance.mInputView.getKeyboard().getKeys();
        if (listKeys != null) {
            for (Key k : listKeys) {
                LatinKey lk = (LatinKey) k;
                if (lk.fancyLabel != null && primaryCode == k.codes[0])
                    return lk.fancyLabel.charAt(dir);
            }
        }
        return primaryCode;
    }

    private void showOptionsMenu() {
        if (mOptionsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            //builder.setIcon(R.drawable.ic_dialog_keyboard);
            builder.setNegativeButton(android.R.string.cancel, null);
            CharSequence itemSettings = getString(R.string.ime_settings);
            CharSequence itemInputMethod = getString(R.string.selectIME);
            builder.setItems(new CharSequence[]{
                            itemSettings, itemInputMethod},
                    new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface di, int position) {
                            di.dismiss();
                            switch (position) {
                                case 0:
                                    launchSettings();
                                    break;
                                case 1:
                                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    inputManager.showInputMethodPicker();
                                    break;
                            }
                        }
                    });
            builder.setTitle(getResources().getString(R.string.ime_name));
            mOptionsDialog = builder.create();
        }

        Window window = mOptionsDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = mInputView.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mOptionsDialog.show();
    }

    public void launchSettings() {
        handleClose();

        Intent intent = new Intent();
        intent.setClass(SlideTypeKeyboard.this, SettingsActivity2.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        //android.os.Debug.waitForDebugger();
        mWordSeparators = getResources().getString(R.string.word_separators);
        mInstance = this;
        bdatabase  = new BDatabase(getApplicationContext());
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {
        if (keyboardQwerty != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        keyboardQwerty = new LatinKeyboard(this, R.xml.qwerty);
        keyboardTwo = new LatinKeyboard(this, R.xml.two);
        keyboardJuin = new LatinKeyboard(this, R.xml.juin);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override
    public View onCreateInputView() {
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(keyboardQwerty); // 預設
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override
    public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String customPref = mySharedPreferences.getString("keyLayout", "0");
        keyLayout = Integer.valueOf(customPref);
        customPref = mySharedPreferences.getString("slideThreshold", "7");
        slideThreshold = Integer.valueOf(customPref);
        LatinKeyboardView.minSlide = 0; // force re-calc


        //android.os.Debug.waitForDebugger();
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates(0);

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_TEXT:
                mCurKeyboard = keyboardQwerty;
                mPredictionOn = true;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
//                updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = keyboardQwerty;
//                updateShiftKeyState(attribute);
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * 預設鍵盤切換回 QWERTY
     */
    @Override
    public void onFinishInput() {
        super.onFinishInput();

        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates(0);

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        mCurKeyboard = keyboardQwerty;
        if (mInputView != null) {
            mInputView.closing();
        }
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
        sEditorInfo = attribute;
    }
    private String lastWord="";
    boolean isPP = false;
    ArrayList<String> PP = new ArrayList<String>();
    private void turnCandidate(boolean prediction) {
        if (mCandidateView != null && !prediction) {
            mComposing.setLength(0);
            mCandidateView.clear();
        }
        mPredictionOn = prediction;
        if (mComposing.length() > 0)
            setCandidatesViewShown(prediction);
    }
    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        if (isPP && PP.size() > 0) {
            setSuggestions(PP, true, true);
            isPP = false;
            PP.clear();
        } else if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates(0);
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        } else if (mComposing.length() == 0){
            setSuggestions(null, false, false);
            mComposing.setLength(0);
            setCandidatesViewShown(false);
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < (completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() - 1);
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length() - 1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LatinKeyboardView.direction = -1;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                	/*
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState()&KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    */
                    LatinKeyboardView.direction = -1;
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (inputConnection == null) {
            handleClose();
            return;
        }
        if (mComposing.length() > 0 && inputConnection != null) {
            inputConnection.finishComposingText();
            // commitText() found to be evil when user adds text inside an existing word
            //inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates(0);
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null && mInputView != null) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (!mInputView.getKeyboard().equals(keyboardTwo)) {
            if (Character.isLetter(code) || code == ',' || code == '.' || code == '[' || code == ']' || code == '\'') {
                return true;
            } else {
                return false;
            }
        } else { // Two
            if (Character.isDigit(code) || code == ';' || code == ',' || code == '.' || code == '=') {
                return true;
            } else {
                return false;
            }
        }
    }

    // Implementation of KeyboardViewListener

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        if (getCurrentInputConnection() == null) {
            handleClose();
            return;
        }
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null)
                        ic.commitText(String.valueOf((char) keyCode), 1);
                    else
                        handleClose();
                }
                break;
        }
    }

    public void onKey(int keycode, int[] keyCodes) {
        mPredictionOn = isAlphabet(keycode);
        int primaryCode = keycode;
        if (LatinKeyboardView.direction != -1)
            primaryCode = getCharFromKey(pressedCode, LatinKeyboardView.direction);
        if (mComposing.length() > 0 && mComposing.charAt(0) >= 256) mComposing.setLength(0);
        if (keycode == -100) {
            switch (primaryCode) {
                case '◀':
                    handleLeft();
                    break;
                case '▲':
                    handleUp();
                    break;
                case '▶':
                    handleRight();
                    break;
                case '▼':
                    handleDown();
                    break;
                default: // ESC
                    if (mComposing.length() > 0) {
                        commitTyped(getCurrentInputConnection());
                    } else
                        handleClose();
                    mCandidateView.clear();
                    mComposing.setLength(0);
                    setCandidatesViewShown(false);
            }
            return;
        }
        if (primaryCode == ' ') {
            if (mPredictionOn || !(mInputView.getKeyboard().equals(keyboardTwo))) {
                if (mCandidateView != null && mCandidateView.size() >= 1) {
                    if (mComposing.length() > 0 && ((int)mComposing.charAt(0)) < 256) {
                        pickSuggestionManually(1);
                    } else pickSuggestionManually(0);
                } else {
                    if (mComposing.length() > 0) {
                        commitTyped(getCurrentInputConnection());
                    } else {
                        sendKey(primaryCode);
                    }
                    mComposing.setLength(0);
                    updateCandidates(0);
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.finishComposingText();
                    }
                }
            } else if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            } else sendKey(primaryCode);
        } else if (isWordSeparator(primaryCode)) {
            // Handle separator
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(primaryCode);
//            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == 9) {
            handleTab();
        } else if (primaryCode == 10) {
            if (mComposing.length() > 0) {
                if (mPredictionOn && mCandidateView != null && mCandidateView.size() > 1)
                    pickSuggestionManually(1);
                else
                    commitTyped(getCurrentInputConnection());
                mComposing.setLength(0);
                updateCandidates(0);
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.finishComposingText();
                }
            } else {
                InputConnection ic = getCurrentInputConnection();
                switch (sEditorInfo.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
                    case EditorInfo.IME_ACTION_DONE:
                        ic.performEditorAction(EditorInfo.IME_ACTION_DONE);
                        break;
                    case EditorInfo.IME_ACTION_GO:
                        ic.performEditorAction(EditorInfo.IME_ACTION_GO);
                        break;
                    case EditorInfo.IME_ACTION_NEXT:
                        ic.performEditorAction(EditorInfo.IME_ACTION_NEXT);
                        break;
                    case EditorInfo.IME_ACTION_NONE:
                        ic.performEditorAction(EditorInfo.IME_ACTION_NONE);
                        break;
                    case EditorInfo.IME_ACTION_SEARCH:
                        ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH);
                        break;
                    case EditorInfo.IME_ACTION_SEND:
                        ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
                        break;
                    default:
                        if (sEditorInfo.imeOptions == 1342177286)//fix for DroidEdit
                        {
                            ic.performEditorAction(EditorInfo.IME_ACTION_GO);
                        } else
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                        break;
                }
            }
        } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            showOptionsMenu();//launchSettings();
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            Keyboard current = mInputView.getKeyboard();
            mInputView.setKeyboard(current);
        } else {
            handleCharacter(primaryCode);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
//        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private int start = 0;
    public void updateCandidates(int forward) {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());

                if (bdatabase == null) bdatabase = new BDatabase(getApplicationContext());
                // wade, 底下根據鍵盤，切換不同的資料庫
                int s = start + forward * 30;
                if (mInputView.getKeyboard().equals(keyboardQwerty)) { // 英瞎
                    b = bdatabase.getB(mComposing.toString().toLowerCase(), s);
                    for (B d : b) {
                        list.add(d.ch);
                    }
                } else if (mInputView.getKeyboard().equals(keyboardJuin)) { // 注音
                    if ((b = bdatabase.getJuin(mComposing.toString().toLowerCase(), s)).size() > 0) {
                        start += forward * b.size();
                        for (B d : b) {
                            list.add(d.ch);
                        }
                    }
                }

                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0 || isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates(0);
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates(0);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
//        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    public void handleArrow(int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        sendDownUpKeyEvents(keyCode);
    }

    private void handleTab() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_TAB);
    }

    private void handleUp() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP);
    }

    private void handleDown() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN);
    }

    private void handleLeft() {
        handleArrow(KeyEvent.KEYCODE_DPAD_LEFT);
    }

    private void handleRight() {
        handleArrow(KeyEvent.KEYCODE_DPAD_RIGHT);
    }

    private void handleShift() {
        if (mInputView == null) return;
        if (LatinKeyboardView.direction == 1) { // 左, 回英瞎
            mCapsLock = false;
            mInputView.setKeyboard(keyboardQwerty);
            mInputView.setNormal();
        } else if (LatinKeyboardView.direction == 2) { // 向上
            mCapsLock = !mCapsLock;
            mInputView.setShifted(mCapsLock);
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (LatinKeyboardView.direction == 3) { // 向右
            mCapsLock = false;
            mInputView.setKeyboard(keyboardJuin);
            mInputView.setNormal();
        } else if (LatinKeyboardView.direction == 4) { // 向下
            mCapsLock = false;
            mInputView.setKeyboard(keyboardTwo);
            mInputView.setNormal();
        } else {
            mInputView.rotateAltShift();
            if (LatinKeyboardView.sAltState) { // 換鍵盤
                Keyboard curKB = mInputView.getKeyboard();
                if (curKB.equals(keyboardQwerty)) mInputView.setKeyboard(keyboardJuin);
                else if (curKB.equals(keyboardJuin)) mInputView.setKeyboard(keyboardTwo);
                else mInputView.setKeyboard(keyboardQwerty);
                mInputView.setNormal();
            }
        }
    }
    private int setTs(int v) {
        if (bdatabase == null) bdatabase  = new BDatabase(getApplicationContext());
        mComposing.setLength(0);
        getCurrentInputConnection().setComposingText(mComposing, 1);
        updateCandidates(0); // TODO, 更新候選區
        setCandidatesViewShown(false);
        return bdatabase.setTs(v);
    }

    private void handleCharacter(int primaryCode) {
        if (isInputViewShown() && LatinKeyboardView.sShiftState) {
            primaryCode = Character.toUpperCase(primaryCode);
        }
        if (primaryCode > 0) {
            if (isAlphabet(primaryCode) && mPredictionOn) {
                mComposing.append((char) primaryCode);
                getCurrentInputConnection().setComposingText(mComposing, 1);
                // updateShiftKeyState(getCurrentInputEditorInfo());
                updateCandidates(0);
            } else {
                if (mComposing.length() > 0) {
                    commitTyped(getCurrentInputConnection());
                }
                getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);
            }
        }
    }

    // Check that our connection hasnt got closed (workaround for a kind of bug)
    private void handleClose() {
        setCandidatesViewShown(false);

        InputConnection ic = getCurrentInputConnection();
        if (ic != null)
            commitTyped(ic);
        requestHideSelf(0);

        mInputView.closing();
    }

    public boolean isWordSeparator(int code) {
        return mWordSeparators.contains(String.valueOf((char) code));
    }

    public void pickSuggestionManually(int index) {
        String res = mCandidateView.getSuggestion(index);
        if (!res.equals("")) {
            getCurrentInputConnection().commitText(res, res.length());
            mComposing.setLength(0);
            if (index > 0) {
                if (bdatabase == null) bdatabase  = new BDatabase(getApplicationContext());
                if (!isPP && b.size()>0)
                    bdatabase.updateRow(b.get(index - 1), mInputView.getKeyboard().equals(keyboardQwerty)?"b":"juin");
                res = res.substring(res.length()-1);
                if (bdatabase.isFreq(res)) {
                    if (bdatabase == null) bdatabase = new BDatabase(getApplicationContext());
                    if (!lastWord.equals("") && !bdatabase.isInPP(lastWord, res))
                        bdatabase.addB(lastWord, res, "pp");
                    PP.clear();
                    b = bdatabase.getPP(res, 0);
                    for (B d : b) {
                        PP.add(d.ch);
                    }
                    if (PP.size() > 0) {
                        isPP = true;
                        mComposing.append(res);
                    } else {
                        isPP = false;
                        setSuggestions(null, false, false);
                    }
                    lastWord = res;
                    if (!isPP) {
                        turnCandidate(false);
                    }
                }
            }
        } else if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
        } else if (mComposing.length() > 0) {
            commitTyped(getCurrentInputConnection());
            mComposing.setLength(0);
        }
    }

    public void swipeRight() {
        onKey(pressedCode, null);
        //handleSlideKey();

    }


    public void swipeLeft() {
        onKey(pressedCode, null);
        //handleSlideKey();
    }

    public void swipeDown() {
        onKey(pressedCode, null);
        //handleSlideKey();
    }

    public void swipeUp() {
        onKey(pressedCode, null);
        //handleSlideKey();
    }

    public void onPress(int primaryCode) {
        pressedCode = primaryCode;
    }

    public void onRelease(int primaryCode) {
        int kk = 3;
        kk = kk + 1;
    }
}
