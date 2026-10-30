package com.stratagemhero.mobile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mobile "hero round" rules, ported from HD2-Stratagem-Hero-Mobile.
 *
 * <p>One round contains {@code min(16, round + 5)} random stratagems and shares
 * a single round timer. Finishing a stratagem adds its length to the score and
 * one second to the clock. A wrong arrow only resets the current sequence;
 * the timer is the only way to fail.</p>
 *
 * <p>The original decompiled score added the <em>next</em> stratagem's length,
 * which mis-attributes the final kill. This port awards the stratagem that was
 * actually completed.</p>
 */
public final class HeroSession {
    public static final int RUNNING = 0;
    public static final int ROUND_CLEAR = 1;
    public static final int OVER = 2;

    public static final int MAX_TARGETS = 16;
    public static final float BASE_ROUND_SECONDS = 30.0f;

    public enum Result {
        IGNORED,
        CORRECT,
        COMPLETE,
        WRONG,
        ROUND_CLEAR,
        GAME_OVER
    }

    private final List<Stratagem> catalog;
    private final Random random;
    private final List<Stratagem> targets = new ArrayList<>();

    private int state = RUNNING;
    private int round;
    private int index;
    private int entered;
    private int score;
    private int roundStartScore;
    private int bestCombo;
    private int combo;
    private int completed;
    private int bestComboRun;
    private int mistakes;
    private int correctSteps;
    private int totalSteps;
    private int lastGain;
    private int clearBonus;
    private int timeBonus;
    private int perfectBonus;
    private int roundCompleted;
    private boolean perfect;
    private float timeLeft;
    private float roundDuration;
    private Stratagem lastCompleted;
    private Stratagem lastMiss;

    public HeroSession(List<Stratagem> catalog, Random random) {
        if (catalog == null || catalog.isEmpty()) {
            throw new IllegalArgumentException("Stratagem catalog is empty");
        }
        this.catalog = new ArrayList<>(catalog);
        this.random = random;
    }

    public void start() {
        round = 1;
        score = 0;
        bestCombo = 0;
        bestComboRun = 0;
        completed = 0;
        mistakes = 0;
        correctSteps = 0;
        totalSteps = 0;
        lastGain = 0;
        lastCompleted = null;
        lastMiss = null;
        setupRound();
    }

    private void setupRound() {
        state = RUNNING;
        index = 0;
        entered = 0;
        combo = 0;
        roundCompleted = 0;
        perfect = true;
        clearBonus = 0;
        timeBonus = 0;
        perfectBonus = 0;
        lastGain = 0;
        lastMiss = null;
        roundStartScore = score;
        roundDuration = Math.min(60.0f, BASE_ROUND_SECONDS + (round - 1) * 3.0f);
        timeLeft = roundDuration;
        targets.clear();
        int count = Math.min(MAX_TARGETS, round + 5);
        for (int i = 0; i < count; i++) {
            targets.add(catalog.get(random.nextInt(catalog.size())));
        }
    }

    public void update(float dtSeconds) {
        if (state != RUNNING) return;
        if (dtSeconds <= 0f) return;
        if (dtSeconds > 0.10f) dtSeconds = 0.10f;
        timeLeft -= dtSeconds;
        if (timeLeft <= 0f) {
            timeLeft = 0f;
            state = OVER;
        }
    }

    public Result input(Direction dir) {
        if (state != RUNNING || dir == null || targets.isEmpty()) return Result.IGNORED;
        Stratagem target = targets.get(index);
        totalSteps++;
        Direction expected = target.sequence[entered];
        if (dir != expected) {
            mistakes++;
            perfect = false;
            combo = 0;
            entered = 0;
            lastMiss = target;
            return Result.WRONG;
        }

        correctSteps++;
        entered++;
        if (entered < target.sequence.length) {
            return Result.CORRECT;
        }

        // Award the stratagem that was just completed (not the next one).
        completed++;
        roundCompleted++;
        combo++;
        if (combo > bestComboRun) bestComboRun = combo;
        if (combo > bestCombo) bestCombo = combo;
        lastCompleted = target;
        lastGain = target.length() * 5;
        score += lastGain;
        timeLeft = Math.min(roundDuration + 15.0f, timeLeft + 1.0f);
        entered = 0;
        index++;

        if (index >= targets.size()) {
            clearBonus = 100 + (round - 1) * 25;
            timeBonus = (int) (timeLeft * 10.0f);
            perfectBonus = perfect ? 250 : 0;
            score += clearBonus + timeBonus + perfectBonus;
            state = ROUND_CLEAR;
            return Result.ROUND_CLEAR;
        }
        return Result.COMPLETE;
    }

    public boolean continueRound() {
        if (state != ROUND_CLEAR) return false;
        round++;
        setupRound();
        return true;
    }

    public int getState() { return state; }
    public boolean isRunning() { return state == RUNNING; }
    public boolean isRoundClear() { return state == ROUND_CLEAR; }
    public boolean isOver() { return state == OVER; }
    public int getRound() { return round; }
    public int getTargetIndex() { return index; }
    public int getTargetCount() { return targets.size(); }
    public int getEnteredCount() { return entered; }
    public Stratagem getCurrent() {
        if (targets.isEmpty()) return null;
        if (index < 0 || index >= targets.size()) return null;
        return targets.get(index);
    }
    public Stratagem getLastCompleted() { return lastCompleted; }
    public Stratagem getLastMiss() { return lastMiss; }
    public int getScore() { return score; }
    public int getRoundScore() { return score - roundStartScore; }
    public int getRoundCompleted() { return roundCompleted; }
    public int getCombo() { return combo; }
    public int getBestCombo() { return bestCombo; }
    public int getBestComboRun() { return bestComboRun; }
    public int getCompleted() { return completed; }
    public int getMistakes() { return mistakes; }
    public int getCorrectSteps() { return correctSteps; }
    public int getTotalSteps() { return totalSteps; }
    public int getLastGain() { return lastGain; }
    public int getClearBonus() { return clearBonus; }
    public int getTimeBonus() { return timeBonus; }
    public int getPerfectBonus() { return perfectBonus; }
    public boolean isPerfectRound() { return perfect; }
    public float getTimeLeft() { return timeLeft; }
    public float getRoundDuration() { return roundDuration; }
    public float getTimeFraction() {
        if (roundDuration <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, timeLeft / roundDuration));
    }
    public float getProgress() {
        Stratagem s = getCurrent();
        if (s == null || s.sequence.length == 0) return 0f;
        return entered / (float) s.sequence.length;
    }
    public float getAccuracy() {
        if (totalSteps == 0) return 1f;
        return correctSteps / (float) totalSteps;
    }
    public char getRank() {
        if (score >= 9000) return 'S';
        if (score >= 6000) return 'A';
        if (score >= 3500) return 'B';
        if (score >= 1800) return 'C';
        return 'D';
    }
}
