package com.wade.libs;
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

        String q = "SELECT * FROM mix WHERE ch = '" + word + "';";
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
        cursor.close();
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

    int FUZZY_EXACT = 0;
    int FUZZY_PREFIX = 1;
    int FUZZY_FULL = 2;
    @SuppressLint("Range")
    private ArrayList<B> query(String k, int start, int max, String table, String field, int fuzzy) {
        ArrayList<B> list = new ArrayList<>();

        String q;
        Cursor cursor;
        int count=0;
        boolean n;

        if (k.indexOf('"') >= 0) return list;
        k.replaceAll("\"", "\"\"");
        q = "select * from "+table+" where ";
        if (fuzzy == FUZZY_EXACT) {
            q += field + " = \"" + k + "\" LIMIT " + max + " OFFSET " + start + ";";
        } else if (fuzzy == FUZZY_PREFIX) {
            q += field + " like \"" + k + "_%\" LIMIT " + max + " OFFSET " + start + ";";
        } else {
            q += field +" like \"%" + k + "%\" LIMIT " + max + " OFFSET " + start + ";";
        }
        Log.i("fsime",q);
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= max){
            B b=new B();
            b.id = cursor.getInt(cursor.getColumnIndex(BDatabase.ID));
            b.ch=cursor.getString(cursor.getColumnIndex(BDatabase.CH));
            if (ts == 1) { b.ch = TS.StoT(b.ch); }
            else if (ts == 2) { b.ch = TS.TtoS(b.ch); }
            if (table == "vocabulary") {
                int idx = b.ch.indexOf(k);
                if (idx < b.ch.length()-1) {
                    b.ch = b.ch.substring(idx+1, idx + 2);
                }
            }
            if (!isIn(list, b)) {
                list.add(b);
                ++count;
            }
            n = cursor.moveToNext();
        }
        cursor.close();
        return list;
    }
    @SuppressLint("Range")
    public ArrayList<String> getWord(String k, int start, int max, String table){
        if (db == null) db = getWritableDatabase();
        if (k.length() == 0) return new ArrayList<>();
        ArrayList<String> list = new ArrayList<>();
        list.add(k);

        ArrayList<B> resExact=new ArrayList<>();

        k = k.toLowerCase(Locale.ENGLISH);

        ArrayList<String> tables = new ArrayList<>();
        if (table.equals("mix")) {
			tables.add("mix");
            tables.add("sym");
            tables.add("ji");
            tables.add("cj");
            max = max + max + max;
		} else {
			tables.add(table);
		}
		for (String t : tables) {
            ArrayList<B> res = query(k, start, max, t, "eng", FUZZY_EXACT);
            resExact.addAll(res);
		}
        if (resExact.size() < max) { // 如果不足，再找更多比對結果
            start = start < resExact.size() ? 0 : start - resExact.size();
            for (String t : tables) {
                ArrayList<B> res = query(k, start, max, t, "eng", FUZZY_PREFIX);
                resExact.addAll(res);
            }
        }

        for (B d : resExact) {
            list.add(d.ch);
        }
		return list;
	}

    @SuppressLint("Range")
    public ArrayList<String> getF(String k, int start, int max, String table){
        if (db == null) db = getWritableDatabase();

        ArrayList<B> resExact=new ArrayList<>();
        ArrayList<B> res = query(k, start, max, table, "ch", FUZZY_PREFIX);
        resExact.addAll(res);

        ArrayList<String> list = new ArrayList<>();
        list.add(k.substring(0,1));
        for (B b : resExact) {
            if (!isIn(list, b.ch)) {
                list.add(b.ch);
            }
        }
        return list;
    }
}
