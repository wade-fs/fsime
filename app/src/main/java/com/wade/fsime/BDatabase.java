package com.wade.fsime;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BDatabase extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "b.db";
    private static final int DATABASE_VERSION = 1;
    private static final String ID="id";
    private static final String ENG="eng";
    private static final String CH="ch";
    private static final String FREQ="freq";
    private SQLiteDatabase db = null;
    protected final Map<String, String> mapJuin;
//    private final Map<String, String> mapJuinEt;
    private final int ts = 0;

    public BDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mapJuin = new HashMap<>();
        mapJuin.put("1", "ㄅ");
        mapJuin.put("2", "ㄉ");
        mapJuin.put("3", "ˇ");
        mapJuin.put("4", "ˋ");
        mapJuin.put("5", "ㄓ");
        mapJuin.put("6", "ˊ");
        mapJuin.put("7", "˙");
        mapJuin.put("8", "ㄚ");
        mapJuin.put("9", "ㄞ");
        mapJuin.put("0", "ㄢ");
        mapJuin.put("-", "ㄦ");

        mapJuin.put("q", "ㄆ");
        mapJuin.put("w", "ㄊ");
        mapJuin.put("e", "ㄍ");
        mapJuin.put("r", "ㄐ");
        mapJuin.put("t", "ㄔ");
        mapJuin.put("y", "ㄗ");
        mapJuin.put("u", "ㄧ");
        mapJuin.put("i", "ㄛ");
        mapJuin.put("o", "ㄟ");
        mapJuin.put("p", "ㄣ");

        mapJuin.put("a", "ㄇ");
        mapJuin.put("s", "ㄋ");
        mapJuin.put("d", "ㄎ");
        mapJuin.put("f", "ㄑ");
        mapJuin.put("g", "ㄕ");
        mapJuin.put("h", "ㄘ");
        mapJuin.put("j", "ㄨ");
        mapJuin.put("k", "ㄜ");
        mapJuin.put("l", "ㄠ");
        mapJuin.put(";", "ㄤ");

        mapJuin.put("z", "ㄈ");
        mapJuin.put("x", "ㄌ");
        mapJuin.put("c", "ㄏ");
        mapJuin.put("v", "ㄒ");
        mapJuin.put("b", "ㄖ");
        mapJuin.put("n", "ㄙ");
        mapJuin.put("m", "ㄩ");
        mapJuin.put(",", "ㄝ");
        mapJuin.put(".", "ㄡ");
        mapJuin.put("/", "ㄥ");
/*
        mapJuinEt = new HashMap<>();
        mapJuinEt.put("1", "˙");
        mapJuinEt.put("2", "ˊ");
        mapJuinEt.put("3", "ˇ");
        mapJuinEt.put("4", "ˋ");
        mapJuinEt.put("7", "ㄑ");
        mapJuinEt.put("8", "ㄢ");
        mapJuinEt.put("9", "ㄣ");
        mapJuinEt.put("0", "ㄤ");
        mapJuinEt.put("-", "ㄥ");
        mapJuinEt.put("=", "ㄦ");

        mapJuinEt.put("q", "ㄟ");
        mapJuinEt.put("w", "ㄝ");
        mapJuinEt.put("e", "ㄧ");
        mapJuinEt.put("r", "ㄜ");
        mapJuinEt.put("t", "ㄊ");
        mapJuinEt.put("y", "ㄡ");
        mapJuinEt.put("u", "ㄩ");
        mapJuinEt.put("i", "ㄞ");
        mapJuinEt.put("o", "ㄛ");
        mapJuinEt.put("p", "ㄆ");

        mapJuinEt.put("a", "ㄚ");
        mapJuinEt.put("s", "ㄙ");
        mapJuinEt.put("d", "ㄉ");
        mapJuinEt.put("f", "ㄈ");
        mapJuinEt.put("g", "ㄐ");
        mapJuinEt.put("h", "ㄏ");
        mapJuinEt.put("j", "ㄖ");
        mapJuinEt.put("k", "ㄎ");
        mapJuinEt.put("l", "ㄌ");
        mapJuinEt.put(";", "ㄗ");
        mapJuinEt.put("'", "ㄘ");

        mapJuinEt.put("z", "ㄠ");
        mapJuinEt.put("x", "ㄨ");
        mapJuinEt.put("c", "ㄒ");
        mapJuinEt.put("v", "ㄍ");
        mapJuinEt.put("b", "ㄅ");
        mapJuinEt.put("n", "ㄋ");
        mapJuinEt.put("m", "ㄇ");
        mapJuinEt.put(",", "ㄓ");
        mapJuinEt.put(".", "ㄔ");
        mapJuinEt.put("/", "ㄕ");
 */
    }

    private String regexp(String s, Map<String, String>map) {
        StringBuilder res = new StringBuilder();
        for (String c : s.split("(?!^)")) {
            if (map.get(c) != null) {
                res.append(map.get(c));
            }
        }
        return res.toString();
    }

    private boolean isIn(ArrayList<B> res, B b) {
        for (B bb : res) {
            if (bb.ch.equals(b.ch)) return true;
        }
        return false;
    }

    @SuppressLint("Range")
    public ArrayList<B> getB(String k, int start){
        if (db == null) db=getWritableDatabase();
        k = k.toLowerCase().replaceAll("[^A-Za-z,\\.'\\[\\]]","").replaceAll("'", "''");
        String q; Cursor cursor; int count=0; boolean n;
        ArrayList<B> resExact=new ArrayList<>();
        if (k.length() == 0) return resExact;
        // 首先找完全比對的結果
        q = "SELECT * FROM b WHERE eng = \"" + k + "\" ORDER BY freq DESC LIMIT 40 OFFSET "+start+";";
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= 30){
            B b=new B();
            b.id = cursor.getInt(cursor.getColumnIndex(BDatabase.ID));
            b.eng=cursor.getString(cursor.getColumnIndex(BDatabase.ENG));
            b.ch=cursor.getString(cursor.getColumnIndex(BDatabase.CH));
            b.freq = cursor.getDouble(cursor.getColumnIndex(BDatabase.FREQ));
            if (ts == 1) b.ch = TS.StoT(b.ch);
            else if (ts == 2) b.ch = TS.TtoS(b.ch);
            if (!isIn(resExact, b)) {
                resExact.add(b);
                ++count;
            }
            n = cursor.moveToNext();
        }
        if (count >= 30) return resExact;

        // 如果不足，再找更多比對結果
        start = start < count ? 0 : start-count;
        q = "SELECT * FROM b WHERE eng LIKE \"" + k + "%\" AND eng != \""+k+"\" ORDER BY freq DESC LIMIT "+(40-count)+" OFFSET "+start+";";
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= 30){
            B b=new B();
            b.id = cursor.getInt(cursor.getColumnIndex(BDatabase.ID));
            b.eng=cursor.getString(cursor.getColumnIndex(BDatabase.ENG));
            b.ch=cursor.getString(cursor.getColumnIndex(BDatabase.CH));
            b.freq = cursor.getDouble(cursor.getColumnIndex(BDatabase.FREQ));
            if (ts == 1) b.ch = TS.StoT(b.ch);
            else if (ts == 2) b.ch = TS.TtoS(b.ch);
            if (!isIn(resExact, b)) {
                resExact.add(b);
                ++count;
            }
            n = cursor.moveToNext();
        }
        cursor.close();
        return resExact;
    }

    @SuppressLint("Range")
    public ArrayList<B> getJuin(String k, int start){
        if (db == null) db = getWritableDatabase();
        k = k.toLowerCase();
        String q; Cursor cursor; int count=0; boolean n;
        ArrayList<B> resExact=new ArrayList<>();
        q = "select * from z where ";
        String m = regexp(k, mapJuin);
        q += "eng like \""+m+"%\" ORDER BY freq DESC LIMIT 40 OFFSET "+start+";";
        Log.d("MyLog", "getJuin("+k+","+start+") Q='"+q+"'");
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= 30){
            B b=new B();
            b.id = cursor.getInt(cursor.getColumnIndex(BDatabase.ID));
            b.eng=cursor.getString(cursor.getColumnIndex(BDatabase.ENG));
            b.ch=cursor.getString(cursor.getColumnIndex(BDatabase.CH));
            b.freq = cursor.getDouble(cursor.getColumnIndex(BDatabase.FREQ));
            if (ts == 1) b.ch = TS.StoT(b.ch);
            else if (ts == 2) b.ch = TS.TtoS(b.ch);
            if (!isIn(resExact, b)) {
                resExact.add(b);
                ++count;
            }
            n = cursor.moveToNext();
        }
        cursor.close();
        return resExact;
    }
}
