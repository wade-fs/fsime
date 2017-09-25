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

import com.wade.fsime.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.PorterDuff.Mode;
import android.inputmethodservice.Keyboard;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
 
/*
 * This class holds the data structures for the keyboard
 * Does not perform any funny task 
 */
public class LatinKeyboard extends Keyboard {

    private Key mEnterKey;
    private Key mShiftKey;
    public int keynow = 0;
    private Context ctx;
    
    public LatinKeyboard(Context context, int xmlLayoutResId, int keyOrder) {
        super(context, xmlLayoutResId);
        ctx = context;
        keynow = keyOrder;
    }

    public LatinKeyboard(Context context, int layoutTemplateResId, 
            CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
        ctx = context;
    }

    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
            XmlResourceParser parser) {
        LatinKey key = new LatinKey(res, parent, x, y, parser);

        if (key.icon == null) {
        	key.icon=new FancyLabelDraw(key);
        	key.iconPreview = new TextDrawable(key);
        }
        
        if (key.codes[0] == 10) {
        	mEnterKey = key;
        } else if (key.codes[0] == -1) {
            mShiftKey = key;
        }
        return key;
    }

    /**
     * TODO: 該不會是可以切換按鈕的圖示吧？
     */
    void setImeOptions(Resources res, int options) {
        if (mEnterKey == null) {
            return;
        }
        
        switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_go_key);
                break;
            case EditorInfo.IME_ACTION_NEXT:
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_return);
                mEnterKey.label = null;
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_search);
                mEnterKey.label = null;
                break;
            case EditorInfo.IME_ACTION_SEND:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_send_key);
                break;
            default:
                mEnterKey.icon = res.getDrawable(
                R.drawable.sym_keyboard_return);
                mEnterKey.label = null;
                break;
        }
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        int superBlind = Integer.valueOf(mySharedPreferences.getString("twoKbType", "0"));
        Log.d("MyLog", "superBlind in Pref = "+superBlind);
        if (keynow != 2) return; // 目前只有2行模式需要切換
        if (superBlind == 0) { // 英數
            mShiftKey.icon = res.getDrawable(R.drawable.sym_keyboard_eng);
            mShiftKey.label = null;
        } else { // 超瞎
            mShiftKey.icon = res.getDrawable(R.drawable.sym_keyboard_super);
            mShiftKey.label = null;
        }
    }
    
    static class LatinKey extends Keyboard.Key {    	    	
    	public CharSequence fancyLabel;
    	
        public LatinKey(Resources res, Keyboard.Row parent, int x, int y, XmlResourceParser parser) {
            super(res, parent, x, y, parser);

            this.fancyLabel=label;
            if (fancyLabel != null)
            	fancyLabel=fancyLabel+"     ";
            this.label=null;
        }
                
    }

}
