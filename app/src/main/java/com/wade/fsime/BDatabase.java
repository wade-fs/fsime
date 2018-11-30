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
    private SQLiteDatabase db = null;
    private Map<String, String[]> mapBS;  // 供兩行模式轉換
    private Map<String, String> mapJuin;
    private int ts = 0;

    public BDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mapBS = new HashMap<>();
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

    }

    private List<String> regexp2(String s, Map<String, String[]>map) {
        List<String> maps = new ArrayList<>();
        if (s.length() > 1) {
            String a = s.substring(0, 1);
            s = s.substring(1);
            if (map.get(a) != null) {
                for (String b : map.get(a)) {
                    for (String d : regexp2(s, map)) {
                        maps.add(b + d);
                    }
                }
            } else {
                for (String d : regexp2(s, map)) {
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

    private String regexp(String s, Map<String, String>map) {
        String res = "";
        for (String c : s.split("(?!^)")) {
            if (map.get(c) != null) {
                res += map.get(c);
            }
        }
        return res;
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
    void addB(String eng, String ch, String tb){
        if (db == null) db=getWritableDatabase();
        String[] columns={ID, ENG, CH, FREQ};
        ContentValues cv = new ContentValues();
        cv.put("eng", eng);
        cv.put("ch", ch);
        cv.put("freq", 14);
        long id = db.insert(tb, null, cv);
    }

    // 需要轉換鍵盤 + 混合嘸蝦米及注音資料庫
    ArrayList<B> getB2(String k, int start) {
        if (db == null) db = getWritableDatabase();
        k = k.toLowerCase()
                .replaceAll("[\"\\|{}<>/=-_()!@#$%^&;:`~]","")
                .replaceAll("'","''")
                .replaceAll("\\[", "\\[[\\]")
                .replaceAll("\\]", "\\[]\\]");
        String q; Cursor cursor; int count=0; boolean n; List<String> m;
        ArrayList<B> resExact=new ArrayList<>();
        q = "select * from b where ";
        m = regexp2(k, mapBS);

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
        cursor.close();
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
        cursor.close();
        return resExact;
    }

    public ArrayList<B> getJuin(String k, int start){
        if (db == null) db = getWritableDatabase();
        k = k.toLowerCase();
        String q; Cursor cursor; int count=0; boolean n;
        ArrayList<B> resExact=new ArrayList<>();
        q = "select * from juin where ";
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
            if (!isIn(resExact, b, 1)) {
                resExact.add(b);
                ++count;
            }
            n = cursor.moveToNext();
        }
        cursor.close();
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
        cursor.close();
        return resExact;
    }

    void updateRow(B b, String tb) {
        if (db == null) db = getWritableDatabase();
        double freq = b.freq + 1/b.freq;
        String q="";
        if (tb.equals("b"))
            q = "UPDATE b SET freq="+freq+" WHERE ch='"+b.ch+"';";
        else q = "UPDATE juin SET freq="+freq+" WHERE eng='"+b.eng+"';";
        Cursor c = db.rawQuery(q, null);
        c.moveToFirst();
        c.close();
    }

    boolean isFreq(String k){
        if (db == null) db=getWritableDatabase();
        k = TS.StoT(k);
        String[] columns={"ch"};
        k = k.replaceAll("'","\'");

        // 先找出完整比對的結果
        Cursor cursor=db.query("r", columns, "ch='"+k+"'", null, null, null, null);
        ArrayList<B> resExact=new ArrayList<>();
        return cursor.moveToFirst();
    }
    boolean isInPP(String eng, String ch){
        if (db == null) db=getWritableDatabase();
        eng = TS.StoT(eng);
        ch  = TS.StoT(ch);
        String[] columns={"eng,ch"};

        // 先找出完整比對的結果
        Cursor cursor=db.query("pp", columns, "eng=\""+eng+"\" and ch=\""+ch+"\"", null, null, null, null);
        return cursor.moveToFirst();
    }

//    public int setTs(int v) {
//        return ts = v;
//    }
}
