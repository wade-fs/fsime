/*
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;

class FancyLabelDraw extends Drawable {
	private final static String TAG="MyLog";
	int width;
	int height; 
	Rect bounds;
	LatinKeyboard.LatinKey key;
	static float horizontalPadding=(float)0.95;
	static float verticalPadding=(float)0.95;
	
	// auxiliary variables
	static char[] letter = new char[1];
	static Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	static float tW0,tMX,tH0,tMY, tM0;
	
	public  FancyLabelDraw(LatinKeyboard.LatinKey aKey) {
		key=aKey;
		bounds=new Rect();
	}
		
	
	@Override
	public void draw(Canvas canvas) {
		drawNumCenter(canvas);
	}
	
	private char getLetter(int idx) {
		char res=key.fancyLabel.charAt(idx);
		if (LatinKeyboardView.sShiftState)
			res=Character.toUpperCase(res);
		return res;
	}

	private void drawNumCenter(Canvas canvas) {
        if (key.fancyLabel != null) {
			float keyX;
			float keyY;
			float textSize;  // 四週次文字
			float textSize2; // 中央主文字
			Paint.FontMetrics pfm;
			int keynow = SlideTypeKeyboard.keynow;
            boolean isV=false;

			keyX = key.width * horizontalPadding / 2;
			keyY =(float)(key.height*verticalPadding / 2);
            mPaint.setTypeface(Typeface.DEFAULT);
			// Set size and correct it
			if (key.height > key.width) {
                isV = true;
				textSize  = (float)(key.width * verticalPadding * (keynow == 2? 0.4 : 0.5));
				textSize2 = (float)(key.width * verticalPadding * (keynow == 2? 0.5 : 0.66));
			} else {
                isV = false;
                textSize  = (float)(key.height * verticalPadding * 0.4);
                textSize2 = (float)(key.height * verticalPadding * 0.6);
			}
            mPaint.setTextSize( textSize );
            pfm = mPaint.getFontMetrics();
            tH0 = (pfm.ascent-pfm.descent);
            tMY = (pfm.ascent+pfm.descent) * (float)0.5;
            mPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mPaint.setColor(Color.YELLOW);
            mPaint.setTextSize( textSize2 );

			// center
			letter[0]=getLetter(0);
			mPaint.getTextBounds("W", 0, 1, bounds);
			tW0=(bounds.right-bounds.left);
			tMX=(bounds.right+bounds.left)/(float)2.0;
            if (keynow == 2)
                canvas.drawText(letter, 0, 1, (float)(keyX*1.1-tMX), keyY-tMY, mPaint);
            else if (keynow == 1)
	    		canvas.drawText(letter, 0, 1, (float)((isV?0:keyX*1.1-tMX)), keyY-tMY*2, mPaint);
            else
                canvas.drawText(letter, 0, 1, (float)(keyX*1.1-tMX), keyY-tMY*2, mPaint);

            mPaint.setTextSize( textSize );
            mPaint.setTypeface(Typeface.DEFAULT);
            mPaint.setColor(Color.WHITE);

            // right
			letter[0]=getLetter(3);
            if (keynow == 2)
                canvas.drawText(letter, 0, 1, keyX-(isV?tMX:0) +tW0, (float)(keyY-tMY-(isV?0:tH0*0.5)), mPaint);
            else if (keynow == 1)
                canvas.drawText(letter, 0, 1, (float)((isV?0:keyX*0.5)+tW0), (float)(keyY-tMY*(isV?2:2.5)), mPaint);
            else
                canvas.drawText(letter, 0, 1, keyX-tMX +tW0, (float)(keyY-tMY*(isV?2:2.5)), mPaint);

			// up
			letter[0]=getLetter(2);
            if (keynow == 2)
                canvas.drawText(letter, 0, 1, keyX-(isV?tMX:0) +(isV?0:tW0), (float)(keyY-tMY+tH0-(isV?0:tH0*0.5)), mPaint);
            else if (keynow == 1)
			    canvas.drawText(letter, 0, 1, (float)((isV?0:keyX*0.5)+tW0), (float)(keyY-tMY*(isV?2:2.5)+tH0), mPaint);
            else
                canvas.drawText(letter, 0, 1, (float)(keyX*(isV?0.5:1)-tMX +tW0), (float)(keyY-tMY*(isV?2:2.5)+tH0), mPaint);

            // left
            letter[0]=getLetter(1);
            canvas.drawText(letter, 0, 1, (float)(isV?keyX-tMX*2:keyX-tMX*2), (float)(keyY-tMY), mPaint);

            // down
			letter[0]=getLetter(4);
            canvas.drawText(letter, 0, 1, (float)(isV?keyX-tMX  :keyX-tMX*2), (float)(keyY*(isV?0.9:0.7)-tMY-tH0), mPaint);
		}
	}

	@Override
	public int getOpacity() {
		// TODO Auto-generated method stub
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int alpha) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setBounds(Rect r) {
		int kk=3+2;
		kk=kk+3;
	}
    
	@Override
	public int getIntrinsicWidth() {
		return (int)(key.width*horizontalPadding);
		//return 10;//width;
	}
	
	// cada vez que se dibuja
	@Override
	public int getIntrinsicHeight() {
		return (int)(key.height*verticalPadding);
	}
	
	@Override
	public int getMinimumWidth() {
		int kk=3+2;
		return kk;
	}

	/*
	@Override
	public void   	  setBounds(int left, int top, int right, int bottom) {
	}
	*/	
	
	@Override
	public void onBoundsChange(Rect newBounds) {
		bounds.set(newBounds);
	}


}

