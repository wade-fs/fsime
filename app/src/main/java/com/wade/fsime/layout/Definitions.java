package com.wade.fsime.layout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

import com.wade.fsime.R;
import com.wade.fsime.layout.builder.KeyboardLayoutBuilder;

public class Definitions {
    private Context context;
    private static final int CODE_ESCAPE = -2;
    private static final int CODE_SYMBOLS = -1;

    public Definitions(Context current) {
        this.context = current;
    }

    // split 是橫放，分成左右兩半，所以每個按鍵的大小不一樣
    @SuppressLint("UseCompatLoadingForDrawables")
    public void addArrowsRow(KeyboardLayoutBuilder keyboard, int mKeyboardState, boolean newRow, boolean split) {
        int CODE_ARROW_LEFT = 5000;
        int CODE_ARROW_DOWN = 5001;
        int CODE_ARROW_UP = 5002;
        int CODE_ARROW_RIGHT = 5003;
        String SYM = "英";
        if (mKeyboardState == R.integer.keyboard_boshiamy) {
            SYM = "嘸";
        } else if (mKeyboardState == R.integer.keyboard_phonetic) {
            SYM = "注";
        } else if (mKeyboardState == R.integer.keyboard_sym) {
            SYM = "符";
        } else if (mKeyboardState == R.integer.keyboard_clipboard) {
            SYM = "剪";
        }
        if (newRow) {
            keyboard.newRow();
        }
        if (split) {
            keyboard.addKey("Esc", CODE_ESCAPE).withSize(1.6f)
                    .addTabKey().withSize(1.6f)
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_left_24dp), CODE_ARROW_LEFT).asRepeatable()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_down_24dp), CODE_ARROW_DOWN).asRepeatable()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_up_24dp), CODE_ARROW_UP).asRepeatable()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_right_24dp), CODE_ARROW_RIGHT).asRepeatable()
                    .addBackspaceKey().withSize(2f).asRepeatable()
                    .addKey(SYM, CODE_SYMBOLS).withSize(1.6f);
        } else {
            keyboard.addKey("Esc", CODE_ESCAPE)
                    .addTabKey()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_left_24dp), CODE_ARROW_LEFT).asRepeatable()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_down_24dp), CODE_ARROW_DOWN).asRepeatable()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_up_24dp), CODE_ARROW_UP).asRepeatable()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_right_24dp), CODE_ARROW_RIGHT).asRepeatable()
                    .addBackspaceKey().asRepeatable()
                    .addKey(SYM, CODE_SYMBOLS);
        }
    }

    // 續上，newRow 為真不同行，為假續行
    @SuppressLint({"UseCompatLoadingForDrawables", "NonConstantResourceId"})
    public void addCopyPasteRow(KeyboardLayoutBuilder keyboard, int mKeyboardState, boolean newRow) {
        String SYM = "英";
        switch (mKeyboardState) {
            case R.integer.keyboard_boshiamy: SYM = "嘸"; break;
            case R.integer.keyboard_phonetic: SYM = "注"; break;
            case R.integer.keyboard_sym: SYM = "符"; break;
            case R.integer.keyboard_clipboard: SYM = "剪"; break;
        }
        if (newRow) {
            keyboard.newRow();
        }
        keyboard.addKey("Esc", CODE_ESCAPE)
                .addTabKey().asRepeatable()
                .addKey(context.getDrawable(R.drawable.ic_select_all_24dp), 53737).asRepeatable()  // Left <
                .addKey(context.getDrawable(R.drawable.ic_cut_24dp), 53738).asRepeatable()         // Down v
                .addKey(context.getDrawable(R.drawable.ic_copy_24dp), 53739).asRepeatable()        // Up >
                .addKey(context.getDrawable(R.drawable.ic_paste_24dp), 53740).asRepeatable()       // Right >
                .addBackspaceKey().asRepeatable()
                .addKey(SYM, CODE_SYMBOLS);
    }

    // 自定義行，常用的是數字行, 與輸入法本身無關
    public static void addCustomRow(KeyboardLayoutBuilder keyboard, String symbols, String longPress, boolean newRow) {
        if (newRow) {
            keyboard.newRow();
        }
        char[] chars = symbols.toCharArray();
        char[] charl = longPress.toCharArray();
        for (int i=0; i<chars.length; i++) {
            if (i < charl.length) {
                keyboard.addKey(chars[i]).onShiftUppercase().withLongPress("" + charl[i]);
            } else {
                keyboard.addKey(chars[i]);
            }
        }
    }

    public static void addDigits(KeyboardLayoutBuilder keyboard, boolean newRow) {
        if (newRow) {
            keyboard.newRow();
        }
        keyboard.addKey('1').withLongPress("!").withJi("ㄅ")
                .addKey('2').withLongPress("@").withJi("ㄉ")
                .addKey('3').withLongPress("#").withJi("ˇ")
                .addKey('4').withLongPress("$").withJi("ˋ")
                .addKey('5').withLongPress("%").withJi("ㄓ")
                .addKey('6').withLongPress("^").withJi("ˊ")
                .addKey('7').withLongPress("&").withJi("˙")
                .addKey('8').withLongPress("*").withJi("ㄚ")
                .addKey('9').withLongPress("(").withJi("ㄞ")
                .addKey('0').withLongPress(")").withJi("ㄢ");
    }

    public static void addQwertyRows1(KeyboardLayoutBuilder keyboard, boolean newRow) {
        if (newRow) {
            keyboard.newRow();
        }
        keyboard.addKey('`').withLongPress("~")
                .addKey('q').onShiftUppercase().withLongPress("Q").withCj("手").withJi("ㄆ")
                .addKey('w').onShiftUppercase().withLongPress("W").withCj("田").withJi("ㄊ")
                .addKey('e').onShiftUppercase().withLongPress("E").withCj("水").withJi("ㄍ")
                .addKey('r').onShiftUppercase().withLongPress("R").withCj("口").withJi("ㄐ")
                .addKey('t').onShiftUppercase().withLongPress("T").withCj("廿").withJi("ㄔ")
                .addKey('y').onShiftUppercase().withLongPress("Y").withCj("卜").withJi("ㄗ")
                .addKey('u').onShiftUppercase().withLongPress("U").withCj("山").withJi("一")
                .addKey('i').onShiftUppercase().withLongPress("I").withCj("戈").withJi("ㄛ")
                .addKey('o').onShiftUppercase().withLongPress("O").withCj("人").withJi("ㄟ")
                .addKey('p').onShiftUppercase().withLongPress("P").withCj("心").withJi("ㄣ")
                .addKey('[').withLongPress("{");
    }

    public static void addQwertyRows2(KeyboardLayoutBuilder keyboard, boolean newRow) {
        if (newRow) {
            keyboard.newRow();
        }
        keyboard.addKey('\\').withLongPress("|")
                .addKey('a').onShiftUppercase().withLongPress("A").withCj("日").withJi("ㄇ")
                .addKey('s').onShiftUppercase().withLongPress("S").withCj("尸").withJi("ㄋ")
                .addKey('d').onShiftUppercase().withLongPress("D").withCj("木").withJi("ㄎ")
                .addKey('f').onShiftUppercase().withLongPress("F").withCj("火").withJi("ㄑ")
                .addKey('g').onShiftUppercase().withLongPress("G").withCj("土").withJi("ㄕ")
                .addKey('h').onShiftUppercase().withLongPress("H").withCj("竹").withJi("ㄘ")
                .addKey('j').onShiftUppercase().withLongPress("J").withCj("十").withJi("ㄨ")
                .addKey('k').onShiftUppercase().withLongPress("K").withCj("大").withJi("ㄜ")
                .addKey('l').onShiftUppercase().withLongPress("L").withCj("中").withJi("ㄠ")
                .addKey(']').withLongPress("}");
    }

    public static void addQwertyRows3(KeyboardLayoutBuilder keyboard, boolean newRow) {
        if (newRow) {
            keyboard.newRow();
        }
        keyboard.addShiftKey()
                .addKey('z').onShiftUppercase().withLongPress("Z").withCj("重").withJi("ㄈ")
                .addKey('x').onShiftUppercase().withLongPress("X").withCj("難").withJi("ㄌ")
                .addKey('c').onShiftUppercase().withLongPress("C").withCj("金").withJi("ㄏ")
                .addKey('v').onShiftUppercase().withLongPress("V").withCj("女").withJi("ㄒ")
                .addKey('b').onShiftUppercase().withLongPress("B").withCj("月").withJi("ㄖ")
                .addKey('n').onShiftUppercase().withLongPress("N").withCj("弓").withJi("ㄙ")
                .addKey('m').onShiftUppercase().withLongPress("M").withCj("一").withJi("ㄩ")
                .addKey(';').withLongPress(":").withJi("ㄤ")
                .addKey('\'').withLongPress("\"");
    }

    public static void addQwertyRows(KeyboardLayoutBuilder keyboard) {
        keyboard.newRow()
                .addKey('`').withLongPress("~")
                .addKey('q').onShiftUppercase().withLongPress("Q").withCj("手").withJi("ㄆ")
                .addKey('w').onShiftUppercase().withLongPress("W").withCj("田").withJi("ㄊ")
                .addKey('e').onShiftUppercase().withLongPress("E").withCj("水").withJi("ㄍ")
                .addKey('r').onShiftUppercase().withLongPress("R").withCj("口").withJi("ㄐ")
                .addKey('t').onShiftUppercase().withLongPress("T").withCj("廿").withJi("ㄔ")
                .addKey('y').onShiftUppercase().withLongPress("Y").withCj("卜").withJi("ㄗ")
                .addKey('u').onShiftUppercase().withLongPress("U").withCj("山").withJi("一")
                .addKey('i').onShiftUppercase().withLongPress("I").withCj("戈").withJi("ㄛ")
                .addKey('o').onShiftUppercase().withLongPress("O").withCj("人").withJi("ㄟ")
                .addKey('p').onShiftUppercase().withLongPress("P").withCj("心").withJi("ㄣ")
                .addKey('[').withLongPress("{")
                .newRow()
                .addKey('\\').withLongPress("|")
                .addKey('a').onShiftUppercase().withLongPress("A").withCj("日").withJi("ㄇ")
                .addKey('s').onShiftUppercase().withLongPress("S").withCj("尸").withJi("ㄋ")
                .addKey('d').onShiftUppercase().withLongPress("D").withCj("木").withJi("ㄎ")
                .addKey('f').onShiftUppercase().withLongPress("F").withCj("火").withJi("ㄑ")
                .addKey('g').onShiftUppercase().withLongPress("G").withCj("土").withJi("ㄕ")
                .addKey('h').onShiftUppercase().withLongPress("H").withCj("竹").withJi("ㄘ")
                .addKey('j').onShiftUppercase().withLongPress("J").withCj("十").withJi("ㄨ")
                .addKey('k').onShiftUppercase().withLongPress("K").withCj("大").withJi("ㄜ")
                .addKey('l').onShiftUppercase().withLongPress("L").withCj("中").withJi("ㄠ")
                .addKey(']').withLongPress("}")
                .newRow()
                .addShiftKey()
                .addKey('z').onShiftUppercase().withLongPress("Z").withCj("重").withJi("ㄈ")
                .addKey('x').onShiftUppercase().withLongPress("X").withCj("難").withJi("ㄌ")
                .addKey('c').onShiftUppercase().withLongPress("C").withCj("金").withJi("ㄏ")
                .addKey('v').onShiftUppercase().withLongPress("V").withCj("女").withJi("ㄒ")
                .addKey('b').onShiftUppercase().withLongPress("B").withCj("月").withJi("ㄖ")
                .addKey('n').onShiftUppercase().withLongPress("N").withCj("弓").withJi("ㄙ")
                .addKey('m').onShiftUppercase().withLongPress("M").withCj("一").withJi("ㄩ")
                .addKey(';').withLongPress(":").withJi("ㄤ")
                .addKey('\'').withLongPress("\"");
    }

    public static void addPhoneticRows(KeyboardLayoutBuilder keyboard) {
        keyboard.newRow()       // 1234567890-=
                .addKey('ㄅ').withLongPress("1")
                .addKey('ㄉ').withLongPress("2")
                .addKey('ˇ').withLongPress("3")
                .addKey('ˋ').withLongPress("4")
                .addKey('ㄓ').withLongPress("5")
                .addKey('ˊ').withLongPress("6")
                .addKey('˙').withLongPress("7")
                .addKey('ㄚ').withLongPress("8")
                .addKey('ㄞ').withLongPress("9")
                .addKey('ㄢ').withLongPress("0")
                .addKey('ㄦ').withLongPress("-")
                .addKey("-").withLongPress("_")
                .addKey("=").withLongPress("+")

                .newRow() // qwertyuiop
                .addKey("\\").withLongPress("|")
                .addKey('ㄆ').withLongPress("q")
                .addKey('ㄊ').withLongPress("w")
                .addKey('ㄍ').withLongPress("e")
                .addKey('ㄐ').withLongPress("r")
                .addKey('ㄔ').withLongPress("t")
                .addKey('ㄗ').withLongPress("y")
                .addKey('一').withLongPress("u")
                .addKey('ㄛ').withLongPress("i")
                .addKey('ㄟ').withLongPress("o")
                .addKey("ㄣ", 12579).withLongPress("p")
                .addKey("[").withLongPress("{")
                .addKey("]").withLongPress("}")

                .newRow()       // asdfghjkl;
                .addKey("`").withLongPress("~")
                .addKey('ㄇ').withLongPress("a")
                .addKey('ㄋ').withLongPress("s")
                .addKey('ㄎ').withLongPress("d")
                .addKey("ㄑ", 12561).withLongPress("f")
                .addKey('ㄕ').withLongPress("g")
                .addKey('ㄘ').withLongPress("h")
                .addKey('ㄨ').withLongPress("j")
                .addKey('ㄜ').withLongPress("k")
                .addKey('ㄠ').withLongPress("l")
                .addKey("ㄤ", 12580).withLongPress(";")
                .addKey("'").withLongPress("\"")
                .addKey("#").withLongPress(":")
                .addKey("$").withLongPress("%")

                .newRow()       // zxcvbnm,./
                .addShiftKey()
                .addKey('ㄈ').withLongPress("z")
                .addKey('ㄌ').withLongPress("x")
                .addKey('ㄏ').withLongPress("c")
                .addKey('ㄒ').withLongPress("v")
                .addKey('ㄖ').withLongPress("b")
                .addKey('ㄙ').withLongPress("n")
                .addKey('ㄩ').withLongPress("m")
                .addKey('ㄝ').withLongPress("!")
                .addKey('ㄡ').withLongPress("@")
                .addKey("ㄥ", 12581).withLongPress("?")
        ;
    }

    public void addSymbolRows(KeyboardLayoutBuilder keyboard) {
        keyboard.newRow()
                .addKey("Home", -18)
                .addKey("End", -19)
                .addKey("Del", -21)
                .addKey("PgUp", -22)
                .addKey("PgDn", -23)
                .newRow()
                .addShiftKey()
                .addKey("F1", -6)
                .addKey("F2", -7)
                .addKey("F3", -8)
                .addKey("F4", -9)
                .addKey("F5", -10)
                .addKey("F6", -11)
                .addKey("F7", -12)
                .newRow()
                .addKey("Ctrl", 17).asModifier().onCtrlShow("CTRL")
                .addKey("F8", -13)
                .addKey("F9", -14)
                .addKey("F10", -15)
                .addKey(context.getDrawable(R.drawable.ic_space_bar_24dp), 32).withSize(2f)
                .addKey("F11", -16)
                .addKey("F12", -17)
                .addEnterKey()
        ;

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void addClipboardActions(KeyboardLayoutBuilder keyboard) {
        keyboard.newRow()
                .addKey(context.getDrawable(R.drawable.ic_select_all_24dp), 53737)
                .addKey(context.getDrawable(R.drawable.ic_cut_24dp), 53738)
                .addKey(context.getDrawable(R.drawable.ic_copy_24dp), 53739)
                .addKey(context.getDrawable(R.drawable.ic_paste_24dp), 53740)
                .addKey(context.getDrawable(R.drawable.ic_undo_24dp), 53741)
                .addKey(context.getDrawable(R.drawable.ic_redo_24dp), 53742)
        ;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void addCustomSpaceRow(KeyboardLayoutBuilder keyboard, boolean newRow, boolean split) {
        if (newRow) {
            keyboard.newRow();
        }
        if (!split) {
            keyboard.addKey("Ctrl", 17).asModifier().onCtrlShow("CTRL")
            .addKey('-').withLongPress("_").withJi("ㄦ")
            .addKey('=').withLongPress("+")
            .addKey(context.getDrawable(R.drawable.ic_space_bar_24dp), 32).withSize(2f)
            .addKey(',').withLongPress("<").withJi("ㄝ")
            .addKey('.').withLongPress(">").withJi("ㄡ")
            .addKey('/').withLongPress("?").withJi("ㄥ")
            .addEnterKey();
        } else {
            keyboard.addKey("Ctrl", 17).asModifier().onCtrlShow("CTRL").withSize(1.2f)
            .addKey('-').withLongPress("_").withJi("ㄦ")
            .addKey('=').withLongPress("+")
            .addKey(context.getDrawable(R.drawable.ic_space_bar_24dp), 32).withSize(2f)
            .addKey(',').withLongPress("<").withJi("ㄝ")
            .addKey('.').withLongPress(">").withJi("ㄡ")
            .addKey('/').withLongPress("?").withJi("ㄥ")
            .addEnterKey().withSize(1.4f);
        }
    }

}
