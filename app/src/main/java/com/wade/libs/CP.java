package com.wade.libs;
public class CP{
   public int id, t;
   public String number, name;
   public double x, y, h;
   public String info;
   CP() {
       id = t = -1;
       x = y = h = 0;
       info = "";
   }
   CP(int id, int t, String number, String name, double x, double y, double h, String info) {
       this.id = id; this.t = t;
       this.number = number; this.name = name;
       this.x = x; this.y = y; this.h = h;
       this.info = info;
   }
}
