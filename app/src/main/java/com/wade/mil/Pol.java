package com.wade.mil;

import com.wade.libs.Tools;

import java.util.Locale;

public class Pol {
    Field[] app = new Field[2];
    int step;
    int mode;
    double d, a;
    Pol() {
        step = 0;
        mode = Const.MODE_AUTO;
        app[0] = new Field("x", "橫坐標", false);
        app[1] = new Field("y", "縱坐標", false);
        d = a = 0.0;
    }
    Pol(int m) {
        super();
        mode = m;
    }
    void setMode(int m) {
        mode = m;
        for (int i=0; i<app.length; i++) {
            app[i].setMode(m);
        }
    }
    String getDesc() {
        return app[step].desc;
    }
    void setValue(String v) {
        app[step].setValue(v);
    }
    String getValue() {
        return app[step].value;
    }
//    double getDegree() {
//        return app[step].degree;
//    }
    boolean next() {
        if (step < app.length-1) {
            step = step + 1;
            return true;
        }
        return false;
    }
    boolean back() {
        if (step > 0) {
            step = step -1;
            return true;
        }
        return false;
    }
    double[] calc() {
        for (int i=0; i<app.length; i++) {
            if (app[i].value.length() == 0) {
                return null;
            }
        }
        double[] res = Tools.POLd(app[1].getValue(), app[0].getValue());
        d = res[0];
        a = res[1];
        return res;
    }
    String string() {
        switch (mode) {
            case Const.MODE_DEGREE, Const.MODE_AUTO -> {
                return String.format(Locale.TAIWAN, "距離%.2f公尺 方位角%.2f度", d, Tools.Deg2Mil(a));
            }
            case Const.MODE_DMS -> {
                return String.format(Locale.TAIWAN, "距離%.2f公尺 方位角%s", d, Tools.Deg2DmsStr2(a));
            }
            default -> {
                return String.format(Locale.TAIWAN, "距離%.2f公尺 方位角%.2f密位", d, Tools.Deg2Mil(a));
            }
        }
    }
}
