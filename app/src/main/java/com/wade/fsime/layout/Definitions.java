package com.wade.fsime.layout;

import android.content.Context;
import android.util.Log;

import com.wade.fsime.R;
import com.wade.fsime.layout.builder.KeyboardLayoutBuilder;

public class Definitions {
    private Context context;
    private static final int CODE_ESCAPE = -2;
    private static final int CODE_SYMBOLS = -1;

    public Definitions(Context current) {
        this.context = current;
    }

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
                    .addBackspaceKey().withSize(2f)
                    .addKey(SYM, CODE_SYMBOLS).onCtrlShow("剪").withSize(1.6f);
        } else {
            keyboard.addKey("Esc", CODE_ESCAPE)
                    .addTabKey()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_left_24dp), CODE_ARROW_LEFT).asRepeatable()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_down_24dp), CODE_ARROW_DOWN).asRepeatable()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_up_24dp), CODE_ARROW_UP).asRepeatable()
                    .addKey(context.getDrawable(R.drawable.ic_keyboard_arrow_right_24dp), CODE_ARROW_RIGHT).asRepeatable()
                    .addBackspaceKey()
                    .addKey(SYM, CODE_SYMBOLS).onCtrlShow("剪");
        }
    }

    public void addCopyPasteRow(KeyboardLayoutBuilder keyboard, int mKeyboardState, boolean newRow) {
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
        keyboard.addKey("Esc", CODE_ESCAPE)
                .addTabKey()
                .addKey(context.getDrawable(R.drawable.ic_select_all_24dp), 53737)  // Left <
                .addKey(context.getDrawable(R.drawable.ic_cut_24dp), 53738)         // Down v
                .addKey(context.getDrawable(R.drawable.ic_copy_24dp), 53739)        // Up >
                .addKey(context.getDrawable(R.drawable.ic_paste_24dp), 53740)       // Right >
                .addBackspaceKey()
                .addKey(SYM, CODE_SYMBOLS).onCtrlShow("剪");
    }


    public static void addCustomRow(KeyboardLayoutBuilder keyboard, String symbols, String longPress, boolean newRow) {
        if (newRow) {
            keyboard.newRow();
        }
        char[] chars = symbols.toCharArray();
        char[] charl = longPress.toCharArray();
        for (int i=0; i<chars.length; i++) {
            if (i < charl.length) {
                keyboard.addKey(chars[i]).withLongPress("" + charl[i]);
            } else {
                keyboard.addKey(chars[i]);
            }
        }
    }

    public static void addQwertyRows1(KeyboardLayoutBuilder keyboard, boolean newRow) {
        if (newRow) {
            keyboard.newRow();
        }
        keyboard.addKey('`').withLongPress("~")
                .addKey('q').onShiftUppercase().withLongPress("Q")
                .addKey('w').onShiftUppercase().withLongPress("W")
                .addKey('e').onShiftUppercase().withLongPress("E")
                .addKey('r').onShiftUppercase().withLongPress("R")
                .addKey('t').onShiftUppercase().withLongPress("T")
                .addKey('y').onShiftUppercase().withLongPress("Y")
                .addKey('u').onShiftUppercase().withLongPress("U")
                .addKey('i').onShiftUppercase().withLongPress("I")
                .addKey('o').onShiftUppercase().withLongPress("O")
                .addKey('p').onShiftUppercase().withLongPress("P")
                .addKey('[').withLongPress("{");
    }

    public static void addQwertyRows2(KeyboardLayoutBuilder keyboard, boolean newRow) {
        if (newRow) {
            keyboard.newRow();
        }
        keyboard.addKey('\\').withLongPress("|")
                .addKey('a').onShiftUppercase().withLongPress("A")
                .addKey('s').onShiftUppercase().withLongPress("S")
                .addKey('d').onShiftUppercase().withLongPress("D")
                .addKey('f').onShiftUppercase().withLongPress("F")
                .addKey('g').onShiftUppercase().withLongPress("G")
                .addKey('h').onShiftUppercase().withLongPress("H")
                .addKey('j').onShiftUppercase().withLongPress("J")
                .addKey('k').onShiftUppercase().withLongPress("K")
                .addKey('l').onShiftUppercase().withLongPress("L")
                .addKey(']').withLongPress("}");
    }

    public static void addQwertyRows3(KeyboardLayoutBuilder keyboard, boolean newRow) {
        if (newRow) {
            keyboard.newRow();
        }
        keyboard.addShiftKey()
                .addKey('z').onShiftUppercase().withLongPress("Z")
                .addKey('x').onShiftUppercase().withLongPress("X")
                .addKey('c').onShiftUppercase().withLongPress("C")
                .addKey('v').onShiftUppercase().withLongPress("V")
                .addKey('b').onShiftUppercase().withLongPress("B")
                .addKey('n').onShiftUppercase().withLongPress("N")
                .addKey('m').onShiftUppercase().withLongPress("M")
                .addKey(';').withLongPress(":")
                .addKey('\'').withLongPress("\"");
    }

    public static void addQwertyRows(KeyboardLayoutBuilder keyboard) {
        keyboard.newRow()
                .addKey('`').withLongPress("~")
                .addKey('q').onShiftUppercase().withLongPress("Q")
                .addKey('w').onShiftUppercase().withLongPress("W")
                .addKey('e').onShiftUppercase().withLongPress("E")
                .addKey('r').onShiftUppercase().withLongPress("R")
                .addKey('t').onShiftUppercase().withLongPress("T")
                .addKey('y').onShiftUppercase().withLongPress("Y")
                .addKey('u').onShiftUppercase().withLongPress("U")
                .addKey('i').onShiftUppercase().withLongPress("I")
                .addKey('o').onShiftUppercase().withLongPress("O")
                .addKey('p').onShiftUppercase().withLongPress("P")
                .addKey('[').withLongPress("{")
                .newRow()
                .addKey('\\').withLongPress("|")
                .addKey('a').onShiftUppercase().withLongPress("A")
                .addKey('s').onShiftUppercase().withLongPress("S")
                .addKey('d').onShiftUppercase().withLongPress("D")
                .addKey('f').onShiftUppercase().withLongPress("F")
                .addKey('g').onShiftUppercase().withLongPress("G")
                .addKey('h').onShiftUppercase().withLongPress("H")
                .addKey('j').onShiftUppercase().withLongPress("J")
                .addKey('k').onShiftUppercase().withLongPress("K")
                .addKey('l').onShiftUppercase().withLongPress("L")
                .addKey(']').withLongPress("}")
                .newRow()
                .addShiftKey()
                .addKey('z').onShiftUppercase().withLongPress("Z")
                .addKey('x').onShiftUppercase().withLongPress("X")
                .addKey('c').onShiftUppercase().withLongPress("C")
                .addKey('v').onShiftUppercase().withLongPress("V")
                .addKey('b').onShiftUppercase().withLongPress("B")
                .addKey('n').onShiftUppercase().withLongPress("N")
                .addKey('m').onShiftUppercase().withLongPress("M")
                .addKey(';').withLongPress(":")
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

    public void addCustomSpaceRow(KeyboardLayoutBuilder keyboard, String symbols, boolean newRow, boolean split) {
        char[] chars = symbols.toCharArray();

        if (newRow) {
            keyboard.newRow();
        }
        if (!split) {
            keyboard.addKey("Ctrl", 17).asModifier().onCtrlShow("CTRL");
            keyboard.addKey("-").withLongPress("_");
            keyboard.addKey("=").withLongPress("+");
            keyboard.addKey(context.getDrawable(R.drawable.ic_space_bar_24dp), 32).withSize(2f);

            int half = (chars.length + 1) / 2;
            for (int i = 0; i < half && chars.length > 0; i++) {
                if ((i + half) < chars.length) {
                    keyboard.addKey(chars[i]).withLongPress("" + chars[i + half]).withSize(.7f);
                } else {
                    keyboard.addKey(chars[i]).withSize(.7f);
                }
            }
            keyboard.addEnterKey();
        } else {
            keyboard.addKey("Ctrl", 17).asModifier().onCtrlShow("CTRL").withSize(1.2f);
            keyboard.addKey("-").withLongPress("_");
            keyboard.addKey("=").withLongPress("+");
            keyboard.addKey(context.getDrawable(R.drawable.ic_space_bar_24dp), 32).withSize(2f);

            int half = (chars.length + 1) / 2;
            for (int i = 0; i < half && chars.length > 0; i++) {
                if ((i+half) < chars.length) {
                    keyboard.addKey(chars[i]).withLongPress("" + chars[i + half]).withSize(1.0f);
                } else {
                    keyboard.addKey(chars[i]).withSize(1.0f);
                }
            }
            keyboard.addEnterKey().withSize(1.4f);
        }
    }

}
