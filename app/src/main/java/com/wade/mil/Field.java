package com.wade.mil;

import com.wade.libs.Tools;

class Field {
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
    private boolean cantainDMS(String v) {
        if (v.indexOf('°') >= 0) return true;
        if (v.indexOf('\'') >= 0) return true;
        if (v.indexOf('"') >= 0) return true;
        return false;
    }
    Field(String n, String d) {
        this(n,d,false,"",0);
    }
    Field(String n, String d, boolean a) {
        this(n,d,a,"",0);
    }
    Field(String n, String d, boolean a, int m) {
        this(n,d,a,"",m);
    }
    Field(String n, String d, boolean a, String v, int m) {
        name = n;
        desc = d;
        isAngle = a;
        setValue(v, m);
    }
    double getValue() {
        if (isAngle) {
            return degree;
        }
        return Tools.parseDouble(value);
    }
    void setValue(String v) {
        value = v;
    }
    void setValue(String v, int m) {
        value = v;
        setMode(m);
    }
    void setMode(int m) {
        if (!isAngle) return;
        if (cantainDMS(value)) {
            degree = Tools.parseDMS2(value);
        } else {
            degree = switch (m) {
                case Const.MODE_DEGREE -> // degree
                        Double.parseDouble(value);
                case Const.MODE_DMS -> // dms
                        Tools.parseDMS(value);
                case Const.MODE_MIL -> // mil
                        Tools.parseDouble(value) * 9.0 / 160.0;
                default -> // auto
                        Tools.autoDeg(value, 0);
            };
        }
    }
}
