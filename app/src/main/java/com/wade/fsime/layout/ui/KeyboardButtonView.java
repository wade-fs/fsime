package com.wade.fsime.layout.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.KeyboardView;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import com.wade.fsime.layout.Box;
import com.wade.fsime.layout.Key;
import com.wade.fsime.theme.UiTheme;

import java.util.Timer;
import java.util.TimerTask;

public class KeyboardButtonView extends View {
    private final Key key;
    private final KeyboardView.OnKeyboardActionListener inputService;
    private final UiTheme uiTheme;
    private Timer timer;
    private String currentLabel = null;
    private boolean isPressed = false;
    private boolean shift = false, ctrl = false;
    private boolean swipe = false;

    public KeyboardButtonView(Context context, Key key, KeyboardView.OnKeyboardActionListener inputService, UiTheme uiTheme) {
        super(context);
        this.inputService = inputService;
        this.key = key;
        this.uiTheme = uiTheme;
        this.currentLabel = key.info.label;
        //Enable shadow
        this.setOutlineProvider(ViewOutlineProvider.BOUNDS);
    }

    float lastX = 0, lastY = 0;

    public boolean isClicked() {
        return isPressed;
    }
    public String getLongPress() {
        return key.info.longPress;
    }
    public String getCurrentLabel() {
        return currentLabel;
    }
    private void Logi(String msg) {
        Log.i("FSIME", msg);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getAction();
        switch(action){
            case MotionEvent.ACTION_DOWN:
                lastX = e.getX();
                lastY = e.getY();
                swipe = false;
                onPress();
                break;
            case MotionEvent.ACTION_UP:
                float diffX = e.getX() - lastX;
                float diffY = e.getY() - lastY;
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffY) > 5.0f) { // 左右
                    swipe = true;
                    if (diffX > 0) {
                        inputService.swipeRight();
                    } else {
                        inputService.swipeLeft();
                    }
                } else if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffX) > 5.0f) { // 上下
                    swipe = true;
                    if (diffY > 0) {
                        inputService.swipeDown();
                    } else {
                        inputService.swipeUp();
                    }
                }
                swipe = false;
                onRelease();
                break;
            default:
                break;
        }
        return true;
    }

    private void onPress() {
        isPressed = true;
        inputService.onPress(key.info.code);
        if (key.info.isRepeatable){
            startRepeating();
        }
        animatePress();
    }

    private void onRelease() {
        isPressed = false;
        if (!swipe) {
            submitKeyEvent();
        }
        if (key.info.code != 0){
            inputService.onRelease(key.info.code);
        }
        if (key.info.isRepeatable){
            stopRepeating();
        }
        animateRelease();
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        Box box = key.box;
        int w = r-l;
        int h = b-t;
        int left = (int)(l + w * box.getLeft());
        int right = (int)(l + w * box.getRight());
        int top = (int)(t + h * box.getTop());
        int bottom = (int)(t + h * box.getBottom());
        super.layout(left, top, right, bottom);
    }

    @Override
    public void draw(Canvas canvas){
        drawButtonBody(canvas);
        drawButtonContent(canvas);
        super.draw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        autoReleaseIfPressed();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        autoReleaseIfPressed();
    }

    private void drawButtonContent(Canvas canvas) {
        float x = this.getWidth()/2;
        float y = this.getHeight()/2 + uiTheme.fontHeight/3;
        if (key.info.longPress != "") {
            if (shift) {
                canvas.drawText(key.info.label, x / 2, y / 2, uiTheme.longPressPaint);
            } else {
                canvas.drawText(key.info.longPress, x / 2, y / 2, uiTheme.longPressPaint);
            }
        }
        if (shift) {
            if (this.key.info.onShiftLabel != null) {
                canvas.drawText(this.key.info.onShiftLabel, x, y + 10, uiTheme.foregroundPaint);
            } else if (this.key.info.longPress != "") {
                canvas.drawText(this.key.info.longPress, x, y + 10, uiTheme.foregroundPaint);
            } else {
                canvas.drawText(this.key.info.label, x, y + 10, uiTheme.foregroundPaint);
            }
        } else if (ctrl) {
            if (this.key.info.onCtrlLabel != null) {
                canvas.drawText(this.key.info.onCtrlLabel, x, y + 10, uiTheme.foregroundPaint);
            } else {
                canvas.drawText(this.key.info.label, x, y + 10, uiTheme.foregroundPaint);
            }
        } else {
            canvas.drawText(this.key.info.label, x, y + 10, uiTheme.foregroundPaint);
        }

        if (key.info.icon != null){
            Drawable d = key.info.icon;
            d.setTint(uiTheme.foregroundPaint.getColor());

            int padding = (int)uiTheme.buttonBodyPadding*2;
            int top;
            int left;
            int squareSize;
            if (this.getWidth() > this.getHeight()){
                top = 2*padding;
                squareSize = (this.getHeight()/2) - top;
                left = (this.getWidth()/2) - squareSize;
            } else {
                left = 2*padding;
                squareSize = this.getWidth()/2-(left);
                top = this.getHeight()/2 - squareSize;
            }
            int right = left + (squareSize*2);
            int bottom = top + (squareSize*2);
            d.setBounds(left,top,right,bottom);
            d.draw(canvas);
        }
    }

    private void drawButtonBody(Canvas canvas) {
        float left = uiTheme.buttonBodyPadding;
        float top = uiTheme.buttonBodyPadding;
        float right = this.getWidth() - uiTheme.buttonBodyPadding;
        float bottom = this.getHeight() - uiTheme.buttonBodyPadding;
        float rx = uiTheme.buttonBodyBorderRadius;
        float ry = uiTheme.buttonBodyBorderRadius;
        canvas.drawRoundRect(left, top, right, bottom, rx, ry, uiTheme.buttonBodyPaint);
    }

    private void submitKeyEvent(){
        if (key.info.code != 0){
            inputService.onKey(key.info.code, null);
        }
        if (this.key.info.outputText != null){
            inputService.onText(key.info.outputText);
        }
    }

    private void autoReleaseIfPressed(){
        if (isPressed){
            onRelease();
        }
    }

    private void stopRepeating() {
        if (timer == null){
            return;
        }
        timer.cancel();
        timer = null;
    }

    private void startRepeating() {
        if (timer != null){
            stopRepeating();
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                submitKeyEvent();
            }
        },500, 200);
    }

    private void animatePress(){
        if (uiTheme.enablePreview){
            this.setTranslationY(-200.0f);
            this.setScaleX(1.2f);
            this.setScaleY(1.2f);
            this.setElevation(21.0f);
        } else {
            this.setAlpha(.1f);
        }
    }
    private void animateRelease() {
        if (uiTheme.enablePreview){
            this.setTranslationY(0.0f);
            this.setScaleX(1.0f);
            this.setScaleY(1.0f);
            this.setElevation(0.0f);
        } else {
            this.animate().alpha(1.0f).setDuration(400);
        }
    }

    public void applyShiftModifier(boolean shiftPressed) {
        this.shift = shiftPressed;
        this.invalidate();
    }

    public void applyCtrlModifier(boolean ctrlPressed) {
        this.ctrl = ctrlPressed;
        this.invalidate();
    }
}
