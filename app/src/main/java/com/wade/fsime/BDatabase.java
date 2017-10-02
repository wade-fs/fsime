package com.wade.fsime;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BDatabase extends SQLiteAssetHelper {
    private static final String TAG="MyLog";
    private static final String DATABASE_NAME = "b.db";
    private static final int DATABASE_VERSION = 1;
    private static final String ID="id";
    private static final String ENG="eng";
    private static final String CH="ch";
    private static final String FREQ="freq";
    private static final String TABLE_B="b";
    private static final String TABLE_Z="z";
    private SQLiteDatabase db = null;
    Map<String, String[]> mapBS;
    private int ts = 0;

    public BDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mapBS = new HashMap<String, String[]>();
        mapBS.put("1", new String[] { "a", "g", "m" });
        mapBS.put("2", new String[] { "b", "x", "z" });
        mapBS.put("3", new String[] { "c", "i", "j" });
        mapBS.put("4", new String[] { "d", "s", "q" });
        mapBS.put("5", new String[] { "e", "h", "p" });
        mapBS.put("6", new String[] { "f", "t", "k" });
        mapBS.put("7", new String[] { "l", "w", "y" });
        mapBS.put("8", new String[] { "n", "u", "'" });
        mapBS.put("9", new String[] { "o", "r" });
        mapBS.put("0", new String[] { "v" });
        mapBS.put(",", new String[] { "," });
        mapBS.put(".", new String[] { "." });
        mapBS.put(";", new String[] { "[", "]" });
    }

    private List<String> regexp(String s, Map<String, String[]>map) {
        List<String> maps = new ArrayList<>();
        if (s.length() > 1) {
            String a = s.substring(0, 1);
            s = s.substring(1);
            if (map.get(a) != null) {
                for (String b : map.get(a)) {
                    for (String d : regexp(s, map)) {
                        maps.add(b + d);
                    }
                }
            } else {
                for (String d : regexp(s, map)) {
                    maps.add(a + d);
                }
            }
        } else if (s.length() == 1){
            if (map.get(s) != null)
                for (String c : map.get(s)) {
                    maps.add(c);
                }
        }
        return maps;
    }

    private boolean isIn(ArrayList<B> res, B b, int field) {
        if (field == 0) { // ENG
            for (B bb : res) {
                if (bb.eng.equals(b.eng)) return true;
            }
        } else if (field == 1) { // CH
            for (B bb : res) {
                if (bb.ch.equals(b.ch)) return true;
            }
        }
        return false;
    }
    public void addB(String eng, String ch, String tb){
        if (db == null) db=getWritableDatabase();
        String[] columns={ID, ENG, CH, FREQ};
        ContentValues cv = new ContentValues();
        cv.put("eng", eng);
        cv.put("ch", ch);
        cv.put("freq", 14);
//        Log.d(TAG, "add "+eng+","+ch+",14 into table "+tb);
        long id = db.insert(tb, null, cv);
    }

    public ArrayList<B> getB2(String k, int start) {
        if (db == null) db = getWritableDatabase();
        k = k.toLowerCase()
                .replaceAll("[\"\\|{}<>/=-_()!@#$%^&;:`~]","")
                .replaceAll("'","''")
                .replaceAll("\\[", "\\[[\\]")
                .replaceAll("\\]", "\\[]\\]");
        String q; Cursor cursor; int count=0; boolean n; List<String> m;
        ArrayList<B> resExact=new ArrayList<>();
        q = "select * from b where ";
        m = regexp(k, mapBS);

        if (m != null && m.size() > 0) {
            boolean isStarted = false;
            for (String s : m) {
                if (isStarted)
                    q += " or eng=\"" + s + "\"";
                else {
                    q += " eng=\"" + s + "\"";
                    isStarted = true;
                }
            }
        } else {
            q += " eng=\""+k+"\"";
        }
        q += " ORDER BY freq DESC LIMIT 40 OFFSET "+start+";";
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
            if (!isIn(resExact, b, 1)) {
                resExact.add(b);
                ++count;
            }
            n = cursor.moveToNext();
        }
        return resExact;
    }
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
            if (!isIn(resExact, b, 1)) {
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
            if (!isIn(resExact, b, 1)) {
                resExact.add(b);
                ++count;
            }
            n = cursor.moveToNext();
        }
        return resExact;
    }

    public ArrayList<B> getC(String k, int start){
        if (db == null) db=getWritableDatabase();
        k = k.replaceAll("[\"\\|{}<>/=-_()!@#$%^&;:`~]", "").toLowerCase();
        String q; Cursor cursor; int count=0; boolean n;
        ArrayList<B> resExact=new ArrayList<>();
        if (k.length() == 0) return resExact;
        q = "SELECT * FROM c WHERE eng = \"" + k + "\" ORDER BY freq DESC LIMIT 30 OFFSET "+start+";";
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= 30){
            B b=new B();
            b.id = cursor.getInt(cursor.getColumnIndex(BDatabase.ID));
            b.eng=cursor.getString(cursor.getColumnIndex(BDatabase.ENG));
            b.ch=cursor.getString(cursor.getColumnIndex(BDatabase.CH));
            b.freq = cursor.getDouble(cursor.getColumnIndex(BDatabase.FREQ));
            resExact.add(b);
            n = cursor.moveToNext();
            ++count;
        }
        if (count >= 30) return resExact;

        start = start < count ? 0 : start-count;
        q = "SELECT * FROM c WHERE eng LIKE \"" + k + "%\" AND eng != \""+k+"\" ORDER BY freq DESC LIMIT "+(30-count)+" OFFSET "+start+";";
        cursor=db.rawQuery(q, null);
        n = cursor.moveToFirst();
        while(n && count <= 30){
            B b=new B();
            b.id = cursor.getInt(cursor.getColumnIndex(BDatabase.ID));
            b.eng=cursor.getString(cursor.getColumnIndex(BDatabase.ENG));
            b.ch=cursor.getString(cursor.getColumnIndex(BDatabase.CH));
            b.freq = cursor.getDouble(cursor.getColumnIndex(BDatabase.FREQ));
            if (!isIn(resExact, b, 0)) {
                resExact.add(b);
                ++count;
            }
            n = cursor.moveToNext();
        }
        return resExact;
    }
    public ArrayList<B> getPP(String k, int start) {
        if (db == null) db=getWritableDatabase();
        k = TS.StoT(k);
        String q; Cursor cursor; int count=0; boolean n;
        ArrayList<B> resExact=new ArrayList<>();
        if (k.length() == 0) return resExact;
        k = TS.StoT(k);
        q = "SELECT * FROM pp WHERE eng = \"" + k + "\" ORDER BY freq DESC LIMIT 30 OFFSET "+start+";";
//        Log.d(TAG, "getPP("+k+","+start+") "+q);
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
//            Log.d(TAG, "\t=> "+b.eng+","+b.ch+","+b.freq);
            if (!isIn(resExact, b, 1)) {
                resExact.add(b);
                ++count;
            }
            n = cursor.moveToNext();
        }
        return resExact;
    }

    public void updateRow(B b, String tb) {
        if (db == null) db = getWritableDatabase();
        double freq = b.freq + 1/b.freq;
        String q="";
        if (tb.equals("b"))
            q = "UPDATE b SET freq="+freq+" WHERE ch='"+b.ch+"';";
        else q = "UPDATE c SET freq="+freq+" WHERE eng='"+b.eng+"';";
        Cursor c = db.rawQuery(q, null);
        c.moveToFirst();
        c.close();
    }
    public boolean isFreq(String k){
        if (db == null) db=getWritableDatabase();
        k = TS.StoT(k);
        String[] columns={"ch"};
        k = k.replaceAll("'","\'");

        // 先找出完整比對的結果
        Cursor cursor=db.query("r", columns, "ch='"+k+"'", null, null, null, null);
        ArrayList<B> resExact=new ArrayList<>();
        return cursor.moveToFirst();
    }
    public boolean isInPP(String eng, String ch){
        if (db == null) db=getWritableDatabase();
        eng = TS.StoT(eng);
        ch  = TS.StoT(ch);
        String[] columns={"eng,ch"};

        // 先找出完整比對的結果
        Cursor cursor=db.query("pp", columns, "eng=\""+eng+"\" and ch=\""+ch+"\"", null, null, null, null);
        return cursor.moveToFirst();
    }

    public int setTs(int v) {
//        Log.d(TAG, "setTs("+v+")");
        return ts = v;
    }
}
