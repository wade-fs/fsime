package com.wade.libs;

/**
 * Created by wade on 2017/4/6.
 * http://blog.ez2learn.com/2009/08/15/lat-lon-to-twd97/
 * https://github.com/Chao-wei-chu/TWD97_change_to_WGS
 * http://140.121.160.124/GEO/ex1.htm
 * https://github.com/g0v/powergrid/blob/master/src/com/ez2learn/android/powergrid/geo/TWD97.java
 *
 */
public class TMParameter{
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
    /*
        k0這個參數，恐怕很多人搞不清楚是甚麼東西。
        k0的全名是中央經線尺度比。
        在TM投影中常用的帶寬是2度，3度，6度。
        因為不同的帶寬就會產生不同的變形量(因為把圓弧伸展到平面上必然的會產生變形)。
        中央經線就橫切面來看，就是那一條圓弧和平面接觸的點。
        沿著中央經線，尺度比是1.0，越向兩側變形量就越大。為了讓整幅圖可以達到一個比較平均的變形量，就加上一個尺度比。讓圓弧和投影面從切線變成割線。
        尺度比一般規定
        2度 0.9999
        3度 1.0000
        6度 0.9996
        尺度比通常出現在美系地圖。
        中國大陸叫高斯克呂格投影，是不用尺度比的。
     */
    // 台灣的 Lon0 = 121, 澎湖金門馬祖 = 119
    //250000.00, 0.0, Math.toRadians(121.0), 0.9999, 6378137.0, 6356752.3141, 0.0818201799960599
    public double getDx(){ return 250000; }
    public double getDy(){ return 0; }
    public double getLon0(double lon){ return lon * Math.PI / 180.0; }
    public double getK0(){ return 0.9999; }
    public double getA(){ return 6378137.0; }
    public double getB(){ return 6356752.314245; }
    public double getE() { return 0.08181919084; } // 0.0818201799960599; 來自
    // https://github.com/g0v/powergrid/blob/master/src/com/ez2learn/android/powergrid/geo/TWD97.java
}
