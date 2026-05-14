package com.wade.fsime

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.wade.libs.BDatabase
import java.util.*

class PracticeActivity : AppCompatActivity() {
    private lateinit var db: BDatabase
    private lateinit var targetWordText: TextView
    private lateinit var hintText: TextView
    private lateinit var practiceInput: EditText
    private lateinit var speedText: TextView
    private lateinit var errorsText: TextView
    private lateinit var levelSpinner: Spinner
    private lateinit var resetButton: Button

    private var currentTargetWord: String? = null
    private var currentTargetCodes: List<String> = emptyList()
    
    private var correctCharCount = 0
    private var errorCount = 0
    private var startTime: Long = 0
    private var isStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_practice)

        db = BDatabase(this)
        
        targetWordText = findViewById(R.id.target_word)
        hintText = findViewById(R.id.hint_text)
        practiceInput = findViewById(R.id.practice_input)
        speedText = findViewById(R.id.speed_text)
        errorsText = findViewById(R.id.errors_text)
        levelSpinner = findViewById(R.id.level_spinner)
        resetButton = findViewById(R.id.reset_button)

        setupLevelSpinner()
        
        practiceInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    checkInput(input)
                }
            }
        })

        resetButton.setOnClickListener {
            resetStats()
        }

        nextWord()
    }

    private fun setupLevelSpinner() {
        val levels = arrayOf(
            "一年級", "二年級", "三年級", "四年級", "五年級", "六年級",
            "非常用字", "注音符號", "數字", "標點符號", "非常用符號",
            "1-6年級", "常用符號", "全部"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        levelSpinner.adapter = adapter
        
        levelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                resetStats()
                nextWord()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun nextWord() {
        val position = levelSpinner.selectedItemPosition
        val levels = when (position) {
            in 0..10 -> listOf(position + 1)
            11 -> listOf(1, 2, 3, 4, 5, 6)
            12 -> listOf(8, 9, 10)
            13 -> (1..11).toList()
            else -> listOf(1)
        }
        
        currentTargetWord = db.getRandomWordForLevels(levels)
        if (currentTargetWord != null) {
            targetWordText.text = currentTargetWord
            currentTargetCodes = db.reverseLookup(currentTargetWord!!)
            hintText.text = currentTargetCodes.joinToString(", ")
            practiceInput.text.clear()
        }
    }

    private fun checkInput(input: String) {
        if (!isStarted) {
            startTime = System.currentTimeMillis()
            isStarted = true
        }

        if (input == currentTargetWord) {
            correctCharCount++
            updateStats()
            nextWord()
        } else if (input.length >= (currentTargetWord?.length ?: 0)) {
            // If the input doesn't match and it's long enough, it's an error
            // Actually, for Chinese IME, it's a bit tricky because you type codes to get words.
            // But here the user might be using the Fsime keyboard to type.
            // If they type the WRONG word, we count an error.
            errorCount++
            updateStats()
            practiceInput.text.clear()
        }
    }

    private fun updateStats() {
        val elapsedMillis = System.currentTimeMillis() - startTime
        val elapsedMinutes = elapsedMillis / 60000.0
        val wpm = if (elapsedMinutes > 0) (correctCharCount / 5.0) / elapsedMinutes else 0.0
        
        speedText.text = getString(R.string.label_speed, wpm)
        errorsText.text = getString(R.string.label_errors, errorCount)
    }

    private fun resetStats() {
        correctCharCount = 0
        errorCount = 0
        isStarted = false
        speedText.text = getString(R.string.label_speed, 0.0)
        errorsText.text = getString(R.string.label_errors, 0)
    }
}
