package com.wade.libs;

/**
 * Created by wade on 2017/6/3.
 */

public class Ellipsoid {
    int id;
    String ellipsoidName;
    double EquatorialRadius;
    double eccentricitySquared;

    Ellipsoid(int Id, String name, double radius, double ecc)
    {
        id = Id; ellipsoidName = name;
        EquatorialRadius = radius;
        eccentricitySquared = ecc;
    }
}
