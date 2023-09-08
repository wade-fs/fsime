package com.wade.mil;

import com.wade.libs.Tools;

public class Field {
    String name;
    String desc;
    boolean isAngle;
    String value;
    double degree;
    // 取得欄位角度值
    double toDegree() {
        return degree;
    }
    // 取得欄位密位值
    double toMil() {
        return degree * 6400 / 360.0;
    }
    // 取得欄位度分秒值
    double[] toDms() {
        return Tools.Deg2Dms3(degree);
    }
    // 取得欄位徑度值
    double toRad() {
        return degree * Math.PI / 180.0;
    }
    private boolean cantainsDMS(String v) {
        if (v.indexOf('°') >= 0) return true;
        if (v.indexOf('\'') >= 0) return true;
        if (v.indexOf('"') >= 0) return true;
        return false;
    }
    public Field(String n, String d, boolean a, String v) {
        this(n,d,a,v,0);
    }
    public Field(String n, String d, boolean a, String v, int m) {
        name = n;
        desc = d;
        isAngle = a;
        if (isAngle) {
            if (cantainsDMS(v)) {
                degree = Tools.parseDMS2(v);
                return;
            }
            degree = switch (m) {
                case 1 -> // degree
                        Double.parseDouble(v);
                case 2 -> // dms
                        Tools.parseDMS(v);
                case 3 -> // mil
                        Tools.parseDouble(v) * 9.0 / 160.0;
                default -> // auto
                        Tools.autoDeg(v, 0);
            };
        }
    }
}
