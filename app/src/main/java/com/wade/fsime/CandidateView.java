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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/*
 * This class will display candidate words based on some dictionary
 * (Feature currently not implemented!)
 */
public class CandidateView extends View {

    private static final int OUT_OF_BOUNDS = -1;

    private CodeBoardIME mService;
    private List<String> mSuggestions;
    private int mSelectedIndex;
    private int mTouchX = OUT_OF_BOUNDS;
    private final Drawable mSelectionHighlight;
    private boolean mTypedWordValid;
    
    private Rect mBgPadding;

    private static final int SCROLL_PIXELS = 20;
    private static final int X_GAP = 10;
    private static final List<String> EMPTY_LIST = new ArrayList<String>();

    public final int mColorNormal;
    private final int mColorRecommended;
    private final int mColorOther;
    private final int mVerticalPadding;
    private final Paint mPaint;
    private boolean mScrolled;
    private int mTargetScrollX;
    
    private int mTotalWidth;
    
    private final GestureDetector mGestureDetector;

    /**
     * Construct a CandidateView for showing suggested words for completion.
     */
    public CandidateView(Context context) {
        super(context);
        mSelectionHighlight = context.getDrawable(android.R.drawable.list_selector_background);
        mSelectionHighlight.setState(new int[] {
                android.R.attr.state_enabled,
                android.R.attr.state_focused,
                android.R.attr.state_window_focused,
                android.R.attr.state_pressed
        });

        Resources r = context.getResources();
        
        setBackgroundColor(r.getColor(R.color.candidate_background));
        
        mColorNormal = r.getColor(R.color.candidate_normal);
        mColorRecommended = r.getColor(R.color.candidate_recommended);
        mColorOther = r.getColor(R.color.candidate_other);
        mVerticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);
        
        mPaint = new Paint();
        mPaint.setColor(mColorNormal);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(0);
        
        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                    float distanceX, float distanceY) {
                mScrolled = true;
                int sx = getScrollX();
                sx += distanceX;
                if (sx < 0) {
                    sx = 0;
                }
                if (sx + getWidth() > mTotalWidth) {                    
                    sx -= distanceX;
                }
                mTargetScrollX = sx;
                scrollTo(sx, getScrollY());
                invalidate();
                return true;
            }
        });
        setHorizontalFadingEdgeEnabled(true);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
    }
    
    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    public void setService(CodeBoardIME listener) {
        mService = listener;
    }
    
    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = resolveSize(50, widthMeasureSpec);
        
        // Get the desired height of the icon menu view (last row of items does
        // not have a divider below)
        Rect padding = new Rect();
        mSelectionHighlight.getPadding(padding);
        final int desiredHeight = ((int)mPaint.getTextSize()) + mVerticalPadding
                + padding.top + padding.bottom;
        
        // Maximum possible width and desired height
        setMeasuredDimension(measuredWidth,
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    /**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
        }
        if (mSuggestions == null) return;
        mTotalWidth = 0;
        
        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 0, 0, 0);
            if (getBackground() != null) {
                getBackground().getPadding(mBgPadding);
            }
        }
        int x = 0;
        final int count = mSuggestions.size(); 
        final int height = getHeight();
        final Rect bgPadding = mBgPadding;
        final Paint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final boolean scrolled = mScrolled;
        final boolean typedWordValid = mTypedWordValid;
        final int y = (int) (((height - mPaint.getTextSize()) / 2) - mPaint.ascent());

        for (int i = 0; i < count; i++) {
            String suggestion = mSuggestions.get(i);
            float textWidth = paint.measureText(suggestion);
            final int wordWidth = (int) textWidth + X_GAP * 2;

            paint.setColor(mColorNormal);
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
                if (canvas != null) {
                    canvas.translate(x, 0);
                    mSelectionHighlight.setBounds(0, bgPadding.top, wordWidth, height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, 0);
                }
                mSelectedIndex = i;
            }

            if (canvas != null) {
                if ((i == 1 && !typedWordValid) || (i == 0 && typedWordValid)) {
                    paint.setFakeBoldText(true);
                    paint.setColor(mColorRecommended);
                } else if (i != 0) {
                    paint.setColor(mColorOther);
                }
                canvas.drawText(suggestion, x + X_GAP, y, paint);
                paint.setColor(mColorOther); 
                canvas.drawLine(x + wordWidth + 0.5f, bgPadding.top, 
                        x + wordWidth + 0.5f, height + 1, paint);
                paint.setFakeBoldText(false);
            }
            x += wordWidth;
        }
        mTotalWidth = x;
        if (mTargetScrollX != getScrollX()) {
            scrollToTarget();
        }
    }
    
    private void scrollToTarget() {
        int sx = getScrollX();
        if (mTargetScrollX > sx) {
            sx += SCROLL_PIXELS;
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        } else {
            sx -= SCROLL_PIXELS;
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        }
        scrollTo(sx, getScrollY());
        invalidate();
    }
    
    public void setSuggestions(List<String> suggestions, boolean completions, boolean typedWordValid) {
        clear();
        if (suggestions != null) {
            mSuggestions = new ArrayList<String>(suggestions);
            mTypedWordValid = typedWordValid;
            scrollTo(0, 0);
            mTargetScrollX = 0;
        }
        invalidate();
    }

    public void clear() {
        mSuggestions = EMPTY_LIST;
        mTouchX = OUT_OF_BOUNDS;
        mSelectedIndex = -1;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {

        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }

        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mScrolled = false;
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE:
            if (y <= 0) {
                // Fling up!?
                if (mSelectedIndex >= 0) {
                    mService.pickSuggestionManually(mSelectedIndex);
                    mSelectedIndex = -1;
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            if (!mScrolled) {
                if (mSelectedIndex >= 0) {
                    mService.pickSuggestionManually(mSelectedIndex);
                }
            }
            mSelectedIndex = -1;
            removeHighlight();
            requestLayout();
            break;
        }
        return true;
    }

    private void removeHighlight() {
        mTouchX = OUT_OF_BOUNDS;
        invalidate();
    }
    public int size() { return mSuggestions==null?0:mSuggestions.size(); }
    public String getSuggestion(int i) { if (mSuggestions.size() > i) return mSuggestions.get(i); else return ""; }

}
