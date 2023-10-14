/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/

package com.wade.fsime;

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.readystatesoftware.android.sqliteassethelper.BuildConfig;
import com.wade.MathParser.MathParser;
import com.wade.libs.BDatabase;
import com.wade.mil.Mil;
import com.wade.utilities.Contexty;
import com.wade.utilities.Mappy;
import com.wade.utilities.Stringy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
/*
  An InputMethodService for the FS Input Method (混瞎輸入法).
*/
public class FsimeService
        extends InputMethodService
        implements CandidatesViewAdapter.CandidateListener, KeyboardView.KeyboardListener {
    private static final String LOG_TAG = "FsimeService";

    public static final String ANGLE_KEY_VALUE_TEXT = "∠";
    public static final String ENTER_KEY_VALUE_TEXT = "ENTER";
    public static final String SHIFT_KEY_VALUE_TEXT = "SHIFT";
    public static final String CTRL_KEY_VALUE_TEXT = "CTRL";
    private static final String TAB_KEY_VALUE_TEXT = "TAB";
    private static final String TAB_SHIFT_KEY_VALUE_TEXT = "↹";
    private static final String ESC_KEY_VALUE_TEXT = "ESC";
    private static final String BACKSPACE_VALUE_TEXT = "BACKSPACE";
    private static final String SPACE_BAR_VALUE_TEXT = "SPACE";
    private static final String KEYBOARD_NAME_FULL = "full";
    private static final String KEYBOARD_NAME_FSIME = "mix";
    private static final String KEYBOARD_NAME_PURE = "pure";
    private static final String KEYBOARD_NAME_DIGIT= "digit";
    private static final String KEYBOARD_NAME_CJ = "cj";
    private static final String KEYBOARD_NAME_JI = "ji";
    private static final String KEYBOARD_NAME_STROKE = "stroke";
    private static final String KEYBOARD_NAME_MIL = "mil";

    Keyboard fullKB, fsimeKB, pureKB, digitKB, jiKB, cjKB, strokeKB, milKB;
    private static final int BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_ASCII = 50;
    private static final int BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8 = 100;

    public static final String PREFERENCES_FILE_NAME = "preferences.txt";
    private static final String KEYBOARD_NAME_PREFERENCE_KEY = "keyboardName";

    private static final String CHARACTERS_FILE_NAME_TRADITIONAL = "characters-traditional.txt";
    private static final String CHARACTERS_FILE_NAME_SIMPLIFIED = "characters-simplified.txt";
    private static final String RANKING_FILE_NAME_TRADITIONAL = "ranking-traditional.txt";
    private static final String RANKING_FILE_NAME_SIMPLIFIED = "ranking-simplified.txt";
    private static final String PHRASES_FILE_NAME_TRADITIONAL = "phrases-traditional.txt";
    private static final String PHRASES_FILE_NAME_SIMPLIFIED = "phrases-simplified.txt";

    private static final int LAG_PREVENTION_CODE_POINT_COUNT = 1400;
    private static final int LARGISH_SORTING_RANK = 3000;
    private static final int RANKING_PENALTY_PER_CHAR = 2 * LARGISH_SORTING_RANK;
    private static final int RANKING_PENALTY_UNPREFERRED = 10 * LARGISH_SORTING_RANK;
    private static final int MAX_PHRASE_LENGTH = 6;

    private Map<Keyboard, String> nameFromKeyboard;
    private Map<String, Keyboard> keyboardFromName;
    private Set<Keyboard> keyboardSet;

    private InputContainer inputContainer;
    private String mComposing = "";
    private List<String> candidateList = new ArrayList<>();

    private int inputOptionsBits;
    private boolean enterKeyHasAction;
    private boolean inputIsPassword;
    BDatabase bdatabase;
    private Mil mil;
    KeyboardPreferences sharedPreferences;
    Map<Integer,String> codeMaps = new HashMap<Integer, String>();
    final int SWIPE_NONE=0, SWIPE_RU=1, SWIPE_LD=2, SWIPE_LU=4, SWIPE_RD=8;

    @Override
    public void onCreate() {
        super.onCreate();

        mil = new Mil();
        codeMaps.put(KeyEvent.KEYCODE_0, "Ctrl0");
        codeMaps.put(KeyEvent.KEYCODE_1, "Ctrl1");
        codeMaps.put(KeyEvent.KEYCODE_2, "Ctrl2");
        codeMaps.put(KeyEvent.KEYCODE_3, "Ctrl3");
        codeMaps.put(KeyEvent.KEYCODE_4, "Ctrl4");
        codeMaps.put(KeyEvent.KEYCODE_5, "Ctrl5");
        codeMaps.put(KeyEvent.KEYCODE_6, "Ctrl6");
        codeMaps.put(KeyEvent.KEYCODE_7, "Ctrl7");
        codeMaps.put(KeyEvent.KEYCODE_8, "Ctrl8");
        codeMaps.put(KeyEvent.KEYCODE_9, "Ctrl9");
        codeMaps.put(KeyEvent.KEYCODE_Q, "CtrlQ");
        codeMaps.put(KeyEvent.KEYCODE_W, "CtrlW");
        codeMaps.put(KeyEvent.KEYCODE_E, "CtrlE");
        codeMaps.put(KeyEvent.KEYCODE_R, "CtrlR");
        codeMaps.put(KeyEvent.KEYCODE_T, "CtrlT");
        codeMaps.put(KeyEvent.KEYCODE_Y, "CtrlY");
        codeMaps.put(KeyEvent.KEYCODE_U, "CtrlU");
        codeMaps.put(KeyEvent.KEYCODE_I, "CtrlI");
        codeMaps.put(KeyEvent.KEYCODE_O, "CtrlO");
        codeMaps.put(KeyEvent.KEYCODE_P, "CtrlP");
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView() {
        bdatabase = new BDatabase(getApplicationContext());
        fullKB = new Keyboard(this, R.xml.keyboard_full, KEYBOARD_NAME_FULL);
        fsimeKB = new Keyboard(this, R.xml.keyboard_fsime, KEYBOARD_NAME_FSIME);
        pureKB = new Keyboard(this, R.xml.keyboard_pure, KEYBOARD_NAME_PURE);
        digitKB = new Keyboard(this, R.xml.keyboard_digit, KEYBOARD_NAME_DIGIT);
        jiKB = new Keyboard(this, R.xml.keyboard_ji, KEYBOARD_NAME_JI);
        cjKB = new Keyboard(this, R.xml.keyboard_cj, KEYBOARD_NAME_CJ);
        strokeKB = new Keyboard(this, R.xml.keyboard_stroke, KEYBOARD_NAME_STROKE);
        milKB = new Keyboard(this, R.xml.keyboard_mil, KEYBOARD_NAME_MIL);

        nameFromKeyboard = new HashMap<>();
        nameFromKeyboard.put(fullKB, KEYBOARD_NAME_FULL);
        nameFromKeyboard.put(fsimeKB, KEYBOARD_NAME_FSIME);
        nameFromKeyboard.put(pureKB, KEYBOARD_NAME_PURE);
        nameFromKeyboard.put(digitKB, KEYBOARD_NAME_DIGIT);
        nameFromKeyboard.put(jiKB, KEYBOARD_NAME_JI);
        nameFromKeyboard.put(cjKB, KEYBOARD_NAME_CJ);
        nameFromKeyboard.put(strokeKB, KEYBOARD_NAME_STROKE);
        nameFromKeyboard.put(milKB, KEYBOARD_NAME_MIL);
        keyboardFromName = Mappy.invertMap(nameFromKeyboard);
        keyboardSet = nameFromKeyboard.keySet();

        inputContainer = (InputContainer) getLayoutInflater().inflate(R.layout.input_container, null);
        inputContainer.initialiseCandidatesView(this);
        inputContainer.initialiseKeyboardView(this, loadSavedKeyboard());

        sharedPreferences = new KeyboardPreferences(this);
        return inputContainer;
    }

    private void setCandidateOrder() {
        final String candidateOrder = sharedPreferences.candidateOrder();
        bdatabase.setTs(switch (candidateOrder) {
            case "TraditionalOnly" -> 1;
            case "SimplifiedOnly" -> 2;
            default -> 0;
        });
    }
    private Keyboard loadSavedKeyboard() {
        final String savedKeyboardName =
                Contexty.loadPreferenceString(getApplicationContext(), PREFERENCES_FILE_NAME, KEYBOARD_NAME_PREFERENCE_KEY);
        final Keyboard savedKeyboard = keyboardFromName.get(savedKeyboardName);
        if (savedKeyboard != null) {
            return savedKeyboard;
        } else {
            return fullKB;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isCommentLine(final String line) {
        return line.startsWith("#") || line.length() == 0;
    }

    @Override
    public void onStartInput(final EditorInfo editorInfo, final boolean isRestarting) {
        super.onStartInput(editorInfo, isRestarting);

        inputOptionsBits = editorInfo.imeOptions;
        enterKeyHasAction = (inputOptionsBits & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0;

        final int inputTypeBits = editorInfo.inputType;
        final int inputClassBits = inputTypeBits & InputType.TYPE_MASK_CLASS;
        final int inputVariationBits = inputTypeBits & InputType.TYPE_MASK_VARIATION;

        switch (inputClassBits) {
            case InputType.TYPE_CLASS_NUMBER ->
                    inputIsPassword = inputVariationBits == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
            case InputType.TYPE_CLASS_TEXT -> {
                switch (inputVariationBits) {
                    case InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ->
                            inputIsPassword = true;
                    default -> inputIsPassword = false;
                }
            }
            default -> inputIsPassword = false;
        }
    }

    @Override
    public void onStartInputView(final EditorInfo editorInfo, final boolean isRestarting) {
        super.onStartInputView(editorInfo, isRestarting);

        updateFullscreenMode(); // needed in API level 31+ so that fullscreen works after rotate whilst keyboard showing
        final boolean isFullscreen = isFullscreenMode();
        inputContainer.setBackground(isFullscreen);

        inputContainer.setCandidateList(candidateList);

        setEnterKeyDisplayText();
        setCandidateOrder();
    }

    private void setEnterKeyDisplayText() {
        String enterKeyDisplayText = switch (inputOptionsBits & EditorInfo.IME_MASK_ACTION) {
            case EditorInfo.IME_ACTION_DONE -> getString(R.string.display_text__done);
            case EditorInfo.IME_ACTION_GO -> getString(R.string.display_text__go);
            case EditorInfo.IME_ACTION_NEXT -> getString(R.string.display_text__next);
            case EditorInfo.IME_ACTION_PREVIOUS -> getString(R.string.display_text__previous);
            case EditorInfo.IME_ACTION_SEARCH -> getString(R.string.display_text__search);
            case EditorInfo.IME_ACTION_SEND -> getString(R.string.display_text__send);
            default -> null;
        };
        if (!enterKeyHasAction || enterKeyDisplayText == null) {
            enterKeyDisplayText = getString(R.string.display_text__return);
        }

        for (final Keyboard keyboard : keyboardSet) {
            for (final Key key : keyboard.getKeyList()) {
                if (key.valueText.equals(ENTER_KEY_VALUE_TEXT)) {
                    key.displayText = enterKeyDisplayText;
                }
            }
        }
        inputContainer.redrawKeyboard();
    }

    @Override
    public void onComputeInsets(final Insets insets) {
        super.onComputeInsets(insets);
        if (inputContainer != null) // check needed in API level 30
        {
            final int candidatesViewTop = inputContainer.getCandidatesViewTop();
            insets.visibleTopInsets = candidatesViewTop;
            insets.contentTopInsets = candidatesViewTop;
        }
    }

    @Override
    public void onCandidate(final String candidate) {
        final InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }

        inputConnection.commitText(candidate, 1);
        mComposing = "";
    }
    private void keyDownUp(int keyEventCode, int meta) {
        InputConnection ic = getCurrentInputConnection();
        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEventCode, 0, meta));
        ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEventCode, 0, meta));
    }
    private void keyDownUps(int[] keyEventCodes, int meta) {
        InputConnection ic = getCurrentInputConnection();
        for (int i=0; i<keyEventCodes.length; i++) {
            ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEventCodes[i], 0, meta));
        }
        for (int i=keyEventCodes.length-1; i>=0; i--) {
            ic.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEventCodes[i], 0, meta));
        }
    }
    private void turnCandidateOff() {
        mComposing = "";
        inputContainer.setCandidateList(new ArrayList<>());
    }
    private void showMilMessage(String inputText) {
        String btnMsg = mil.getBtnMsg(inputText);
        if (btnMsg.length() > 0) {
            final List<String> list = Collections.singletonList(btnMsg);
            setCandidateList(list);
        }
    }
    public void onKeyMil(final String valueText) {
        final InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) return;
        if (valueText.matches("[0123456789\\+\\-\\*/°'\"()\\.]")) {
            effectStrokeAppendMil(valueText);
            return;
        }

        switch (valueText) {
            case BACKSPACE_VALUE_TEXT:
                effectBackspace(inputConnection);
                break;
            case TAB_KEY_VALUE_TEXT, TAB_SHIFT_KEY_VALUE_TEXT: // 下一欄
                break;
            case ESC_KEY_VALUE_TEXT: // 從第一個欄位開始
                turnCandidateOff();
                break;
            case SPACE_BAR_VALUE_TEXT: // 下一欄與計算
                effectSpaceKey(inputConnection);
                break;

            case ENTER_KEY_VALUE_TEXT: // 無條件送出
                effectEnterKey(inputConnection);
                break;
            case ANGLE_KEY_VALUE_TEXT:
                inputContainer.getKeyboard().setShiftText(ANGLE_KEY_VALUE_TEXT, mil.nextMode());
                break;
            default: // TODO: 功能鍵
                mil.setApp(valueText);
        }
    }
    public void onCtrlKey(final String valueText) {

    }
    @Override
    public void onKey(final String valueText) {
        if (inputContainer.getKeyboard().name.equals(KEYBOARD_NAME_MIL)) {
            onKeyMil(valueText);
            return;
        }

        final InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }

        switch (valueText) {
            case "⎆" -> keyDownUps(new int[]{KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_DEL}, 0);
            case "⇱" -> keyDownUp(KeyEvent.KEYCODE_MOVE_HOME, 0);
            case "⇲" -> keyDownUp(KeyEvent.KEYCODE_MOVE_END, 0);
            case "⇞" -> keyDownUp(KeyEvent.KEYCODE_PAGE_UP, 0);
            case "⇟" -> keyDownUp(KeyEvent.KEYCODE_PAGE_DOWN, 0);
            case "←" -> keyDownUp(KeyEvent.KEYCODE_DPAD_LEFT, 0);
            case "↑" -> keyDownUp(KeyEvent.KEYCODE_DPAD_UP, 0);
            case "↓" -> keyDownUp(KeyEvent.KEYCODE_DPAD_DOWN, 0);
            case "→" -> keyDownUp(KeyEvent.KEYCODE_DPAD_RIGHT, 0);
            case BACKSPACE_VALUE_TEXT -> effectBackspace(inputConnection);
            case TAB_KEY_VALUE_TEXT, TAB_SHIFT_KEY_VALUE_TEXT ->
                    keyDownUp(KeyEvent.KEYCODE_TAB, inputContainer.getKeyboard().shiftMode);
            case ESC_KEY_VALUE_TEXT -> turnCandidateOff();
            case SPACE_BAR_VALUE_TEXT -> effectSpaceKey(inputConnection);
            case ENTER_KEY_VALUE_TEXT -> effectEnterKey(inputConnection);
            default -> {
                if (inputContainer.getKeyboard().ctrlMode != 0) {
                    KeyCharacterMap mKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
                    KeyEvent[] events = mKeyCharacterMap.getEvents(valueText.toCharArray());

                    for (KeyEvent event2 : events) {
                        int keycode = event2.getKeyCode();
                        if (event2.getAction() == 0 && keycode != KeyEvent.KEYCODE_SHIFT_LEFT) {
                            if (codeMaps.containsKey(keycode)) {
                                String hk = sharedPreferences.getHotkey(codeMaps.get(keycode));
                                if (hk.length() > 0) {
                                    effectStrokeAppend(hk);
                                } else if (inputContainer.getKeyboard().shiftMode != 0) {
                                    keyDownUp(keycode, KeyEvent.META_CTRL_ON + KeyEvent.META_SHIFT_ON);
                                }
                                break;
                            }
                            keyDownUp(keycode, KeyEvent.META_CTRL_ON);
                            turnCandidateOff();
                            return;
                        }
                    }
                } else {
                    effectStrokeAppend(valueText);
                }
            }
        }
    }
    @Override
    public void onLongPress(final String inputText) {
        if (inputContainer.getKeyboard().name.equals(KEYBOARD_NAME_MIL)) {
            showMilMessage(inputText);
            return;
        }
        String valueText, shiftText;
        if (inputText.length() == 2) {
            valueText = inputText.substring(0, 1);
            shiftText = inputText.substring(1, 2);
        } else {
            valueText = inputText;
            shiftText = "";
        }
        switch (valueText) {
            case SPACE_BAR_VALUE_TEXT -> Contexty.showSystemKeyboardChanger(this);
            case ESC_KEY_VALUE_TEXT -> {
                final InputConnection inputConnection = getCurrentInputConnection();
                String w = getTextBeforeCursor(inputConnection, 1);
                if (w.length() > 0) {
                    ArrayList<String> comp = bdatabase.getCompose(w.substring(0, 1));
                    comp.add(0, w);
                    setCandidateList(comp);
                }
            }
            default -> {
                if (shiftText.length() > 0) {
                    effectStrokeAppend(shiftText);
                } else {
                    effectStrokeAppend(valueText);
                }
            }
        }
    }

    @Override
    public void onSwipe(final String valueText) {
        Keyboard keyboard = inputContainer.getKeyboard();
        String keyboardName = keyboard.name;

        if (valueText.equals(SPACE_BAR_VALUE_TEXT)) {
            if (keyboardName == null) {
                return;
            }
            if ((keyboard.swipeDir & (SWIPE_RU | SWIPE_RD)) > 0) { // right full > fsime > pure > digit > ji > cj > stroke > mil
                keyboard = switch (keyboardName) {
                    case KEYBOARD_NAME_FULL -> keyboardFromName.get(KEYBOARD_NAME_FSIME);
                    case KEYBOARD_NAME_FSIME -> keyboardFromName.get(KEYBOARD_NAME_PURE);
                    case KEYBOARD_NAME_PURE -> keyboardFromName.get(KEYBOARD_NAME_DIGIT);
                    case KEYBOARD_NAME_DIGIT -> keyboardFromName.get(KEYBOARD_NAME_JI);
                    case KEYBOARD_NAME_JI -> keyboardFromName.get(KEYBOARD_NAME_CJ);
                    case KEYBOARD_NAME_CJ -> keyboardFromName.get(KEYBOARD_NAME_STROKE);
                    case KEYBOARD_NAME_STROKE -> keyboardFromName.get(KEYBOARD_NAME_MIL);
                    default -> keyboardFromName.get(KEYBOARD_NAME_FULL);
                };
            } else if ((keyboard.swipeDir & (SWIPE_LD | SWIPE_LU)) > 0) { // left  mil > stroke > cj > ji > digit > pure > fsime > full
                keyboard = switch (keyboardName) {
                    case KEYBOARD_NAME_MIL -> keyboardFromName.get(KEYBOARD_NAME_STROKE);
                    case KEYBOARD_NAME_STROKE -> keyboardFromName.get(KEYBOARD_NAME_CJ);
                    case KEYBOARD_NAME_CJ -> keyboardFromName.get(KEYBOARD_NAME_JI);
                    case KEYBOARD_NAME_JI -> keyboardFromName.get(KEYBOARD_NAME_DIGIT);
                    case KEYBOARD_NAME_DIGIT -> keyboardFromName.get(KEYBOARD_NAME_PURE);
                    case KEYBOARD_NAME_PURE -> keyboardFromName.get(KEYBOARD_NAME_FSIME);
                    case KEYBOARD_NAME_FSIME -> keyboardFromName.get(KEYBOARD_NAME_FULL);
                    default -> keyboardFromName.get(KEYBOARD_NAME_MIL);
                };
            }
            inputContainer.setKeyboard(keyboard);
            inputContainer.redrawKeyboard();
            return;
        } else {
            effectStrokeAppend(valueText);
        }
    }

    private List<String> computeCandidateList(final String mComposing) {
        if (inputContainer.getKeyboard().name.equals(KEYBOARD_NAME_MIL) || mComposing.length() == 0) {
            return Collections.emptyList();
        }

        return bdatabase.getWord(mComposing, 0, 30, inputContainer.getKeyboard().name);
    }
    private void effectStrokeAppendMil(final String key) {
        final String exp = mComposing + key;
        final MathParser parser = MathParser.create();
        final List<String> list = new ArrayList<>();
        list.add(exp);
        String[] exps = exp.split(";");
        try {
            for (int i=0; i<exps.length-1; i++) {
                parser.addExpression(exps[i]);
            }
            double res = parser.parse(exps[exps.length-1]);
            list.add(Double.toString(res));
        } catch (Exception e) {
        }
        mComposing = exp;
        setCandidateList(list);
    }

    private void effectStrokeAppend(final String key) {
        final String newInputSequence = mComposing + key;
        final List<String> list = computeCandidateList(newInputSequence);
        if (list.size() == 1) {
            MathParser parser = MathParser.create();
            try {
                String exp = list.get(0);
                String[] exps = exp.split(";");
                for (int i=0; i<exps.length-1; i++) {
                    parser.addExpression(exps[i]);
                }
                double res = parser.parse(exps[exps.length-1]);
                list.add(Double.toString(res));
            } catch (Exception e) {

            }
        }
        mComposing = newInputSequence;
        setCandidateList(list);
    }
    private void effectBackspace(final InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            mComposing = Stringy.removeSuffixRegex("(?s).", mComposing);
            final List<String> newCandidateList = computeCandidateList(mComposing);

            setCandidateList(newCandidateList);

            inputContainer.setKeyRepeatIntervalMilliseconds(BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8);
        } else {
            final String upToOneCharacterBeforeCursor = getTextBeforeCursor(inputConnection, 1);

            if (upToOneCharacterBeforeCursor.length() > 0) {
                final CharSequence selection = inputConnection.getSelectedText(0);
                if (TextUtils.isEmpty(selection)) {
                    inputConnection.deleteSurroundingTextInCodePoints(1, 0);
                } else {
                    inputConnection.commitText("", 1);
                }
            } else if (inputContainer.getKeyboard().shiftMode != 0) {
                keyDownUp(KeyEvent.KEYCODE_DEL, KeyEvent.META_SHIFT_ON);
            } else {
                keyDownUp(KeyEvent.KEYCODE_DEL, 0);
            }

            final int nextBackspaceIntervalMilliseconds =
                    (Stringy.isAscii(upToOneCharacterBeforeCursor))
                            ? BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_ASCII
                            : BACKSPACE_REPEAT_INTERVAL_MILLISECONDS_UTF_8;
            inputContainer.setKeyRepeatIntervalMilliseconds(nextBackspaceIntervalMilliseconds);
        }
    }

    private void effectSpaceKey(final InputConnection inputConnection) {
        if (mComposing.length() > 0 && candidateList.size() > 1) {
            onCandidate(getCandidate(1));
        } else if (candidateList.size() > 0) {
            onCandidate(getCandidate(0));
        } else {
            inputConnection.commitText(" ", 1);
        }
    }

    private void effectEnterKey(final InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            onCandidate(getCandidate(0));
        } else if (enterKeyHasAction) {
            inputConnection.performEditorAction(inputOptionsBits);
        } else {
            inputConnection.commitText("\n", 1);
        }
    }

    @Override
    public void saveKeyboard(final Keyboard keyboard) {
        final String keyboardName = nameFromKeyboard.get(keyboard);
        Contexty.savePreferenceString(
                getApplicationContext(),
                PREFERENCES_FILE_NAME,
                KEYBOARD_NAME_PREFERENCE_KEY,
                keyboardName
        );
    }

    private void setCandidateList(final List<String> candidateList) {
        this.candidateList = candidateList;
        inputContainer.setCandidateList(candidateList);
    }

    private String getCandidate(int idx) {
        try {
            if (candidateList.size() > idx) {
                return candidateList.get(idx);
            } else {
                return "";
            }
        } catch (IndexOutOfBoundsException exception) {
            return "";
        }
    }

    private String getTextBeforeCursor(final InputConnection inputConnection, final int characterCount) {
        if (inputIsPassword) {
            return ""; // don't read passwords
        }

        final String textBeforeCursor = (String) inputConnection.getTextBeforeCursor(characterCount, 0);

        if (textBeforeCursor != null) {
            return textBeforeCursor;
        } else {
            return "";
        }
    }
}
