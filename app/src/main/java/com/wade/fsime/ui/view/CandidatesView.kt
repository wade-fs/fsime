/*
  Copyright 2021 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.fsime.ui.view

import com.wade.fsime.ui.adapter.CandidatesViewAdapter

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wade.fsime.ui.adapter.CandidatesViewAdapter.CandidateListener

/*
  A view that holds candidates.
*/
class CandidatesView(context: Context, attributes: AttributeSet?) :
    RecyclerView(context, attributes) {
    var candidatesViewAdapter: CandidatesViewAdapter? = null
        private set

    init {
        initialiseCandidatesViewAdapter(context)
    }

    private fun initialiseCandidatesViewAdapter(context: Context) {
        candidatesViewAdapter = CandidatesViewAdapter(context, ArrayList())
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = candidatesViewAdapter
    }

    fun setCandidateListener(candidateListener: CandidateListener?) {
        candidatesViewAdapter!!.setCandidateListener(candidateListener)
    }
}