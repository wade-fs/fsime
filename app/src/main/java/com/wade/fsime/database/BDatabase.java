package com.wade.fsime.database;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import androidx.annotation.RequiresApi;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import java.util.ArrayList;
import java.util.Map;

public class BDatabase extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "b.db";
    private static final int DATABASE_VERSION = 1;
    private static final String ID="id";
    private static final String ENG="eng";
    private static final String CH="ch";
    private static final String FREQ="freq";
    private SQLiteDatabase db = null;
    private int ts = 0;
    Map<String, String> e2j  = new HashMap<String, String>() {{
        put("a", "ㄇ"); put("b", "ㄖ"); put("c", "ㄏ"); put("d", "ㄎ"); put("e", "ㄍ");
        put("f", "ㄑ"); put("g", "ㄕ"); put("h", "ㄘ"); put("i", "ㄛ"); put("j", "ㄨ");
        put("k", "ㄜ"); put("l", "ㄠ"); put("m", "ㄩ"); put("n", "ㄙ"); put("o", "ㄟ");
        put("p", "ㄣ"); put("q", "ㄆ"); put("r", "ㄐ"); put("s", "ㄋ"); put("t", "ㄔ");
        put("u", "ㄧ"); put("v", "ㄒ"); put("w", "ㄊ"); put("x", "ㄌ"); put("y", "ㄗ");
        put("z", "ㄈ"); put("1", "ㄅ"); put("2", "ㄉ"); put("3", "ˇ"); put("4", "ˋ");
        put("5", "ㄓ"); put("6", "ˊ"); put("7", "˙"); put("8", "ㄚ"); put("9", "ㄞ");
        put("0", "ㄢ"); put("-", "ㄦ"); put(";", "ㄤ"); put(",", "ㄝ"); put(".", "ㄡ");
        put("/", "ㄥ");
    }};
// A  B  C  D  E  F  G  H  I  J  K  L  M  N  O  P  Q  R  S  T  U  V  W  X  Y  Z
// ㄇ ㄖ ㄏ ㄎ ㄍ ㄑ ㄕ ㄘ ㄛ ㄨ ㄜ ㄠ ㄩ ㄙ ㄟ ㄣ ㄆ ㄐ ㄋ ㄔ ㄧ ㄒ ㄊ ㄌ ㄗ ㄈ
// 1  2  3 4 5  6 7 8  9  0  -  ;  ,  .  /
// ㄅ ㄉ ˇ ˋ ㄓ ˊ ˙ ㄚ ㄞ ㄢ ㄦ ㄤ ㄝ ㄡ ㄥ
    public BDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void setTs(int t) { ts = t; }

    private boolean isIn(ArrayList<B> res, B b) {
        for (B bb : res) {
            if (bb.ch.equals(b.ch)) return true;
        }
        return false;
    }

    private boolean isIn(ArrayList<String> res, String b) {
        for (String bb : res) {
            if (bb.equals(b)) return true;
        }
        return false;
    }

    public ArrayList<String> getCompose(String word) {
        if (db == null) db=getWritableDatabase();
        ArrayList<String> composes = new ArrayList<>();

        String q = "SELECT * FROM b WHERE ch = \"" + word + "\";";
        Cursor cursor = db.rawQuery(q, null);
        boolean next = cursor.moveToFirst();
        while (next) {
            int idx = cursor.getColumnIndex(BDatabase.ENG);
            if (idx >= 0) {
                String compose = cursor.getString(idx);
                composes.add(compose);
            }
            next = cursor.moveToNext();
        }
        return composes;
    }

    public void saveCompose(String ch, ArrayList<String> composes) {
        if (ch.length() != 1) {
            return;
        }
        if (db == null) db=getWritableDatabase();
        db.delete("b", "ch=?", new String[]{ch});
        for (String item : composes) {
            ContentValues values = new ContentValues();
            values.put("eng", item);
            values.put("ch", ch);
            db.insert("b", null, values);
        }
    }

    private void Logi(String msg) {
        Log.i("FSIME", msg);
    }

    @SuppressLint("Range")
    public ArrayList<String> getWord(String k, int start, int max, String table){
        if (db == null) db = getWritableDatabase();
		ArrayList<String> list = new ArrayList<>();
		list.add(k);

        k = k.toLowerCase(Locale.ENGLISH);
        String q; Cursor cursor; int count=0; boolean n;
        ArrayList<B> resExact=new ArrayList<>();
        if (k.length() == 0) return list;
		ArrayList<String> tables = new ArrayList<>();

        if (table.equals("b")) { // mix
			tables.add("b"); tables.add("z"); tables.add("c");
            max = max + max + max;
		} else {
			tables.add(table);
		}
        String pre = "SELECT * FROM ";
        String kk = "";
		for (String t : tables) {
            kk = "";
            if (t.equals("z")) {
                for (String s : k.split("")) {
                    kk = kk + e2j.get(s);
                }
            } else {
                kk = k;
            }
            String post = " WHERE eng = \"" + kk + "\" ORDER BY freq DESC LIMIT "+max+" OFFSET "+start+";";
            // 首先找完全比對的結果
            q = pre + t + post;
            cursor=db.rawQuery(q, null);
            n = cursor.moveToFirst();
            while(n && count <= max){
                B b=new B();
                b.id = cursor.getInt(cursor.getColumnIndex(BDatabase.ID));
                b.eng=cursor.getString(cursor.getColumnIndex(BDatabase.ENG));
                b.ch=cursor.getString(cursor.getColumnIndex(BDatabase.CH));
                b.freq = cursor.getDouble(cursor.getColumnIndex(BDatabase.FREQ));
                if (ts == 1) { b.ch = TS.StoT(b.ch); }
                else if (ts == 2) { b.ch = TS.TtoS(b.ch); }
                if (!isIn(resExact, b)) {
                    resExact.add(b);
                    ++count;
                }
                n = cursor.moveToNext();
            }
		}
        kk = k;
        if (count < 30) { // 如果不足，再找更多比對結果
            start = start < count ? 0 : start - count;
            q = "SELECT * FROM "+table+" WHERE eng LIKE \"" + kk + "%\" AND eng != \"" + kk + "\" ORDER BY freq DESC LIMIT " + (max - count) + " OFFSET " + start + ";";
            cursor = db.rawQuery(q, null);
            n = cursor.moveToFirst();
            while (n && count <= max) {
                B b = new B();
                b.id = cursor.getInt(cursor.getColumnIndex(BDatabase.ID));
                b.eng = cursor.getString(cursor.getColumnIndex(BDatabase.ENG));
                b.ch = cursor.getString(cursor.getColumnIndex(BDatabase.CH));
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
        }
        for (B d : resExact) {
            list.add(d.ch);
        }
		return list;
	}

    @SuppressLint("Range")
    public ArrayList<String> getF(String k, int start, int max){
        if (db == null) db = getWritableDatabase();
		ArrayList<String> list = new ArrayList<>();
		list.add(k.substring(0,1));

        String q; Cursor cursor; int count=0; boolean n;
        q = "select * from f where ";
        q += "ch like \""+k+"%\" LIMIT "+max+" OFFSET "+start+";";
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= max){
            B b=new B();
            b.id = cursor.getInt(cursor.getColumnIndex(BDatabase.ID));
            b.ch=cursor.getString(cursor.getColumnIndex(BDatabase.CH));
            if (ts == 1) b.ch = TS.StoT(b.ch);
            else if (ts == 2) b.ch = TS.TtoS(b.ch);
            b.ch = b.ch.substring(1,2);
            if (!isIn(list, b.ch)) {
                list.add(b.ch);
                ++count;
            }
            n = cursor.moveToNext();
        }
        cursor.close();
        return list;
    }
}
