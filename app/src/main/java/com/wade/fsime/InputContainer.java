/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/

package com.wade.fsime;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import java.util.List;

/*
  A container that holds:
    1. Main input plane:
      - Candidates view
      - Keyboard view
    2. Key preview plane (overlaid)
*/
public class InputContainer extends FrameLayout {
    private CandidatesView candidatesView;
    private CandidatesViewAdapter candidatesViewAdapter;
    private KeyboardView keyboardView;

    public InputContainer(final Context context, final AttributeSet attributes) {
        super(context, attributes);
    }

    public void initialiseCandidatesView(final CandidatesViewAdapter.CandidateListener candidateListener) {
        candidatesView = findViewById(R.id.candidates_view);
        candidatesView.setCandidateListener(candidateListener);
        candidatesViewAdapter = candidatesView.getCandidatesViewAdapter();
    }

    public void initialiseKeyboardView(
            final KeyboardView.KeyboardListener keyboardListener,
            final Keyboard keyboard
    ) {
        keyboardView = findViewById(R.id.keyboard_view);
        keyboardView.setKeyboardListener(keyboardListener);
        keyboardView.setMainInputPlane(findViewById(R.id.main_input_plane));
        keyboardView.setKeyboard(keyboard);
    }

    public void setBackground(final boolean isFullscreen) {
        final int backgroundResourceId =
                (isFullscreen)
                        ? R.color.fill_fullscreen
                        : 0; // none
        setBackgroundResource(backgroundResourceId);
    }

    public void setCandidateList(final List<String> candidateList) {
        candidatesViewAdapter.updateCandidateList(candidateList);
        candidatesView.scrollToPosition(0);
    }

    public int getCandidatesViewTop() {
        return candidatesView.getTop();
    }

    public Keyboard getKeyboard() {
        return keyboardView.getKeyboard();
    }

    public void setKeyboard(final Keyboard keyboard) {
        keyboardView.setKeyboard(keyboard);
    }

    public void setKeyRepeatIntervalMilliseconds(final int milliseconds) {
        keyboardView.setKeyRepeatIntervalMilliseconds(milliseconds);
    }

    public void redrawKeyboard() {
        keyboardView.invalidate();
    }
}
