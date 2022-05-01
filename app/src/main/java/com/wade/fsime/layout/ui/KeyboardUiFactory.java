package com.wade.fsime.layout.ui;

import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.wade.fsime.layout.Box;
import com.wade.fsime.layout.Key;
import com.wade.fsime.theme.ThemeDefinitions;
import com.wade.fsime.theme.ThemeInfo;
import com.wade.fsime.theme.UiTheme;

import java.util.Collection;

public class KeyboardUiFactory {

    private final KeyboardView.OnKeyboardActionListener inputService;
    public ThemeInfo theme = ThemeDefinitions.Default();

    public KeyboardUiFactory(KeyboardView.OnKeyboardActionListener inputService) {
        this.inputService = inputService;
    }

    public KeyboardLayoutView createKeyboardView(Context context, Collection<Key> keys){
        UiTheme uiTheme = UiTheme.buildFromInfo(this.theme);
        KeyboardLayoutView layout = createKeyGroupView(context, uiTheme);
        for (Key key :keys){
            RelativeLayout.LayoutParams params = getKeyLayoutParams(key);
            View view = createKeyView(context, key, uiTheme);
            layout.addView(view,params);
        }
        return layout;
    }

    private KeyboardLayoutView createKeyGroupView(Context context, UiTheme uiTheme){
        KeyboardLayoutView layoutView = new KeyboardLayoutView(context, uiTheme);
        return layoutView;
    }

    private KeyboardButtonView createKeyView(Context context, Key key, UiTheme uiTheme) {
        KeyboardButtonView view =  new KeyboardButtonView(context, key, inputService, uiTheme);
        Box box = key.box;
        view.layout((int)box.getLeft(), (int)box.getTop(), (int)box.getRight(), (int)box.getBottom());
        return view;
    }

    private RelativeLayout.LayoutParams getKeyLayoutParams(Key key) {
        int width = (int) key.box.width;
        int height = (int) key.box.height;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.leftMargin = (int)key.box.x;
        params.topMargin = (int)key.box.y;
        return params;
    }
}
