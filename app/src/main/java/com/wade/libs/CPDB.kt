// 平均是 22.028
// 計算公式是 (x/1000)^2 + (y/1000 - 2000)^2
package com.wade.libs

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

class CPDB(context: Context?) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private var db: SQLiteDatabase? = null
    fun getCpByNumber(number: String): List<CP>? {
        if (db == null) db = writableDatabase
        if (db == null) {
            return null
        }
        var idIdx: Int
        var tIdx: Int
        var numberIdx: Int
        var nameIdx: Int
        var xIdx: Int
        var yIdx: Int
        var hIdx: Int
        var infoIdx: Int
        // 先找出完整比對的結果
        val cursor = db!!.rawQuery(
            "select * from " + TABLE + " where " +
                    "number like '%" + number + "' or " +
                    "number like '" + number + "%' or " +
                    "number like '%" + number + "%' or " +
                    "number = '" + number + "'",
            null
        )
        val cps: MutableList<CP> = ArrayList()
        while (cursor.moveToNext()) {
            idIdx = cursor.getColumnIndex(ID)
            if (idIdx < 0) idIdx = 0
            tIdx = cursor.getColumnIndex(T)
            if (tIdx < 0) tIdx = 0
            numberIdx = cursor.getColumnIndex(Number)
            if (numberIdx < 0) numberIdx = 0
            nameIdx = cursor.getColumnIndex(Name)
            if (nameIdx < 0) nameIdx = 0
            xIdx = cursor.getColumnIndex(X)
            if (xIdx < 0) xIdx = 0
            yIdx = cursor.getColumnIndex(Y)
            if (yIdx < 0) yIdx = 0
            hIdx = cursor.getColumnIndex(H)
            if (hIdx < 0) hIdx = 0
            infoIdx = cursor.getColumnIndex(INFO)
            if (infoIdx < 0) infoIdx = 0
            val cp = CP(
                cursor.getInt(idIdx),
                cursor.getInt(tIdx),
                cursor.getString(numberIdx),
                cursor.getString(nameIdx),
                cursor.getDouble(xIdx),
                cursor.getDouble(yIdx),
                cursor.getDouble(hIdx),
                cursor.getString(infoIdx)
            )
            cps.add(cp)
        }
        return cps
    }

    fun getCp(x: Double, y: Double, l: Double): List<CP?>? {
        if (db == null) db = writableDatabase
        if (db == null) {
            return null
        }

        // 先找出完整比對的結果
        var cps: MutableList<CP?> = ArrayList()
        var distance = l
        while (cps.size < 3 && distance <= 50000) {
            cps = ArrayList()
            if (l == 0.0) distance += 1000.0
            val q = "select * from " + TABLE + " where " +
                    (x - distance) + " <= x and x <= " + (x + distance) + " and " +
                    (y - distance) + " <= y and y <= " + (y + distance)
            var idIdx: Int
            var tIdx: Int
            var numberIdx: Int
            var nameIdx: Int
            var xIdx: Int
            var yIdx: Int
            var hIdx: Int
            var infoIdx: Int
            val cursor = db!!.rawQuery(q, null)
            while (cursor.moveToNext()) {
                idIdx = cursor.getColumnIndex(ID)
                if (idIdx < 0) idIdx = 0
                tIdx = cursor.getColumnIndex(T)
                if (tIdx < 0) tIdx = 0
                numberIdx = cursor.getColumnIndex(Number)
                if (numberIdx < 0) numberIdx = 0
                nameIdx = cursor.getColumnIndex(Name)
                if (nameIdx < 0) nameIdx = 0
                xIdx = cursor.getColumnIndex(X)
                if (xIdx < 0) xIdx = 0
                yIdx = cursor.getColumnIndex(Y)
                if (yIdx < 0) yIdx = 0
                hIdx = cursor.getColumnIndex(H)
                if (hIdx < 0) hIdx = 0
                infoIdx = cursor.getColumnIndex(INFO)
                if (infoIdx < 0) infoIdx = 0
                val cp = CP(
                    cursor.getInt(idIdx),
                    cursor.getInt(tIdx),
                    cursor.getString(numberIdx),
                    cursor.getString(nameIdx),
                    cursor.getDouble(xIdx),
                    cursor.getDouble(yIdx),
                    cursor.getDouble(hIdx),
                    cursor.getString(infoIdx)
                )
                if (len(cp.y - y, cp.x - x) <= distance) {
                    cps.add(cp)
                }
            }
            if (l != 0.0) break
        }
        return cps
    }

    private fun len(dx: Double, dy: Double): Double {
        return Math.sqrt(dx * dx + dy * dy)
    }

    companion object {
        const val TAG = "MyLog"
        private const val DATABASE_NAME = "cp.db"
        private const val DATABASE_VERSION = 1
        private const val ID = "id"
        private const val T = "t"
        private const val Number = "number"
        private const val Name = "name"
        private const val X = "x"
        private const val Y = "y"
        private const val H = "h"
        private const val INFO = "info"
        private const val TABLE = "cp"
    }
}
