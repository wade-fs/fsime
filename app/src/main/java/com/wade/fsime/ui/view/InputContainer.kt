/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.fsime.ui.view

import com.wade.fsime.R
import com.wade.fsime.engine.Keyboard
import com.wade.fsime.ui.adapter.CandidatesViewAdapter

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.wade.fsime.ui.adapter.CandidatesViewAdapter.CandidateListener
import com.wade.fsime.ui.view.KeyboardView.KeyboardListener

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
    private var handwritingView: HandwritingView? = null
    private var handwritingContainer: View? = null

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
        handwritingView = findViewById<HandwritingView>(R.id.handwriting_view)
        handwritingContainer = findViewById<View>(R.id.handwriting_container)
        findViewById<View>(R.id.btn_close_handwriting).setOnClickListener {
            showHandwriting(false)
        }
        findViewById<View>(R.id.btn_clear_handwriting).setOnClickListener {
            clearHandwriting()
        }
        findViewById<View>(R.id.btn_recognize_handwriting).setOnClickListener {
            val ink = handwritingView?.getCurrentInk()
            if (ink != null && !ink.strokes.isEmpty()) {
                handwritingListener?.onInkFinished(ink)
            }
        }
    }

    private var handwritingListener: HandwritingView.HandwritingListener? = null
    fun setHandwritingListener(listener: HandwritingView.HandwritingListener) {
        handwritingView?.setHandwritingListener(listener)
        handwritingListener = listener
    }

    fun showHandwriting(show: Boolean) {
        if (show) {
            keyboardView?.visibility = View.GONE
            handwritingContainer?.visibility = View.VISIBLE
        } else {
            keyboardView?.visibility = View.VISIBLE
            handwritingContainer?.visibility = View.GONE
            handwritingView?.clear()
        }
    }

    fun isHandwritingVisible(): Boolean {
        return handwritingContainer?.visibility == View.VISIBLE
    }

    fun clearHandwriting() {
        handwritingView?.clear()
    }

    fun setBackground(isFullscreen: Boolean) {
        val backgroundResourceId = if (isFullscreen) R.color.fill_fullscreen else 0 // none
        setBackgroundResource(backgroundResourceId)
    }

    fun setCandidateList(candidateList: List<String>) {
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
