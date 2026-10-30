package com.stratagemhero.mobile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rules port of StratagemHero_Effect's classic game.
 *
 * A round starts with three to thirty random stratagems. Each completed
 * stratagem scores 5 points per arrow and revives the round timer by
 * 2.5 / sqrt(round) seconds (capped at the round maximum). A wrong arrow only
 * locks input for 0.4 seconds and costs the perfect bonus; it never takes time
 * or score. Emptying a round adds 2.5 seconds, then awards the round bonus,
 * a time bonus and the perfect bonus. The next round starts after 3.5 seconds
 * of results and 1.25 seconds of ready time.
 */
public final class ClassicSession {
    public static final int READY = 0;
    public static final int PLAYING = 1;
    public static final int ROUND_SCORE = 2;
    public static final int OVER = 3;

    public static final float ROUND_READY_SECONDS = 1.25f;
    public static final float ROUND_SECONDS = 15.0f;
    public static final float ROUND_SCORE_SECONDS = 3.5f;
    public static final float TIME_REVIVE_AFTER_COMPLETE = 2.5f;
    public static final float WRONG_INPUT_LOCK = 0.4f;
    public static final int SCORE_PER_ARROW = 5;
    public static final int PERFECT_BONUS = 100;

    public enum Result { IGNORED, CORRECT, COMPLETE, WRONG }

    private final List<Stratagem> catalog;
    private final Random random;
    private final List<Stratagem> queue = new ArrayList<>();

    private int state = READY;
    private int round;
    private int score;
    private float timeLeft;
    private float timeMax;
    private float stateTimer;
    private float inputLock;
    private float wrongFlash;
    private Stratagem current;
    private Stratagem lastCompleted;
    private int entered;
    private int roundTargets;
    private int completedThisRound;
    private int arrowsThisRound;
    private int totalCompleted;
    private int totalArrows;
    private int roundBonus;
    private int timeBonus;
    private int perfectBonus;
    private int lastRoundScore;
    private int bestRoundScore;
    private boolean perfect = true;
    private boolean wasWrong;

    public ClassicSession(List<Stratagem> catalog, Random random) {
        this.catalog = new ArrayList<>(catalog);
        this.random = random != null ? random : new Random();
        if (this.catalog.isEmpty()) {
            throw new IllegalArgumentException("Stratagem catalog is empty");
        }
    }

    /** Starts directly in round one so the app's 3 second countdown acts as ready time. */
    public void start() {
        score = 0;
        totalCompleted = 0;
        totalArrows = 0;
        bestRoundScore = 0;
        round = 1;
        beginRound();
    }

    private void beginRound() {
        perfect = true;
        wasWrong = false;
        completedThisRound = 0;
        arrowsThisRound = 0;
        roundBonus = 0;
        timeBonus = 0;
        perfectBonus = 0;
        lastRoundScore = 0;
        roundTargets = Math.max(3, Math.min(30, (int) ((round + 4) / 1.3f)));
        timeMax = getRoundTime(round);
        timeLeft = timeMax;
        inputLock = 0f;
        wrongFlash = 0f;
        stateTimer = 0f;
        entered = 0;
        queue.clear();
        lastCompleted = null;
        for (int i = 0; i < roundTargets; i++) {
            queue.add(catalog.get(random.nextInt(catalog.size())));
        }
        current = queue.remove(0);
        state = PLAYING;
    }

    public static float getRoundTime(int round) {
        return ROUND_SECONDS / (float) Math.pow(Math.max(1, round), 0.3);
    }

    public static float getRoundRevive(int round) {
        return TIME_REVIVE_AFTER_COMPLETE / (float) Math.sqrt(Math.max(1, round));
    }

    public void update(float dtSeconds) {
        if (dtSeconds <= 0f || state == OVER) return;
        inputLock = Math.max(0f, inputLock - dtSeconds);
        wrongFlash = Math.max(0f, wrongFlash - dtSeconds);
        if (state == PLAYING) {
            timeLeft = Math.max(0f, timeLeft - dtSeconds);
            if (timeLeft <= 0f) {
                timeLeft = 0f;
                state = OVER;
            }
            return;
        }
        if (state == ROUND_SCORE) {
            stateTimer -= dtSeconds;
            if (stateTimer <= 0f) {
                state = READY;
                stateTimer = ROUND_READY_SECONDS;
            }
            return;
        }
        if (state == READY) {
            stateTimer -= dtSeconds;
            if (stateTimer <= 0f) {
                round++;
                beginRound();
            }
        }
    }

    public Result input(Direction dir) {
        lastCompleted = null;
        if (state != PLAYING || current == null || dir == null) return Result.IGNORED;
        if (inputLock > 0f) return Result.IGNORED;

        Direction expected = current.sequence[entered];
        if (dir != expected) {
            entered = 0;
            wasWrong = true;
            perfect = false;
            inputLock = WRONG_INPUT_LOCK;
            wrongFlash = WRONG_INPUT_LOCK;
            return Result.WRONG;
        }

        entered++;
        totalArrows++;
        arrowsThisRound++;
        if (entered < current.sequence.length) return Result.CORRECT;

        int length = current.length();
        score += length * SCORE_PER_ARROW;
        completedThisRound++;
        totalCompleted++;
        lastCompleted = current;

        if (queue.isEmpty()) {
            // The reference adds the completion time before entering ROUND_SCORE.
            timeLeft += TIME_REVIVE_AFTER_COMPLETE;
            finishRound();
        } else {
            current = queue.remove(0);
            entered = 0;
            timeLeft = moveToward(timeLeft, timeMax, getRoundRevive(round));
        }
        return Result.COMPLETE;
    }

    private void finishRound() {
        roundBonus = arrowsThisRound * SCORE_PER_ARROW;
        timeBonus = (int) (timeLeft / timeMax * 100f);
        perfectBonus = perfect ? PERFECT_BONUS : 0;
        lastRoundScore = roundBonus + timeBonus + perfectBonus;
        score += lastRoundScore;
        if (lastRoundScore > bestRoundScore) bestRoundScore = lastRoundScore;
        state = ROUND_SCORE;
        stateTimer = ROUND_SCORE_SECONDS;
        entered = current == null ? 0 : current.length();
    }

    private static float moveToward(float from, float to, float delta) {
        if (delta <= 0f) return from;
        if (from < to) return Math.min(to, from + delta);
        if (from > to) return Math.max(to, from - delta);
        return to;
    }

    public int getState() { return state; }
    public boolean isOver() { return state == OVER; }
    public boolean isPlaying() { return state == PLAYING; }
    public int getRound() { return round; }
    public int getScore() { return score; }
    public float getTimeLeft() { return timeLeft; }
    public float getTimeMax() { return timeMax; }
    public float getTimeFraction() {
        return timeMax <= 0f ? 0f : Math.max(0f, Math.min(1f, timeLeft / timeMax));
    }
    public Stratagem getCurrent() { return current; }
    public Stratagem getLastCompleted() { return lastCompleted; }
    public int getEnteredCount() { return entered; }
    public float getProgress() {
        if (state == ROUND_SCORE) return 1f;
        if (current == null || current.length() == 0) return 0f;
        return Math.min(1f, entered / (float) current.length());
    }
    public int getRoundTargets() { return roundTargets; }
    public int getCompletedThisRound() { return completedThisRound; }
    public int getTargetsRemaining() { return Math.max(0, roundTargets - completedThisRound); }
    public int getArrowsThisRound() { return arrowsThisRound; }
    public int getTotalCompleted() { return totalCompleted; }
    public int getTotalArrows() { return totalArrows; }
    public int getRoundBonus() { return roundBonus; }
    public int getTimeBonus() { return timeBonus; }
    public int getPerfectBonus() { return perfectBonus; }
    public int getLastRoundScore() { return lastRoundScore; }
    public int getBestRoundScore() { return bestRoundScore; }
    public boolean isPerfectRound() { return perfect; }
    public boolean wasWrongThisRound() { return wasWrong; }
    public float getInputLock() { return inputLock; }
    public float getWrongFlash() { return wrongFlash; }
    public float getStateTimer() { return stateTimer; }
}
