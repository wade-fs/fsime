package com.wade.fsime.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import java.util.Arrays

class BDatabase(context: Context?) :
    SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private var db: SQLiteDatabase? = null
    private var ts = 0

    val FUZZY_EXACT = 0
    val FUZZY_PREFIX = 1
    val FUZZY_FULL = 2

    init {
        setForcedUpgrade(DATABASE_VERSION)
        initializeUserTable()
    }

    private fun initializeUserTable() {
        val db = writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS user_learning (context TEXT, ch TEXT, freq INTEGER, PRIMARY KEY (context, ch))")
    }

    fun setTs(t: Int) {
        ts = t
    }

    fun updateUsage(prevChar: String, code: String, ch: String) {
        val db = writableDatabase
        // 1. Record bigram association (context = previous character)
        val contextKey = if (prevChar.isEmpty()) "" else prevChar.substring(prevChar.length - 1)
        db.execSQL(
            "INSERT INTO user_learning (context, ch, freq) VALUES (?, ?, 1) " +
            "ON CONFLICT(context, ch) DO UPDATE SET freq = freq + 1",
            arrayOf(contextKey, ch)
        )

        // 2. Record code association (context = "code:" + input sequence)
        if (code.isNotEmpty()) {
            db.execSQL(
                "INSERT INTO user_learning (context, ch, freq) VALUES (?, ?, 1) " +
                "ON CONFLICT(context, ch) DO UPDATE SET freq = freq + 1",
                arrayOf("code:$code", ch)
            )
        }
    }

    private fun isIn(res: ArrayList<B>, b: B): Boolean {
        for (bb in res) {
            if (bb.ch == b.ch) return true
        }
        return false
    }

    private fun isIn(res: ArrayList<String>, b: String?): Boolean {
        for (bb in res) {
            if (bb == b) return true
        }
        return false
    }

    fun getCompose(word: String): ArrayList<String> {
        val db = writableDatabase
        val composes = ArrayList<String>()
        val q = "SELECT eng FROM boshiamy WHERE ch = ?;"
        val cursor = db.rawQuery(q, arrayOf(word))
        val engIdx = cursor.getColumnIndex(ENG)
        if (engIdx != -1) {
            while (cursor.moveToNext()) {
                composes.add(cursor.getString(engIdx))
            }
        }
        cursor.close()
        return composes
    }

    fun saveCompose(ch: String, composes: ArrayList<String>) {
        if (ch.length != 1) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("boshiamy", "ch=?", arrayOf(ch))
            val values = ContentValues()
            for (item in composes) {
                values.clear()
                values.put("eng", item)
                values.put("ch", ch)
                db.insert("boshiamy", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun batchImportMix(lines: List<String>): Int {
        val db = writableDatabase
        var count = 0
        db.beginTransaction()
        try {
            val sql = "INSERT OR IGNORE INTO boshiamy (ch, eng) VALUES (?, ?)"
            val statement = db.compileStatement(sql)
            for (line in lines) {
                val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                if (parts.size >= 2) {
                    statement.clearBindings()
                    statement.bindString(1, parts[0])
                    statement.bindString(2, parts[1])
                    if (statement.executeInsert() != -1L) {
                        count++
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return count
    }

    fun reverseLookup(word: String): ArrayList<String> {
        val db = writableDatabase
        val codes = ArrayList<String>()
        val q = "SELECT eng FROM boshiamy WHERE ch = ? ORDER BY length(eng) ASC;"
        val cursor = db.rawQuery(q, arrayOf(word))
        val engIdx = cursor.getColumnIndex(ENG)
        if (engIdx != -1) {
            while (cursor.moveToNext()) {
                val code = cursor.getString(engIdx)
                if (!codes.contains(code)) {
                    codes.add(code)
                }
            }
        }
        cursor.close()
        return codes
    }

    @SuppressLint("Range")
    private fun query(
        k: String,
        start: Int,
        max: Int,
        table: String,
        field: String,
        fuzzy: Int
    ): ArrayList<B> {
        val list = ArrayList<B>()
        val db = writableDatabase
        val useFreq = table == "ngram"
        
        // Context key for user learning: 
        // If it's ngram, use k (the context), else use "code:" + k
        val userContextKey = if (useFreq) k else "code:$k"

        val q: String
        val selectionArgs: Array<String>
        
        // We join with user_learning for ALL queries to prioritize user choices
        val baseFreqColumn = if (useFreq) "t.freq" else "0"
        val whereClause = if (fuzzy == FUZZY_EXACT) "t.$field = ?" else "t.$field LIKE ?"
        val pattern = when (fuzzy) {
            FUZZY_EXACT -> k
            FUZZY_PREFIX -> if (useFreq) "$k%" else "${k}_%"
            else -> "%$k%"
        }

        q = """
            SELECT t.*, (IFNULL(u.freq, 0) * 100000 + $baseFreqColumn) as total_freq 
            FROM $table t 
            LEFT JOIN user_learning u ON u.context = ? AND u.ch = t.ch
            WHERE $whereClause 
            ORDER BY total_freq DESC 
            LIMIT ? OFFSET ?;
        """.trimIndent()
        
        selectionArgs = arrayOf(userContextKey, pattern, max.toString(), start.toString())
        
        val cursor = db.rawQuery(q, selectionArgs)
        val idIdx = cursor.getColumnIndex(ID)
        val chIdx = cursor.getColumnIndex(CH)
        val engIdx = cursor.getColumnIndex(ENG)
        val freqIdx = cursor.getColumnIndex(FREQ)
        val totalFreqIdx = cursor.getColumnIndex("total_freq")
        
        if (ts != 0 && !TS.isInitialized()) {
            val mapping = getTSMapping()
            TS.initMapping(mapping["UTF8T"] ?: "", mapping["UTF8S"] ?: "")
        }
        
        while (cursor.moveToNext() && list.size < max) {
            val b = B()
            if (idIdx != -1) b.id = cursor.getInt(idIdx)
            if (chIdx != -1) b.ch = cursor.getString(chIdx)
            if (engIdx != -1) b.eng = cursor.getString(engIdx)
            if (totalFreqIdx != -1) {
                b.freq = cursor.getDouble(totalFreqIdx)
            } else if (useFreq && freqIdx != -1) {
                b.freq = cursor.getDouble(freqIdx)
            }
            
            val originalCh = b.ch
            if (originalCh != null) {
                b.ch = when (ts) {
                    1 -> TS.StoT(originalCh)
                    2 -> TS.TtoS(originalCh)
                    else -> originalCh
                }
            }
            
            if (list.none { it.ch == b.ch }) {
                list.add(b)
            }
        }
        cursor.close()
        return list
    }

    @SuppressLint("Range")
    fun getWord(k: String, start: Int, max: Int, table: String): ArrayList<String> {
        if (k.isEmpty()) return ArrayList()
        
        val searchKey = k.lowercase()
        val resultList = ArrayList<String>()
        resultList.add(k)
        
        val tables = mutableListOf<String>()
        var limitMax = max
        
        val targetTable = if (table in listOf("boshiamy", "ji", "cj", "stroke", "sym")) table else "boshiamy"
        if (targetTable == "boshiamy") {
            tables.addAll(listOf("boshiamy", "sym", "ji", "cj", "stroke"))
            limitMax *= 3
        } else {
            tables.addAll(listOf(targetTable, "sym"))
        }

        val candidates = ArrayList<B>()
        
        // Phase 1: Exact Match
        for (t in tables) {
            val results = query(searchKey, start, limitMax, t, "eng", FUZZY_EXACT)
            for (b in results) {
                val chars = b.ch ?: continue
                if (chars.length > 1) {
                    for (char in chars) {
                        candidates.add(B().apply { ch = char.toString(); eng = b.eng })
                        limitMax--
                    }
                } else {
                    candidates.add(b)
                    limitMax--
                }
                if (limitMax <= 0) break
            }
            if (limitMax <= 0) break
        }

        // Phase 2: Prefix Match (if needed)
        if (limitMax > 0) {
            val adjustedStart = if (start < candidates.size) 0 else start - candidates.size
            for (t in tables) {
                val results = query(searchKey, adjustedStart, limitMax, t, "eng", FUZZY_PREFIX)
                for (b in results) {
                    val chars = b.ch ?: continue
                    if (chars.length > 1) {
                        for (char in chars) {
                            candidates.add(B().apply { ch = char.toString(); eng = b.eng })
                            limitMax--
                        }
                    } else {
                        candidates.add(b)
                        limitMax--
                    }
                    if (limitMax <= 0) break
                }
                if (limitMax <= 0) break
            }
        }

        for (cand in candidates) {
            cand.ch?.let { resultList.add(it) }
        }
        return resultList
    }

    @SuppressLint("Range")
    fun getPhrase(tb: String, k: String, start: Int, max: Int): ArrayList<String> {
        if (db == null) db = writableDatabase
        val list = ArrayList<String>()
        
        // Use ngram table for better next-word prediction
        // We look at the last 1 or 2 characters for context
        val context = if (k.length >= 2) k.substring(k.length - 2) else k
        
        val predictions = ArrayList<B>()
        
        // 1. Try 2-char context if available
        if (context.length == 2) {
            predictions.addAll(query(context, 0, max, "ngram", "context", FUZZY_EXACT))
        }
        
        // 2. If not enough, try 1-char context (last char)
        if (predictions.size < max && k.isNotEmpty()) {
            val lastChar = k.substring(k.length - 1)
            val p1 = query(lastChar, 0, max - predictions.size, "ngram", "context", FUZZY_EXACT)
            for (p in p1) {
                if (!predictions.any { it.ch == p.ch }) {
                    predictions.add(p)
                }
            }
        }
        
        // 3. Fallback to most common starting characters (unigrams)
        if (predictions.size < max) {
            val unigrams = query("", 0, max - predictions.size, "ngram", "context", FUZZY_EXACT)
            for (p in unigrams) {
                if (!predictions.any { it.ch == p.ch }) {
                    predictions.add(p)
                }
            }
        }

        // Predictions gathered, now format the result list
        for (b in predictions) {
            val suggestion = b.ch ?: ""
            if (suggestion.isNotEmpty() && !list.contains(suggestion)) {
                list.add(suggestion)
            }
        }
        
        return list
    }

    fun getRandomWordForLevel(level: Int): String? {
        return getRandomWordForLevels(listOf(level))
    }

    fun getRandomWordForLevels(levels: List<Int>): String? {
        if (db == null) db = writableDatabase
        val placeholders = levels.joinToString(",") { "?" }
        val q = "SELECT ch FROM practice_levels WHERE level IN ($placeholders) ORDER BY RANDOM() LIMIT 1;"
        val cursor = db!!.rawQuery(q, levels.map { it.toString() }.toTypedArray())
        var word: String? = null
        if (cursor.moveToFirst()) {
            word = cursor.getString(0)
        }
        cursor.close()
        return word
    }

    fun getTSMapping(): Map<String, String> {
        val db = readableDatabase
        val mapping = mutableMapOf<String, String>()
        val cursor = db.rawQuery("SELECT key, value FROM ts_mapping", null)
        val keyIdx = cursor.getColumnIndex("key")
        val valIdx = cursor.getColumnIndex("value")
        if (keyIdx != -1 && valIdx != -1) {
            while (cursor.moveToNext()) {
                mapping[cursor.getString(keyIdx)] = cursor.getString(valIdx)
            }
        }
        cursor.close()
        return mapping
    }

    companion object {
        private const val DATABASE_NAME = "b.db"
        private const val DATABASE_VERSION = 9
        private const val ID = "id"
        private const val ENG = "eng"
        private const val CH = "ch"
        private const val FREQ = "freq"
    }
}
