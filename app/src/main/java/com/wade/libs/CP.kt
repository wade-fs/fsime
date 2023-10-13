package com.wade.libs

class CP {
    var id: Int
    var t: Int
    var number: String? = null
    var name: String? = null
    @JvmField
    var x: Double
    @JvmField
    var y: Double
    var h: Double
    var info: String

    internal constructor() {
        t = -1
        id = t
        h = 0.0
        y = h
        x = y
        info = ""
    }

    internal constructor(
        id: Int,
        t: Int,
        number: String?,
        name: String?,
        x: Double,
        y: Double,
        h: Double,
        info: String
    ) {
        this.id = id
        this.t = t
        this.number = number
        this.name = name
        this.x = x
        this.y = y
        this.h = h
        this.info = info
    }
}