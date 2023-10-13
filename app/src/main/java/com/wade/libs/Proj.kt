package com.wade.libs

/**
 * Created by wade on 2017/4/6.
 * http://blog.ez2learn.com/2009/08/15/lat-lon-to-twd97/
 * https://github.com/Chao-wei-chu/TWD97_change_to_WGS
 * http://140.121.160.124/GEO/ex1.htm
 * https://github.com/g0v/powergrid/blob/master/src/com/ez2learn/android/powergrid/geo/TWD97.java
 * 公式計算請參考 http://www.uwgb.edu/dutchs/UsefulData/UTMFormulas.htm
 */
class Proj {
    private val tm = TMParameter()
    private val dx = tm.dx
    private val dy = tm.dy
    private var lon0 = tm.getLon0(LONG0TW)
    private var k0 = tm.k0
    private val a = tm.a
    private val b = tm.b
    private val e = Math.sqrt(1 - Math.pow(b, 2.0) / Math.pow(a, 2.0))
    private val ellipsoid = arrayOf(
        Ellipsoid(-1, "Placeholder", 0.0, 0.0),
        Ellipsoid(1, "Airy", 6377563.0, 0.00667054),
        Ellipsoid(2, "Australian National", 6378160.0, 0.006694542),
        Ellipsoid(3, "Bessel 1841", 6377397.0, 0.006674372),
        Ellipsoid(4, "Bessel 1841 (Nambia) ", 6377484.0, 0.006674372),
        Ellipsoid(5, "Clarke 1866", 6378206.0, 0.006768658),
        Ellipsoid(6, "Clarke 1880", 6378249.0, 0.006803511),
        Ellipsoid(7, "Everest", 6377276.0, 0.006637847),
        Ellipsoid(8, "Fischer 1960 (Mercury) ", 6378166.0, 0.006693422),
        Ellipsoid(9, "Fischer 1968", 6378150.0, 0.006693422),
        Ellipsoid(10, "GRS 1967", 6378160.0, 0.006694605),
        Ellipsoid(11, "GRS 1980", 6378137.0, 0.00669438),
        Ellipsoid(12, "Helmert 1906", 6378200.0, 0.006693422),
        Ellipsoid(13, "Hough", 6378270.0, 0.00672267),
        Ellipsoid(14, "International", 6378388.0, 0.00672267),
        Ellipsoid(15, "Krassovsky", 6378245.0, 0.006693422),
        Ellipsoid(16, "Modified Airy", 6377340.0, 0.00667054),
        Ellipsoid(17, "Modified Everest", 6377304.0, 0.006637847),
        Ellipsoid(18, "Modified Fischer 1960", 6378155.0, 0.006693422),
        Ellipsoid(19, "South American 1969", 6378160.0, 0.006694542),
        Ellipsoid(20, "WGS 60", 6378165.0, 0.006693422),
        Ellipsoid(21, "WGS 66", 6378145.0, 0.006694542),
        Ellipsoid(22, "WGS-72", 6378135.0, 0.006694318),
        Ellipsoid(23, "WGS-84", 6378137.0, 0.00669438)
    )

    fun LL2UTM(lon: Double, lat: Double, zone: Int): DoubleArray {
        val a = 6378137.0
        val eccSquared = 0.00669438
        val ee = e * e // == 0.00669438
        k0 = 0.9996
        val LongOrigin: Double
        val eccPrimeSquared: Double
        val N: Double
        val T: Double
        val C: Double
        val A: Double
        val M: Double
        val LongTemp = lon + 180.0 - Math.floor((lon + 180.0) / 360.0) * 360.0 - 180.0
        val LatRad = lat * deg2rad
        val LongRad = LongTemp * deg2rad
        val LongOriginRad: Double
        val ZoneNumber = if (zone == 0) Math.floor((LongTemp + 180) / 6).toInt() + 1 else zone
        LongOrigin = (ZoneNumber - 1) * 6 - 180.0 + 3
        LongOriginRad = LongOrigin * deg2rad
        eccPrimeSquared = ee / (1 - ee)
        N = a / Math.sqrt(1 - ee * Math.sin(LatRad) * Math.sin(LatRad))
        T = Math.tan(LatRad) * Math.tan(LatRad)
        C = eccPrimeSquared * Math.cos(LatRad) * Math.cos(LatRad)
        A = Math.cos(LatRad) * (LongRad - LongOriginRad)
        M = a * ((1 - ee / 4 - 3 * ee * ee / 64 - 5 * ee * ee * ee / 256) * LatRad
                - (3 * ee / 8 + 3 * ee * ee / 32 + 45 * ee * ee * ee / 1024) * Math.sin(2 * LatRad)
                + (15 * ee * ee / 256 + 45 * ee * ee * ee / 1024) * Math.sin(4 * LatRad)
                - 35 * ee * ee * ee / 3072 * Math.sin(6 * LatRad))
        val UTM0 =
            k0 * N * (A + (1 - T + C) * A * A * A / 6 + (5 - 18 * T + T * T + 72 * C - 58 * eccPrimeSquared) * A * A * A * A * A / 120) + 500000.0
        val UTM1 =
            k0 * (M + N * Math.tan(LatRad) * (A * A / 2 + (5 - T + 9 * C + 4 * C * C) * A * A * A * A / 24 + (61 - 58 * T + T * T + 600 * C - 330 * eccPrimeSquared) * A * A * A * A * A * A / 720))
        return doubleArrayOf(UTM0, UTM1, ZoneNumber.toDouble())
    }

    fun UTM2LL(lon: Double, lat: Double, ZoneNumber: Int, ReferenceEllipsoid: Int): DoubleArray {
        var X: Double
        var Y: Double
        val k0 = 0.9996
        val a = ellipsoid[ReferenceEllipsoid].EquatorialRadius
        val eccSquared = ellipsoid[ReferenceEllipsoid].eccentricitySquared
        val eccPrimeSquared: Double
        val e1 = (1 - Math.sqrt(1 - eccSquared)) / (1 + Math.sqrt(1 - eccSquared))
        val N1: Double
        val T1: Double
        val C1: Double
        val R1: Double
        val D: Double
        val M: Double
        val LongOrigin: Double
        val mu: Double
        val phi1: Double
        val phi1Rad: Double
        val x: Double
        val y: Double
        x = lon - 500000.0
        y = lat
        LongOrigin = ((ZoneNumber - 1) * 6 - 180 + 3).toDouble() //+3 puts origin in middle of zone
        eccPrimeSquared = eccSquared / (1 - eccSquared)
        M = y / k0
        mu =
            M / (a * (1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5 * eccSquared * eccSquared * eccSquared / 256))
        phi1Rad =
            mu + (3 * e1 / 2 - 27 * e1 * e1 * e1 / 32) * Math.sin(2 * mu) + (21 * e1 * e1 / 16 - 55 * e1 * e1 * e1 * e1 / 32) * Math.sin(
                4 * mu
            ) + 151 * e1 * e1 * e1 / 96 * Math.sin(6 * mu)
        phi1 = phi1Rad * rad2deg
        N1 = a / Math.sqrt(1 - eccSquared * Math.sin(phi1Rad) * Math.sin(phi1Rad))
        T1 = Math.tan(phi1Rad) * Math.tan(phi1Rad)
        C1 = eccPrimeSquared * Math.cos(phi1Rad) * Math.cos(phi1Rad)
        R1 = a * (1 - eccSquared) / Math.pow(
            1 - eccSquared * Math.sin(phi1Rad) * Math.sin(phi1Rad),
            1.5
        )
        D = x / (N1 * k0)
        Y =
            phi1Rad - N1 * Math.tan(phi1Rad) / R1 * (D * D / 2 - (5 + 3 * T1 + 10 * C1 - 4 * C1 * C1 - 9 * eccPrimeSquared) * D * D * D * D / 24 +
                    (61 + 90 * T1 + 298 * C1 + 45 * T1 * T1 - 252 * eccPrimeSquared - 3 * C1 * C1) * D * D * D * D * D * D / 720)
        Y = Y * rad2deg
        X =
            (D - (1 + 2 * T1 + C1) * D * D * D / 6 + (5 - 2 * C1 + 28 * T1 - 3 * C1 * C1 + 8 * eccPrimeSquared + 24 * T1 * T1) * D * D * D * D * D / 120) / Math.cos(
                phi1Rad
            )
        X = LongOrigin + X * rad2deg
        return doubleArrayOf(X, Y)
    }

    fun LL2TM2(lon: Double, lat: Double): DoubleArray {
        var lon = lon
        var lat = lat
        val dx = tm.dx
        val dy = tm.dy
        val k0 = 0.9999
        val a = tm.a
        val b = tm.b
        val e = Math.sqrt(1 - Math.pow(b, 2.0) / Math.pow(a, 2.0))
        val area: Double
        area = if (lon >= 120.0) LONG0TW else LONG0PH
        lon0 = tm.getLon0(area)
        lon = (lon - Math.floor((lon + 180) / 360) * 360) * deg2rad
        lat = lat * Math.PI / 180
        val ee = e * e
        val e2 = ee / (1 - ee)
        val n = (a - b) / (a + b)
        val nn = n * n
        val nnn = nn * n
        val nnnn = nnn * n
        val nnnnn = nnnn * n
        val nu = a / Math.sqrt(1 - ee * Math.pow(Math.sin(lat), 2.0))
        val p = lon - lon0
        val A = a * (1 - n + 5 / 4.0 * (nn - nnn) + 81 / 64.0 * (nnnn - nnnnn))
        val B = 3 * a * n / 2.0 * (1 - n + 7 / 8.0 * (nn - nnn) + 55 / 64.0 * (nnnn - nnnnn))
        val C = 15 * a * nn / 16.0 * (1 - n + 3 / 4.0 * (nn - nnn))
        val D = 35 * a * nnn / 48.0 * (1 - n + 11 / 16.0 * (nn - nnn))
        val E = 315 * a * nnnn / 51.0 * (1 - n)
        val S =
            A * lat - B * Math.sin(2 * lat) + C * Math.sin(4 * lat) - D * Math.sin(6 * lat) + E * Math.sin(
                8 * lat
            )
        val K1 = S * k0
        val K2 = k0 * nu * Math.sin(2 * lat) / 4.0
        val K3 = k0 * nu * Math.sin(lat) * Math.pow(Math.cos(lat), 3.0) / 24.0 *
                (5 - Math.pow(Math.tan(lat), 2.0) + 9 * e2 * Math.pow(
                    Math.cos(lat),
                    2.0
                ) + 4 * Math.pow(e2, 2.0) * Math.pow(
                    Math.cos(lat), 4.0
                ))
        val y = K1 + K2 * Math.pow(p, 2.0) + K3 * Math.pow(p, 4.0)
        val K4 = k0 * nu * Math.cos(lat)
        val K5 = k0 * nu * Math.pow(Math.cos(lat), 3.0) / 6.0 * (1 - Math.pow(
            Math.tan(lat),
            2.0
        ) + e2 * Math.pow(
            Math.cos(lat), 2.0
        ))
        val x = K4 * p + K5 * Math.pow(p, 3.0) + dx
        return doubleArrayOf(x, y, area)
    }

    fun TM2LL(x: Double, y: Double, area: Double): DoubleArray {
        var x = x
        var y = y
        val dx = tm.dx
        val dy = tm.dy
        val lon0: Double
        val k0 = tm.k0
        val a = tm.a
        val b = tm.b
        val e = tm.e
        lon0 =
            if (area == 0.0 || area == 51.0 || area == LONG0TW) tm.getLon0(LONG0TW) else tm.getLon0(
                LONG0PH
            )
        x -= dx
        y -= dy
        val M = y / k0
        val mu =
            M / (a * (1.0 - Math.pow(e, 2.0) / 4.0 - 3 * Math.pow(e, 4.0) / 64.0 - 5 * Math.pow(
                e,
                6.0
            ) / 256.0))
        val e1 = (1.0 - Math.pow(1.0 - Math.pow(e, 2.0), 0.5)) / (1.0 + Math.pow(
            1.0 - Math.pow(e, 2.0), 0.5
        ))
        val J1 = 3 * e1 / 2 - 27 * Math.pow(e1, 3.0) / 32.0
        val J2 = 21 * Math.pow(e1, 2.0) / 16 - 55 * Math.pow(e1, 4.0) / 32.0
        val J3 = 151 * Math.pow(e1, 3.0) / 96.0
        val J4 = 1097 * Math.pow(e1, 4.0) / 512.0
        val fp =
            mu + J1 * Math.sin(2 * mu) + J2 * Math.sin(4 * mu) + J3 * Math.sin(6 * mu) + J4 * Math.sin(
                8 * mu
            )

        // Calculate Latitude and Longitude
        val e2 = Math.pow(e * a / b, 2.0)
        val C1 = Math.pow(e2 * Math.cos(fp), 2.0)
        val T1 = Math.pow(Math.tan(fp), 2.0)
        val R1 = a * (1 - Math.pow(e, 2.0)) / Math.pow(
            1 - Math.pow(e, 2.0) * Math.pow(
                Math.sin(fp),
                2.0
            ), 3.0 / 2.0
        )
        val N1 = a / Math.pow(1 - Math.pow(e, 2.0) * Math.pow(Math.sin(fp), 2.0), 0.5)
        val D = x / (N1 * k0)

        // lat
        val Q1 = N1 * Math.tan(fp) / R1
        val Q2 = Math.pow(D, 2.0) / 2.0
        val Q3 = (5 + 3 * T1 + 10 * C1 - 4 * Math.pow(C1, 2.0) - 9 * e2) * Math.pow(D, 4.0) / 24.0
        val Q4 = (61 + 90 * T1 + 298 * C1 + 45 * Math.pow(T1, 2.0) - 3 * Math.pow(
            C1,
            2.0
        ) - 252 * e2) * Math.pow(D, 6.0) / 720.0
        val lat = fp - Q1 * (Q2 - Q3 + Q4)

        // long
        val Q6 = (1 + 2 * T1 + C1) * Math.pow(D, 3.0) / 6
        val Q7 = (5 - 2 * C1 + 28 * T1 - 3 * Math.pow(C1, 2.0) + 8 * e2 + 24 * Math.pow(
            T1,
            2.0
        )) * Math.pow(D, 5.0) / 120.0
        val lon = lon0 + (D - Q6 + Q7) / Math.cos(fp)
        return doubleArrayOf(Math.toDegrees(lon), Math.toDegrees(lat))
    }

    fun twd67ToTwd97(x: Double, y: Double): DoubleArray {
        val a = 0.00001549
        val b = 0.000006521
        val x97 = x + 807.8 + a * x + b * y
        val y97 = y - 248.6 + a * y + b * x
        return doubleArrayOf(x97, y97)
    }

    fun twd97ToTwd67(x: Double, y: Double): DoubleArray {
        val a = 0.00001549
        val b = 0.000006521
        val x67 = x - 807.8 - a * x - b * y
        val y67 = y + 248.6 - a * y - b * x
        return doubleArrayOf(x67, y67)
    }

    companion object {
        private const val TAG = "MyLog"
        private const val LONG0TW = 121.0
        private const val LONG0PH = 119.0
        private const val deg2rad = Math.PI / 180.0
        private const val rad2deg = 180.0 / Math.PI
    }
}