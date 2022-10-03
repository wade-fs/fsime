package com.wade.fsime.theme;

public class ThemeDefinitions {

    private static final int whiteColor = 0xffffffff;
    private static final int mainColor = 0xffffff00;
    private static final int cjColor = 0xff00ffff;
    private static final int blackColor = 0xff000000;

    public static ThemeInfo Default(){
        return MaterialDark();
    }


    public static ThemeInfo MaterialDark(){
        ThemeInfo theme = new ThemeInfo();
        theme.foregroundColor = whiteColor;
        theme.backgroundColor = 0xff263238;
        theme.mainColor = mainColor;
        theme.cjColor = cjColor;
        return theme;
    }

    public static ThemeInfo MaterialWhite(){
        ThemeInfo theme = Default();
        theme.foregroundColor = blackColor;
        theme.backgroundColor = 0xffeceff1;
        theme.mainColor = mainColor;
        return theme;
    }

    public static ThemeInfo PureBlack(){
        ThemeInfo theme = MaterialDark();
        theme.backgroundColor = blackColor;
        return theme;
    }

    public static ThemeInfo White(){
        ThemeInfo theme = MaterialWhite();
        theme.backgroundColor = whiteColor;
        return theme;
    }

    public static ThemeInfo Blue(){
        ThemeInfo theme = MaterialDark();
        theme.backgroundColor = 0xff0d47a1;
        return theme;
    }

    public static ThemeInfo Purple(){
        ThemeInfo theme = MaterialDark();
        theme.backgroundColor = 0xff4a148c;
        return theme;
    }

}
