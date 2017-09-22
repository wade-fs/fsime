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
import android.view.Display;
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
import java.util.Arrays;
import java.util.List;

import com.wade.fsime.LatinKeyboard.LatinKey;
import com.wade.fsime.R;

public class SlideTypeKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {
    private final static String TAG="MyLog";

    static int pressedCode;
    static int slideThreshold;

    BDatabase bdatabase;
    private ArrayList<B> b;
    static int keynow = 0;

    static final boolean PROCESS_HARD_KEYS = false;

    private LatinKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;

    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mMetaState;
    private LatinKeyboard mQwertyKeyboard;
    private static SlideTypeKeyboard mInstance;

    SharedPreferences mySharedPreferences;
    private AlertDialog mOptionsDialog;
    private int imeFirst = -1;
    private int superBlind = 0;

    @Override
    public boolean onEvaluateFullscreenMode() {
    	return false;
    }

    private void showOptionsMenu() {
    	if (mOptionsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            //builder.setIcon(R.drawable.ic_dialog_keyboard);
            builder.setNegativeButton(android.R.string.cancel, null);
            CharSequence itemSettings = getString(R.string.ime_settings);
            CharSequence itemInputMethod = getString(R.string.selectIME);
            builder.setItems(new CharSequence[] {
                    itemSettings, itemInputMethod},
                    new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface di, int position) {
                    di.dismiss();
                    switch (position) {
                        case 0:
                            launchSettings();
                            break;
                        case 1:
                            InputMethodManager inputManager = (InputMethodManager) getSystemService (Context.INPUT_METHOD_SERVICE);
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
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /** 
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard  = new LatinKeyboard(this, R.xml.qwerty, 0);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        if (mInputView.getKeyboard() == null) setKB(imeFirst);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    private void turnCandidate(boolean b) {
        Log.d(TAG, "turnCandidate("+b+")");
        if (mCandidateView != null && !b) {
            mComposing.setLength(0);
            mCandidateView.clear();
        }
        mPredictionOn = b;
        if (mComposing.length() > 0)
            setCandidatesViewShown(b);
    }
    private void setKB(int n) {
        Log.d(TAG, "setKB("+n+") "+keynow+" / "+(mInputView==null?"null":"haveView") + " / "+superBlind);
        if (n >= 0) keynow = n;
        mMetaState = 0;

        if (mInputView != null) {
            switch (keynow) {
                case 1:
                    mQwertyKeyboard = new LatinKeyboard(this, R.xml.fs, 1);
                    break;
                case 2:
                    mQwertyKeyboard = new LatinKeyboard(this, R.xml.two, 2);
                    break;
                default:
                    mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty, 0);
                    break;
            }
            mInputView.setKeyboard(mQwertyKeyboard);
            mQwertyKeyboard.setImeOptions(getResources(), sEditorInfo.imeOptions);
            if (mComposing.length() > 0) {
                mComposing.setLength(0);
                updateCandidates(0);
            }
            turnCandidate(keynow < 2 || superBlind == 1);
            mCapsLock = false;
            mInputView.setShifted(false);
        } else {
            switch (keynow) {
                case 0:
                    mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty, 0);
                    break;
                case 1:
                    mQwertyKeyboard = new LatinKeyboard(this, R.xml.fs, 1);
                    break;
                case 2:
                    mQwertyKeyboard = new LatinKeyboard(this, R.xml.two, 2);
                    break;
            }
            mQwertyKeyboard.setImeOptions(getResources(), sEditorInfo.imeOptions);
        }
        setInputType(keynow == 2 ? null : sEditorInfo);
    }
    private void setInputType(EditorInfo attribute) {
        if (mInputView != null && keynow == 1) {
            mPredictionOn = true;
        }
        else turnCandidate(false);
        if (attribute != null) {
            if (keynow != 1) turnCandidate(false);
            switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
                case EditorInfo.TYPE_CLASS_TEXT:
                    int a = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
                    if (a == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                        a == EditorInfo.TYPE_TEXT_VARIATION_PHONETIC ||
                        a == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
                    {
                        mPredictionOn = false;
                    } else {
                        Log.d(TAG, "setInputType(...) " + keynow);
                        if ((keynow < 2) || (superBlind==1)) turnCandidate(true);
                        if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                            mCompletionOn = false;
                        }
                    }
                    break;
                default:
            }
        }
    }
    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        if (mySharedPreferences == null) mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String customPref = mySharedPreferences.getString("slideThreshold", "7");
        slideThreshold = Integer.valueOf(customPref);
        mInputView.minSlide=0; // force re-calc
        imeFirst = Integer.valueOf(mySharedPreferences.getString("ImePriority", "-1"));
        superBlind = Integer.valueOf(mySharedPreferences.getString("twoKbType", "0"));

        sEditorInfo = attribute;
        mComposing.setLength(0);
        updateCandidates(0);
        
        mMetaState = 0;
        mCompletionOn = false;
        mCompletions = null;

        if (mQwertyKeyboard == null) setKB(imeFirst);
        mQwertyKeyboard.setImeOptions(getResources(), attribute.imeOptions);
        setInputType(attribute);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates(0);
        
        mPredictionOn = false;
        setCandidatesViewShown(false);

        if (mInputView != null) {
            mInputView.closing();
        }
    }
    EditorInfo sEditorInfo;
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        imeFirst = Integer.valueOf(mySharedPreferences.getString("ImePriority", "-1"));
        superBlind = Integer.valueOf(mySharedPreferences.getString("twoKbType", "0"));

        sEditorInfo = attribute;
        setKB(imeFirst);
        setInputType(attribute);
        mInputView.closing();
    }
    
    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd)
    {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
//        Log.d(TAG, "onUpdateSelection("+candidatesStart+"/"+isPP+"/"+(b==null?"=":b.size())+"/"+(PP==null?"-":PP.size())+"/"+mComposing+"/"+mCompletionOn);
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
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i=0; i<(completions != null ? completions.length : 0); i++) {
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
            char accent = mComposing.charAt(mComposing.length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length()-1);
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
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {

        mInputView.direction=-1;
    	switch (keyCode) {
            case -100:
                return true;
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
                    mInputView.direction=-1;
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }
        
        return super.onKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }
        
        return super.onKeyUp(keyCode, event);
    }

    /**
     * TODO 送走之後，候選區應該關閉
     */
    private void commitTyped(InputConnection inputConnection) {
    	if (inputConnection == null) {
    		handleClose();
    	} else if (mComposing.length() > 0) {
        	inputConnection.finishComposingText();
            mComposing.setLength(0);
            updateCandidates(0);
        }
    }

    /**
     * TODO: 因為要加入 Alt, 所以這邊要特別處理
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
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }
    
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
                InputConnection ic=getCurrentInputConnection();
                if (ic != null)
                    ic.commitText(String.valueOf((char) keyCode), 1);
                else
                    handleClose();
                break;
        }
    }

    public void onKey(int keycode, int[] keyCodes) {
//        Log.d(TAG, "onKey("+(char)keycode+") "+mPredictOn+"/"+superBlind+"/"+(mCandidateView==null?"null":mCandidateView.size())+"/"+mComposing);
        int primaryCode = keycode;
        if (mInputView.direction != -1)
            primaryCode=getCharFromKey(pressedCode, mInputView.direction);
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
                    if (mComposing.length()>0) {
                        mComposing.setLength(0);
                        setCandidatesViewShown(false);
                        getCurrentInputConnection().commitText("", 0);
                    } else
                        handleClose();
            }
            return;
        }

        switch (primaryCode) {
            case ' ':
                if (mPredictionOn || (keynow == 2 && superBlind == 1)) {
                    if (mCandidateView != null && mCandidateView.size() > 1) {
                        pickSuggestionManually(1);
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
                break;
            case Keyboard.KEYCODE_DELETE: handleBackspace(); break;
            case 61: handleTab(); break;
            case Keyboard.KEYCODE_SHIFT: handleShift(); break;
            case 10:
                if (mComposing.length() > 0) {
                    if ((mPredictionOn || (keynow == 2 && superBlind == 1)) && mCandidateView != null && mCandidateView.size() > 1)
                        pickSuggestionManually(1);
                    else
                        commitTyped(getCurrentInputConnection());
                    mComposing.setLength(0);
                    updateCandidates(0);
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.finishComposingText();
                    }
                }
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
                break;
            case Keyboard.KEYCODE_CANCEL:
                handleClose();
                break;
            case Keyboard.KEYCODE_MODE_CHANGE:
                InputMethodManager inputManager = (InputMethodManager) getSystemService (Context.INPUT_METHOD_SERVICE);
                inputManager.showInputMethodPicker();
                break;
            case LatinKeyboardView.KEYCODE_OPTIONS:
                showOptionsMenu();
                break;
            default:
                handleCharacter(primaryCode, keyCodes);
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
        //updateShiftKeyState(getCurrentInputEditorInfo());
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid)
    {
        Log.d(TAG, "setSuggestions()"+mCompletionOn+"/"+mComposing+(suggestions == null?"null":Arrays.toString(suggestions.toArray())));
        if (mCandidateView != null) {
            if (mComposing.length() == 1 && mComposing.charAt(0) > 'z') suggestions.add(0, mComposing.toString());
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
        setCandidatesViewShown(suggestions != null && suggestions.size() > 0);
    }
    
    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates(0);
        } else if (length == 1) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates(0);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        //updateShiftKeyState(getCurrentInputEditorInfo());
    }
    public void handleArrow(int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        sendDownUpKeyEvents(keyCode);
    }

    /*
        handleArrow(KeyEvent.KEYCODE_DPAD_LEFT);
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN);
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP);
        handleArrow(KeyEvent.KEYCODE_DPAD_RIGHT);
     */
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
        if (mInputView == null) {
            return;
        }

        if (mInputView.direction == 1) { // 向左
            setKB(0);
        } else if (mInputView.direction == 2) { // 向上
            mCapsLock = !mCapsLock;
            mInputView.setShifted(mCapsLock);
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mInputView.direction == 3) { // 向右
            setKB(1);
        } else if (mInputView.direction == 4) { // 向下
            if (keynow == 2) { // 切換超瞎及英數
                SharedPreferences.Editor editor1 = mySharedPreferences.edit();
                editor1.putString("twoKbType",superBlind == 0?"1":"0");
                editor1.commit();
                superBlind = 1-superBlind;
                Log.d(TAG, "將 superBlind 切換成 "+superBlind);
            }
            setKB(2);
        } else { // click, 沒有滑動
            setKB((keynow+1)%3);
            mCapsLock = false;
            mInputView.setShifted(false);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
        mQwertyKeyboard.setImeOptions(getResources(), sEditorInfo.imeOptions);
    }
    
    static int getCharFromKey(int primaryCode, int direction) {
    	if (direction==-1 || primaryCode=='\n')
    		return primaryCode; // hardware key
    	
        List<Key> listKeys = mInstance.mInputView.getKeyboard().getKeys();
        if (listKeys != null) {
        	for (Key k: listKeys) {
        		LatinKey lk=(LatinKey)k;
        		if (lk.fancyLabel!= null && primaryCode == k.codes[0])
        			return lk.fancyLabel.charAt(direction);				
        	}
    	}
    	return primaryCode;
    }

    private int setTs(int v) {
        mComposing.setLength(0);
        getCurrentInputConnection().setComposingText(mComposing, 1);
        updateCandidates(0);
        setCandidatesViewShown(false);
        return bdatabase.setTs(v);
    }
    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.sShiftState) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (keynow == 1 || (keynow == 2 && superBlind == 1)) mPredictionOn = true;

        if (primaryCode > 0) {
            if (mPredictionOn) {
                mComposing.append((char)primaryCode);
                String c = mComposing.toString();
                if (keynow == 1)
                    c = c.replaceAll("[^A-Za-z,\\.'\\[\\]]","");
                else if (keynow == 2 && superBlind == 1)
                    c = c.replaceAll("[^A-Za-z0-9,\\.'\\[\\]]","");
                if (c.toLowerCase().equals(",,s")) setTs(2);
                else if (c.toLowerCase().equals(",,t")) setTs(1);
                else if (c.toLowerCase().equals(",,n")) setTs(0);
                else {
                    mComposing.setLength(0);
                    mComposing.append(c);
                    getCurrentInputConnection().setComposingText(mComposing, 1);
                    updateCandidates(0);
                }
            } else {
                if (isInputViewShown()) {
                    if (mInputView.isShifted()) {
                        primaryCode = Character.toUpperCase(primaryCode);
                    }
                }
                if (mComposing.length() > 0) {
                    commitTyped(getCurrentInputConnection());
                }
                getCurrentInputConnection().commitText(String.valueOf((char)primaryCode),1);
            }
        }
    }

    // Check that our connection hasnt got closed (workaround for a kind of bug)
    private void handleClose() {
    	setCandidatesViewShown(false);
    	
    	InputConnection ic=getCurrentInputConnection();
    	if (ic != null)
    		commitTyped(ic);
        requestHideSelf(0);

        mInputView.closing();
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
                // 將使用者打的字放第一個
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());
                // 接下來判斷哪個鍵盤，採用不同的 table
                if (bdatabase == null) bdatabase = new BDatabase(getApplicationContext());
                // wade, 底下根據鍵盤，切換不同的資料庫
                int s = start + forward * 30;
                if (keynow == 0) {
                    b = bdatabase.getC(mComposing.toString().toLowerCase(), s);
                    for (B d : b) {
                        list.add(d.eng);
                    }
                } else if (keynow == 1) {
                    if ((b = bdatabase.getB(mComposing.toString().toLowerCase(), s)).size() > 0) {
                        start += forward * b.size();
                        for (B d : b) {
                            list.add(d.ch);
                        }
                    }
                } else if (keynow == 2 && superBlind == 1) {
                    if ((b = bdatabase.getB2(mComposing.toString().toLowerCase(), s)).size() > 0) {
                        start += forward * b.size();
                        for (B d : b) {
                            list.add(d.ch);
                        }
                    }
                }
//                Log.d(TAG, "updateCandidates("+forward+") "+b.size()+"/"+mComposing);
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }
    private String lastWord="";

    boolean isPP = false;
    ArrayList<String> PP = new ArrayList<String>();
    public void pickSuggestionManually(int index) {
        String res = mCandidateView.getSuggestion(index);
//        Log.d(TAG, "pickSuggestionManually("+index+") "+res+"/"+isPP+"/"+mCompletionOn+"/"+mComposing+"/"+b.size());
        if (!res.equals("")) {
            getCurrentInputConnection().commitText(res, res.length());
            mComposing.setLength(0);
            if (index > 0) {
                if (bdatabase == null) bdatabase  = new BDatabase(getApplicationContext());
                if (!isPP && b.size()>0)
                    bdatabase.updateRow(b.get(index - 1), keynow==1?"b":"c");
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
    	onKey(pressedCode,null);
    }
    
    public void swipeLeft() {
    	onKey(pressedCode,null);
    }

    public void swipeDown() {
    	onKey(pressedCode,null);
    }

    public void swipeUp() {
    	onKey(pressedCode,null);
    }
    
    public void onPress(int primaryCode) {
    	pressedCode=primaryCode;
    }
    
    public void onRelease(int primaryCode) {
    	int kk=3;
    	kk=kk+1;
    }
}
