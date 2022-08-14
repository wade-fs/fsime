package com.wade.fsime;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import java.util.ArrayList;

public class BDatabase extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "b.db";
    private static final int DATABASE_VERSION = 1;
    private static final String ID="id";
    private static final String ENG="eng";
    private static final String CH="ch";
    private static final String FREQ="freq";
    private SQLiteDatabase db = null;
    private final int ts = 0;

    public BDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private boolean isIn(ArrayList<B> res, B b) {
        for (B bb : res) {
            if (bb.ch.equals(b.ch)) return true;
        }
        return false;
    }

    @SuppressLint("Range")
    public ArrayList<B> getB(String k, int start, int max){
        if (db == null) db=getWritableDatabase();
        k = k.toLowerCase().replaceAll("[^A-Za-z,\\.'\\[\\]]","").replaceAll("'", "''");
        String q; Cursor cursor; int count=0; boolean n;
        ArrayList<B> resExact=new ArrayList<>();
        if (k.length() == 0) return resExact;
        // 首先找完全比對的結果
        q = "SELECT * FROM b WHERE eng = \"" + k + "\" ORDER BY freq DESC LIMIT "+max+" OFFSET "+start+";";
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= max){
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
        q = "SELECT * FROM b WHERE eng LIKE \"" + k + "%\" AND eng != \""+k+"\" ORDER BY freq DESC LIMIT "+(max-count)+" OFFSET "+start+";";
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= max){
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
    public ArrayList<B> getJuin(String k, int start, int max){
        if (db == null) db = getWritableDatabase();
        String q; Cursor cursor; int count=0; boolean n;
        ArrayList<B> resExact=new ArrayList<>();
        q = "select * from z where ";
        q += "eng like \""+k+"%\" ORDER BY freq DESC LIMIT "+max+" OFFSET "+start+";";
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= max){
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
    public ArrayList<B> getF(String k, int start, int max){
        if (db == null) db = getWritableDatabase();
        String q; Cursor cursor; int count=0; boolean n;
        ArrayList<B> resExact=new ArrayList<>();
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