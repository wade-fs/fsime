package com.wade.mil;

import static java.util.Map.entry;

import java.util.Map;
import java.util.Objects;

public class Mil {
    int angleMode = Const.MODE_DEGREE; // 0:auto 1:degree 2:dms 3:mil
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
}
