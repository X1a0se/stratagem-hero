package com.stratagemhero.mobile;

/** Chooses A/B round-complete cues without ever repeating the same cue twice in a row. */
final class RoundCompletePicker {
    private int last = -1;

    int next(int candidate) {
        int value = candidate == 0 ? 0 : 1;
        if (value == last) value = 1 - last;
        last = value;
        return value;
    }
}
