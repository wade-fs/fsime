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
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.EditText;

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


  AlertDialog.Builder candidateOrderDialogBuilder;
  Dialog candidateOrderDialog;
  KeyboardPreferences sharedPreferences;

  Map<Integer,String> id2Key = new HashMap<Integer, String>();

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);
    loadSavedCandidateOrderPreference();
    findViewById(R.id.input_method_settings_button).setOnClickListener(this);
    findViewById(R.id.change_keyboard_button).setOnClickListener(this);
//    findViewById(R.id.candidate_order_button).setOnClickListener(this);
    findViewById(R.id.test_input).requestFocus();
    sharedPreferences = new KeyboardPreferences(this);

    int[] hkIds = new int[]{R.id.Ctrl1,R.id.Ctrl2,R.id.Ctrl3,R.id.Ctrl4,R.id.Ctrl5,
            R.id.Ctrl6,R.id.Ctrl7,R.id.Ctrl8,R.id.Ctrl9,R.id.Ctrl0,
            R.id.CtrlQ,R.id.CtrlW,R.id.CtrlE,R.id.CtrlR,R.id.CtrlT,
            R.id.CtrlY,R.id.CtrlU,R.id.CtrlI,R.id.CtrlO,R.id.CtrlP
    };
    String[] hkKeys = new String[]{
            "Ctrl1", "Ctrl2", "Ctrl3", "Ctrl4", "Ctrl5",
            "Ctrl6", "Ctrl7", "Ctrl8", "Ctrl9", "Ctrl0",
            "CtrlQ", "CtrlW", "CtrlE", "CtrlR", "CtrlT",
            "CtrlY", "CtrlU", "CtrlI", "CtrlO", "CtrlP"
    };
    for (int i=0; i<hkIds.length; i++) {
      id2Key.put(hkIds[i], hkKeys[i]);
    }
    for (int id: hkIds) {
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
  }
  public static boolean isTraditionalPreferred(final String candidateOrderPreference)
  {
    if (candidateOrderPreference == null) {
      return true;
    }

    return !candidateOrderPreference.equals(CANDIDATE_ORDER_PREFER_SIMPLIFIED_FIRST);
  }

  private String loadSavedCandidateOrderPreference()
  {
    return loadSavedCandidateOrderPreference(getApplicationContext());
  }
  private void setCandidateOrderButtonText(final String candidateOrderPreference)
  {
//    final TextView candidateOrderButton = findViewById(R.id.candidate_order_button);
//    final String candidateOrderButtonText =
//            (isTraditionalPreferred(candidateOrderPreference))
//                    ? getString(R.string.label__main_activity__traditional_first)
//                    : getString(R.string.label__main_activity__simplified_first);
//    candidateOrderButton.setText(candidateOrderButtonText);
  }

  public static String loadSavedCandidateOrderPreference(final Context context)
  {
    return
      Contexty.loadPreferenceString(context, FsimeService.PREFERENCES_FILE_NAME, CANDIDATE_ORDER_PREFERENCE_KEY);
  }

  private void saveCandidateOrderPreference(final String candidateOrderPreference)
  {
    Contexty.savePreferenceString(
      getApplicationContext(),
      FsimeService.PREFERENCES_FILE_NAME,
      CANDIDATE_ORDER_PREFERENCE_KEY,
      candidateOrderPreference
    );
  }
  private void showCandidateOrderDialog()
  {
    candidateOrderDialogBuilder = new AlertDialog.Builder(this, R.style.StrokeInputAlert);
    candidateOrderDialogBuilder
            .setTitle(R.string.label__main_activity__candidate_order)
            .setView(R.layout.candidate_order_dialog)
            .setCancelable(true);

    candidateOrderDialog = candidateOrderDialogBuilder.create();
    final int dialog_size = ViewGroup.LayoutParams.WRAP_CONTENT;
    candidateOrderDialog.show();
    candidateOrderDialog.getWindow().setLayout(dialog_size, dialog_size);

    final RadioGroup candidateOrderRadioGroup = candidateOrderDialog.findViewById(R.id.candidate_order_radio_group);
    final Button traditionalFirstButton = candidateOrderDialog.findViewById(R.id.traditional_first_button);
    final Button simplifiedFirstButton = candidateOrderDialog.findViewById(R.id.simplified_first_button);

    final boolean traditionalIsPreferred = isTraditionalPreferred(loadSavedCandidateOrderPreference());
    final int savedCandidateOrderButtonId =
            (traditionalIsPreferred)
                    ? R.id.traditional_first_button
                    : R.id.simplified_first_button;
    candidateOrderRadioGroup.check(savedCandidateOrderButtonId);

    traditionalFirstButton.setOnClickListener(this);
    simplifiedFirstButton.setOnClickListener(this);
  }

  @Override
  public void onClick(final View view)
  {
    final int viewId = view.getId();
    if (viewId == R.id.input_method_settings_button) {
      Contexty.showSystemInputMethodSettings(this);
    } else if (viewId == R.id.change_keyboard_button) {
      Contexty.showSystemKeyboardChanger(this);
//    } else if (viewId == R.id.candidate_order_button) {
//      showCandidateOrderDialog();
    } else if (viewId == R.id.traditional_first_button) {
      saveCandidateOrderPreference(CANDIDATE_ORDER_PREFER_TRADITIONAL_FIRST);
      setCandidateOrderButtonText(CANDIDATE_ORDER_PREFER_TRADITIONAL_FIRST);
      candidateOrderDialog.dismiss();
    } else if (viewId == R.id.simplified_first_button) {
      saveCandidateOrderPreference(CANDIDATE_ORDER_PREFER_SIMPLIFIED_FIRST);
      setCandidateOrderButtonText(CANDIDATE_ORDER_PREFER_SIMPLIFIED_FIRST);
      candidateOrderDialog.dismiss();
    }
  }
}
