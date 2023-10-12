/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.fsime

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.wade.utilities.Contexty.loadPreferenceString
import com.wade.utilities.Contexty.savePreferenceString
import com.wade.utilities.Contexty.showSystemInputMethodSettings
import com.wade.utilities.Contexty.showSystemKeyboardChanger

/*
  The main activity of the application.
*/
class MainActivity : AppCompatActivity(), View.OnClickListener {
    var candidateOrderDialogBuilder: AlertDialog.Builder? = null
    var candidateOrderDialog: Dialog? = null
    var sharedPreferences: KeyboardPreferences? = null
    var id2Key: MutableMap<Int, String> = HashMap()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        loadSavedCandidateOrderPreference()
        findViewById<View>(R.id.input_method_settings_button).setOnClickListener(this)
        findViewById<View>(R.id.change_keyboard_button).setOnClickListener(this)
        //    findViewById(R.id.candidate_order_button).setOnClickListener(this);
        findViewById<View>(R.id.test_input).requestFocus()
        sharedPreferences = KeyboardPreferences(this)
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

    private fun loadSavedCandidateOrderPreference(): String? {
        return loadSavedCandidateOrderPreference(applicationContext)
    }

    private fun setCandidateOrderButtonText(candidateOrderPreference: String) {
//    final TextView candidateOrderButton = findViewById(R.id.candidate_order_button);
//    final String candidateOrderButtonText =
//            (isTraditionalPreferred(candidateOrderPreference))
//                    ? getString(R.string.label__main_activity__traditional_first)
//                    : getString(R.string.label__main_activity__simplified_first);
//    candidateOrderButton.setText(candidateOrderButtonText);
    }

    private fun saveCandidateOrderPreference(candidateOrderPreference: String) {
        savePreferenceString(
            applicationContext,
            FsimeService.PREFERENCES_FILE_NAME,
            CANDIDATE_ORDER_PREFERENCE_KEY,
            candidateOrderPreference
        )
    }

    override fun onClick(view: View) {
        val viewId = view.id
        if (viewId == R.id.input_method_settings_button) {
            showSystemInputMethodSettings(this)
        } else if (viewId == R.id.change_keyboard_button) {
            showSystemKeyboardChanger(this)
            //    } else if (viewId == R.id.candidate_order_button) {
//      showCandidateOrderDialog();
        } else if (viewId == R.id.traditional_first_button) {
            saveCandidateOrderPreference(CANDIDATE_ORDER_PREFER_TRADITIONAL_FIRST)
            setCandidateOrderButtonText(CANDIDATE_ORDER_PREFER_TRADITIONAL_FIRST)
            candidateOrderDialog!!.dismiss()
        } else if (viewId == R.id.simplified_first_button) {
            saveCandidateOrderPreference(CANDIDATE_ORDER_PREFER_SIMPLIFIED_FIRST)
            setCandidateOrderButtonText(CANDIDATE_ORDER_PREFER_SIMPLIFIED_FIRST)
            candidateOrderDialog!!.dismiss()
        }
    }

    companion object {
        const val CANDIDATE_ORDER_PREFERENCE_KEY = "candidateOrderPreference"
        const val CANDIDATE_ORDER_PREFER_TRADITIONAL_FIRST = "TRADITIONAL_FIRST"
        const val CANDIDATE_ORDER_PREFER_SIMPLIFIED_FIRST = "SIMPLIFIED_FIRST"
        @JvmStatic
        fun isTraditionalPreferred(candidateOrderPreference: String?): Boolean {
            return if (candidateOrderPreference == null) {
                true
            } else candidateOrderPreference != CANDIDATE_ORDER_PREFER_SIMPLIFIED_FIRST
        }

        @JvmStatic
        fun loadSavedCandidateOrderPreference(context: Context?): String? {
            return loadPreferenceString(
                context!!,
                FsimeService.PREFERENCES_FILE_NAME,
                CANDIDATE_ORDER_PREFERENCE_KEY
            )
        }
    }
}