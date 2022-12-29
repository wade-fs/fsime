// 平均是 22.028
// 計算公式是 (x/1000)^2 + (y/1000 - 2000)^2
package com.wade.libs;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CPDB extends SQLiteAssetHelper {
    final static String TAG = "MyLog";

    private static final String DATABASE_NAME = "cp.db";
    private static final int DATABASE_VERSION = 1;
    private static final String ID="id";
    private static final String T="t";
    private static final String Number="number";
    private static final String Name="name";
    private static final String X="x";
    private static final String Y="y";
    private static final String H="h";
    private static final String INFO="info";

    private static final String TABLE = "cp";
    private SQLiteDatabase db = null;

    public CPDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public List<CP> getCpByNumber(String number){
        if (db == null) db=getWritableDatabase();
        if (db == null) {
            Log.d(TAG, "Not connect cp.db");
            return null;
        }

        int idIdx, tIdx, numberIdx, nameIdx, xIdx, yIdx, hIdx, infoIdx;
        // 先找出完整比對的結果
        Cursor cursor = db.rawQuery("select * from " + TABLE + " where "+
                "number like '%"+number+"' or "+
                "number like '"+number+"%' or "+
                "number like '%"+number+"%' or "+
                "number = '"+number+"'",
                null);
        List<CP> cps = new ArrayList<>();
        while (cursor.moveToNext()) {
            idIdx = cursor.getColumnIndex(ID);          if (idIdx < 0) idIdx = 0;
            tIdx = cursor.getColumnIndex(T);            if (tIdx < 0) tIdx = 0;
            numberIdx = cursor.getColumnIndex(Number);  if (numberIdx < 0) numberIdx = 0;
            nameIdx = cursor.getColumnIndex(Name);      if (nameIdx < 0) nameIdx = 0;
            xIdx = cursor.getColumnIndex(X);            if (xIdx < 0) xIdx = 0;
            yIdx = cursor.getColumnIndex(Y);            if (yIdx < 0) yIdx = 0;
            hIdx = cursor.getColumnIndex(H);            if (hIdx < 0) hIdx = 0;
            infoIdx = cursor.getColumnIndex(INFO);      if (infoIdx < 0) infoIdx = 0;
            CP cp = new CP(cursor.getInt(idIdx),
                    cursor.getInt(tIdx),
                    cursor.getString(numberIdx),
                    cursor.getString(nameIdx),
                    cursor.getDouble(xIdx),
                    cursor.getDouble(yIdx),
                    cursor.getDouble(hIdx),
                    cursor.getString(infoIdx)
            );
            cps.add(cp);
        }
        return cps;
    }
    public List<CP> getCp(double x, double y, double l){
        if (db == null) db=getWritableDatabase();
        if (db == null) {
            Log.d(TAG, "Not connect cp.db");
            return null;
        }

        // 先找出完整比對的結果
        List<CP> cps = new ArrayList<>();
        double distance = l;
        while (cps.size() < 3 && distance <= 50000) {
            cps = new ArrayList<>();
            if (l == 0) distance += 1000;
            String q = "select * from " + TABLE + " where " +
                    (x - distance) + " <= x and x <= " + (x + distance) + " and " +
                    (y - distance) + " <= y and y <= " + (y + distance);
            Cursor cursor = db.rawQuery(q, null);
            while (cursor.moveToNext()) {
                CP cp = new CP(cursor.getInt(cursor.getColumnIndex(ID)),
                        cursor.getInt(cursor.getColumnIndex(T)),
                        cursor.getString(cursor.getColumnIndex(Number)),
                        cursor.getString(cursor.getColumnIndex(Name)),
                        cursor.getDouble(cursor.getColumnIndex(X)),
                        cursor.getDouble(cursor.getColumnIndex(Y)),
                        cursor.getDouble(cursor.getColumnIndex(H)),
                        cursor.getString(cursor.getColumnIndex(INFO))
                );
                if (len(cp.y - y, cp.x - x) <= distance) {
//                    Log.d(TAG, "add CP : "+cp.id+", "+cp.number+","+cp.name+" for distance "+distance);
                    cps.add(cp);
                }
            }
            if (l != 0) break;
        }
        return cps;
    }
    private double len(double dx, double dy) { return Math.sqrt(dx*dx + dy*dy); }
}
