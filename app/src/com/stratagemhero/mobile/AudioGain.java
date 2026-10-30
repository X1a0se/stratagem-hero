package com.stratagemhero.mobile;

/**
 * Linear volume mapping for the two user-controlled audio busses.
 *
 * <p>BGM samples are unchanged, so 100% is the original 0.24 gain and 200% is 0.48.
 * The 65 Mission Control voice clips are pre-gained by +6.02 dB (2x) and peak-limited
 * by tools/apply_voice_gain.py; playback therefore starts at 0.45 instead of 0.90.
 * That keeps 100% at the previous average loudness while 200% reaches twice the gain.</p>
 */
final class AudioGain {
    static final int MAX_PERCENT = 200;
    static final float BGM_BASE = 0.24f;
    static final float VOICE_BASE = 0.45f;

    private AudioGain() {}

    static int clampPercent(int percent) {
        if (percent < 0) return 0;
        if (percent > MAX_PERCENT) return MAX_PERCENT;
        return percent;
    }

    static float bgmVolume(int percent) {
        return BGM_BASE * clampPercent(percent) / 100f;
    }

    static float voiceVolume(int percent) {
        return VOICE_BASE * clampPercent(percent) / 100f;
    }
}
