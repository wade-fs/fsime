// 平均是 22.028
// 計算公式是 (x/1000)^2 + (y/1000 - 2000)^2
package com.wade.libs;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/* 0 在 app/build.gradle 中要加 dependence
 *dependencies {
 *        compile 'com.readystatesoftware.sqliteasset:sqliteassethelper:+'
 *        }
 */
public class XYZ extends SQLiteAssetHelper {
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
            return -10000;
        }

        // 先找出完整比對的結果
        B bru=new B(), blu=new B(), bld=new B(), brd=new B();

// 4. use Cursor to retrieve rows
        Cursor clu = db.rawQuery("select * from "+TABLE+" where X <= "+x+" and Y >= "+y+" and L="+l+" order by x desc, y asc limit 1;", null);
        if(clu.moveToFirst()){
// 5. get the field from row
            blu.x  = clu.getDouble(clu.getColumnIndex(X));
            blu.y  = clu.getDouble(clu.getColumnIndex(Y));
            blu.z  = clu.getDouble(clu.getColumnIndex(H));
        }
// 6. XYZ xyz = new XYZ(context); // 這個定義在 Activity 中
        Cursor cru = db.rawQuery("select * from "+TABLE+" where X >= "+x+" and Y >= "+y+" and L="+l+" order by x asc, y asc limit 1;", null);
        if(cru.moveToFirst()){
            bru.x  = cru.getDouble(cru.getColumnIndex(X));
            bru.y  = cru.getDouble(cru.getColumnIndex(Y));
            bru.z  = cru.getDouble(cru.getColumnIndex(H));
        }

        Cursor cld = db.rawQuery("select * from "+TABLE+" where X <= "+x+" and Y <= "+y+" and L="+l+" order by x desc, y desc limit 1;", null);
        if(cld.moveToFirst()){
            bld.x  = cld.getDouble(cld.getColumnIndex(X));
            bld.y  = cld.getDouble(cld.getColumnIndex(Y));
            bld.z  = cld.getDouble(cld.getColumnIndex(H));
        }

        Cursor crd = db.rawQuery("select * from "+TABLE+" where X >= "+x+" and Y <= "+y+" and L="+l+" order by x asc, y desc limit 1;", null);
        if(crd.moveToFirst()){
            brd.x  = crd.getDouble(crd.getColumnIndex(X));
            brd.y  = crd.getDouble(crd.getColumnIndex(Y));
            brd.z  = crd.getDouble(crd.getColumnIndex(H));
        }

        double h=0, r=100000000;
        if (blu.x > 0) {
            r = sqr(x-blu.x, y-blu.y); h = blu.z;
        }
        if (bru.x > 0) {
            double r1 = sqr(x-bru.x, y-bru.y);
            if (r1 < r) {
                h = bru.z;
                r = r1;
            }
        }
        if (bld.x > 0) {
            double r1 = sqr(x - bld.x, y - bld.y);
            if (r1 < r) {
                h = bld.z;
                r = r1;
            }
        }
        if (brd.x > 0) {
            if (sqr(x-brd.x, y-brd.y) < r) {
                h = brd.z;
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
