package com.stratagemhero.mobile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Free-call sandbox. Any full stratagem code matching a known prefix is called
 * in; there is no score and no time limit. The mode is kept in its own class so
 * Classic and Effect can follow the StratagemHero_Effect rules exactly.
 */
public final class GameSession {
    public static final int READY = 0;
    public static final int RUNNING = 1;
    public static final int OVER = 2;

    public enum Result { IGNORED, CORRECT, COMPLETE, WRONG }

    private final List<Stratagem> catalog;
    private final Random random;
    private final StringBuilder buffer = new StringBuilder();

    private int state = READY;
    private int completed;
    private int mistakes;
    private int correctSteps;
    private int totalSteps;
    private Stratagem lastCompleted;

    public GameSession(List<Stratagem> catalog, Random random) {
        this.catalog = new ArrayList<>(catalog);
        this.random = random != null ? random : new Random();
        if (this.catalog.isEmpty()) {
            throw new IllegalArgumentException("Stratagem catalog is empty");
        }
    }

    /** Retained for source compatibility; the session is always free-call now. */
    public GameSession(List<Stratagem> catalog, Random random, boolean freeCall) {
        this(catalog, random);
    }

    public void start() {
        state = RUNNING;
        buffer.setLength(0);
        completed = 0;
        mistakes = 0;
        correctSteps = 0;
        totalSteps = 0;
        lastCompleted = null;
    }

    /** Free mode is untimed. */
    public void update(float dtSeconds) {
    }

    public Result input(Direction dir) {
        if (state != RUNNING || dir == null) return Result.IGNORED;
        totalSteps++;
        buffer.append(dir.code);
        String code = buffer.toString();

        Stratagem exact = null;
        boolean isPrefix = false;
        for (Stratagem s : catalog) {
            if (s.code.startsWith(code)) {
                isPrefix = true;
                if (s.code.equals(code)) {
                    exact = s;
                    break;
                }
            }
        }

        if (exact == null && !isPrefix) {
            mistakes++;
            buffer.setLength(0);
            return Result.WRONG;
        }
        correctSteps++;
        if (exact != null) {
            completed++;
            lastCompleted = exact;
            buffer.setLength(0);
            return Result.COMPLETE;
        }
        return Result.CORRECT;
    }

    public int getState() { return state; }
    public boolean isOver() { return state == OVER; }
    public boolean isFreeCall() { return true; }
    public String getInputCode() { return buffer.toString(); }
    public int getEnteredCount() { return buffer.length(); }
    public int getScore() { return 0; }
    public int getCompleted() { return completed; }
    public int getMistakes() { return mistakes; }
    public int getCorrectSteps() { return correctSteps; }
    public int getTotalSteps() { return totalSteps; }
    public Stratagem getLastCompleted() { return lastCompleted; }
    public float getAccuracy() {
        if (totalSteps == 0) return 1f;
        return correctSteps / (float) totalSteps;
    }
    public float getProgress() {
        return Math.min(1f, buffer.length() / 7f);
    }
}
