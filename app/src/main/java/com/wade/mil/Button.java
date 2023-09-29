package com.wade.mil;

import java.util.ArrayList;
import java.util.List;

public interface Button {
    String name = "Button";
    List<Field> field = new ArrayList<>();
    int step = -1;
    int mode = Const.MODE_AUTO;
    double d=0.0f, a=0.0f;
    void setName(String n);
    void addField(String n, String d, boolean a, int m);
    void setMode(int m);
    String getDesc();
    void setValue(String v);
    String getValue();
    //    double getDegree() {
//        return field[step].degree;
//    }
    boolean next();
    boolean back();
    double[] calc();
    String string();
}
