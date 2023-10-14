package com.wade.libs;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class BDatabase extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "b.db";
    private static final int DATABASE_VERSION = 1;
    private static final String ID="id";
    private static final String ENG="eng";
    private static final String CH="ch";
    private static final String FREQ="freq";
    private SQLiteDatabase db = null;
    private int ts = 0; // 1: 僅正體, 2: 僅簡體
    public BDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void setTs(int t) {
        ts = t;
    }

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
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= max){
            B b = new B();
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
    public ArrayList<String> getWord(String k, int start, int max, String table) {
        if (db == null) db = getWritableDatabase();
        if (k.length() == 0) return new ArrayList<>();
        ArrayList<String> list = new ArrayList<>();
        list.add(k);

        ArrayList<B> resExact=new ArrayList<>();

        k = k.toLowerCase(Locale.ENGLISH);

        ArrayList<String> tables = new ArrayList<>();
        if (!(table.equals("mix") || table.equals("ji") || table.equals("cj") ||
            table.equals("stroke") ||table.equals("sym"))) {
            table = "mix";
        }
        if (table.equals("mix")) {
			tables.add("mix");
            tables.add("sym");
            tables.add("ji");
            tables.add("cj");
            tables.add("stroke");
            max = max + max + max;
		} else {
			tables.add(table);
            tables.add("sym");
		}
		for (String t : tables) {
            ArrayList<B> r = query(k, start, max, t, "eng", FUZZY_EXACT);
            ArrayList<B> res = new ArrayList<>();
            for (B b : r) {
                if (b.ch.length() > 1) { // 多個中文字
                    String[] s = b.ch.split("");
                    for (String ch : Arrays.asList(s)) {
                        B bb = new B();
                        bb.ch = ch;
                        bb.eng = b.eng;
                        res.add(bb);
                        max--;
                    }
                } else {
                    res.add(b);
                    max--;
                }
                if (max <= 0) {
                    break;
                }
            }
            resExact.addAll(res);
            if (max <= 0) {
                break;
            }
		}
        if (max > 0) { // 如果不足，再找更多比對結果
            start = start < resExact.size() ? 0 : start - resExact.size();
            for (String t : tables) {
                ArrayList<B> r = query(k, start, max, t, "eng", FUZZY_PREFIX);
                ArrayList<B> res = new ArrayList<>();
                for (B b : r) {
                    if (b.ch.length() > 1) { // 多個中文字
                        String[] s = b.ch.split("");
                        for (String ch : Arrays.asList(s)) {
                            B bb = new B();
                            bb.ch = ch;
                            bb.eng = b.eng;
                            res.add(bb);
                            max--;
                        }
                    } else {
                        res.add(b);
                        max--;
                    }
                    if (max <= 0) {
                        break;
                    }
                }
                resExact.addAll(res);
                if (max <= 0) {
                    break;
                }
            }
        }

        for (B d : resExact) {
            list.add(d.ch);
        }
		return list;
	}

    @SuppressLint("Range")
    public ArrayList<String> getVocabulary(String k, int start, int max){
        if (db == null) db = getWritableDatabase();

        ArrayList<B> resExact=new ArrayList<>();
        ArrayList<B> res = query(k, start, max, "vocabulary", "ch", FUZZY_PREFIX);
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
