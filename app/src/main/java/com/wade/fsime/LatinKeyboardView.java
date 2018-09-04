/*
 * Copyright (C) 2008-2009 Google Inc.
 * Copyright (C) 2009 Alejandro Grijalba
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */



package com.wade.fsime;


import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;

/* This class is the View of the keyboard.
 * Currently extends KeyboardView class.
 * It's main duty is to receive touch events.
 */
public class LatinKeyboardView extends KeyboardView {
	
    static final int KEYCODE_OPTIONS = -100;
    static int direction;
    
    static boolean sShiftState;
    static boolean sAltState;
    
    static int screenW, screenH;
    
    static long downTime=0;
        
    @Override
    public boolean setShifted(boolean newState) {
    	sShiftState=newState;
    	if (newState)
    		sAltState=false;
    	
    	super.setShifted(!(sShiftState || sAltState));
    	
    	return super.setShifted(sShiftState || sAltState);
    //	invalidate();
    // 	return sShiftState;
    }

    public void setAlt(boolean newState) {
    	sAltState=newState;
    	
    	if (newState)
    		sShiftState=false;
    	
    	super.setShifted(!(sShiftState || sAltState));
    	super.setShifted(sShiftState || sAltState);
    }
    
    public void setNormal() {
		setShifted(false);
		setAlt(false);
    }
    
    public void rotateAltShift() {
    	if (!sShiftState && !sAltState) {
			setShifted(true);
		} else if (sShiftState && !sAltState) {
			setShifted(false);
			setAlt(true);
		} else {
    		setNormal();
		}
    }
    
    // calculate slide threshold
    public void calcMinSlide() {
    	int min = Math.min(screenW, screenH);
    	minSlide=(min*SlideTypeKeyboard.slideThreshold)/100;
    }
    
    
    public LatinKeyboardView(Context context, AttributeSet attrs) {
      super(context, attrs);
      this.setPreviewEnabled(false);
      Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
      screenW = display.getWidth();
      screenH = display.getHeight();
      //setProximityCorrectionEnabled(false);
      
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setPreviewEnabled(false);
    	Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    	screenW = display.getWidth();
    	screenH = display.getHeight();
    	
        //setProximityCorrectionEnabled(false);
    }
        

	// 
    @Override
    protected boolean onLongPress(Key key) {    	
        if (key.codes[0] == '\n') {
        	direction=-1;
        	// force launch options activity
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
        	return super.onLongPress(key);
        }        
    }

    public float downX;
    public float downY;
    public static float minSlide=0;
    private int lastDirection=-2;

    public boolean onTouchEvent(MotionEvent me) {
		if (minSlide==0) calcMinSlide();
		int act = me.getAction();
		if (act==android.view.MotionEvent.ACTION_DOWN) {
			downTime=me.getEventTime();
			lastDirection=direction=0;
			downX=me.getX();
			downY=me.getY();
		} else if (act==android.view.MotionEvent.ACTION_UP || act==android.view.MotionEvent.ACTION_MOVE) {
			float dy=me.getY()-downY;
			float dx=me.getX()-downX;
			if (Math.abs(dx)>minSlide || Math.abs(dy)>minSlide) {
				if (dy > dx) {
					if (dy > -dx) { direction=4;
					} else { direction=1; }
				} else {
					if (dy > -dx) { direction=3;
					} else { direction=2; }
				}
			} else { direction=0; }
			if (act==android.view.MotionEvent.ACTION_MOVE) {
				if (lastDirection!=direction) {
					lastDirection=direction;
					downTime=me.getEventTime();
					me.setLocation(downX, downY);
				} else {
					return true;
				}
			}
		}
		return super.onTouchEvent(me);
    }

  
}
