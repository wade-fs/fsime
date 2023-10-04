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

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.wade.utilities.Contexty;

/*
  The main activity of the application.
*/
public class MainActivity
  extends AppCompatActivity
  implements View.OnClickListener, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
{
  public static final String CANDIDATE_ORDER_PREFERENCE_KEY = "candidateOrderPreference";
  public static final String CANDIDATE_ORDER_PREFER_TRADITIONAL_FIRST = "TRADITIONAL_FIRST";
  public static final String CANDIDATE_ORDER_PREFER_SIMPLIFIED_FIRST = "SIMPLIFIED_FIRST";
  

  AlertDialog.Builder candidateOrderDialogBuilder;
  Dialog candidateOrderDialog;

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setTitle(R.string.label__main_activity__welcome);
    setContentView(R.layout.main_activity);
    
    findViewById(R.id.input_method_settings_button).setOnClickListener(this);
    findViewById(R.id.change_keyboard_button).setOnClickListener(this);

    findViewById(R.id.test_input).requestFocus();
  }
  @Override
  public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
    // Instantiate the new Fragment
    final Bundle args = pref.getExtras();
    final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
            getClassLoader(),
            pref.getFragment());
    fragment.setArguments(args);
    fragment.setTargetFragment(caller, 0);
    // Replace the existing Fragment with the new Fragment
    getSupportFragmentManager().beginTransaction()
            .replace(R.id.settings_container, fragment)
            .addToBackStack(null)
            .commit();
    return true;
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

  @Override
  public void onClick(final View view)
  {
    final int viewId = view.getId();
    if (viewId == R.id.input_method_settings_button)
    {
      Contexty.showSystemInputMethodSettings(this);
    }
    else if (viewId == R.id.change_keyboard_button)
    {
      Contexty.showSystemKeyboardChanger(this);
    }
  }
}
