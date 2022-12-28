// 平均是 22.028
// 計算公式是 (x/1000)^2 + (y/1000 - 2000)^2
package com.wade.libs;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/* 0 在 app/build.gradle 中要加 dependence
 *dependencies {
 *        compile 'com.readystatesoftware.sqliteasset:sqliteassethelper:+'
 *        }
 */
// 1. extends /home/wade/src/均利/APK/wade_libs/app/src/main/java/com/wade/libs/XYZ.java
public class XYZ extends SQLiteAssetHelper {
    final static String TAG = "MyLog";

    private static final String DATABASE_NAME = "Data.db";
    private static final int DATABASE_VERSION = 2;
    private static final String ID="id";
    private static final String X="x";
    private static final String Y="y";
    private static final String H="z";
    private static final String L="l";

    private static final String TABLE = "xyzl";
    private SQLiteDatabase db = null;
// 2. constructor super(....)
    public XYZ(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public double getHeight(double x, double y, int l){
// 3. getWritableDatabase()
        if (db == null) db=getWritableDatabase();
        if (db == null) {
            Log.d(TAG, "Not connect Data.db");
            return -10000;
        }

        // 先找出完整比對的結果
        B bru=new B(), blu=new B(), bld=new B(), brd=new B();

        Log.d(TAG, "LU select * from "+TABLE+" where X <= "+x+" and Y >= "+y+" and L="+l+" order by x desc, y asc limit 1;", null);
// 4. use Cursor to retrieve rows
        Cursor clu = db.rawQuery("select * from "+TABLE+" where X <= "+x+" and Y >= "+y+" and L="+l+" order by x desc, y asc limit 1;", null);
        if(clu.moveToFirst()){
// 5. get the field from row
            blu.x  = clu.getDouble(clu.getColumnIndex(X));
            blu.y  = clu.getDouble(clu.getColumnIndex(Y));
            blu.z  = clu.getDouble(clu.getColumnIndex(H));
            Log.d(TAG, String.format(" LU(%.2f,%.2f) : %.3f", blu.x, blu.y, blu.z));
        } else Log.d(TAG, "No LU");
// 6. XYZ xyz = new XYZ(context); // 這個定義在 Activity 中
        Log.d(TAG, "RU select * from "+TABLE+" where X >= "+x+" and Y >= "+y+" and L="+l+" order by x asc, y asc limit 1;");
        Cursor cru = db.rawQuery("select * from "+TABLE+" where X >= "+x+" and Y >= "+y+" and L="+l+" order by x asc, y asc limit 1;", null);
        if(cru.moveToFirst()){
            bru.x  = cru.getDouble(cru.getColumnIndex(X));
            bru.y  = cru.getDouble(cru.getColumnIndex(Y));
            bru.z  = cru.getDouble(cru.getColumnIndex(H));
            Log.d(TAG, String.format(" RU(%.2f,%.2f) : %.3f", bru.x, bru.y, bru.z));
        } else Log.d(TAG, "No RU");

        Log.d(TAG,"LD select * from "+TABLE+" where X <= "+x+" and Y <= "+y+" and L="+l+" order by x desc, y desc limit 1;");
        Cursor cld = db.rawQuery("select * from "+TABLE+" where X <= "+x+" and Y <= "+y+" and L="+l+" order by x desc, y desc limit 1;", null);
        if(cld.moveToFirst()){
            bld.x  = cld.getDouble(cld.getColumnIndex(X));
            bld.y  = cld.getDouble(cld.getColumnIndex(Y));
            bld.z  = cld.getDouble(cld.getColumnIndex(H));
            Log.d(TAG, String.format(" LD(%.2f,%.2f) : %.3f", bld.x, bld.y, bld.z));
        } else Log.d(TAG, "No LD");

        Log.d(TAG, "RD select * from "+TABLE+" where X >= "+x+" and Y <= "+y+" and L="+l+" order by x asc, y desc limit 1;");
        Cursor crd = db.rawQuery("select * from "+TABLE+" where X >= "+x+" and Y <= "+y+" and L="+l+" order by x asc, y desc limit 1;", null);
        if(crd.moveToFirst()){
            brd.x  = crd.getDouble(crd.getColumnIndex(X));
            brd.y  = crd.getDouble(crd.getColumnIndex(Y));
            brd.z  = crd.getDouble(crd.getColumnIndex(H));
            Log.d(TAG, String.format(" RD(%.2f,%.2f) : %.3f", brd.x, brd.y, brd.z));
        } else Log.d(TAG, "No RD");

        double h=0, r=100000000;
        if (blu.x > 0) {
            r = sqr(x-blu.x, y-blu.y); h = blu.z;
            Log.d(TAG, String.format("*LU(%.2f,%.2f) : %.3f", blu.x, blu.y, blu.z));
        }
        if (bru.x > 0) {
            double r1 = sqr(x-bru.x, y-bru.y);
            if (r1 < r) {
                h = bru.z;
                r = r1;
                Log.d(TAG, String.format("*RU(%.2f,%.2f) : %.3f", bru.x, bru.y, bru.z));
            }
        }
        if (bld.x > 0) {
            double r1 = sqr(x - bld.x, y - bld.y);
            if (r1 < r) {
                h = bld.z;
                r = r1;
                Log.d(TAG, String.format("*LD(%.2f,%.2f) : %.3f", bld.x, bld.y, bld.z));
            }
        }
        if (brd.x > 0) {
            if (sqr(x-brd.x, y-brd.y) < r) {
                h = brd.z;
                Log.d(TAG, String.format("*RD(%.2f,%.2f) : %.3f", brd.x, brd.y, brd.z));
            }
        }
        return h;
    }
    private double sqr(double x, double y) { return Math.sqrt(x*x + y*y); }

// x. define class to match database table definition
    public class B{
        public int id;
        public double x, y, z, l;
        B() {
            id = -1;
            x = y = z = l = 0;
        }
    }
}
