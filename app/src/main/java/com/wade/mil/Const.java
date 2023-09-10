package com.wade.mil;

import static java.util.Map.entry;

import java.util.Map;

public class Const {
    final static int MODE_DEGREE = 1;
    final static int MODE_DMS = 2;
    final static int MODE_MIL = 3;
    final static int MODE_AUTO = 0;

    final static Map<String, String> btnMsg = Map.ofEntries(
            entry("∠Deg", "設定角度模式: 角度、度分秒、密位、徑度"),
            entry("rad", "徑度轉角度"),
            entry("dms", "度分秒轉角度"),
            entry("mil", "密位轉角度"),
            entry("GPS", "GPS三角測量"),
            entry("POL", "直角坐標(X,Y)轉換成極坐標(長，角度)"),
            entry("REC", "極坐標(長，角度)轉換成直角坐標(X,Y)"),
            entry("PADS", "方位確認系統"),
            entry("坐換", "坐標系統及高程基準轉換"),
            entry("前交", "前方交會法"),
            entry("導線", "導線法"),
            entry("天體", "天體觀測"),
            entry("方距", "方位角、距離計算"),
            entry("內角", "內角換算"),
            entry("三角", "三角測量"),
            entry("方格", "方格統一計算"),
            entry("標高", "標高計算"),
            entry("三邊", "三邊測量"),
            entry("一反", "一點反交會"),
            entry("二反", "二點反交會"),
            entry("三反", "三點反交會")
    );
    final static Map<Integer, String>modeText = Map.ofEntries(
        entry(MODE_AUTO, "自動"),
        entry(MODE_DEGREE, "度"),
        entry(MODE_DMS, "度分秒"),
        entry(MODE_MIL, "密位")
    );
}
