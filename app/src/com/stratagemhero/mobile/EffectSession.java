package com.stratagemhero.mobile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Rules port of StratagemHero_Effect's five public effect modes. All modes
 * share one class so the UI can switch on {@link #getMode()}; the original
 * Godot project uses a separate slide scene per mode.
 */
public final class EffectSession {
    public static final int PLAYING = 1;
    public static final int INTERMISSION = 2;
    public static final int OVER = 3;

    public static final float SINGLE_TIMER_MAX = 8.0f;
    public static final float SINGLE_TIME_REVIVE = 4.0f;
    public static final float SINGLE_TIME_DECREASE = 0.3f;
    public static final float WRONG_PROTECT_TIME = 0.6f;
    public static final float DICTATION_TIMER_MULTIPLIER = 6.0f;
    public static final float DICTATION_DECREASE_MULTIPLIER = 5.0f;
    public static final float INTERMISSION_SECONDS = 4.3f;

    public static final int MULTI_LANES = 4;
    public static final int MULTI_COUNT_MIN = 8;
    public static final int MULTI_COUNT_MAX = 28;

    public static final int WALL_ARROWS = 8;
    public static final float WALL_TIME_MAX = 15.0f;
    public static final float WALL_TIME_DECREASE = 0.75f;

    public static final float TERMINAL_TIMER_MAX = 20.0f;
    public static final float TERMINAL_REVIVE_BASE = 15.0f;
    public static final float TERMINAL_TIME_DECREASE = 0.75f;

    public enum Result { IGNORED, CORRECT, COMPLETE, WRONG }

    /** One line in Multi-Lines mode. */
    public static final class Lane {
        private Stratagem target;
        private int entered;
        private boolean done;
        private float wrongFlash;

        public Stratagem getTarget() { return target; }
        public int getEnteredCount() { return entered; }
        public boolean isDone() { return done; }
        public boolean wasStarted() { return entered > 0; }
        public float getWrongFlash() { return wrongFlash; }
        public float getProgress() {
            if (target == null || target.length() == 0) return 0f;
            return Math.min(1f, entered / (float) target.length());
        }
    }

    private final List<Stratagem> catalog;
    private final Random random;
    private final EffectMode mode;
    private final boolean oneHeart;

    private int state = PLAYING;
    private int round = 1;
    private int score;
    private int arrowCompleted;
    private int arrowsThisRound;
    private float totalTimer;
    private float timeLeft;
    private float timeMax;
    private float wrongProtect;
    private float wrongFlash;
    private boolean perfect = true;

    private int roundTargets;
    private int completedThisRound;
    private final List<Stratagem> queue = new ArrayList<>();
    private Stratagem current;
    private Stratagem lastCompleted;
    private int entered;
    private int lastTimeBonus;
    private int lastPerfectBonus;
    private int lastRoundScore;
    private float intermissionLeft;

    private final Lane[] lanes = new Lane[MULTI_LANES];
    private final List<Stratagem> pool = new ArrayList<>();

    private final List<Direction> wall = new ArrayList<>();

    private final List<Direction> terminalLine = new ArrayList<>();
    private int terminalEntered;
    private int linesCompleted;

    public EffectSession(List<Stratagem> catalog, Random random, EffectMode mode, boolean oneHeart) {
        this.catalog = new ArrayList<>(catalog);
        this.random = random != null ? random : new Random();
        this.mode = mode != null ? mode : EffectMode.NONE;
        this.oneHeart = oneHeart;
        if (this.catalog.isEmpty()) {
            throw new IllegalArgumentException("Stratagem catalog is empty");
        }
        for (int i = 0; i < lanes.length; i++) lanes[i] = new Lane();
    }

    public void start() {
        state = PLAYING;
        round = 1;
        score = 0;
        arrowCompleted = 0;
        arrowsThisRound = 0;
        totalTimer = 0f;
        wrongProtect = 0f;
        wrongFlash = 0f;
        perfect = true;
        lastTimeBonus = 0;
        lastPerfectBonus = 0;
        lastRoundScore = 0;
        intermissionLeft = 0f;
        completedThisRound = 0;
        roundTargets = 0;
        terminalEntered = 0;
        linesCompleted = 0;
        terminalLine.clear();
        wall.clear();
        pool.clear();
        queue.clear();
        lastCompleted = null;
        for (Lane lane : lanes) {
            lane.target = null;
            lane.entered = 0;
            lane.done = false;
            lane.wrongFlash = 0f;
        }
        switch (mode) {
            case GREATWALL:
                timeMax = WALL_TIME_MAX;
                timeLeft = WALL_TIME_MAX;
                fillWall();
                break;
            case TERMINAL:
                timeMax = TERMINAL_TIMER_MAX;
                timeLeft = TERMINAL_TIMER_MAX;
                buildTerminalLine();
                break;
            case MULTILINES:
                beginMultiRound();
                break;
            case NONE:
            case DICTATION:
            default:
                beginSingleRound();
                break;
        }
    }

    private void beginSingleRound() {
        perfect = true;
        arrowsThisRound = 0;
        completedThisRound = 0;
        lastTimeBonus = 0;
        lastPerfectBonus = 0;
        lastRoundScore = 0;
        wrongProtect = 0f;
        wrongFlash = 0f;
        roundTargets = Math.max(3, Math.min(16, (int) (round * 0.5f) + 2));
        timeMax = getSingleTimerMax(round);
        if (mode == EffectMode.DICTATION) timeMax *= DICTATION_TIMER_MULTIPLIER;
        timeLeft = timeMax;
        queue.clear();
        for (int i = 0; i < roundTargets; i++) {
            queue.add(catalog.get(random.nextInt(catalog.size())));
        }
        current = queue.remove(0);
        lastCompleted = null;
        entered = 0;
        state = PLAYING;
    }

    private void beginMultiRound() {
        perfect = true;
        arrowsThisRound = 0;
        completedThisRound = 0;
        lastTimeBonus = 0;
        lastPerfectBonus = 0;
        lastRoundScore = 0;
        wrongProtect = 0f;
        wrongFlash = 0f;
        roundTargets = Math.max(MULTI_COUNT_MIN, Math.min(MULTI_COUNT_MAX, (int) (round * 0.5f) + 2));
        timeMax = getMultiTimerMax(round);
        timeLeft = timeMax;
        pool.clear();
        for (int i = 0; i < roundTargets; i++) {
            pool.add(catalog.get(random.nextInt(catalog.size())));
        }
        lastCompleted = null;
        for (Lane lane : lanes) {
            lane.target = pool.remove(pool.size() - 1);
            lane.entered = 0;
            lane.done = false;
            lane.wrongFlash = 0f;
        }
        state = PLAYING;
    }

    public void update(float dtSeconds) {
        if (dtSeconds <= 0f || state == OVER) return;
        if (state == INTERMISSION) {
            intermissionLeft -= dtSeconds;
            if (intermissionLeft <= 0f) {
                round++;
                if (mode == EffectMode.MULTILINES) beginMultiRound();
                else beginSingleRound();
            }
            return;
        }

        totalTimer += dtSeconds;
        wrongProtect = Math.max(0f, wrongProtect - dtSeconds);
        wrongFlash = Math.max(0f, wrongFlash - dtSeconds);
        for (Lane lane : lanes) {
            lane.wrongFlash = Math.max(0f, lane.wrongFlash - dtSeconds);
        }
        timeLeft = Math.max(0f, timeLeft - dtSeconds);
        if (timeLeft <= 0f) {
            timeLeft = 0f;
            state = OVER;
        }
    }

    public Result input(Direction dir) {
        lastCompleted = null;
        if (state != PLAYING || dir == null) return Result.IGNORED;
        switch (mode) {
            case GREATWALL:
                return inputWall(dir);
            case TERMINAL:
                return inputTerminal(dir);
            case MULTILINES:
                return inputMulti(dir);
            case NONE:
            case DICTATION:
            default:
                return inputSingle(dir);
        }
    }

    private Result inputSingle(Direction dir) {
        if (current == null) return Result.IGNORED;
        Direction expected = current.sequence[entered];
        if (dir != expected) {
            entered = 0;
            perfect = false;
            penalty(SINGLE_TIME_DECREASE, mode == EffectMode.DICTATION);
            wrongFlash = 0.35f;
            return Result.WRONG;
        }

        entered++;
        if (entered < current.sequence.length) return Result.CORRECT;

        int length = current.length();
        lastCompleted = current;
        score += length;
        arrowCompleted += length;
        arrowsThisRound += length;
        completedThisRound++;
        float revive = getSingleRevive(round);
        if (mode == EffectMode.DICTATION) revive *= DICTATION_TIMER_MULTIPLIER;
        timeLeft = moveToward(timeLeft, timeMax, revive);
        if (queue.isEmpty()) {
            finishRound();
        } else {
            current = queue.remove(0);
            entered = 0;
        }
        return Result.COMPLETE;
    }

    private Result inputMulti(Direction dir) {
        boolean hasProgress = false;
        for (Lane lane : lanes) {
            if (!lane.done && lane.entered > 0) {
                hasProgress = true;
                break;
            }
        }

        boolean anyCorrect = false;
        boolean anyWrong = false;
        boolean anyCompleted = false;
        for (Lane lane : lanes) {
            if (lane.done || lane.target == null) continue;
            if (hasProgress && lane.entered == 0) continue;
            Direction expected = lane.target.sequence[lane.entered];
            if (dir != expected) {
                lane.entered = 0;
                lane.wrongFlash = 0.35f;
                anyWrong = true;
                continue;
            }
            anyCorrect = true;
            lane.entered++;
            if (lane.entered < lane.target.sequence.length) continue;

            int length = lane.target.length();
            Stratagem completed = lane.target;
            score += length;
            arrowCompleted += length;
            arrowsThisRound += length;
            completedThisRound++;
            lastCompleted = completed;
            anyCompleted = true;
            timeLeft = moveToward(timeLeft, timeMax, getMultiRevive(round));
            if (pool.isEmpty()) {
                lane.done = true;
                if (allLanesDone()) finishRound();
            } else {
                lane.target = pool.remove(pool.size() - 1);
                lane.entered = 0;
            }
        }

        if (anyCorrect) {
            return anyCompleted ? Result.COMPLETE : Result.CORRECT;
        }
        if (anyWrong) {
            perfect = false;
            penalty(SINGLE_TIME_DECREASE, false);
            wrongFlash = 0.35f;
            return Result.WRONG;
        }
        return Result.IGNORED;
    }

    private Result inputWall(Direction dir) {
        if (wall.isEmpty()) return Result.IGNORED;
        if (dir != wall.get(0)) {
            wrongFlash = 0.35f;
            penalty(WALL_TIME_DECREASE, false);
            return Result.WRONG;
        }
        wall.remove(0);
        wall.add(randomDirection());
        arrowCompleted++;
        arrowsThisRound = arrowCompleted;
        score = arrowCompleted;
        float revive = (1f / (float) Math.max(1.0, Math.pow(arrowCompleted, 0.8))) * WALL_TIME_MAX;
        timeLeft = moveToward(timeLeft, WALL_TIME_MAX, revive);
        return Result.CORRECT;
    }

    private Result inputTerminal(Direction dir) {
        if (terminalLine.isEmpty()) return Result.IGNORED;
        Direction expected = terminalLine.get(terminalEntered);
        if (dir != expected) {
            terminalEntered = 0;
            penalty(TERMINAL_TIME_DECREASE, false);
            wrongFlash = 0.35f;
            return Result.WRONG;
        }
        terminalEntered++;
        if (terminalEntered < terminalLine.size()) return Result.CORRECT;

        int length = terminalLine.size();
        arrowCompleted += length;
        score = arrowCompleted;
        float revive = TERMINAL_REVIVE_BASE * clamp(1.5f / (linesCompleted + 1f), 0f, 1f);
        timeLeft = moveToward(timeLeft, TERMINAL_TIMER_MAX, revive);
        linesCompleted++;
        buildTerminalLine();
        return Result.COMPLETE;
    }

    private void penalty(float base, boolean dictationPenalty) {
        if (wrongProtect > 0.01f) return;
        wrongProtect = WRONG_PROTECT_TIME;
        float decrease = oneHeart ? timeMax : base;
        if (dictationPenalty) decrease *= DICTATION_DECREASE_MULTIPLIER;
        timeLeft = Math.max(0f, timeLeft - decrease);
        if (timeLeft <= 0f) {
            timeLeft = 0f;
            state = OVER;
        }
    }

    private void finishRound() {
        lastTimeBonus = (int) (timeLeft / timeMax * 10f);
        lastPerfectBonus = perfect ? arrowsThisRound : 0;
        lastRoundScore = lastTimeBonus + lastPerfectBonus;
        score += lastRoundScore;
        state = INTERMISSION;
        intermissionLeft = INTERMISSION_SECONDS;
    }

    private boolean allLanesDone() {
        for (Lane lane : lanes) if (!lane.done) return false;
        return true;
    }

    private void fillWall() {
        while (wall.size() < WALL_ARROWS) wall.add(randomDirection());
    }

    private void buildTerminalLine() {
        terminalLine.clear();
        int lineNumber = linesCompleted + 1;
        int count = (int) (Math.log(lineNumber) * 1.5) + 5;
        count = Math.max(5, Math.min(12, count));
        for (int i = 0; i < count; i++) terminalLine.add(randomDirection());
        terminalEntered = 0;
    }

    private Direction randomDirection() {
        Direction[] all = Direction.values();
        return all[random.nextInt(all.length)];
    }

    public static float getSingleTimerMax(int round) {
        return SINGLE_TIMER_MAX / (float) Math.pow(Math.max(1, round), 0.15);
    }

    public static float getSingleRevive(int round) {
        return SINGLE_TIME_REVIVE / (float) Math.sqrt(Math.max(1, round));
    }

    public static float getMultiTimerMax(int round) {
        return SINGLE_TIMER_MAX / (float) Math.pow(Math.max(1, round), 0.15);
    }

    public static float getMultiRevive(int round) {
        return SINGLE_TIME_REVIVE / (float) Math.sqrt(Math.max(1, round));
    }

    public static int multiTargetCount(int round) {
        return Math.max(MULTI_COUNT_MIN, Math.min(MULTI_COUNT_MAX, (int) (round * 0.5f) + 2));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float moveToward(float from, float to, float delta) {
        if (delta <= 0f) return from;
        if (from < to) return Math.min(to, from + delta);
        if (from > to) return Math.max(to, from - delta);
        return to;
    }

    public EffectMode getMode() { return mode; }
    public boolean isOneHeart() { return oneHeart; }
    public int getState() { return state; }
    public boolean isOver() { return state == OVER; }
    public boolean isIntermission() { return state == INTERMISSION; }
    public float getIntermissionLeft() { return intermissionLeft; }
    public int getRound() { return round; }
    public int getScore() { return score; }
    public int getArrowCompleted() { return arrowCompleted; }
    public int getArrowsThisRound() { return arrowsThisRound; }
    public float getTotalTimer() { return totalTimer; }
    public int getSpeed() {
        return totalTimer <= 0f ? 0 : Math.round(arrowCompleted * 60f / totalTimer);
    }
    public float getTimeLeft() { return timeLeft; }
    public float getTimeMax() { return timeMax; }
    public float getTimeFraction() {
        return timeMax <= 0f ? 0f : Math.max(0f, Math.min(1f, timeLeft / timeMax));
    }
    public float getWrongProtect() { return wrongProtect; }
    public float getWrongFlash() { return wrongFlash; }
    public boolean isPerfect() { return perfect; }
    public int getLastTimeBonus() { return lastTimeBonus; }
    public int getLastPerfectBonus() { return lastPerfectBonus; }
    public int getLastRoundScore() { return lastRoundScore; }

    public Stratagem getCurrent() { return current; }
    public Stratagem getLastCompleted() { return lastCompleted; }
    public int getEnteredCount() { return entered; }
    public float getProgress() {
        if (current == null || current.length() == 0) return 0f;
        return Math.min(1f, entered / (float) current.length());
    }
    public int getRoundTargets() { return roundTargets; }
    public int getCompletedThisRound() { return completedThisRound; }
    public List<Stratagem> getQueue() { return Collections.unmodifiableList(queue); }

    public Lane[] getLanes() { return lanes; }
    public int getPoolRemaining() { return pool.size(); }

    public List<Direction> getWall() { return Collections.unmodifiableList(wall); }

    public List<Direction> getTerminalLine() { return Collections.unmodifiableList(terminalLine); }
    public int getTerminalEntered() { return terminalEntered; }
    public int getLinesCompleted() { return linesCompleted; }
    public int getTerminalLineNumber() { return linesCompleted + 1; }
}
