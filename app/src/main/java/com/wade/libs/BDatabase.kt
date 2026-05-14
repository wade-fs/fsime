package com.wade.libs

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import com.wade.libs.TS.StoT
import com.wade.libs.TS.TtoS
import java.util.Arrays

class BDatabase(context: Context?) :
    SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private var db: SQLiteDatabase? = null
    private var ts = 0

    init {
        setForcedUpgrade(DATABASE_VERSION)
    }

    fun setTs(t: Int) {
        ts = t
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
        if (db == null) db = writableDatabase
        val composes = ArrayList<String>()
        val q = "SELECT * FROM mix WHERE ch = '$word';"
        val cursor = db!!.rawQuery(q, null)
        var next = cursor.moveToFirst()
        while (next) {
            val idx = cursor.getColumnIndex(ENG)
            if (idx >= 0) {
                val compose = cursor.getString(idx)
                composes.add(compose)
            }
            next = cursor.moveToNext()
        }
        cursor.close()
        return composes
    }

    fun saveCompose(ch: String, composes: ArrayList<String>) {
        if (ch.length != 1) {
            return
        }
        if (db == null) db = writableDatabase
        db!!.delete("b", "ch=?", arrayOf(ch))
        for (item in composes) {
            val values = ContentValues()
            values.put("eng", item)
            values.put("ch", ch)
            db!!.insert("b", null, values)
        }
    }

    fun reverseLookup(word: String): ArrayList<String> {
        if (db == null) db = writableDatabase
        val codes = ArrayList<String>()
        // Query the 'mix' table for the English code (eng) corresponding to the character (ch)
        // Ordering by length(eng) ASC so shorter codes appear first
        val q = "SELECT eng FROM mix WHERE ch = ? ORDER BY length(eng) ASC;"
        val cursor = db!!.rawQuery(q, arrayOf(word))
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

    var FUZZY_EXACT = 0
    var FUZZY_PREFIX = 1
    var FUZZY_FULL = 2
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
        var q: String
        val cursor: Cursor
        var count = 0
        var n: Boolean
        if (k.indexOf('"') >= 0) return list
        k.replace("\"".toRegex(), "\"\"")
        val useFreq = table == "ngram"
        val orderBy = if (useFreq) " ORDER BY freq DESC" else ""
        
        q = "select * from $table where "
        q += if (fuzzy == FUZZY_EXACT) {
            "$field = \"$k\"$orderBy LIMIT $max OFFSET $start;"
        } else if (fuzzy == FUZZY_PREFIX) {
            val pattern = if (useFreq) "$k%" else k + "_%"
            "$field like \"$pattern\"$orderBy LIMIT $max OFFSET $start;"
        } else {
            "$field like \"%$k%\"$orderBy LIMIT $max OFFSET $start;"
        }
        cursor = db!!.rawQuery(q, null)
        n = cursor.moveToFirst()
        val idIdx = cursor.getColumnIndex(ID)
        val chIdx = cursor.getColumnIndex(CH)
        val freqIdx = cursor.getColumnIndex(FREQ)
        while (n && count <= max) {
            val b = B()
            if (idIdx != -1) b.id = cursor.getInt(idIdx)
            if (chIdx != -1) b.ch = cursor.getString(chIdx)
            if (useFreq && freqIdx != -1) {
                b.freq = cursor.getDouble(freqIdx)
            }
            val _ch : String? = b.ch
            if (_ch != null) {
                if (ts == 1) {
                    b.ch = StoT(_ch)
                } else if (ts == 2) {
                    b.ch = TtoS(_ch)
                }
            }
            if (!isIn(list, b)) {
                list.add(b)
                ++count
            }
            n = cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    @SuppressLint("Range")
    fun getWord(k: String, start: Int, max: Int, table: String): ArrayList<String> {
        var k = k
        var start = start
        var max = max
        var table = table
        if (db == null) db = writableDatabase
        if (k.length == 0) return ArrayList()
        val list = ArrayList<String>()
        list.add(k)
        val resExact = ArrayList<B>()
        k = k.lowercase()
        val tables = ArrayList<String>()
        if (!(table == "mix" || table == "ji" || table == "cj" || table == "stroke" || table == "sym")) {
            table = "mix"
        }
        if (table == "mix") {
            tables.add("mix")
            tables.add("sym")
            tables.add("ji")
            tables.add("cj")
            tables.add("stroke")
            max = max + max + max
        } else {
            tables.add(table)
            tables.add("sym")
        }
        for (t in tables) {
            val r = query(k, start, max, t, "eng", FUZZY_EXACT)
            val res = ArrayList<B>()
            for (b in r) {
                if (b.ch!!.length > 1) { // 多個中文字
                    val s = b.ch!!.split("".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (ch in Arrays.asList<String>(*s)) {
                        val bb = B()
                        bb.ch = ch
                        bb.eng = b.eng
                        res.add(bb)
                        max--
                    }
                } else {
                    res.add(b)
                    max--
                }
                if (max <= 0) {
                    break
                }
            }
            resExact.addAll(res)
            if (max <= 0) {
                break
            }
        }
        if (max > 0) { // 如果不足，再找更多比對結果
            start = if (start < resExact.size) 0 else start - resExact.size
            for (t in tables) {
                val r = query(k, start, max, t, "eng", FUZZY_PREFIX)
                val res = ArrayList<B>()
                for (b in r) {
                    if (b.ch!!.length > 1) { // 多個中文字
                        val s =
                            b.ch!!.split("".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        for (ch in Arrays.asList<String>(*s)) {
                            val bb = B()
                            bb.ch = ch
                            bb.eng = b.eng
                            res.add(bb)
                            max--
                        }
                    } else {
                        res.add(b)
                        max--
                    }
                    if (max <= 0) {
                        break
                    }
                }
                resExact.addAll(res)
                if (max <= 0) {
                    break
                }
            }
        }
        for (d in resExact) {
            list.add(d.ch!!)
        }
        return list
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

    companion object {
        private const val DATABASE_NAME = "b.db"
        private const val DATABASE_VERSION = 7
        private const val ID = "id"
        private const val ENG = "eng"
        private const val CH = "ch"
        private const val FREQ = "freq"
    }
}
