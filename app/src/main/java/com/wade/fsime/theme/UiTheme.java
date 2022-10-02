package com.wade.fsime.theme;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import androidx.core.graphics.ColorUtils;

public class UiTheme {

    public Paint foregroundPaint;
    public Paint mainPaint;
    public Paint longPressPaint;
    public int backgroundColor;
    public float fontHeight;

    public float buttonBodyPadding = 5.0f;
    public Paint buttonBodyPaint;
    public float buttonBodyBorderRadius = 8.0f;
    public boolean enablePreview = false;
    public boolean enableBorder;
    public float portraitSize;
    public float landscapeSize;

    private UiTheme(){
        this.foregroundPaint = new Paint();
        this.mainPaint = new Paint();
        this.longPressPaint = new Paint();
        this.buttonBodyPaint = new Paint();
        backgroundColor = 0xff000000;
    }

    public static UiTheme buildFromInfo(ThemeInfo info){
        UiTheme theme = new UiTheme();
        theme.portraitSize = info.size;
        theme.landscapeSize = info.sizeLandscape;
        theme.enablePreview = info.enablePreview;
        theme.enableBorder = info.enableBorder;
        // background - darker border
        if(info.enableBorder){
            theme.backgroundColor = ColorUtils.blendARGB(info.backgroundColor, Color.BLACK, 0.2f);
        } else {
            theme.backgroundColor = info.backgroundColor;
        }
        // button body
        theme.buttonBodyPaint.setColor(info.backgroundColor);
        // foreground
        theme.fontHeight = info.fontSize;
        theme.foregroundPaint.setColor(info.foregroundColor);
        theme.foregroundPaint.setTextSize(theme.fontHeight);
        theme.foregroundPaint.setTextAlign(Paint.Align.CENTER);
        theme.foregroundPaint.setAntiAlias(true);
        theme.foregroundPaint.setTypeface(Typeface.DEFAULT);
        theme.mainPaint.setColor(info.mainColor);
        theme.mainPaint.setTextSize(theme.fontHeight);
        theme.mainPaint.setTextAlign(Paint.Align.CENTER);
        theme.mainPaint.setAntiAlias(true);
        theme.mainPaint.setTypeface(Typeface.DEFAULT);
        // longPress
        theme.longPressPaint.setColor(info.foregroundColor);
        theme.fontHeight = info.fontSize/2;
        theme.longPressPaint.setTextSize(theme.fontHeight);
        theme.longPressPaint.setTextAlign(Paint.Align.CENTER);
        theme.longPressPaint.setAntiAlias(true);
        theme.longPressPaint.setTypeface(Typeface.DEFAULT);

        return theme;
    }
}
