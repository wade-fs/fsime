package com.wade.mil;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Mil {
    int angleMode = Const.MODE_AUTO; // 0:auto 1:degree 2:dms 3:mil
    public static String appName = "";
    private Pol appPol = new Pol();
    public static final List<String> apps = new ArrayList<>(List.of(
            "GPS", "PADS", "前交", "導線",
            "POL", "REC", "坐換", "方距", "內角", "三角",
            "rad", "天體", "方格", "標高", "三邊",
            "dms", "mil", "一反", "二反", "三反"));
    public String getBtnMsg(String btnText) {
        return Objects.requireNonNullElse(Const.btnMsg.get(btnText), "");
    }
    public String setMode(int mode) {
        angleMode = mode;
        return Objects.requireNonNullElse(Const.modeText.get(mode), Const.modeText.get(Const.MODE_DEGREE));
    }
    public String nextMode() {
        angleMode = (angleMode+1)%4;
        return Objects.requireNonNullElse(Const.modeText.get(angleMode), Const.modeText.get(Const.MODE_DEGREE));
    }
    public void setApp(String name) {
        if (name.length() > 0) {
            if (appName.equals(name)) appName = "";
            else appName = name;
        }
    }
}
