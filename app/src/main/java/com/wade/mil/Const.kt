package com.wade.mil

import java.util.Map

object Const {
    const val MODE_DEGREE = 1
    const val MODE_DMS = 2
    const val MODE_MIL = 3
    const val MODE_AUTO = 0
    val btnMsg = Map.ofEntries(
        Map.entry("∠Deg", "設定角度模式: 角度、度分秒、密位、徑度"),
        Map.entry("rad", "徑度轉角度"),
        Map.entry("dms", "度分秒轉角度"),
        Map.entry("mil", "密位轉角度"),
        Map.entry("GPS", "GPS三角測量"),
        Map.entry("POL", "直角坐標(X,Y)轉換成極坐標(長，角度)"),
        Map.entry("REC", "極坐標(長，角度)轉換成直角坐標(X,Y)"),
        Map.entry("PADS", "方位確認系統"),
        Map.entry("坐換", "坐標系統及高程基準轉換"),
        Map.entry("前交", "前方交會法"),
        Map.entry("導線", "導線法"),
        Map.entry("天體", "天體觀測"),
        Map.entry("方距", "方位角、距離計算"),
        Map.entry("內角", "內角換算"),
        Map.entry("三角", "三角測量, 請依序輸入三角度"),
        Map.entry("方格", "方格統一計算"),
        Map.entry("標高", "標高計算"),
        Map.entry("三邊", "三邊測量, 請依序輸入三邊長"),
        Map.entry("一反", "一點反交會"),
        Map.entry("二反", "二點反交會"),
        Map.entry("三反", "三點反交會")
    )
    val modeText = Map.ofEntries(
        Map.entry(MODE_AUTO, "自動"),
        Map.entry(MODE_DEGREE, "度"),
        Map.entry(MODE_DMS, "度分秒"),
        Map.entry(MODE_MIL, "密位")
    )
}