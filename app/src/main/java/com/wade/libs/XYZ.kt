// 平均是 22.028
// 計算公式是 (x/1000)^2 + (y/1000 - 2000)^2
package com.wade.libs

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

/* 0 在 app/build.gradle 中要加 dependence
 *dependencies {
 *        compile 'com.readystatesoftware.sqliteasset:sqliteassethelper:+'
 *        }
 */
class XYZ  // 2. constructor super(....)
    (context: Context?) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private var db: SQLiteDatabase? = null
    fun getHeight(x: Double, y: Double, l: Int): Double {
// 3. getWritableDatabase()
        if (db == null) db = writableDatabase
        if (db == null) {
            return (-10000).toDouble()
        }

        // 先找出完整比對的結果
        val bru: B = B()
        val blu: B = B()
        val bld: B = B()
        val brd: B = B()
        // 4. use Cursor to retrieve rows
        val clu = db!!.rawQuery(
            "select * from " + TABLE + " where X <= " + x + " and Y >= " + y + " and L=" + l + " order by x desc, y asc limit 1;",
            null
        )
        if (clu.moveToFirst()) {
// 5. get the field from row
            blu.x = clu.getDouble(clu.getColumnIndex(X))
            blu.y = clu.getDouble(clu.getColumnIndex(Y))
            blu.z = clu.getDouble(clu.getColumnIndex(H))
        }
        // 6. XYZ xyz = new XYZ(context); // 這個定義在 Activity 中
        val cru = db!!.rawQuery(
            "select * from " + TABLE + " where X >= " + x + " and Y >= " + y + " and L=" + l + " order by x asc, y asc limit 1;",
            null
        )
        if (cru.moveToFirst()) {
            bru.x = cru.getDouble(cru.getColumnIndex(X))
            bru.y = cru.getDouble(cru.getColumnIndex(Y))
            bru.z = cru.getDouble(cru.getColumnIndex(H))
        }
        val cld = db!!.rawQuery(
            "select * from " + TABLE + " where X <= " + x + " and Y <= " + y + " and L=" + l + " order by x desc, y desc limit 1;",
            null
        )
        if (cld.moveToFirst()) {
            bld.x = cld.getDouble(cld.getColumnIndex(X))
            bld.y = cld.getDouble(cld.getColumnIndex(Y))
            bld.z = cld.getDouble(cld.getColumnIndex(H))
        }
        val crd = db!!.rawQuery(
            "select * from " + TABLE + " where X >= " + x + " and Y <= " + y + " and L=" + l + " order by x asc, y desc limit 1;",
            null
        )
        if (crd.moveToFirst()) {
            brd.x = crd.getDouble(crd.getColumnIndex(X))
            brd.y = crd.getDouble(crd.getColumnIndex(Y))
            brd.z = crd.getDouble(crd.getColumnIndex(H))
        }
        var h = 0.0
        var r = 100000000.0
        if (blu.x > 0) {
            r = sqr(x - blu.x, y - blu.y)
            h = blu.z
        }
        if (bru.x > 0) {
            val r1 = sqr(x - bru.x, y - bru.y)
            if (r1 < r) {
                h = bru.z
                r = r1
            }
        }
        if (bld.x > 0) {
            val r1 = sqr(x - bld.x, y - bld.y)
            if (r1 < r) {
                h = bld.z
                r = r1
            }
        }
        if (brd.x > 0) {
            if (sqr(x - brd.x, y - brd.y) < r) {
                h = brd.z
            }
        }
        return h
    }

    private fun sqr(x: Double, y: Double): Double {
        return Math.sqrt(x * x + y * y)
    }

    // x. define class to match database table definition
    inner class B internal constructor() {
        var id: Int
        var x: Double
        var y: Double
        var z: Double
        var l = 0.0

        init {
            id = -1
            z = l
            y = z
            x = y
        }
    }

    companion object {
        const val TAG = "MyLog"
        private const val DATABASE_NAME = "Data.db"
        private const val DATABASE_VERSION = 2
        private const val ID = "id"
        private const val X = "x"
        private const val Y = "y"
        private const val H = "z"
        private const val L = "l"
        private const val TABLE = "xyzl"
    }
}
