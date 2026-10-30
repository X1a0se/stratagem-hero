package com.stratagemhero.mobile;

import android.content.Context;
import android.content.SharedPreferences;

/** Small persistent store for records and audio preferences. */
final class Prefs {
    private static final String FILE = "stratagem_hero";
    private static final String KEY_SOUND = "sound";
    private static final String KEY_HAPTICS = "haptics";
    private static final String KEY_VOICE = "voice";
    private static final String KEY_BGM_VOLUME = "bgm_volume";
    private static final String KEY_VOICE_VOLUME = "voice_volume";
    private static final String KEY_PAD = "pad";
    private static final String KEY_EFFECT_MODE = "effect_mode";
    private static final String KEY_EFFECT_ONE_HEART = "effect_one_heart";
    private static final String KEY_BEST = "best_";

    private final SharedPreferences sp;

    Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    boolean sound() { return sp.getBoolean(KEY_SOUND, true); }
    void setSound(boolean on) { sp.edit().putBoolean(KEY_SOUND, on).apply(); }

    boolean haptics() { return sp.getBoolean(KEY_HAPTICS, true); }
    void setHaptics(boolean on) { sp.edit().putBoolean(KEY_HAPTICS, on).apply(); }

    boolean voice() { return sp.getBoolean(KEY_VOICE, true); }
    void setVoice(boolean on) { sp.edit().putBoolean(KEY_VOICE, on).apply(); }

    int bgmVolume() { return clampVolume(sp.getInt(KEY_BGM_VOLUME, 100)); }
    void setBgmVolume(int percent) {
        sp.edit().putInt(KEY_BGM_VOLUME, clampVolume(percent)).apply();
    }

    int voiceVolume() { return clampVolume(sp.getInt(KEY_VOICE_VOLUME, 100)); }
    void setVoiceVolume(int percent) {
        sp.edit().putInt(KEY_VOICE_VOLUME, clampVolume(percent)).apply();
    }

    private static int clampVolume(int percent) {
        if (percent < 0) return 0;
        if (percent > 200) return 200;
        return percent;
    }

    boolean pad() { return sp.getBoolean(KEY_PAD, true); }
    void setPad(boolean on) { sp.edit().putBoolean(KEY_PAD, on).apply(); }

    String effectMode() { return sp.getString(KEY_EFFECT_MODE, "none"); }
    void setEffectMode(String key) { sp.edit().putString(KEY_EFFECT_MODE, key).apply(); }

    boolean effectOneHeart() { return sp.getBoolean(KEY_EFFECT_ONE_HEART, false); }
    void setEffectOneHeart(boolean on) { sp.edit().putBoolean(KEY_EFFECT_ONE_HEART, on).apply(); }

    int best(String mode) { return sp.getInt(KEY_BEST + mode, 0); }

    /** Returns true when this run set a new record. */
    boolean submit(String mode, int score) {
        if (score <= best(mode)) return false;
        sp.edit().putInt(KEY_BEST + mode, score).apply();
        return true;
    }

    void resetRecords() {
        SharedPreferences.Editor e = sp.edit();
        for (String key : sp.getAll().keySet()) {
            if (key.startsWith(KEY_BEST)) e.remove(key);
        }
        e.apply();
    }
}
