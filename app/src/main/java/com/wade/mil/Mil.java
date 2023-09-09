package com.wade.mil;

import static java.util.Map.entry;

import java.util.Map;
import java.util.Objects;

public class Mil {
    public String getBtnMsg(String btnText) {
        return Objects.requireNonNullElse(Const.btnMsg.get(btnText), "");
    }
}
