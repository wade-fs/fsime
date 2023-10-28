package com.wade.libs

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import com.wade.libs.TS.StoT
import com.wade.libs.TS.TtoS
import java.util.Arrays

class BDatabase(context: Context?) :
    SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private var db: SQLiteDatabase? = null
    private var ts = 0
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
        q = "select * from $table where "
        q += if (fuzzy == FUZZY_EXACT) {
            "$field = \"$k\" LIMIT $max OFFSET $start;"
        } else if (fuzzy == FUZZY_PREFIX) {
            field + " like \"" + k + "_%\" LIMIT " + max + " OFFSET " + start + ";"
        } else {
            "$field like \"%$k%\" LIMIT $max OFFSET $start;"
        }
        cursor = db!!.rawQuery(q, null)
        n = cursor.moveToFirst()
        while (n && count <= max) {
            val b = B()
            b.id = cursor.getInt(cursor.getColumnIndex(ID))
            b.ch = cursor.getString(cursor.getColumnIndex(CH))
            val _ch : String? = b.ch
            if (_ch != null) {
                if (ts == 1) {
                    b.ch = StoT(_ch)
                } else if (ts == 2) {
                    b.ch = TtoS(_ch)
                }
            }
            if (table === "vocabulary") {
                val idx = b.ch!!.indexOf(k)
                if (idx < b.ch!!.length - 1) {
                    b.ch = b.ch!!.substring(idx + 1, idx + 2)
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
    fun getVocabulary(k: String, start: Int, max: Int): ArrayList<String> {
        if (db == null) db = writableDatabase
        val resExact = ArrayList<B>()
        val res = query(k, start, max, "vocabulary", "ch", FUZZY_PREFIX)
        resExact.addAll(res)
        val list = ArrayList<String>()
        list.add(k.substring(0, 1))
        for (b in resExact) {
            if (!isIn(list, b.ch)) {
                list.add(b.ch!!)
            }
        }
        return list
    }

    companion object {
        private const val DATABASE_NAME = "b.db"
        private const val DATABASE_VERSION = 1
        private const val ID = "id"
        private const val ENG = "eng"
        private const val CH = "ch"
        private const val FREQ = "freq"
    }
}
