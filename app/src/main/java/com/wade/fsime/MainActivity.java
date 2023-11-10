/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/

package com.wade.fsime;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wade.utilities.Contexty;

import java.util.HashMap;
import java.util.Map;

/*
  The main activity of the application.
*/
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String CANDIDATE_ORDER_PREFERENCE_KEY = "candidateOrderPreference";
    public static final String CANDIDATE_ORDER_PREFER_TRADITIONAL_FIRST = "TRADITIONAL_FIRST";
    public static final String CANDIDATE_ORDER_PREFER_SIMPLIFIED_FIRST = "SIMPLIFIED_FIRST";
    KeyboardPreferences sharedPreferences;

    Map<Integer, String> id2Key = new HashMap<Integer, String>();
    Map<Integer, String> id2Use = new HashMap<Integer, String>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        findViewById(R.id.input_method_settings_button).setOnClickListener(this);
        findViewById(R.id.change_keyboard_button).setOnClickListener(this);
        findViewById(R.id.candidate_order_button).setOnClickListener(this);
        findViewById(R.id.test_input).requestFocus();
        sharedPreferences = new KeyboardPreferences(this);
        setCandidateOrderButtonText();
        int[] hkIds = new int[]{R.id.Ctrl1, R.id.Ctrl2, R.id.Ctrl3, R.id.Ctrl4, R.id.Ctrl5,
                R.id.Ctrl6, R.id.Ctrl7, R.id.Ctrl8, R.id.Ctrl9, R.id.Ctrl0,
                R.id.CtrlQ, R.id.CtrlW, R.id.CtrlE, R.id.CtrlR, R.id.CtrlT,
                R.id.CtrlY, R.id.CtrlU, R.id.CtrlI, R.id.CtrlO, R.id.CtrlP
        };
        String[] hkKeys = new String[]{
                "Ctrl1", "Ctrl2", "Ctrl3", "Ctrl4", "Ctrl5",
                "Ctrl6", "Ctrl7", "Ctrl8", "Ctrl9", "Ctrl0",
                "CtrlQ", "CtrlW", "CtrlE", "CtrlR", "CtrlT",
                "CtrlY", "CtrlU", "CtrlI", "CtrlO", "CtrlP"
        };
        for (int i = 0; i < hkIds.length; i++) {
            id2Key.put(hkIds[i], hkKeys[i]);
        }
        for (int id : hkIds) {
            EditText etHk = findViewById(id);
            String hkStr = sharedPreferences.getHotkey(id2Key.get(etHk.getId()));
            etHk.setText(hkStr);
            etHk.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    EditText et = (EditText) v;
                    if (!hasFocus) {
                        sharedPreferences.write(id2Key.get(et.getId()), et.getText().toString());
                    }
                }
            });
        }

        int useIds[] = {R.id.ckCj, R.id.ckJi, R.id.ckStroke, R.id.ckPhrase};
        String useKeys[] = {"ck_use_cj", "ck_use_ji", "ck_use_stroke", "ck_phrase"};
        for (int i = 0; i<useIds.length; i++) {
            id2Use.put(useIds[i], useKeys[i]);
        }

        for (int id: useIds) {
            CheckBox ck = findViewById(id);
            Boolean useKb = sharedPreferences.getUseKb(id2Use.get(ck.getId()));
            ck.setChecked(useKb);
            ck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox ckb = (CheckBox) v;
                    sharedPreferences.write(id2Use.get(ckb.getId()), ck.isChecked());
                }
            });
        }

    }
    private void setCandidateOrderButtonText() {
        final TextView candidateOrderButton = findViewById(R.id.candidate_order_button);
        final String candidateOrder = sharedPreferences.candidateOrder();
        final String candidateOrderButtonText = getString(switch (candidateOrder) {
            case "TraditionalOnly" -> R.string.traditional_only;
            case "SimplifiedOnly" -> R.string.simplified_only;
            default -> R.string.chinese_both;
        });
        candidateOrderButton.setText(candidateOrderButtonText);
    }

    private void setNextCandidateOrder() {
        final String candidateOrder = sharedPreferences.candidateOrder();
        final String nextCandidateOrder = switch (candidateOrder) {
            case "TraditionalOnly" -> "SimplifiedOnly";
            case "SimplifiedOnly" -> "ChineseBoth";
            default -> "TraditionalOnly";
        };
        sharedPreferences.write("candidateOrder", nextCandidateOrder);
        setCandidateOrderButtonText();
    }

    @Override
    public void onClick(final View view) {
        final int viewId = view.getId();
        switch (viewId) {
            case R.id.input_method_settings_button -> Contexty.showSystemInputMethodSettings(this);
            case R.id.change_keyboard_button -> Contexty.showSystemKeyboardChanger(this);
            case R.id.candidate_order_button -> {
                setNextCandidateOrder();
                view.requestFocusFromTouch();
//                view.requestPointerCapture();
            }
        }

    }
}
