package com.wade.mil

import java.util.Objects

class Mil {
    var angleMode = Const.MODE_AUTO // 0:auto 1:degree 2:dms 3:mil
    private val appPol = Pol()
    fun getBtnMsg(btnText: String?): String {
        return Objects.requireNonNullElse(Const.btnMsg[btnText], "")
    }

    fun setMode(mode: Int): String? {
        angleMode = mode
        return Const.modeText[Const.MODE_DEGREE]?.let {
            Objects.requireNonNullElse(Const.modeText[mode],
                it
            )
        }
    }

    fun nextMode(): String? {
        angleMode = (angleMode + 1) % 4
        return Const.modeText[Const.MODE_DEGREE]?.let {
            Objects.requireNonNullElse(
                Const.modeText[angleMode],
                it
            )
        }
    }

    // 按按鈕時，設定 app, 並返回第一個參數
    fun setApp(name: String): String {
        if (name.length > 0) {
            if (appName == name) { // 取消 app
                appName = ""
            } else {
                appName = name
            }
        }
        return appName
    }

    companion object {
        @JvmField
        var appName = ""
        val appNames: List<String> = ArrayList(
            listOf(
                "GPS", "PADS", "前交", "導線",
                "POL", "REC", "坐換", "方距", "內角", "三角",
                "rad", "天體", "方格", "標高", "三邊",
                "dms", "mil", "一反", "二反", "三反"
            )
        )
    }
}