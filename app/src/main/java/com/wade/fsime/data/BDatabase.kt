package com.wade.fsime.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import com.wade.fsime.data.repository.CandidatePhrase
import com.wade.fsime.data.repository.CodeType
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
        initializeUserPhrasesTable()
    }

    private fun initializeUserTable() {
        val db = writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS user_learning (context TEXT, ch TEXT, freq INTEGER, PRIMARY KEY (context, ch))")
    }

    private fun initializeUserPhrasesTable() {
        val db = writableDatabase
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS user_phrases (
                phrase TEXT PRIMARY KEY,
                boshiamy_code TEXT,
                zhuyin_code TEXT,
                freq REAL DEFAULT 1.0,
                selected_count INTEGER DEFAULT 1,
                source TEXT NOT NULL,
                confidence REAL DEFAULT 1.0,
                created_at INTEGER NOT NULL,
                last_used INTEGER NOT NULL
            );
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_phrases_boshiamy ON user_phrases (boshiamy_code, freq DESC);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_phrases_zhuyin ON user_phrases (zhuyin_code, freq DESC);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_phrases_lru ON user_phrases (last_used ASC);")
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
        
        // Ensure TS is initialized if needed
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
            
            // Deduplicate: check if this character is already in the list
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
        // Only add the search key if it's alphanumeric (code), not a Chinese character
        resultList.add(k)
        
        val tables = mutableListOf<String>()
        var limitMax = max
        
        val targetTable = when (table) {
            "zhuyin", "ji" -> "ji"
            "cj" -> "cj"
            "stroke" -> "stroke"
            "sym" -> "sym"
            else -> "boshiamy"
        }
        if (targetTable == "boshiamy") {
            tables.addAll(listOf("boshiamy", "sym", "ji", "cj", "stroke"))
            limitMax *= 3
        } else {
            tables.addAll(listOf(targetTable, "sym"))
        }

        val candidates = ArrayList<String>()

        // 0. Search user_phrases first
        val codeType = if (targetTable == "ji") CodeType.ZHUYIN else CodeType.BOSHIAMY
        val userPhrases = searchUserPhrases(searchKey, codeType, max)
        for (up in userPhrases) {
            if (!candidates.contains(up.phrase)) {
                candidates.add(up.phrase)
                limitMax--
                if (limitMax <= 0) break
            }
        }
        
        // Helper to add unique characters, handling surrogate pairs correctly
        fun addCandidates(b: B) {
            val s = b.ch ?: return
            var i = 0
            while (i < s.length) {
                val cp = s.codePointAt(i)
                val charStr = String(Character.toChars(cp))
                if (!candidates.contains(charStr)) {
                    candidates.add(charStr)
                    limitMax--
                }
                i += Character.charCount(cp)
            }
        }
        
        // Phase 1: Exact Match
        for (t in tables) {
            val results = query(searchKey, start, limitMax, t, "eng", FUZZY_EXACT)
            for (b in results) {
                addCandidates(b)
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
                    addCandidates(b)
                    if (limitMax <= 0) break
                }
                if (limitMax <= 0) break
            }
        }

        for (cand in candidates) {
            if (!resultList.contains(cand)) {
                resultList.add(cand)
            }
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

    fun generateBoshiamyCode(phrase: String): String? {
        if (phrase.isEmpty()) return null
        val shortestCodes = mutableListOf<String>()
        for (i in 0 until phrase.length) {
            val charStr = phrase[i].toString()
            val codes = reverseLookup(charStr)
            val shortest = codes.minByOrNull { it.length } ?: return null
            shortestCodes.add(shortest)
        }

        return when (phrase.length) {
            1 -> shortestCodes[0]
            2 -> {
                val c1 = shortestCodes[0].take(2)
                val c2 = shortestCodes[1].take(2)
                c1 + c2
            }
            3 -> {
                shortestCodes[0].take(1) + shortestCodes[1].take(1) + shortestCodes[2].take(1)
            }
            else -> {
                shortestCodes[0].take(1) + shortestCodes[1].take(1) + shortestCodes[2].take(1) + shortestCodes.last().take(1)
            }
        }
    }

    fun generateZhuyinCode(phrase: String): String? {
        if (phrase.isEmpty()) return null
        val db = writableDatabase
        val syllables = mutableListOf<String>()
        for (i in 0 until phrase.length) {
            val charStr = phrase[i].toString()
            val cursor = db.rawQuery("SELECT eng FROM ji WHERE ch = ? LIMIT 1", arrayOf(charStr))
            val eng = if (cursor.moveToFirst()) cursor.getString(0) else null
            cursor.close()
            if (eng == null) return null
            syllables.add(eng)
        }
        return syllables.joinToString(" ")
    }

    fun incrementUserPhraseUsage(
        phrase: String,
        boshiamyCode: String? = null,
        zhuyinCode: String? = null,
        source: String = "selected",
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (phrase.length < 2) return
        val db = writableDatabase
        val bCode = boshiamyCode ?: generateBoshiamyCode(phrase)
        val zCode = zhuyinCode ?: generateZhuyinCode(phrase)

        db.execSQL(
            """
            INSERT INTO user_phrases (phrase, boshiamy_code, zhuyin_code, freq, selected_count, source, confidence, created_at, last_used)
            VALUES (?, ?, ?, 1.0, 1, ?, 1.0, ?, ?)
            ON CONFLICT(phrase) DO UPDATE SET
                freq = freq + 1.0,
                selected_count = selected_count + 1,
                last_used = ?,
                boshiamy_code = COALESCE(?, boshiamy_code),
                zhuyin_code = COALESCE(?, zhuyin_code);
            """.trimIndent(),
            arrayOf<Any?>(phrase, bCode, zCode, source, timestamp, timestamp, timestamp, bCode, zCode)
        )
    }

    fun batchInsertMinedPhrases(phrases: List<CandidatePhrase>) {
        if (phrases.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val sql = """
                INSERT INTO user_phrases (phrase, boshiamy_code, zhuyin_code, freq, selected_count, source, confidence, created_at, last_used)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(phrase) DO UPDATE SET
                    freq = freq + 1.0,
                    confidence = MAX(confidence, excluded.confidence),
                    last_used = excluded.last_used;
            """.trimIndent()
            val stmt = db.compileStatement(sql)
            for (p in phrases) {
                val bCode = p.boshiamyCode ?: generateBoshiamyCode(p.phrase)
                val zCode = p.zhuyinCode ?: generateZhuyinCode(p.phrase)
                stmt.clearBindings()
                stmt.bindString(1, p.phrase)
                if (bCode != null) stmt.bindString(2, bCode) else stmt.bindNull(2)
                if (zCode != null) stmt.bindString(3, zCode) else stmt.bindNull(3)
                stmt.bindDouble(4, p.freq)
                stmt.bindLong(5, p.selectedCount.toLong())
                stmt.bindString(6, p.source)
                stmt.bindDouble(7, p.confidence)
                stmt.bindLong(8, p.createdAt)
                stmt.bindLong(9, p.lastUsed)
                stmt.execute()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun searchUserPhrases(code: String, type: CodeType, limit: Int = 30): List<CandidatePhrase> {
        val list = mutableListOf<CandidatePhrase>()
        val db = writableDatabase
        val column = if (type == CodeType.BOSHIAMY) "boshiamy_code" else "zhuyin_code"

        val sql = """
            SELECT phrase, boshiamy_code, zhuyin_code, freq, selected_count, source, confidence, created_at, last_used
            FROM user_phrases
            WHERE $column = ? OR $column LIKE ?
            ORDER BY (CASE WHEN $column = ? THEN 1 ELSE 0 END) DESC, freq DESC
            LIMIT ?;
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(code, "$code%", code, limit.toString()))
        val pIdx = cursor.getColumnIndex("phrase")
        val bIdx = cursor.getColumnIndex("boshiamy_code")
        val zIdx = cursor.getColumnIndex("zhuyin_code")
        val fIdx = cursor.getColumnIndex("freq")
        val sIdx = cursor.getColumnIndex("selected_count")
        val srcIdx = cursor.getColumnIndex("source")
        val cIdx = cursor.getColumnIndex("confidence")
        val caIdx = cursor.getColumnIndex("created_at")
        val luIdx = cursor.getColumnIndex("last_used")

        while (cursor.moveToNext()) {
            list.add(
                CandidatePhrase(
                    phrase = cursor.getString(pIdx),
                    boshiamyCode = if (bIdx != -1) cursor.getString(bIdx) else null,
                    zhuyinCode = if (zIdx != -1) cursor.getString(zIdx) else null,
                    freq = if (fIdx != -1) cursor.getDouble(fIdx) else 1.0,
                    selectedCount = if (sIdx != -1) cursor.getInt(sIdx) else 1,
                    source = if (srcIdx != -1) cursor.getString(srcIdx) else "selected",
                    confidence = if (cIdx != -1) cursor.getDouble(cIdx) else 1.0,
                    createdAt = if (caIdx != -1) cursor.getLong(caIdx) else 0L,
                    lastUsed = if (luIdx != -1) cursor.getLong(luIdx) else 0L
                )
            )
        }
        cursor.close()
        return list
    }

    fun decayAndPruneMinedPhrases(halfLifeDays: Double = 7.0, minWeight: Double = 0.2): Int {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val lambda = Math.log(2.0) / (halfLifeDays * 86400000.0)

        val cursor = db.rawQuery("SELECT phrase, freq, last_used FROM user_phrases WHERE source = 'mined'", null)
        val toDelete = mutableListOf<String>()
        val pIdx = cursor.getColumnIndex("phrase")
        val fIdx = cursor.getColumnIndex("freq")
        val lIdx = cursor.getColumnIndex("last_used")

        while (cursor.moveToNext()) {
            val phrase = cursor.getString(pIdx)
            val freq = cursor.getDouble(fIdx)
            val lastUsed = cursor.getLong(lIdx)
            val timeDiff = (now - lastUsed).coerceAtLeast(0L)
            val decayedWeight = freq * Math.exp(-lambda * timeDiff)
            if (decayedWeight < minWeight) {
                toDelete.add(phrase)
            }
        }
        cursor.close()

        if (toDelete.isNotEmpty()) {
            db.beginTransaction()
            try {
                for (phrase in toDelete) {
                    db.delete("user_phrases", "phrase = ?", arrayOf(phrase))
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        return toDelete.size
    }

    fun exportUserPhrases(): List<String> {
        val db = readableDatabase
        val lines = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT phrase, boshiamy_code, zhuyin_code, freq, source FROM user_phrases ORDER BY freq DESC", null)
        val pIdx = cursor.getColumnIndex("phrase")
        val bIdx = cursor.getColumnIndex("boshiamy_code")
        val zIdx = cursor.getColumnIndex("zhuyin_code")
        val fIdx = cursor.getColumnIndex("freq")
        val sIdx = cursor.getColumnIndex("source")
        while (cursor.moveToNext()) {
            val p = cursor.getString(pIdx)
            val b = cursor.getString(bIdx) ?: ""
            val z = cursor.getString(zIdx) ?: ""
            val f = cursor.getDouble(fIdx)
            val s = cursor.getString(sIdx)
            lines.add("$p\t$b\t$z\t$f\t$s")
        }
        cursor.close()
        return lines
    }

    fun importUserPhrases(lines: List<String>): Int {
        val db = writableDatabase
        var count = 0
        db.beginTransaction()
        try {
            val sql = """
                INSERT OR REPLACE INTO user_phrases (phrase, boshiamy_code, zhuyin_code, freq, selected_count, source, confidence, created_at, last_used)
                VALUES (?, ?, ?, ?, 1, ?, 1.0, ?, ?)
            """.trimIndent()
            val stmt = db.compileStatement(sql)
            val now = System.currentTimeMillis()
            for (line in lines) {
                val parts = line.split("\t")
                if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                    val phrase = parts[0].trim()
                    val bCode = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: generateBoshiamyCode(phrase)
                    val zCode = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: generateZhuyinCode(phrase)
                    val freq = parts.getOrNull(3)?.toDoubleOrNull() ?: 1.0
                    val source = parts.getOrNull(4)?.takeIf { it.isNotBlank() } ?: "manual"

                    stmt.clearBindings()
                    stmt.bindString(1, phrase)
                    if (bCode != null) stmt.bindString(2, bCode) else stmt.bindNull(2)
                    if (zCode != null) stmt.bindString(3, zCode) else stmt.bindNull(3)
                    stmt.bindDouble(4, freq)
                    stmt.bindString(5, source)
                    stmt.bindLong(6, now)
                    stmt.bindLong(7, now)
                    stmt.execute()
                    count++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return count
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
