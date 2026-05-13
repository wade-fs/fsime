package com.wade.libs

/**
 * Created by wade on 2017/4/6.
 * http://blog.ez2learn.com/2009/08/15/lat-lon-to-twd97/
 * https://github.com/Chao-wei-chu/TWD97_change_to_WGS
 * http://140.121.160.124/GEO/ex1.htm
 * https://github.com/g0v/powergrid/blob/master/src/com/ez2learn/android/powergrid/geo/TWD97.java
 *
 */
class TMParameter {
    val dx: Double
        /*
        lat是我們輸入的緯度，
        long是我們輸入的經度，
        接著就是關於投影的一些參數，
        a是地球赤道半徑(Equatorial Radius)，
        而b是兩極半徑(Polar Radius)，
        而long0是指中央經線，也就是我所說的卷紙接觸地球的線，
        而k0是延著long0的縮放比例，長度單位都是公尺，
        而角度都是用弧度，我之前一直算錯就有因為是用成角度來計算，全部都要是弧度才對，我們在此用的參數是
        a = 6378137.0公尺
        b = 6356752.314245公尺
        long0 = 121度
        k0 = 0.9999
    */
        get() = 250000.0
    val dy: Double
        get() = 0.0

    fun getLon0(lon: Double): Double {
        return lon * Math.PI / 180.0
    }

    val k0: Double
        get() = 0.9999
    val a: Double
        get() = 6378137.0
    val b: Double
        get() = 6356752.314245
    val e: Double
        get() = 0.08181919084 // 0.0818201799960599; 來自
    // https://github.com/g0v/powergrid/blob/master/src/com/ez2learn/android/powergrid/geo/TWD97.java
}