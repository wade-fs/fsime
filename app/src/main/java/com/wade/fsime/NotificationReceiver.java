package com.wade.fsime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.inputmethod.InputMethodManager;

public class NotificationReceiver extends BroadcastReceiver {
    private CodeBoardIME mIME;
    static public final String ACTION_SHOW = "com.wade.fsime.SHOW";
    static public final String ACTION_SETTINGS = "com.wade.fsime.SETTINGS";

    NotificationReceiver(CodeBoardIME ime) {
        super();
        mIME = ime;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(ACTION_SHOW)) {
            InputMethodManager imm = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInputFromInputMethod(mIME.mToken, InputMethodManager.SHOW_FORCED);
            }
        } else if (action.equals(ACTION_SETTINGS)) {
            context.startActivity(new Intent(mIME, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("notification", 1));

        }
    }
}
