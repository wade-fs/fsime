package com.wade.fsime;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.media.MediaPlayer; // for keypress sound

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.ColorUtils;

import com.wade.fsime.layout.Box;
import com.wade.fsime.layout.Definitions;
import com.wade.fsime.layout.Key;
import com.wade.fsime.layout.builder.KeyboardLayoutBuilder;
import com.wade.fsime.layout.builder.KeyboardLayoutException;
import com.wade.fsime.layout.ui.KeyboardLayoutView;
import com.wade.fsime.layout.ui.KeyboardUiFactory;
import com.wade.fsime.theme.ThemeDefinitions;
import com.wade.fsime.theme.ThemeInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class CodeBoardIME extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {
    private static final String NOTIFICATION_CHANNEL_ID = "Codeboard";
    static int pressedCode;
    EditorInfo sEditorInfo;
    private boolean vibratorOn;
    private int vibrateLength;
    private boolean soundOn;
    private boolean use_boshiamy=true, disable_normal=false, use_phonetic=true;
    private int maxMatch = 30;
    private boolean shiftLock = false;
    private boolean ctrlLock = false;
    private boolean shift = false;
    private boolean ctrl = false;
    private int mCurKeyboard = R.integer.keyboard_boshiamy;
    private Timer timerLongPress = null;
    private KeyboardUiFactory mKeyboardUiFactory = null;
    private KeyboardLayoutView mCurrentKeyboardLayoutView = null;
    private CandidateView mCandidateView;
    private final StringBuilder mComposing = new StringBuilder();
    private boolean swipe;
    private int mPhoneOrientation = Configuration.ORIENTATION_PORTRAIT;

    BDatabase bdatabase;
    private ArrayList<B> b;

    // normal -> boshiamy -> phonetic
    //    -> sym
    //    -> clipboard
    private void nextKeyboard() {
        if (ctrl) {
            mCurKeyboard = R.integer.keyboard_clipboard;
            return;
        }
        if (mCurKeyboard == R.integer.keyboard_clipboard) {
            if (disable_normal) {
                if (use_boshiamy) {
                    mCurKeyboard = R.integer.keyboard_boshiamy;
                } else if (use_phonetic) {
                    mCurKeyboard = R.integer.keyboard_phonetic;
                } else {
                    mCurKeyboard = R.integer.keyboard_sym;
                }
            } else {
                mCurKeyboard = R.integer.keyboard_normal;
            }
        } else if (mCurKeyboard == R.integer.keyboard_normal) {
            if (use_boshiamy) {
                mCurKeyboard = R.integer.keyboard_boshiamy;
            } else if (use_phonetic) {
                mCurKeyboard = R.integer.keyboard_phonetic;
            } else {
                mCurKeyboard = R.integer.keyboard_sym;
            }
        } else if (mCurKeyboard == R.integer.keyboard_boshiamy) {
            if (use_phonetic)
                mCurKeyboard = R.integer.keyboard_phonetic;
            else mCurKeyboard = R.integer.keyboard_sym;
        } else if (mCurKeyboard == R.integer.keyboard_phonetic) {
            mCurKeyboard = R.integer.keyboard_sym;
        } else if (disable_normal) {
            if (use_boshiamy) {
                mCurKeyboard = R.integer.keyboard_boshiamy;
            } else if (use_phonetic) {
                mCurKeyboard = R.integer.keyboard_phonetic;
            } else {
                mCurKeyboard = R.integer.keyboard_sym;
            }
        } else {
            mCurKeyboard = R.integer.keyboard_normal;
        }
    }

    private boolean processSpecialKey(int primaryCode) {
        boolean res = true;
        InputConnection ic = getCurrentInputConnection();
        switch (primaryCode) {
            //First handle cases that  don't use shift/ctrl meta modifiers
            case 53737:
                ic.performContextMenuAction(android.R.id.selectAll);
                break;
            case 53738:
                ic.performContextMenuAction(android.R.id.cut);
                break;
            case 53739:
                ic.performContextMenuAction(android.R.id.copy);
                break;
            case 53740:
                ic.performContextMenuAction(android.R.id.paste);
                break;
            case 53741:
                ic.performContextMenuAction(android.R.id.undo);
                break;
            case 53742:
                ic.performContextMenuAction(android.R.id.redo);
                break;
            case -1: // SYM, 切換鍵盤
                turnCandidate(false);
                if (shift) {
                    shift = false;
                    shiftLock = false;
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT));
                }
                if (ctrl) {
                    mCurKeyboard = R.integer.keyboard_clipboard;
                    ctrl = false;
                    ctrlLock = false;
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                } else {
                    nextKeyboard();
                }
                setInputView(onCreateInputView());
                controlKeyUpdateView();
                shiftKeyUpdateView();
                break;

            case KeyEvent.KEYCODE_CTRL_LEFT:
                if (!ctrlLock && !ctrl) {
                    ctrl = true;
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
                } else if (!ctrlLock && ctrl) {
                    ctrl = false;
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                }
                controlKeyUpdateView();
                break;

            case 16: //KEYCODE_SHIFT_LEFT
                // emulates press of shift key - this helps for selection with arrow keys
                if (!shiftLock && !shift) {
                    //Simple shift to true
                    shift = true;
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT));
                } else if (!shiftLock && shift) {
                    //Simple remove shift
                    shift = false;
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT));
                }
                //else if (shift && shiftLock) {
                //Stay shifted if previously shifted
                //}
                shiftKeyUpdateView();
                break;
            default:
                res = false;
        }
        return res;
    }

    private int primary2ke(int primaryCode) {
        char code = (char) primaryCode;
        int ke = 0;
        switch (primaryCode) {
            case 9:
                ke = -KeyEvent.KEYCODE_TAB;
                break;
            case -2:
                ke = KeyEvent.KEYCODE_ESCAPE;
                break;
            case KeyEvent.KEYCODE_SPACE:
                ke = KeyEvent.KEYCODE_SPACE;
                break;
            case -5:
                ke = KeyEvent.KEYCODE_DEL;
                break;
            case -4:
                ke = KeyEvent.KEYCODE_ENTER;
                break;
            case -6:
                ke = -KeyEvent.KEYCODE_F1;
                break;
            case -7:
                ke = -KeyEvent.KEYCODE_F2;
                break;
            case -8:
                ke = -KeyEvent.KEYCODE_F3;
                break;
            case -9:
                ke = -KeyEvent.KEYCODE_F4;
                break;
            case -10:
                ke = -KeyEvent.KEYCODE_F5;
                break;
            case -11:
                ke = -KeyEvent.KEYCODE_F6;
                break;
            case -12:
                ke = -KeyEvent.KEYCODE_F7;
                break;
            case -13:
                ke = -KeyEvent.KEYCODE_F8;
                break;
            case -14:
                ke = -KeyEvent.KEYCODE_F9;
                break;
            case -15:
                ke = -KeyEvent.KEYCODE_F10;
                break;
            case -16:
                ke = -KeyEvent.KEYCODE_F11;
                break;
            case -17:
                ke = -KeyEvent.KEYCODE_F12;
                break;
            case -18:
                ke = -KeyEvent.KEYCODE_MOVE_HOME;
                break;
            case -19:
                ke = -KeyEvent.KEYCODE_MOVE_END;
                break;
            case -20:
                ke = -KeyEvent.KEYCODE_INSERT;
                break;
            case -21:
                ke = -KeyEvent.KEYCODE_FORWARD_DEL;
                break;
            case -22:
                ke = -KeyEvent.KEYCODE_PAGE_UP;
                break;
            case -23:
                ke = -KeyEvent.KEYCODE_PAGE_DOWN;
                break;

            //These are like a directional joystick - can jump outside the inputConnection
            case 5000:
                ke = -KeyEvent.KEYCODE_DPAD_LEFT;
                break;
            case 5001:
                ke = -KeyEvent.KEYCODE_DPAD_DOWN;
                break;
            case 5002:
                ke = -KeyEvent.KEYCODE_DPAD_UP;
                break;
            case 5003:
                ke = -KeyEvent.KEYCODE_DPAD_RIGHT;
                break;
            default:
                if (Character.isLetter(code)) {
                    ke = KeyEvent.keyCodeFromString("KEYCODE_" + Character.toUpperCase(code));
                }
        }
        return ke;
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length >= 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
        } else {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            keyDownUp(KeyEvent.KEYCODE_DEL, 0);
            mCandidateView.setSuggestions(null, false, false);
        }
    }

    public void onPress(final int primaryCode) {
        swipe = false;
        pressedCode = primaryCode;
        if (soundOn) {
            MediaPlayer keypressSoundPlayer = MediaPlayer.create(this, R.raw.keypress_sound);
            keypressSoundPlayer.start();
            keypressSoundPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
        }
        if (vibratorOn) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(vibrateLength);
        }

        clearLongPressTimer();
        timerLongPress = new Timer();
        timerLongPress.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                CodeBoardIME.this.onKeyLongPress(primaryCode);
                            } catch (Exception e) {

                            }
                        }
                    };
                    uiHandler.post(runnable);
                } catch (Exception e) {

                }
            }
        }, ViewConfiguration.getLongPressTimeout());
    }

    @Override
    public void onKey(int primaryCode, int[] KeyCodes) {
        // move to processKey();
    }

    public void onKeyLongPress(int keyCode) {
        // Process long-click here
        // This is following an onKey()
        InputConnection ic = getCurrentInputConnection();
        if (keyCode == 16) { // Shift
            shiftLock = !shiftLock;
            if (shiftLock) {
                shift = true;
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT));
            } else {
                shift = false;
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT));
            }
            shiftKeyUpdateView();
        }

        if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT) { // Ctrl
            ctrlLock = !ctrlLock;
            if (ctrlLock) {
                ctrl = true;
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
            } else {
                ctrl = false;
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
            }
            controlKeyUpdateView();
        }

        if (keyCode == KeyEvent.KEYCODE_SPACE) { // SPACE
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.showInputMethodPicker();
        }

        if (vibratorOn) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(vibrateLength);
        }
    }

    public void onRelease(int primaryCode) {
        if (!swipe) processKey();
        clearLongPressTimer();
    }

    private void processKey() {
        int primaryCode = pressedCode;
        //NOTE: Long press goes second, this is onDown
        InputConnection ic = getCurrentInputConnection();
        char code = (char) primaryCode;

        if (!processSpecialKey(primaryCode)) { // normal key
            int meta = 0;
            if (shift) {
                meta = KeyEvent.META_SHIFT_ON;
                code = Character.toUpperCase(code);
            }
            if (ctrl) {
                meta = meta | KeyEvent.META_CTRL_ON;
            }
            int ke = primary2ke(primaryCode);
            if (ke < 0) { // 特殊字，例如上下左右等等
                keyDownUp(-ke, meta);
            } else if (ke != 0  || ",.[]".indexOf(code) >= 0) {
				if (ctrl) {
                    keyDownUp(ke, meta);
                } else if (mCurKeyboard == R.integer.keyboard_boshiamy || mCurKeyboard == R.integer.keyboard_phonetic) {
                    if (ke == KeyEvent.KEYCODE_DEL) {
                        handleBackspace();
                    } else if (ke == KeyEvent.KEYCODE_SPACE) {
                        if (mCandidateView == null || mCandidateView.size() == 0) {
                            ic.commitText(String.valueOf(code), 1);
                        } else if (mCandidateView.size() > 1) {
                            pickSuggestionManually(1);
                            return;
                        }
                    } else if (ke == KeyEvent.KEYCODE_ENTER) {
                        if (mComposing.length() > 0)
                            pickSuggestionManually(0);
                        else
                            keyDownUp(ke, 0);
                    } else if (ke == KeyEvent.KEYCODE_ESCAPE) {
                        turnCandidate(false);
                    } else {
                        mComposing.append(code);
                    }
                    if (mComposing.length() > 0) {
                        updateCandidates(0, "");
                    } else {
                        setCandidatesViewShown(false);
                    }
                } else {
                    keyDownUp(ke, meta);
                }
            } else {
                mComposing.append(String.valueOf(code));
                updateCandidates(0, "");
            }
            if (shift && !shiftLock) {
                shift = false;
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT));
                shiftKeyUpdateView();
            }
            if (ctrl && !ctrlLock) {
                ctrl = false;
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
                controlKeyUpdateView();
            }
        }
    }

    public void onText(CharSequence text) {
        Logi("onText() code "+text.toString());
        getCurrentInputConnection().commitText(text, 1);
        clearLongPressTimer();
    }

    @Override
    public void swipeLeft() {
        Logi("swipeLeft() code "+mCurrentKeyboardLayoutView.getKey());
    }

    @Override
    public void swipeRight() {
        Logi("swipeRight() code "+mCurrentKeyboardLayoutView.getKey());
    }

    @Override
    public void swipeDown() {
        Logi("swipeDown() code "+mCurrentKeyboardLayoutView.getKey());
    }

    @Override
    public void swipeUp() {
        Logi("swipeUp() code "+mCurrentKeyboardLayoutView.getKey());
        swipe = true; // prevent processKey() @ onRelease()

        //getCurrentInputConnection().commitText(mCurrentKeyboardLayoutView.getKey(), 1);
        pressedCode = mCurrentKeyboardLayoutView.getKey().hashCode();
        processKey();
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        clearLongPressTimer();
    }

    @Override
    public void onViewClicked(boolean focusChanged) {
        super.onViewClicked(focusChanged);
        clearLongPressTimer();
    }

    private void turnCandidate(boolean prediction) {
        if (mCandidateView != null && !prediction) {
            mComposing.setLength(0);
            mCandidateView.clear();
        }
        setCandidatesViewShown(prediction);
    }

    /**
     * 每次發送字之後，應關閉候選區
     */
    public void pickSuggestionManually(int index) {
        String res = mCandidateView.getSuggestion(index);
        if (res.length() > 0) {
            InputConnection ic = getCurrentInputConnection();
            ic.commitText(res, res.length());
            ic.finishComposingText();
            mComposing.setLength(0);
            if (res.length() == 1) {
                updateCandidates(0, res);
            } else {
                updateCandidates(0, "");
                turnCandidate(false);
            }
        } else {
            turnCandidate(false);
        }
    }

    private void keyDownUp(int keyEventCode, int meta) {
        InputConnection ic = getCurrentInputConnection();
        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEventCode, 0, meta));
        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEventCode, 0, meta));
    }

    private void Logi(String msg) {
        Log.i("FSIME", msg);
    }
    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private int start = 0;
    /**
     * 更新候選區，其內容是由 mComposing 從資料庫產生，第一筆就是輸入組字
     */
    private void updateCandidates(int forward, String freq) { // 候選區是捲動式的，要往前 forward 幾個字
        // 為了防呆，也為了讓思考不要去管鍵盤是哪一個，在此阻止非自建輸入法顯示候選區
        if (mCurKeyboard != R.integer.keyboard_boshiamy && mCurKeyboard != R.integer.keyboard_phonetic) {
            return;
        }
        if (freq.length() > 0) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(freq.substring(0,1));
            if (bdatabase == null) bdatabase = new BDatabase(getApplicationContext());
            int s = start + forward * 30;
            b = bdatabase.getF(freq, s, maxMatch);
            start += forward * b.size();
            for (B d : b) {
                list.add(d.ch);
            }
            setSuggestions(list, true, true);
        } else if (mComposing.length() > 0) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(mComposing.toString());

            if (bdatabase == null) bdatabase = new BDatabase(getApplicationContext());
            // wade, 底下根據鍵盤，切換不同的資料庫
            int s = start + forward * 30;
            if (mCurKeyboard == R.integer.keyboard_boshiamy) { // 英瞎
                b = bdatabase.getB(mComposing.toString().toLowerCase(), s, maxMatch);
                for (B d : b) {
                    list.add(d.ch);
                }
            } else if (mCurKeyboard == R.integer.keyboard_phonetic) { // 注音
                if ((b = bdatabase.getJuin(mComposing.toString(), s, maxMatch)).size() > 0) {
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

    /**
     * 會導致 出現候選區
     */
    public void setSuggestions(List<String> suggestions, boolean completions, boolean typedWordValid) {
        if (suggestions == null || suggestions.size() == 0) {
            setCandidatesViewShown(false);
            mCandidateView.setSuggestions(null, false, false);
            return;
        }
        setCandidatesViewShown(true);
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    @Override
    public View onCreateInputView() {
        bdatabase  = new BDatabase(getApplicationContext());
        if (mKeyboardUiFactory == null) {
            mKeyboardUiFactory = new KeyboardUiFactory(this);
        }
        KeyboardPreferences sharedPreferences = new KeyboardPreferences(this);
        setNotification(sharedPreferences.getNotification());
        if (sharedPreferences.getCustomTheme()) {
            mKeyboardUiFactory.theme = getDefaultThemeInfo();
            mKeyboardUiFactory.theme.foregroundColor = sharedPreferences.getFgColor();
            mKeyboardUiFactory.theme.backgroundColor = sharedPreferences.getBgColor();
        } else {
            mKeyboardUiFactory.theme = setThemeByIndex(sharedPreferences, sharedPreferences.getThemeIndex());
        }
        maxMatch = sharedPreferences.getMaxMatch();
        // Keyboard Features
        vibrateLength = sharedPreferences.getVibrateLength();
        vibratorOn = sharedPreferences.isVibrateEnabled();
        soundOn = sharedPreferences.isSoundEnabled();
        use_boshiamy = sharedPreferences.useBoshiamy();
        disable_normal = sharedPreferences.isDisabledNormal();
        use_phonetic = sharedPreferences.usePhonetic();
        mKeyboardUiFactory.theme.enablePreview = sharedPreferences.isPreviewEnabled();
        mKeyboardUiFactory.theme.enableBorder = sharedPreferences.isBorderEnabled();
        mKeyboardUiFactory.theme.fontSize = sharedPreferences.getFontSizeAsSp();
        int mSize = sharedPreferences.getPortraitSize();
        int sizeLandscape = sharedPreferences.getLandscapeSize();
        mKeyboardUiFactory.theme.size = mSize / 100.0f;
        mKeyboardUiFactory.theme.sizeLandscape = sizeLandscape / 100.0f;
        if (sharedPreferences.getNavBarDark()) {
            Objects.requireNonNull(getWindow().getWindow()).
                    setNavigationBarColor(
                            ColorUtils.blendARGB(mKeyboardUiFactory.theme.backgroundColor,
                                    Color.BLACK, 0.2f));
        } else if (sharedPreferences.getNavBar()) {
            Objects.requireNonNull(getWindow().getWindow()).
                    setNavigationBarColor(mKeyboardUiFactory.theme.backgroundColor);
        }
        //Key Layout
        boolean mToprow = sharedPreferences.getTopRowActions();
        String mCustomSymbolsMain = sharedPreferences.getCustomSymbolsMain();
        String mCustomSymbolsLongPress = sharedPreferences.getCustomSymbolsLongPress();
        String mCustomSymbolsMain2 = sharedPreferences.getCustomSymbolsMain2();
        String mCustomSymbolsSym = sharedPreferences.getCustomSymbolsSym();
        String mCustomSymbolsSym2 = sharedPreferences.getCustomSymbolsSym2();
        String mCustomSymbolsSym3 = sharedPreferences.getCustomSymbolsSym3();
        String mCustomSymbolsSym4 = sharedPreferences.getCustomSymbolsSym4();
        String mCustomSymbolsMainBottom = sharedPreferences.getCustomSymbolsMainBottom();

        //Need this to get resources for drawables
        Definitions definitions = new Definitions(this);
        try {
            KeyboardLayoutBuilder builder = new KeyboardLayoutBuilder(this);
            builder.setBox(Box.create(0, 0, 1, 1));

            if (mCurKeyboard == R.integer.keyboard_normal || mCurKeyboard == R.integer.keyboard_boshiamy) {
                if (mPhoneOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (mToprow) {
                        definitions.addCopyPasteRow(builder, mCurKeyboard, true);
                    } else {
                        definitions.addArrowsRow(builder, mCurKeyboard, true, false);
                    }

                    if (!mCustomSymbolsMain.isEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsMain, mCustomSymbolsLongPress, true);
                    }
                    if (!mCustomSymbolsMain2.isEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsMain2, "", true);
                    }
                    Definitions.addQwertyRows(builder);
                    definitions.addCustomSpaceRow(builder, mCustomSymbolsMainBottom, true, false);
                } else {
                    // 第一行
                    if (mToprow) {
                        definitions.addCopyPasteRow(builder, mCurKeyboard, false);
                    } else {
                        definitions.addArrowsRow(builder, mCurKeyboard, false, true);
                    }
                    Definitions.addQwertyRows1(builder, false);

                    // 第二行
                    if (!mCustomSymbolsMain.isEmpty()) { // digits
                        Definitions.addCustomRow(builder, mCustomSymbolsMain, mCustomSymbolsLongPress, true);
                    }
                    Definitions.addQwertyRows2(builder, false);
//                    if (!mCustomSymbolsMain2.isEmpty()) {
//                        Definitions.addCustomRow(builder, mCustomSymbolsMain2, "", true);
//                    }

                    // 第三行
                    definitions.addCustomSpaceRow(builder, mCustomSymbolsMainBottom, true, true);
                    Definitions.addQwertyRows3(builder, false);
                }
            } else {
                if (mToprow) {
                    definitions.addCopyPasteRow(builder, mCurKeyboard, true);
                } else {
                    definitions.addArrowsRow(builder, mCurKeyboard, true, false);
                }

                if (mCurKeyboard == R.integer.keyboard_sym) {
                    if (!mCustomSymbolsSym.isEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsSym, "", true);
                    }
                    if (!mCustomSymbolsSym2.isEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsSym2, "", true);
                    }
                    if (!mCustomSymbolsSym3.isEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsSym3, "", true);
                    }
                    if (!mCustomSymbolsSym4.isEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsSym4, "", true);
                    }
                    if (mCustomSymbolsSym3.isEmpty() && mCustomSymbolsSym4.isEmpty()) {
                        definitions.addSymbolRows(builder);
                    } else {
                        definitions.addCustomSpaceRow(builder, mCustomSymbolsMainBottom, true, false);
                    }
                } else if (mCurKeyboard == R.integer.keyboard_phonetic) {
                    if (!mCustomSymbolsMain2.isEmpty()) {
                        Definitions.addCustomRow(builder, mCustomSymbolsMain2, "", true);
                    }
                    Definitions.addPhoneticRows(builder);
                    definitions.addCustomSpaceRow(builder, mCustomSymbolsMainBottom, true, false);
                } else if (mCurKeyboard == R.integer.keyboard_clipboard) {
                    definitions.addClipboardActions(builder);

                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard.hasPrimaryClip()
                            && clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                        ClipData pr = clipboard.getPrimaryClip();
                        //Android only allows one item in Clipboard
                        String s = pr.getItemAt(0).getText().toString();
                        builder.newRow().addKey(s);
                    } else {
                        builder.newRow().addKey("Nothing copied").withOutputText("");
                    }
                    builder.addKey(sharedPreferences.getPin1());
                    builder.newRow()
                            .addKey(sharedPreferences.getPin2())
                            .addKey(sharedPreferences.getPin3());
                    builder.newRow()
                            .addKey(sharedPreferences.getPin4())
                            .addKey(sharedPreferences.getPin5());
                    builder.newRow()
                            .addKey(sharedPreferences.getPin6())
                            .addKey(sharedPreferences.getPin7());
                }
            }

            Collection<Key> keyboardLayout = builder.build();
            mCurrentKeyboardLayoutView = mKeyboardUiFactory.createKeyboardView(this, keyboardLayout);
            return mCurrentKeyboardLayoutView;

        } catch (KeyboardLayoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mPhoneOrientation = Configuration.ORIENTATION_PORTRAIT;
        } else {
            mPhoneOrientation = Configuration.ORIENTATION_LANDSCAPE;
        }

        return mCandidateView;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mPhoneOrientation = Configuration.ORIENTATION_LANDSCAPE;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            mPhoneOrientation = Configuration.ORIENTATION_PORTRAIT;
        }
    }

    @Override
    public void onUpdateExtractingVisibility(EditorInfo ei) {
        ei.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        super.onUpdateExtractingVisibility(ei);
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        setInputView(onCreateInputView());
        sEditorInfo = attribute;
        turnCandidate(false);
    }

    public void controlKeyUpdateView() {
        mCurrentKeyboardLayoutView.applyCtrlModifier(ctrl);
    }

    public void shiftKeyUpdateView() {
        mCurrentKeyboardLayoutView.applyShiftModifier(shift);
    }

    private void clearLongPressTimer() {
        if (timerLongPress != null) {
            timerLongPress.cancel();
        }
        timerLongPress = null;
    }

    private ThemeInfo setThemeByIndex(KeyboardPreferences keyboardPreferences, int index) {
        ThemeInfo themeInfo = ThemeDefinitions.Default();
        switch (index) {
            case 0:
                switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                    case Configuration.UI_MODE_NIGHT_YES:
                        themeInfo = ThemeDefinitions.MaterialDark();
                        break;
                    case Configuration.UI_MODE_NIGHT_NO:
                        themeInfo = ThemeDefinitions.MaterialWhite();
                        break;
                }
                break;
            case 1:
                themeInfo = ThemeDefinitions.MaterialDark();
                break;
            case 2:
                themeInfo = ThemeDefinitions.MaterialWhite();
                break;
            case 3:
                themeInfo = ThemeDefinitions.PureBlack();
                break;
            case 4:
                themeInfo = ThemeDefinitions.White();
                break;
            case 5:
                themeInfo = ThemeDefinitions.Blue();
                break;
            case 6:
                themeInfo = ThemeDefinitions.Purple();
                break;
            default:
                themeInfo = ThemeDefinitions.Default();
                break;
        }
        keyboardPreferences.setBgColor(String.valueOf(themeInfo.backgroundColor));
        keyboardPreferences.setFgColor(String.valueOf(themeInfo.foregroundColor));
        return themeInfo;
    }

    private ThemeInfo getDefaultThemeInfo() {
        return ThemeDefinitions.Default();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private NotificationReceiver mNotificationReceiver;

    private static final int NOTIFICATION_ONGOING_ID = 1001;

    //Code from Hacker keyboard's source
    private void setNotification(boolean visible) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (visible && mNotificationReceiver == null) {
            CharSequence text = "Keyboard notification enabled.";

            createNotificationChannel();
            mNotificationReceiver = new NotificationReceiver(this);
            final IntentFilter pFilter = new IntentFilter(NotificationReceiver.ACTION_SHOW);
            pFilter.addAction(NotificationReceiver.ACTION_SETTINGS);
            registerReceiver(mNotificationReceiver, pFilter);

            Intent notificationIntent = new Intent(NotificationReceiver.ACTION_SHOW);
            PendingIntent contentIntent =
                    PendingIntent.getBroadcast(getApplicationContext(), 1, notificationIntent, 0);

            Intent configIntent = new Intent(NotificationReceiver.ACTION_SETTINGS);
            PendingIntent configPendingIntent =
                    PendingIntent.getBroadcast(getApplicationContext(), 2, configIntent, 0);

            String title = "Show Codeboard Keyboard";
            String body = "Select this to open the keyboard. Disable in settings.";

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.drawable.icon_large)
                            .setColor(0xff220044)
                            .setAutoCancel(false)
                            .setTicker(text)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setContentIntent(contentIntent)
                            .setOngoing(true)
                            .addAction(R.drawable.icon_large, getString(R.string.notification_action_open_keyboard),
                                    contentIntent)
                            .addAction(R.drawable.icon_large, getString(R.string.notification_action_settings),
                                    configPendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(NOTIFICATION_ONGOING_ID, mBuilder.build());

        } else if (!visible && mNotificationReceiver != null) {
            mNotificationManager.cancel(NOTIFICATION_ONGOING_ID);
            unregisterReceiver(mNotificationReceiver);
            mNotificationReceiver = null;
        }
    }

    @Override
    public AbstractInputMethodImpl onCreateInputMethodInterface() {
        return new MyInputMethodImpl();
    }

    IBinder mToken;

    public class MyInputMethodImpl extends InputMethodImpl {
        @Override
        public void attachToken(IBinder token) {
            super.attachToken(token);
            if (mToken == null) {
                mToken = token;
            }
        }
    }
}
