/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.fsime

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.wade.fsime.CandidatesViewAdapter.CandidateListener
import com.wade.fsime.KeyboardView.KeyboardListener

/*
  A container that holds:
    1. Main input plane:
      - Candidates view
      - Keyboard view
    2. Key preview plane (overlaid)
*/
class InputContainer(context: Context?, attributes: AttributeSet?) : FrameLayout(
    context!!, attributes
) {
    private var candidatesView: CandidatesView? = null
    private var candidatesViewAdapter: CandidatesViewAdapter? = null
    private var keyboardView: KeyboardView? = null
    fun initialiseCandidatesView(candidateListener: CandidateListener?) {
        val cv : CandidatesView = findViewById(R.id.candidates_view)
        cv.setCandidateListener(candidateListener)
        candidatesViewAdapter = cv.candidatesViewAdapter
        candidatesView = cv
    }

    fun initialiseKeyboardView(
        keyboardListener: KeyboardListener?,
        keyboard: Keyboard?
    ) {
        val kv : KeyboardView = findViewById(R.id.keyboard_view)
        kv.setKeyboardListener(keyboardListener)
        kv.setMainInputPlane(findViewById(R.id.main_input_plane))
        kv.keyboard = keyboard
        keyboardView = kv
    }

    fun setBackground(isFullscreen: Boolean) {
        val backgroundResourceId = if (isFullscreen) R.color.fill_fullscreen else 0 // none
        setBackgroundResource(backgroundResourceId)
    }

    fun setCandidateList(candidateList: List<String?>?) {
        candidatesViewAdapter!!.updateCandidateList(candidateList)
        candidatesView!!.scrollToPosition(0)
    }

    val candidatesViewTop: Int
        get() = candidatesView!!.top
    var keyboard: Keyboard?
        get() = keyboardView!!.keyboard
        set(keyboard) {
            keyboardView!!.keyboard = keyboard
        }

    fun setKeyRepeatIntervalMilliseconds(milliseconds: Int) {
        keyboardView!!.setKeyRepeatIntervalMilliseconds(milliseconds)
    }

    fun redrawKeyboard() {
        keyboardView!!.invalidate()
    }
}