package com.wade.libs;

import android.graphics.PointF;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.wade.libs.arity.Symbols;
import com.wade.libs.arity.SyntaxException;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wade on 2017/2/20.
 */

public class Tools {
    final static String TAG = "MyLog";

    /* 底下一堆 Casio FX880P 提供的 Basic 裡的函數 */
    public static double INT(double d) {
        if (d > 0) return FIX(d);
        else if (d == 0.0) return 0;
        else return FIX(d)-1;
    }
    /// FIX 只取整數部份，跟 INT() 在正數時很像
    public static double FIX(double f) {
        if (f < 0) return - Math.floor(-f);
        else return Math.floor(f);
    }
    public static String FIX(String s) {
        if (s.indexOf('-')== 0) s = s.replace("-", "");
        if (s.indexOf('.') >= 0) return s.substring(0, s.indexOf('.'));
        else return s;
    }
    public static double FRAC(double d) { return d - FIX(d); }
    public static String FRAC(String s) {
        if (s.indexOf('-')== 0) s = s.replace("-", "");
        int idx = s.indexOf('.');
        if (idx >= 0 && idx < (s.length()-1)) {
            return s.substring(idx+1,s.length());
        }
        return "";
    }
    public static int SGN(String s) {
        if (s.indexOf('-') == 0) return -1;
        return 1;
    }
    public static int SGN(double d) {
        if (d > 0) return 1;
        else if (d == 0) return 0;
        else return -1;
    }
    // POL(X, Y) 轉成極座標的 Distance, Angle
    // C: d = POL(dy, dx, x, y) ==> return [d, y]
    public static double[] POL(double dy, double dx) {
        double[] ret = new double[2];
        PointF v = new PointF((float)dx, (float)dy);
        ret[0] = lengthOfVector(v);
        ret[1] = azimuth(v);
        while (ret[1] < 0) ret[1] += Math.PI*2;
        return ret;
    }
    public static double[] POLd(double dy, double dx) {
        double[] res = POL(dy, dx);
        res[1] = res[1] * 180/Math.PI;
        return res;
    }
    public static double len(double dx, double dy) {
        return Math.sqrt(dx*dx + dy*dy);
    }
    // REC(Distance, Angle) 轉成直角座標 X,Y, 跟 POL() 相反
    // C: x = REC(d, a, x, y) ==> return [x, y];
    public static double[] REC(double d, double a) {
        double[] ret = new double[2];
        ret[0] = d * COS(a);
        ret[1] = d * SIN(a);
        return ret;
    }
    public static double DEG(double d, double m, double s) {
        return d + m/60.0 + s/3600.0;
    }
    public static double DEG(double d, double m) {
        return DEG(d, m, 0);
    }
    public static String LEFTS(String s, int n) {
        if (s.length() >= n)
            return s.substring(0, n);
        else return s;
    }
    public static String RIGHTS(String s, int n) {
        if (s.length() >= n) return s.substring(s.length()-n);
        else return s;
    }
    // MIDS() 因為 BASIC 的索引是從 1 開始，所以在使用時要 -1
    public static String MIDS(String s, int start, int n) {
        --start;
        if ((s.length()+start) >= n) return s.substring(start, n);
        else return s.substring(start);
    }

    public static double innerProduct(PointF v1, PointF v2) { // v1, v2 視為向量
        return v1.x * v2.x + v1.y * v2.y;
    }
    public static double lengthOfVector(PointF v1) {
        return Math.sqrt(v1.x*v1.x + v1.y*v1.y);
    }
    public static double lengthOfXY(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    }
    public static double intersectionAngle(PointF v1, PointF v2) {
        double l1 = lengthOfVector(v1);
        double l2 = lengthOfVector(v2);
        if (l1 > 0 && l2 > 0) {
            return Math.acos(innerProduct(v1, v2) / (l1 * l2));
        } else return -100.0;  // 夾角介於 -PI .. PI, 所以不可能是 -100
    }
    public static double azimuth(double x1, double y1, double x2, double y2) {
        return (Rad2Deg(azimuth(new PointF((float)(x2-x1), (float)(y2-y1)))) + 360.0) % 360.0;
    }
    public static double azimuth(PointF v1, PointF v2) { // 方位角，而不是迪卡爾座標的方向角
        return azimuth(new PointF((float)(v2.x-v1.x), (float)(v2.y-v1.y)));
    }
    public static double azimuth(PointF v) { // 方位角，而不是迪卡爾座標的方向角
        return Math.PI/2 - vectorAngle(v);
    }
    public static double vectorAngle(PointF v1, PointF v2) { // 迪卡爾座標的方向角
        return vectorAngle(new PointF((float)(v2.x-v1.x), (float)(v2.y-v1.y)));
    }
    public static double vectorAngle(PointF v) { // 迪卡爾座標的方向角
        double rad;
        if (Math.abs(v.x-0.00000001) <= 0.00000001) rad = Math.PI/2;
        else if (Math.abs(v.y-0.00000001) <= 0.00000001) rad = 0;
        else rad = Math.atan(v.y / v.x);
        if (v.x < 0) {
            rad = Math.PI + rad; // rad > 0
        } else {
            if (v.y < 0) rad = 2*Math.PI + rad; // rad < 0
        }
        return rad;
    }

    public static boolean parseDoubleException = false;
    public static double parseDouble(String s) {
        int sign = SGN(s);
        s = s.replaceAll("[^\\d.]", "");
        if (s.length() == 0) return 0;
        double r = 0;
        try {
            parseDoubleException = false;
            r = sign * Double.parseDouble(s);
        } catch (NumberFormatException e) {
            r = 0;
            parseDoubleException = true;
        }
        return r;
    }
    public static double parseDouble(EditText et) {
        if (et == null) return 0;
        else return parseDouble(et.getText().toString().trim());
    }

    public static double Deg2Rad(double a) { return Math.PI * a / 180.0; }
    public static double Deg2Mil(double a) { return (a * 160.0 / 9.0); }
    public static String Deg2DmsStr(double a) { // 45^00'0.000"
        double[] res = Deg2Dms3(a);
        return String.format("%s%d°%02d'%d\"", (res[0]==-1?"-":""), (int)res[1], (int)res[2], (int)Math.round(res[3]));
    }
    public static String Deg2DmsStr2(double a) { // 45^00'0.000"
        double[] res = Deg2Dms3(a);
        return String.format("%s%d°%02d'%.3f\"", (res[0]==-1?"-":""), (int)res[1], (int)res[2], res[3]);
    }
    public static double[] Deg2Dms3(double a) { // 45^00'0.000"
        int SGN=a>0?1:-1;
        a = Math.abs(a);
        double d = FIX(a);
        double frac = FRAC(a);
        double m = frac * 60;
        double s = FRAC(m) * 60.0;
        if (Math.abs(s-60) < 0.01) {
            s = 0;
            m++;
        }
        if (Math.floor(m) == 60.0) {
            m = 0;
            d += 1;
        }
        return new double[] { SGN, d, INT(m), s };
    }
    public static double Deg2Dms(double a) {
        double[] res = Deg2Dms3(a);
        return res[0] * (res[1] + res[2]/100 + res[3]/10000);
    }

    public static double Mil2Deg(double m) { return (m * 9.0 / 160.0); }
    public static double Mil2Rad(double m) { return Deg2Rad(Mil2Deg(m)); }
    public static double Mil2Dms(double m) { return Deg2Dms(Mil2Deg(m)); }
    public static String Mil2DmsStr(double r) { return Deg2DmsStr(Mil2Deg(r)); }

    public static double Rad2Deg(double r) { return 180.0 / Math.PI * r; }
    public static double Rad2Dms(double r) { return Deg2Dms(Rad2Deg(r)); }
    public static double Rad2Mil(double r) { return Deg2Mil(Rad2Deg(r)); }
    public static String Rad2DmsStr(double r) { return Deg2DmsStr(Rad2Deg(r)); }

    public static double Dms2Rad(double dms) { return Deg2Rad(Dms2Deg(dms)); }
    public static double Dms2Mil(double dms) { return Deg2Mil(Dms2Deg(dms)); }
    public static double Dms2Deg(double dms) { // 度.分秒　轉成 ##.## 度
        double m, s;
        m = FRAC(dms) * 100;
        s = FRAC(dms*100)*100;
        return FIX(dms) + FIX(m)/60.0 + s/3600.0;
    }
    public static double Dms2Rad(String exp) { return Deg2Rad(Dms2Deg(exp)); }
    public static double Dms2Mil(String exp) { return Deg2Mil(Dms2Deg(exp)); }
    public static String Dms2Deg(String D, String M, String S) {
//        Log.d(TAG, "Dms2Deg("+D+","+M+","+S+")");
        double d = parseDouble(D);
        double m = parseDouble(M);
        double s = parseDouble(S);
        return String.format("%f", d+(m/60.0)+(s/3600.0));
    }
    public static double Dms2Deg(String exp) { // 度.分秒　轉成 ##.## 度
//        Log.d(TAG, "Dms2Deg("+exp+")");
        if (exp.indexOf('\'') >= 0 || exp.indexOf('"') >= 0 || exp.indexOf('°') >= 0 || exp.indexOf('^') >= 0) {
            lastAutoDegreeMode = 2;
            String[] res = exp.split("['^\"°]");
            double d, m, s;
            if (res.length == 1) { d = parseDouble(res[0]); m = s = 0; }
            else if (res.length == 2) { d = parseDouble(res[0]); m = parseDouble(res[1])/60.0; s = 0;}
            else { d = parseDouble(res[0]); m = parseDouble(res[1])/60.0; s = parseDouble(res[2])/3600.0; }
            return  d + m + s;
        } else {
            double dms = parseDouble(exp);
            int SGN=dms>0?1:-1;
            dms = Math.abs(dms);
            double r = 0, m, s;
            m = FRAC(dms) * 100;
            s = FRAC(dms * 100) * 100;
            r = FIX(dms) + INT(m) / 60.0 + s / 3600.0;
            return SGN*r;
        }
    }
    // 底下三角函數，直接轉換成角度運算，符合原程式碼
    public static double SIN(double z) { return Math.sin(z * Math.PI / 180.0); }
    public static double COS(double z) { return Math.cos(z * Math.PI / 180.0); }
    public static double TAN(double z) { return Math.tan(z * Math.PI / 180.0); }
    public static double ASIN(double z) { return 180.0/Math.PI * Math.asin(z); }
    public static double ACOS(double z) { return 180.0/Math.PI * Math.acos(z); }
    public static double ATAN(double z) { return 180.0/Math.PI * Math.atan(z); }
    public static double SQR(double z) { return Math.sqrt(z); }
    public static int parseInt(String s) {
        return Integer.valueOf(s.trim()).intValue();
    }

    private static int lastAutoDegreeMode=0;
    public static int getLastAutoDegreeMode() { return lastAutoDegreeMode; }
    private static double parseDMS(String exp) { // 保須保證4位數
        String deg = FIX(exp);
        String frac = FRAC(exp);
        String m = LEFTS(frac, 2);
        String s = RIGHTS(frac, 2);
        double M = parseDouble(m);
        double S = parseDouble(s);
        double d = Math.abs(parseDouble(deg)) + M / 60.0 + S / 3600.0;
        return SGN(exp) * d;
    }
    private static double parseDMS2(String exp) {
        double d=0, m=0, s=0, sgn=SGN(exp);
        if (exp.charAt(0) == '+' || exp.charAt(0) == '-') exp = exp.substring(1, exp.length()-1);
        String D, M, S;
        char dc = '\0';
        if (exp.indexOf('°') > 0) dc = '°';
        else if (exp.indexOf('^') > 0) dc = '^';
        else if (exp.indexOf('.') > 0) dc = '.';
        if (dc != '\0') {
            D = exp.substring(0, exp.indexOf(dc));
            d = parseDouble(D);
            if (exp.indexOf(dc) < exp.length()-1)
                exp = exp.substring(exp.indexOf(dc)+1, exp.length());
            else exp = "";
        }
        dc = '\'';
        if (exp.indexOf(dc) > 0) {
            M = exp.substring(0, exp.indexOf(dc));
            m = parseDouble(M);
            if (exp.indexOf(dc) < exp.length()-1)
                exp = exp.substring(exp.indexOf(dc)+1, exp.length());
            else exp = "";
        }
        dc = '"';
        if (exp.indexOf(dc) > 0) {
            S = exp.substring(0, exp.indexOf(dc));
            s = parseDouble(S);
        }
        return sgn*(d + m/60 + s/3600);
    }
    private static double parseMil(String exp) {
//        Log.d(TAG, "parseMil("+exp+")");
        exp = exp.replaceAll("M", "");
        return parseDouble(exp) * 9 / 160;
    }
    public static String ang2Str(double ang) {
        ang = (ang+360)%360;
        return String.format("%.2f密位(%s)", Deg2Mil(ang), Deg2DmsStr2(ang));
    }

    private static String cleanString(String exp) {
        if (exp == null) return "";
        exp = exp.trim().replaceAll(" ", "");
        if (exp.isEmpty()) return "";
        while (exp.length() > 1 && (exp.lastIndexOf('+') == exp.length() - 1 ||
                exp.lastIndexOf('-') == exp.length() - 1 ||
                exp.lastIndexOf('.') == exp.length() - 1)) {
            exp = exp.substring(0, exp.length() - 1);
        }
        if (exp.equals("+") || exp.equals("-") || exp.equals(".")) return "";
        return exp;
    }
    private static double autoDeg(String exp, int degreeMode) {
        if (exp.indexOf('\'') > 0 || exp.indexOf('"') > 0 || exp.indexOf('^') > 0 || exp.indexOf('°') > 0) return parseDMS2(exp);
        else if (degreeMode == 3 || exp.indexOf('M') == (exp.length()-1) || exp.indexOf('m') == (exp.length()-1)) {
            return parseMil(exp);
        }
        else if (degreeMode == 2) {
            if (exp.indexOf('.') == 0) exp = exp + ".0000";
            else while (FRAC(exp).length() < 4) exp += "0";
            if (FRAC(exp).length() > 4) {
                exp = exp.substring(0, exp.indexOf('.') + 5);
                Log.d(TAG, "\t" + exp);
            }
            return parseDMS(exp);
        }
        else if (degreeMode == 1) return parseDouble(exp);
        else if (degreeMode == 0) {
            int f = FRAC(exp).length();
            if (f < 4) { // 密位
                return parseMil(exp);
            } else if (f == 4) { // 度分秒
                return parseDMS(exp);
            } else {
                return parseDouble(exp);
            }
        }
        else return 0.0;
    }
    private static char get() {
        if (express.size() > 0) return express.remove(0);
        else return '\0';
    }
    private static char peek() {
        if (express.size() > 0) return express.get(0).charValue();
        else return '\0';
    }
    private static double number(int degreeMode) {
        String exp = "";
        char p = peek();
        while (p >= '0' && p<='9' || p == '.' || p == '^' || p == '\'' || p == '"' || p == 'm' || p == 'M' || p=='°') {
            exp += get();
            p = peek();
        }
        return autoDeg(exp, degreeMode);
    }
    private static double factor(int degreeMode) {
        char p = peek();
        if (p >= '0' && p <= '9') {
            return number(degreeMode);
        } else if (p == '(') {
            get();
            double result = expression(degreeMode);
            get();
            return result;
        } else if (p == '-') {
            get();
            return -factor(degreeMode);
        }
        return 0; // error
    }
    private static double term(int degreeMode) {
        double result = factor(degreeMode);
        char p = peek();
        while (p == '*' || p == '/' || p == '%') {
            get();
            if (p == '*') result *= factor(1);
            else if (p == '/') result /= factor(1);
            else result %= factor(1);
            p = peek();
        }
        return result;
    }
    private static double expression(int degreeMode) {
        double result = term(degreeMode);
        while (peek() == '+' || peek() == '-') {
            char c = get();
            double t = term(degreeMode);
            if (c == '+') result += t;
            else result -= t;
        }
        return result;
    }

    /* degreeMode 0:自動, 1:角度, 2:度分秒, 3:密位
     * 其中如果 exp 含有 ^'" 的符號，則強迫採用 度分秒, degreeMode 失效
     */
    public static double auto2Deg(String exp, int degreeMode) {
        asList(exp);
        return expression(degreeMode);
    }
    private static ArrayList<Character> express;
    private static void asList(final String string) {
        express = new ArrayList<Character>();
        for (char c : string.toCharArray()) { express.add(c); }
    }

    public static double auto2Rad(String exp, int degreeMode) {
        return Deg2Rad(auto2Deg(exp, degreeMode));
    }
    public static double[] traverse3(double x, double y, double h, double p, double d, double a, double v) {
        double[] res = len2Height(h, d, v, 0, 0, 1);
        h = res[0]; d = res[1];
        a = (a+p)%360.0;
        x = x + d*Tools.SIN(a);
        y = y + d*Tools.COS(a);
        p = (a+180.0)%360.0;
        return new double[] {x, y, h, a, p, d};
    }
    public static double[] Pads(double x1, double y1, double h1, double p1, double a1, double v1,
                                double x2, double y2, double h2, double p2, double a2, double v2)
    {
        double[] res = new double[5];
        res[3] = (a1 + p1 + 360)%360;
        res[4] = (a2 + p2 + 360)%360;

        // 先判斷左右
        double[] oo = Tools.POLd(y2-y1, x2-x1);
        double a = res[3] - oo[1]; if (a < 0) a += 360; if (a > 180) a -= 180;
        double b = res[4] - oo[1]; if (b < 0) b += 360; if (b > 180) b -= 180;

        double olx, oly, olh, olv;
        double orx, ory, orh, orv;
        if (a < b) { // O2 在左, O1 在右
            double xx = a; a = b; b = xx;
            olx = x2; oly = y2; olh = h2;
            olv = v2;
            orx = x1; ory = y1; orh = h1;
            orv = v1;
        } else { // O1 左，O2 右
            oo[1] = (oo[1] + 180) % 360;
            olx = x1; oly = y1; olh = h1;
            olv = v1;
            orx = x2; ory = y2; orh = h2;
            orv = v2;
        }
        boolean fl = olv != 0;
        boolean fr = orv != 0;
        if (!fl && !fr) { fl = fr = true; }
        double e = oo[0] / Tools.SIN(a-b);
        double l = e * Tools.SIN(b);
        double r = e * Tools.SIN(a);
        // 這邊透過 PADS() 求目標的時候，因為兩邊長是算出來的，基本上是平距，因此垂直角應該換算成高低角
        if (45.0 < olv && olv <= 135) olv = (90.0-olv+360)%360;
        if (45.0 < orv && orv <= 135) orv = (90.0-orv+360)%360;
        if (l <= 0 || r <= 0) {
            Log.d(TAG, String.format(Locale.CHINESE, "請檢查各項數值，尤其與角度有關的部份, l=%.2f, r=%.2f", l, r));
            return null;
        }
        double[] resr, resl;
        resr = Tools.traverse3(orx, ory, orh, oo[1], r, b, orv);
        resl = Tools.traverse3(olx, oly, olh, oo[1], l, a, olv);
        if (fl && fr) {
            res[0] = (resr[0] + resl[0])/2;
            res[1] = (resr[1] + resl[1])/2;
            res[2] = (resr[2] + resl[2])/2;
        } else if (fr) {
            res[0] = resr[0];
            res[1] = resr[1];
            res[2] = resr[2];
        } else if (fl) {
            res[0] = resl[0];
            res[1] = resl[1];
            res[2] = resl[2];
        }
        return res;
    }
    public static double[] len2(double d, double v) {
        if (45.0 < v && v < 135.0) {
            return new double[] { d * Math.abs(Tools.SIN(v)), (90 - v + 360)%360 };
        }
        return new double[] { d, v };
    }
    public static double[] len2Height(double h, double d, double v, double e, double c, int invert) {
        double[] res = len2(d, v);
        d = res[0]; v = res[1];

        return new double[] { h + invert * (d * TAN(v) + e - c) + 6.75039e-8 * d * d, d };
    }
}
