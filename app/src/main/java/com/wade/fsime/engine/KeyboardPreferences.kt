package com.wade.fsime.engine

import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.preference.PreferenceManager

class KeyboardPreferences(contextWrapper: ContextWrapper) {
    private val preferences: SharedPreferences
    private val res: Resources

    init {
        res = contextWrapper.resources
        preferences = PreferenceManager.getDefaultSharedPreferences(contextWrapper)
    }
    fun candidateOrder(): String {
        return safeRead("candidateOrder", "ChineseBoth")
    }

    fun angleUnit(): String {
        return safeRead("angle_unit", "degree")
    }

    fun getHotkey(k: String): String {
        return safeRead(k, "")
    }
    fun getUseKb(k: String): Boolean {
        return safeRead(k, false)
    }

//    fun resetAllToDefault() {
//        val editor = preferences.edit()
//        editor.clear()
//        editor.apply()
//    }

    //    private boolean read(String key, boolean defaultValue) { return preferences.getBoolean(key, defaultValue); }
    //    private int read(String key, int defaultValue) { return preferences.getInt(key, defaultValue); }
    fun read(key: String, defaultValue: String): String {
        return preferences.getString(key, defaultValue) ?: defaultValue
    }

    private fun safeRead(key: String, defaultValue: String): String {
        return read(key, defaultValue)
    }
    fun write(key: String?, value: String?) {
        preferences.edit().putString(key, value).apply()
    }
    private fun read(key: String, defaultValue: Boolean): Boolean? {
        return preferences.getBoolean(key, defaultValue)
    }
    private fun safeRead(key: String, defaultValue: Boolean): Boolean{
        return read(key, defaultValue) ?: return false
    }
    fun write(key: String?, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
}
