package com.wade.fsime;

import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.preference.PreferenceManager;

public class KeyboardPreferences {
    private SharedPreferences preferences;
    private Resources res;

    public KeyboardPreferences(ContextWrapper contextWrapper) {
        res = contextWrapper.getResources();
        this.preferences =
                PreferenceManager.getDefaultSharedPreferences(contextWrapper);
    }
    public boolean isTraditionalOnly() {
        return safeRead("candidateOrder", "TraditionalOnly").equals("TraditionalOnly");
    }
    public boolean isSimplifiedOnly() {
        return safeRead("candidateOrder", "TraditionalOnly").equals("SimplifiedOnly");
    }
    public boolean isChineseBoth() {
        return safeRead("candidateOrder", "TraditionalOnly").equals("ChineseBoth");
    }
    public String candidateOrder() {
        return safeRead("candidateOrder", "TraditionalOnly");
    }
    public String getHotkey(String k) {
        return safeRead(k, "");
    }
    public Boolean getUseKb(String k) {
        return read(k, false);
    }
//    private boolean read(String key, boolean defaultValue) { return preferences.getBoolean(key, defaultValue); }
//    private int read(String key, int defaultValue) { return preferences.getInt(key, defaultValue); }
    private String read(String key, String defaultValue) { return preferences.getString(key, defaultValue); }
    private Boolean read(String key, Boolean defaultValue) { return preferences.getBoolean(key, defaultValue); }
    private String safeRead(String key, String defaultValue) {
        String s = read(key, defaultValue);
        if (s == null) {
            return "0";
        }
        return s;
    }

//    private void write(String key, boolean value) { preferences.edit().putBoolean(key, value).apply(); }
//    private void write(String key, int value) { preferences.edit().putInt(key, value).apply(); }
    void write(String key, String value) { preferences.edit().putString(key, value).apply(); }
    void write(String key, Boolean value) { preferences.edit().putBoolean(key, value).apply(); }
}
