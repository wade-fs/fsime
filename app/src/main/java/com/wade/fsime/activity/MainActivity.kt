/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.fsime.activity

import com.wade.fsime.R
import com.wade.fsime.engine.KeyboardPreferences

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wade.fsime.util.Contexty.showSystemInputMethodSettings
import com.wade.fsime.util.Contexty.showSystemKeyboardChanger


/*
  The main activity of the application.
*/
class MainActivity : AppCompatActivity(), View.OnClickListener {
    var sharedPreferences: KeyboardPreferences? = null
    var id2Key: MutableMap<Int, String> = HashMap()
    var id2Use: MutableMap<Int, String> = HashMap()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        findViewById<View>(R.id.setup_ime_button).setOnClickListener(this)
        findViewById<View>(R.id.candidate_order_button).setOnClickListener(this);
        findViewById<View>(R.id.practice_button).setOnClickListener(this)
        findViewById<View>(R.id.db_management_button).setOnClickListener(this)
        findViewById<View>(R.id.digit_guide_button).setOnClickListener(this)
        findViewById<View>(R.id.ck_show_shortest_code_button).setOnClickListener(this)
        findViewById<View>(R.id.hint_font_size_button).setOnClickListener(this)
        findViewById<View>(R.id.test_input).requestFocus()
        sharedPreferences = KeyboardPreferences(this)
        
        updateAllSettingsButtonText()

        val hkIds = intArrayOf(
            R.id.Ctrl1, R.id.Ctrl2, R.id.Ctrl3, R.id.Ctrl4, R.id.Ctrl5,
            R.id.Ctrl6, R.id.Ctrl7, R.id.Ctrl8, R.id.Ctrl9, R.id.Ctrl0,
            R.id.CtrlQ, R.id.CtrlW, R.id.CtrlE, R.id.CtrlR, R.id.CtrlT,
            R.id.CtrlY, R.id.CtrlU, R.id.CtrlI, R.id.CtrlO, R.id.CtrlP
        )
        val hkKeys = arrayOf(
            "Ctrl1", "Ctrl2", "Ctrl3", "Ctrl4", "Ctrl5",
            "Ctrl6", "Ctrl7", "Ctrl8", "Ctrl9", "Ctrl0",
            "CtrlQ", "CtrlW", "CtrlE", "CtrlR", "CtrlT",
            "CtrlY", "CtrlU", "CtrlI", "CtrlO", "CtrlP"
        )
        for (i in hkIds.indices) {
            id2Key[hkIds[i]] = hkKeys[i]
        }
        for (id in hkIds) {
            val etHk = findViewById<EditText>(id)
            val hkStr = sharedPreferences!!.getHotkey(id2Key[etHk.id]!!)
            etHk.setText(hkStr)
            etHk.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                val et = v as EditText
                if (!hasFocus) {
                    sharedPreferences!!.write(id2Key[et.id], et.text.toString())
                }
            }
        }
    }

    private fun updateAllSettingsButtonText() {
        setCandidateOrderButtonText()
        setHintFontSizeButtonText()
        setShowShortestCodeButtonText()
    }

    private fun setShowShortestCodeButtonText() {
        val button = findViewById<Button>(R.id.ck_show_shortest_code_button)
        val isEnabled = sharedPreferences!!.getUseKb("ck_show_shortest_code")
        val statusText = if (isEnabled) getString(R.string.status_on) else getString(R.string.status_off)
        button.text = "${getString(R.string.ck_show_shortest_code)}: $statusText"
    }

    private fun setCandidateOrderButtonText() {
        val candidateOrderButton = findViewById<Button>(R.id.candidate_order_button)
        val candidateOrder = sharedPreferences!!.candidateOrder()
        val statusText = getString(
            when (candidateOrder) {
                "TraditionalOnly" -> R.string.traditional_only
                "SimplifiedOnly" -> R.string.simplified_only
                else -> R.string.chinese_both
            }
        )
        candidateOrderButton.text = "${getString(R.string.text__main_activity__candidate_order)}: $statusText"
    }

    private fun setHintFontSizeButtonText() {
        val button = findViewById<Button>(R.id.hint_font_size_button)
        val size = sharedPreferences!!.read("hintFontSize", "Small")
        val text = when (size) {
            "Small" -> getString(R.string.font_size_small)
            "Medium" -> getString(R.string.font_size_medium)
            "Large" -> getString(R.string.font_size_large)
            else -> getString(R.string.font_size_small)
        }
        button.text = "${getString(R.string.pref_hint_font_size)}: $text"
    }

    private fun toggleShowShortestCode() {
        val current = sharedPreferences!!.getUseKb("ck_show_shortest_code")
        sharedPreferences!!.write("ck_show_shortest_code", !current)
        setShowShortestCodeButtonText()
    }

    private fun setNextHintFontSize() {
        val current = sharedPreferences!!.read("hintFontSize", "Small")
        val next = when (current) {
            "Small" -> "Medium"
            "Medium" -> "Large"
            else -> "Small"
        }
        sharedPreferences!!.write("hintFontSize", next)
        setHintFontSizeButtonText()
    }
    private fun setNextCandidateOrder() {
        val candidateOrder = sharedPreferences!!.candidateOrder()
        val nextCandidateOrder = when (candidateOrder) {
            "TraditionalOnly" -> "SimplifiedOnly"
            "SimplifiedOnly" -> "ChineseBoth"
            else -> "TraditionalOnly"
        }
        sharedPreferences!!.write("candidateOrder", nextCandidateOrder)
        setCandidateOrderButtonText()
    }

    override fun onClick(view: View) {
        val viewId = view.id
        when (viewId) {
            R.id.setup_ime_button -> {
                if (!com.wade.fsime.util.Contexty.isInputMethodEnabled(this)) {
                    // Step 1: Guide to enable
                    com.wade.fsime.util.Contexty.showSystemInputMethodSettings(this)
                } else {
                    // Step 2: Already enabled, show picker to "use"
                    com.wade.fsime.util.Contexty.showSystemKeyboardChanger(this)
                }
            }
            R.id.practice_button -> {
                startActivity(Intent(this, PracticeActivity::class.java))
            }
            R.id.db_management_button -> {
                startActivity(Intent(this, DatabaseManagementActivity::class.java))
            }
            R.id.digit_guide_button -> {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.label_digit_keyboard_guide)
                    .setMessage(R.string.digit_keyboard_guide_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
            R.id.candidate_order_button -> {
                setNextCandidateOrder()
                view.requestFocusFromTouch()
            }
            R.id.hint_font_size_button -> {
                setNextHintFontSize()
                view.requestFocusFromTouch()
            }
            R.id.ck_show_shortest_code_button -> {
                toggleShowShortestCode()
                view.requestFocusFromTouch()
            }
        }
    }
}
