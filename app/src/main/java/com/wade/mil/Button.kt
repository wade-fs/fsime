package com.wade.mil

interface Button {
    fun setName(n: String?)
    fun addField(n: String?, d: String?, a: Boolean, m: Int)
    fun setMode(m: Int)
    val desc: String?
    var value: String?

    //    double getDegree() {
    //        return field[step].degree;
    //    }
    operator fun next(): Boolean
    fun back(): Boolean
    fun calc(): DoubleArray?
    fun string(): String?

    companion object {
        const val name = "Button"
        val field: List<Field> = ArrayList()
        const val step = -1
        const val mode = Const.MODE_AUTO
        const val d = 0.0
        const val a = 0.0
    }
}