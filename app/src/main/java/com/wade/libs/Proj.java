package com.wade.libs;

/**
 * Created by wade on 2017/4/6.
 * http://blog.ez2learn.com/2009/08/15/lat-lon-to-twd97/
 * https://github.com/Chao-wei-chu/TWD97_change_to_WGS
 * http://140.121.160.124/GEO/ex1.htm
 * https://github.com/g0v/powergrid/blob/master/src/com/ez2learn/android/powergrid/geo/TWD97.java
 * 公式計算請參考 http://www.uwgb.edu/dutchs/UsefulData/UTMFormulas.htm
 */

public class Proj {
    private final static String TAG="MyLog";
    private final static double LONG0TW=121.0;
    private final static double LONG0PH=119.0;
    private final static double deg2rad = Math.PI / 180.0;
    private final static double rad2deg = 180.0 / Math.PI;
    private TMParameter tm = new TMParameter();
    private double dx = tm.getDx();
    private double dy = tm.getDy();
    private double lon0 = tm.getLon0(LONG0TW);
    private double k0 = tm.getK0();
    private double a = tm.getA();
    private double b = tm.getB();
    private double e = Math.sqrt((1 - Math.pow(b, 2) / Math.pow(a, 2)));
    private Ellipsoid[] ellipsoid = new Ellipsoid[]{
            new Ellipsoid(-1, "Placeholder", 0, 0),
            new Ellipsoid(1, "Airy", 6377563, 0.00667054),
            new Ellipsoid(2, "Australian National", 6378160, 0.006694542),
            new Ellipsoid(3, "Bessel 1841", 6377397, 0.006674372),
            new Ellipsoid(4, "Bessel 1841 (Nambia) ", 6377484, 0.006674372),
            new Ellipsoid(5, "Clarke 1866", 6378206, 0.006768658),
            new Ellipsoid(6, "Clarke 1880", 6378249, 0.006803511),
            new Ellipsoid(7, "Everest", 6377276, 0.006637847),
            new Ellipsoid(8, "Fischer 1960 (Mercury) ", 6378166, 0.006693422),
            new Ellipsoid(9, "Fischer 1968", 6378150, 0.006693422),
            new Ellipsoid(10, "GRS 1967", 6378160, 0.006694605),
            new Ellipsoid(11, "GRS 1980", 6378137, 0.00669438),
            new Ellipsoid(12, "Helmert 1906", 6378200, 0.006693422),
            new Ellipsoid(13, "Hough", 6378270, 0.00672267),
            new Ellipsoid(14, "International", 6378388, 0.00672267),
            new Ellipsoid(15, "Krassovsky", 6378245, 0.006693422),
            new Ellipsoid(16, "Modified Airy", 6377340, 0.00667054),
            new Ellipsoid(17, "Modified Everest", 6377304, 0.006637847),
            new Ellipsoid(18, "Modified Fischer 1960", 6378155, 0.006693422),
            new Ellipsoid(19, "South American 1969", 6378160, 0.006694542),
            new Ellipsoid(20, "WGS 60", 6378165, 0.006693422),
            new Ellipsoid(21, "WGS 66", 6378145, 0.006694542),
            new Ellipsoid(22, "WGS-72", 6378135, 0.006694318),
            new Ellipsoid(23, "WGS-84", 6378137, 0.00669438)
    };

    public double[] LL2UTM(double lon, double lat, int zone) {
        double a = 6378137;
        double eccSquared = 0.00669438;
        double ee = e * e; // == 0.00669438
        k0 = 0.9996;
        double LongOrigin, eccPrimeSquared;
        double N, T, C, A, M;
        double LongTemp = lon+180.0-Math.floor((lon+180.0)/360.0)*360.0-180.0;
        double LatRad = lat * deg2rad;
        double LongRad = LongTemp * deg2rad;
        double LongOriginRad;
        int ZoneNumber = zone == 0?(int)Math.floor((LongTemp + 180) / 6) + 1:zone;

        LongOrigin = (ZoneNumber - 1) * 6 - 180.0 + 3;
        LongOriginRad = LongOrigin * deg2rad;

        eccPrimeSquared = (ee) / (1-ee);
        N = a/Math.sqrt(1-ee*Math.sin(LatRad) * Math.sin(LatRad));
        T = Math.tan(LatRad) * Math.tan(LatRad);
        C = eccPrimeSquared * Math.cos(LatRad) * Math.cos(LatRad);
        A = Math.cos(LatRad) * (LongRad - LongOriginRad);

        M = a * (( 1-ee/4 - 3 * ee*ee / 64 - 5 * ee * ee * ee / 256) * LatRad
                - (3*ee / 8 + 3 * ee * ee / 32 + 45 * ee * ee * ee / 1024) * Math.sin(2*LatRad)
                + (15 * ee*ee / 256 + 45 * ee*ee*ee/1024)*Math.sin(4*LatRad)
                - (35*ee*ee*ee/3072) * Math.sin(6*LatRad));
        double UTM0 = (k0 *N*(A+(1-T+C)*A*A*A/6 +
                (5-18*T+T*T+72*C -58*eccPrimeSquared)*A*A*A*A*A/120) + 500000.0);
        double UTM1 = (k0*(M+N*Math.tan(LatRad)*(A*A/2+(5-T+9*C+4*C*C)*A*A*A*A/24
        +(61-58*T+T*T+600*C-330*eccPrimeSquared)*A*A*A*A*A*A/720)));

        return new double[] {UTM0, UTM1, ZoneNumber};
    }
    public double[] UTM2LL(double lon, double lat, int ZoneNumber, int ReferenceEllipsoid) {
        double X, Y;
        double k0 = 0.9996;
        double a = ellipsoid[ReferenceEllipsoid].EquatorialRadius;
        double eccSquared = ellipsoid[ReferenceEllipsoid].eccentricitySquared;
        double eccPrimeSquared;
        double e1 = (1-Math.sqrt(1-eccSquared))/(1+Math.sqrt(1-eccSquared));
        double N1, T1, C1, R1, D, M;
        double LongOrigin;
        double mu, phi1, phi1Rad;
        double x, y;
        x = lon - 500000.0;
        y = lat;
        LongOrigin = (ZoneNumber - 1)*6 - 180 + 3;  //+3 puts origin in middle of zone
        eccPrimeSquared = (eccSquared)/(1-eccSquared);
        M = y / k0;
        mu = M/(a*(1-eccSquared/4-3*eccSquared*eccSquared/64-5*eccSquared*eccSquared*eccSquared/256));
        phi1Rad = mu + (3*e1/2-27*e1*e1*e1/32)*Math.sin(2*mu) + (21*e1*e1/16-55*e1*e1*e1*e1/32)*Math.sin(4*mu) +(151*e1*e1*e1/96)*Math.sin(6*mu);
        phi1 = phi1Rad*rad2deg;

        N1 = a/Math.sqrt(1-eccSquared*Math.sin(phi1Rad)*Math.sin(phi1Rad));
        T1 = Math.tan(phi1Rad)*Math.tan(phi1Rad);
        C1 = eccPrimeSquared*Math.cos(phi1Rad)*Math.cos(phi1Rad);
        R1 = a*(1-eccSquared)/Math.pow(1-eccSquared*Math.sin(phi1Rad)*Math.sin(phi1Rad), 1.5);
        D = x/(N1*k0);

        Y = phi1Rad - (N1*Math.tan(phi1Rad)/R1)*(D*D/2-(5+3*T1+10*C1-4*C1*C1-
                9*eccPrimeSquared)*D*D*D*D/24 +
                (61+90*T1+298*C1+45*T1*T1-252*eccPrimeSquared-3*C1*C1)*D*D*D*D*D*D/720);
        Y = Y * rad2deg;
        X = (D-(1+2*T1+C1)*D*D*D/6+(5-2*C1+28*T1-3*C1*C1+8*eccPrimeSquared+24*T1*T1) *D*D*D*D*D/120)/Math.cos(phi1Rad);
        X = LongOrigin + X * rad2deg;
        return new double[] { X, Y};
    }

    public double[] LL2TM2(double lon, double lat) {
        double dx = tm.getDx();
        double dy = tm.getDy();
        double k0 = 0.9999;
        double a = tm.getA();
        double b = tm.getB();
        double e = Math.sqrt((1 - Math.pow(b, 2) / Math.pow(a, 2)));

        double area;
        if (lon >= 120.0) area = LONG0TW;
        else area = LONG0PH;
        lon0 = tm.getLon0(area);
        lon = (lon - Math.floor((lon + 180) / 360) * 360) * deg2rad;
        lat = lat * Math.PI / 180;
        double ee = e * e;
        double e2 = ee / (1-ee);
        double n = (a-b)/(a+b);
        double nn = n * n;
        double nnn = nn * n;
        double nnnn = nnn * n;
        double nnnnn = nnnn * n;
        double nu = a/Math.sqrt(1-ee*(Math.pow(Math.sin(lat), 2)));
        double p = lon - lon0;

        double A = a * (1 - n + (5/4.0) * (nn- nnn) + (81 / 64.0) * (nnnn - nnnnn));
        double B = (3 * a * n / 2.0) * (1 - n + (7/8.0) * (nn - nnn) + (55/64.0) * (nnnn - nnnnn));
        double C = (15 * a * nn / 16.0) * (1 - n + (3/4.0) * (nn - nnn));
        double D = (35 * a * nnn / 48.0) * (1 - n + (11/16.0) * (nn - nnn));
        double E = (315 * a * nnnn / 51.0) * (1 - n);

        double S = A * lat - B * Math.sin(2*lat) + C* Math.sin(4*lat) - D * Math.sin(6*lat) + E * Math.sin(8*lat);
        double K1 = S * k0;
        double K2 = k0 * nu * Math.sin(2*lat) / 4.0;
        double K3 = (k0 * nu * Math.sin(lat) * Math.pow(Math.cos(lat), 3) / 24.0) *
                ( 5 - Math.pow(Math.tan(lat), 2) + 9 * e2 * Math.pow(Math.cos(lat),2) + 4 * Math.pow(e2,2) * Math.pow(Math.cos(lat),4));

        double y = K1 + K2 * Math.pow(p, 2) + K3 * Math.pow(p, 4);

        double K4 = k0 * nu * Math.cos(lat);
        double K5 = (k0 * nu * Math.pow(Math.cos(lat), 3) / 6.0) * (1 - Math.pow(Math.tan(lat), 2) + e2 * Math.pow(Math.cos(lat), 2));
        double x = K4 * p + K5 * Math.pow(p, 3) + dx;

        return new double[] {x, y, area};
    }
    public double[] TM2LL(double x, double y, double area) {
        double dx = tm.getDx();
        double dy = tm.getDy();
        double lon0;
        double k0 = tm.getK0();
        double a = tm.getA();
        double b = tm.getB();
        double e = tm.getE();
        if (area == 0 || area == 51 || area == LONG0TW) lon0 = tm.getLon0(LONG0TW);
        else lon0 = tm.getLon0(LONG0PH);
        x -= dx;
        y -= dy;
        double M = y/k0;
        double mu = M/(a*(1.0 - Math.pow(e, 2)/4.0 - 3*Math.pow(e, 4)/64.0 - 5*Math.pow(e, 6)/256.0));
        double e1 = (1.0 - Math.pow((1.0 - Math.pow(e, 2)), 0.5)) / (1.0 + Math.pow((1.0 - Math.pow(e, 2)), 0.5));
        double J1 = (3*e1/2 - 27*Math.pow(e1, 3)/32.0);
        double J2 = (21*Math.pow(e1, 2)/16 - 55*Math.pow(e1, 4)/32.0);
        double J3 = (151*Math.pow(e1, 3)/96.0);
        double J4 = (1097*Math.pow(e1, 4)/512.0);

        double fp = mu + J1*Math.sin(2*mu) + J2*Math.sin(4*mu) + J3*Math.sin(6*mu) + J4*Math.sin(8*mu);

        // Calculate Latitude and Longitude

        double e2 = Math.pow((e*a/b), 2);
        double C1 = Math.pow(e2*Math.cos(fp), 2);
        double T1 = Math.pow(Math.tan(fp), 2);
        double R1 = a*(1-Math.pow(e, 2))/Math.pow((1-Math.pow(e, 2)*Math.pow(Math.sin(fp), 2)), (3.0/2.0));
        double N1 = a/Math.pow((1-Math.pow(e, 2)*Math.pow(Math.sin(fp), 2)), 0.5);

        double D = x/(N1*k0);

        // lat
        double Q1 = N1*Math.tan(fp)/R1;
        double Q2 = (Math.pow(D, 2)/2.0);
        double Q3 = (5 + 3*T1 + 10*C1 - 4*Math.pow(C1, 2) - 9*e2)*Math.pow(D, 4)/24.0;
        double Q4 = (61 + 90*T1 + 298*C1 + 45*Math.pow(T1, 2) - 3*Math.pow(C1, 2) - 252*e2)*Math.pow(D, 6)/720.0;
        double lat = fp - Q1*(Q2 - Q3 + Q4);

        // long
        double Q5 = D;
        double Q6 = (1 + 2*T1 + C1)*Math.pow(D, 3)/6;
        double Q7 = (5 - 2*C1 + 28*T1 - 3*Math.pow(C1, 2) + 8*e2 + 24*Math.pow(T1, 2))*Math.pow(D, 5)/120.0;
        double lon = lon0 + (Q5 - Q6 + Q7)/Math.cos(fp);

        return new double[] {Math.toDegrees(lon), Math.toDegrees(lat)};
    }

    public double[] twd67ToTwd97(double x, double y) {
        double a = 0.00001549;
        double b = 0.000006521;
        double x97 = x + 807.8 + a*x + b*y;
        double y97 = y - 248.6 + a*y + b*x;
        return new double[] {x97, y97};
    }
    public double[] twd97ToTwd67(double x, double y) {
        double a = 0.00001549;
        double b = 0.000006521;
        double x67 = x - 807.8 - a*x - b*y;
        double y67 = y + 248.6 - a*y - b*x;
        return new double[] {x67, y67};
    }
}
