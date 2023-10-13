/*
  Copyright 2021 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.fsime

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.wade.fsime.CandidatesViewAdapter.ButtonHolder

/*
  An adapter which holds candidate buttons.
*/
class CandidatesViewAdapter internal constructor(
    context: Context?,
    private val candidateList: MutableList<String>
) : RecyclerView.Adapter<ButtonHolder>() {
    private val layoutInflater: LayoutInflater
    private var candidateListener: CandidateListener? = null

    init {
        layoutInflater = LayoutInflater.from(context)
    }

    fun setCandidateListener(candidateListener: CandidateListener?) {
        this.candidateListener = candidateListener
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateCandidateList(candidateList: List<String>) {
        this.candidateList.clear()
        val cl = candidateList!!.toList()
        this.candidateList.addAll(cl)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ButtonHolder {
        val candidateButton =
            layoutInflater.inflate(R.layout.candidate_button, viewGroup, false) as Button
        return ButtonHolder(candidateButton)
    }

    override fun onBindViewHolder(buttonHolder: ButtonHolder, candidateIndex: Int) {
        val candidate = candidateList[candidateIndex]
        buttonHolder.candidateButton.text = candidate
    }

    override fun getItemCount(): Int {
        return candidateList.size
    }

    interface CandidateListener {
        fun onCandidate(candidate: String?)
    }

    inner class ButtonHolder(candidateButton: Button) : RecyclerView.ViewHolder(candidateButton),
        View.OnClickListener {
        val candidateButton: Button

        init {
            candidateButton.setOnClickListener(this)
            this.candidateButton = candidateButton
        }

        override fun onClick(view: View) {
            if (candidateListener != null) {
                candidateListener!!.onCandidate(candidateButton.text as String)
            }
        }
    }
}