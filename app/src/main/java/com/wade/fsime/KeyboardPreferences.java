package com.wade.fsime;

import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.preference.PreferenceManager;

public class KeyboardPreferences {
    private SharedPreferences preferences;
    private Resources res;

    public KeyboardPreferences(ContextWrapper contextWrapper) {
        res = contextWrapper.getResources();
        this.preferences =
                PreferenceManager.getDefaultSharedPreferences(contextWrapper);
    }

    public boolean isFirstStart() {
        return read("FIRST_START", true);
    }

    public void setFirstStart(boolean value) {
        write("FIRST_START", value);
    }

    public void resetAllToDefault() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        setFirstStart(false);
    }

    private boolean read(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    private void write(String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private int read(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    private String read(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    private String safeRead(String key, String defaultValue) {
        String s = read(key, defaultValue);
        if (s == null) {
            return "0";
        }
        return s;
    }

    private void write(String key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

}
