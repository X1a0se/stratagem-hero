package com.stratagemhero.mobile;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.Random;

/**
 * Game-audio bank extracted from HELLDIVERS 2 plus watch haptics.
 * Stratagem Hero cues are used for the arcade loop; call-in voice lines are
 * grouped the same way as the original ship's Mission Control announcements.
 */
final class SoundHaptics {
    private final SoundPool soundPool;
    private final Vibrator vibrator;
    private final Random random = new Random();

    private final int[] inputBeeps;
    private final int inputOkId;
    private final int inputFailId;
    private final int heroReadyId;
    private final int roundCompleteA;
    private final int roundCompleteB;
    private final int roundFailedId;
    private final int ballLandId;
    private final int heroLoopId;

    private final int[] voiceSupport;
    private final int[] voiceEquipment;
    private final int[] voiceSentry;
    private final int[] voiceFortification;
    private final int[] voiceEagle;
    private final int[] voiceOrbital;
    private final int[] voiceBarrage;
    private final int[] voiceMinefield;
    private final int[] voiceEmplacement;
    private final int[] voiceWalker;
    private final int[] voiceOrbitalLaser;
    private final int[] voiceOrbitalGas;
    private final int[] voiceOrbitalEms;
    private final int[] voiceOrbitalSmoke;
    private final int[] voiceResupply;
    private final int[] voiceReinforce;
    private final int[] voiceSos;
    private final int[] voiceRearm;
    private final int[] voiceHellbomb;
    private final int[] voiceObjective;

    private final RoundCompletePicker roundCompletePicker = new RoundCompletePicker();

    private boolean enabled = true;
    private boolean hapticsEnabled = true;
    private boolean voiceEnabled = true;
    private boolean loopRequested;
    private boolean loopPaused;
    private int beepCursor;
    private int loopStream;
    private int bgmPercent = 100;
    private int voicePercent = 100;

    SoundHaptics(Context context) {
        Context app = context.getApplicationContext();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attrs)
                .build();

        inputBeeps = new int[]{
                load(app, R.raw.input_beep_1),
                load(app, R.raw.input_beep_2),
                load(app, R.raw.input_beep_3),
                load(app, R.raw.input_beep_4)
        };
        inputOkId = load(app, R.raw.input_ok);
        inputFailId = load(app, R.raw.input_fail);
        heroReadyId = load(app, R.raw.hero_ready);
        roundCompleteA = load(app, R.raw.hero_round_complete_1);
        roundCompleteB = load(app, R.raw.hero_round_complete_2);
        roundFailedId = load(app, R.raw.hero_round_failed);
        ballLandId = load(app, R.raw.ball_land);
        heroLoopId = load(app, R.raw.hero_loop);

        voiceSupport = loadAll(app,
                R.raw.voice_support_1, R.raw.voice_support_2,
                R.raw.voice_support_3, R.raw.voice_support_4);
        voiceEquipment = loadAll(app,
                R.raw.voice_equipment_1, R.raw.voice_equipment_2,
                R.raw.voice_equipment_3, R.raw.voice_equipment_4);
        voiceSentry = loadAll(app,
                R.raw.voice_sentry_1, R.raw.voice_sentry_2,
                R.raw.voice_sentry_3, R.raw.voice_sentry_4);
        voiceFortification = loadAll(app,
                R.raw.voice_fort_1, R.raw.voice_fort_2,
                R.raw.voice_fort_3, R.raw.voice_fort_4);
        voiceEagle = loadAll(app,
                R.raw.voice_eagle_1, R.raw.voice_eagle_2,
                R.raw.voice_eagle_3, R.raw.voice_eagle_4);
        voiceOrbital = loadAll(app,
                R.raw.voice_orbital_1, R.raw.voice_orbital_2,
                R.raw.voice_orbital_3, R.raw.voice_orbital_4);
        voiceBarrage = loadAll(app,
                R.raw.voice_barrage_1, R.raw.voice_barrage_2, R.raw.voice_barrage_3);
        voiceMinefield = loadAll(app,
                R.raw.voice_minefield_1, R.raw.voice_minefield_2,
                R.raw.voice_minefield_3, R.raw.voice_minefield_4);
        voiceEmplacement = loadAll(app,
                R.raw.voice_emplacement_1, R.raw.voice_emplacement_2,
                R.raw.voice_emplacement_3);
        voiceWalker = loadAll(app,
                R.raw.voice_walker_1, R.raw.voice_walker_2,
                R.raw.voice_walker_3, R.raw.voice_walker_4);
        voiceOrbitalLaser = loadAll(app,
                R.raw.voice_orbital_laser_1, R.raw.voice_orbital_laser_2,
                R.raw.voice_orbital_laser_3);
        voiceOrbitalGas = loadAll(app,
                R.raw.voice_orbital_gas_1, R.raw.voice_orbital_gas_2,
                R.raw.voice_orbital_gas_3);
        voiceOrbitalEms = loadAll(app,
                R.raw.voice_orbital_ems_1, R.raw.voice_orbital_ems_2,
                R.raw.voice_orbital_ems_3);
        voiceOrbitalSmoke = loadAll(app,
                R.raw.voice_orbital_smoke_1, R.raw.voice_orbital_smoke_2,
                R.raw.voice_orbital_smoke_3);
        voiceResupply = loadAll(app, R.raw.voice_resupply_1, R.raw.voice_resupply_2);
        voiceReinforce = loadAll(app, R.raw.voice_reinforce_1, R.raw.voice_reinforce_2);
        voiceSos = loadAll(app,
                R.raw.voice_sos_1, R.raw.voice_sos_2,
                R.raw.voice_sos_3, R.raw.voice_sos_4);
        voiceRearm = loadAll(app,
                R.raw.voice_rearm_1, R.raw.voice_rearm_2, R.raw.voice_rearm_3);
        voiceHellbomb = loadAll(app, R.raw.voice_hellbomb_1, R.raw.voice_hellbomb_2);
        voiceObjective = loadAll(app, R.raw.voice_objective_1, R.raw.voice_objective_2);

        vibrator = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool pool, int sampleId, int status) {
                if (loopRequested && enabled && !loopPaused && loopStream == 0) playHeroLoop();
            }
        });
    }

    private int load(Context context, int resId) {
        return soundPool.load(context, resId, 1);
    }

    private int[] loadAll(Context context, int... resIds) {
        int[] ids = new int[resIds.length];
        for (int i = 0; i < resIds.length; i++) ids[i] = load(context, resIds[i]);
        return ids;
    }

    void setEnabled(boolean on) {
        enabled = on;
        if (!on) stopHeroLoop();
    }

    void setHapticsEnabled(boolean on) {
        hapticsEnabled = on;
    }

    void setVoiceEnabled(boolean on) {
        voiceEnabled = on;
    }

    void setBgmPercent(int percent) {
        bgmPercent = AudioGain.clampPercent(percent);
        if (loopStream != 0) {
            float volume = AudioGain.bgmVolume(bgmPercent);
            soundPool.setVolume(loopStream, volume, volume);
        }
    }

    void setVoicePercent(int percent) {
        voicePercent = AudioGain.clampPercent(percent);
    }

    void startHeroLoop() {
        loopRequested = true;
        loopPaused = false;
        playHeroLoop();
    }

    private void playHeroLoop() {
        if (!enabled || loopPaused || loopStream != 0 || heroLoopId == 0) return;
        float volume = AudioGain.bgmVolume(bgmPercent);
        loopStream = soundPool.play(heroLoopId, volume, volume, 0, -1, 1.0f);
    }

    void pauseHeroLoop() {
        loopPaused = true;
        if (!enabled || loopStream == 0) return;
        soundPool.pause(loopStream);
    }

    void resumeHeroLoop() {
        loopPaused = false;
        if (!enabled) return;
        if (loopStream != 0) soundPool.resume(loopStream);
        else startHeroLoop();
    }

    void stopHeroLoop() {
        loopRequested = false;
        loopPaused = false;
        if (loopStream == 0) return;
        soundPool.stop(loopStream);
        loopStream = 0;
    }

    boolean isEnabled() {
        return enabled;
    }

    private void play(int id, float volume, float rate) {
        play(id, volume, rate, 1);
    }

    private void play(int id, float volume, float rate, int priority) {
        if (!enabled || id == 0) return;
        soundPool.play(id, volume, volume, priority, 0, rate);
    }

    /** Mission Control voice lines have their own switch, independent of sound effects. */
    private void playVoice(int id) {
        if (!voiceEnabled || id == 0) return;
        float volume = AudioGain.voiceVolume(voicePercent);
        if (volume <= 0f) return;
        soundPool.play(id, volume, volume, 1, 0, 1.0f);
    }

    void tick() {
        int id = inputBeeps[beepCursor++ % inputBeeps.length];
        play(id, 0.55f, 1.0f);
        vibrateOneShot(9, 75);
    }

    void success() {
        play(inputOkId, 0.72f, 1.0f);
        successBuzz();
    }

    void error() {
        play(inputFailId, 0.68f, 1.0f);
        errorBuzz();
    }

    void start() {
        play(heroReadyId, 0.8f, 1.0f);
        vibrateOneShot(22, 125);
    }

    void countdown(boolean last) {
        play(inputBeeps[last ? 3 : 1], 0.45f, last ? 1.04f : 0.9f);
        vibrateOneShot(last ? 25 : 13, last ? 140 : 90);
    }

    void gameOver() {
        play(roundFailedId, 0.85f, 1.0f);
        if (!canVibrate()) return;
        long[] timing = {0, 90, 55, 180};
        int[] amps = {0, 150, 0, 190};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    void roundComplete() {
        int variant = roundCompletePicker.next(random.nextBoolean() ? 0 : 1);
        int id = variant == 0 ? roundCompleteA : roundCompleteB;
        if (id == 0) id = variant == 0 ? roundCompleteB : roundCompleteA;
        play(id, 0.82f, 1.0f, 2);
        if (!canVibrate()) return;
        long[] timing = {0, 18, 35, 28, 45, 45};
        int[] amps = {0, 120, 0, 140, 0, 190};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    /** A HELLDIVERS 2 call-in: beacon/ball landing plus a grouped Mission Control voice line. */
    void callIn(Stratagem s) {
        if (s == null) return;
        play(ballLandId, 0.82f, 1.0f);
        int voice = voiceFor(s);
        playVoice(voice);
        if (!canVibrate()) return;
        long[] timing = {0, 14, 30, 18, 45, 90};
        int[] amps = {0, 90, 0, 120, 0, 160};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    private int voiceFor(Stratagem s) {
        switch (VoiceCue.forStratagem(s)) {
            case RESUPPLY:
                return pick(voiceResupply);
            case REINFORCE:
                return pick(voiceReinforce);
            case SOS:
                return pick(voiceSos);
            case REARM:
                return pick(voiceRearm);
            case HELLBOMB:
                return pick(voiceHellbomb);
            case OBJECTIVE:
                return pick(voiceObjective);
            case SUPPORT:
                return pick(voiceSupport);
            case EQUIPMENT:
                return pick(voiceEquipment);
            case SENTRY:
                return pick(voiceSentry);
            case FORTIFICATION:
                return pick(voiceFortification);
            case EMPLACEMENT:
                return pick(voiceEmplacement);
            case MINE:
                return pick(voiceMinefield);
            case WALKER:
                return pick(voiceWalker);
            case EAGLE:
                return pick(voiceEagle);
            case BARRAGE:
                return pick(voiceBarrage);
            case ORBITAL:
                return pick(voiceOrbital);
            case ORBITAL_LASER:
                return pick(voiceOrbitalLaser);
            case ORBITAL_GAS:
                return pick(voiceOrbitalGas);
            case ORBITAL_EMS:
                return pick(voiceOrbitalEms);
            case ORBITAL_SMOKE:
                return pick(voiceOrbitalSmoke);
            case NONE:
            default:
                return 0;
        }
    }

    private int pick(int[] ids) {
        if (ids == null || ids.length == 0) return 0;
        return ids[random.nextInt(ids.length)];
    }

    private void successBuzz() {
        if (!canVibrate()) return;
        long[] timing = {0, 12, 28, 16};
        int[] amps = {0, 120, 0, 75};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    private void errorBuzz() {
        if (!canVibrate()) return;
        long[] timing = {0, 32, 28, 42};
        int[] amps = {0, 180, 0, 140};
        vibrator.vibrate(VibrationEffect.createWaveform(timing, amps, -1));
    }

    private boolean canVibrate() {
        return hapticsEnabled && vibrator != null && vibrator.hasVibrator();
    }

    private void vibrateOneShot(long ms, int amplitude) {
        if (!canVibrate()) return;
        vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude));
    }

    void release() {
        stopHeroLoop();
        soundPool.release();
    }
}
