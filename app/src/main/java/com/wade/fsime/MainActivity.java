/*
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


import java.util.List;

import android.media.audiofx.BassBoost;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



/* MainActivity is what launches when you click on the desktop icon,
 It is not used for everyday IME operation.
*/

/* (Enabling our IME consists of 2 parts: enable it in settings, and pick it in IME picker.)
 We can detect whether it has been enabled, but not if it is the current picked one.
*/

public class MainActivity extends ListActivity {
    final static private String TAG="MyLog";
    private static final int M_SETTINGS = 0;
    private static final int M_ENABLE = 1;
    protected int dontAsk=0;
    protected String versionName;
    BDatabase bdatabase;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.help);

        try {
            dontAsk = state.getInt("dontAsk");
        } catch (Exception e) {
            dontAsk = 0;
        }

        // populate main menu
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        String[] menu = getResources().getStringArray(R.array.main_menu);
        String[] menuDesc = getResources().getStringArray(R.array.main_menu_desc);

        // convert 2 arrays of strings -> one list of maps of 2 strings
        for (int i = 0; i < menu.length; i++) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("text1", menu[i]);
            map.put("text2", menuDesc[i]);
            list.add(map);
        }
        String[] from = {"text1", "text2"};
        int[] to = {android.R.id.text1, android.R.id.text2};

        // let built-in function do the magic (put the strings into the textviews)
        SimpleAdapter sa = new SimpleAdapter(this.getApplicationContext(),
                list,
                android.R.layout.simple_list_item_2,
                from,
                to);

        setListAdapter(sa);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent;

        switch (position) {
            case M_SETTINGS:
                intent = new Intent();
                intent.setClass(MainActivity.this, SettingsActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setFlags(/*Intent.FLAG_ACTIVITY_NO_HISTORY|*/Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case M_ENABLE:
                if (getIMEStatus() == 0) {
                    dontAsk = 1;
                    launchLocaleSettings();
                } else {
                    dontAsk = 2;
                    showPicker();
                }
                break;
        }
    }

    // When launched, whether to show "Enable" dialogs and IME picker
    @Override
    protected void onStart() {
        super.onStart();
        int imeStatus = getIMEStatus();
        if (imeStatus == 0) {
            dontAsk = 1;
            launchLocaleSettings();
        } else if (imeStatus < 2) {
            dontAsk = 2;
            showPicker();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle s) {
        dontAsk = s.getChar("dontAsk");
    }

    @Override
    protected void onSaveInstanceState(Bundle s) {
        s.putInt("dontAsk", dontAsk);
    }

    // Launch device keyboard settings
    protected void launchLocaleSettings() {
        Intent queryIntent = new Intent(Intent.ACTION_MAIN);
        queryIntent.setClassName("com.android.settings", "com.android.settings.LanguageSettings");
        queryIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(queryIntent);

    }

    // Show android IME picker
    //  (due to a kind of bug we need to show it in a delayed runnable)
    void showPicker() {
        Runnable run = new Runnable() {
            public void run() {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputManager.showInputMethodPicker();
            }
        };

        Handler h = new android.os.Handler();
        h.postDelayed(run, 500);
    }

    // return true if our IME is enabled in android settings
    protected int getIMEStatus() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> inputMethodInfoList = inputManager.getEnabledInputMethodList();
        String myPackageName = this.getClass().getPackage().getName();
        int res = 0;
        for (InputMethodInfo i : inputMethodInfoList) {
            if (i.getPackageName().equals(myPackageName)) {
                res = 1;
                break;
            }
        }
        String curIME = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (curIME.contains(myPackageName)) res = 2;
        return res;
    }
}
