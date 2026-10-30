package com.stratagemhero.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The whole game is one custom view so it can stay a single APK with no
 * support libraries. Portrait uses a 450-unit wide console; landscape uses a
 * 1280-unit wide console with the call-in stage on the left and the arc pad on
 * the right. Both follow the same rules and share every screen.
 */
public final class GameView extends View {
    private static final int S_MENU = 0;
    private static final int S_HELP = 1;
    private static final int S_COUNTDOWN = 2;
    private static final int S_CLASSIC = 3;
    private static final int S_EFFECT = 4;
    private static final int S_FREE = 5;
    private static final int S_OVER = 8;
    private static final int S_PAUSED = 9;
    private static final int S_CATALOG = 10;
    private static final int S_DETAIL = 11;
    private static final int S_SETTINGS = 12;
    private static final int S_EFFECT_SETUP = 13;

    private static final int M_CLASSIC = 0;
    private static final int M_EFFECT = 1;
    private static final int M_FREE = 2;

    private static final float DESIGN_W = 450f;
    private static final float LANDSCAPE_W = 1280f;
    private static final float LIST_TOP = 104f;
    private static final float ROW_H = 64f;
    private static final float SWIPE_H = 16f;
    private static final float SWIPE_V = 24f;
    private static final float SWIPE_H_RELEASE = 12f;
    private static final float SWIPE_V_RELEASE = 20f;
    private static final float EDGE_GUARD = 18f;
    private static final float PRESS_HOLD = 0.15f;
    private static final float FLASH_HOLD = 0.145f;
    private static final float GHOST_LIFE = 0.45f;
    private static final String HANGING_PUNCT = "，。、；：！？）」』》";

    private final Prefs prefs;
    private final IconBank icons;
    private final SoundHaptics sound;
    private final Random random = new Random();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Ghost> ghosts = new ArrayList<>();

    private ClassicSession classic;
    private EffectSession effect;
    private GameSession free;
    private EffectMode effectMode = EffectMode.NONE;
    private boolean effectOneHeart;

    private int screen = S_MENU;
    private int mode = M_CLASSIC;
    private int selectedMode = M_CLASSIC;
    private int pausedFrom = S_CLASSIC;
    private int catalogFrom = S_MENU;
    private int settingsFrom = S_MENU;

    private float scale = 1f;
    private float offsetX;
    private float offsetY;
    private float designW = DESIGN_W;
    private float designH = 800f;
    private int viewW;
    private int viewH;
    private int safeLeftPx;
    private int safeTopPx;
    private int safeRightPx;
    private int safeBottomPx;
    private boolean landscape;
    private boolean orientationKnown;

    private long lastFrameMs;
    private float now;
    private boolean hostPaused;
    private boolean released;

    private float countdown;
    private int lastCount = 99;

    private float shakeUntil;
    private float wrongUntil;
    private float successUntil;
    private float pressUntil;
    private float revealUntil;
    private Direction pressDir;

    private float downX;
    private float downY;
    private float catalogLastY;
    private float swipeStartX;
    private float swipeStartY;
    private float systemCancelDx;
    private float systemCancelDy;
    private long inputLockUntilMs;
    private Direction lastInputDir;
    private float lastInputAt = -10f;
    private boolean swipeTracking;
    private boolean swipeConsumed;
    private boolean systemSwipeCanceled;
    private boolean pauseTouch;
    private boolean catalogTouch;
    private boolean listTouch;
    private boolean catalogDragging;
    private int volumeRow = -1;

    private float catalogScroll;
    private Stratagem detailStratagem;

    private int finalMode;
    private EffectMode finalEffectMode = EffectMode.NONE;
    private int finalScore;
    private int finalCompleted;
    private int finalRound;
    private int finalArrows;
    private int finalLines;
    private float finalDuration;
    private int finalSpeed;
    private int finalBest;
    private boolean finalRecord;

    private static final class Ghost {
        final Direction dir;
        final float x;
        final float y;
        final float born;

        Ghost(Direction dir, float x, float y, float born) {
            this.dir = dir;
            this.x = x;
            this.y = y;
            this.born = born;
        }
    }

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        prefs = new Prefs(context);
        icons = new IconBank(context);
        sound = new SoundHaptics(context);
        sound.setEnabled(prefs.sound());
        sound.setHapticsEnabled(prefs.haptics());
        sound.setVoiceEnabled(prefs.voice());
        sound.setBgmPercent(prefs.bgmVolume());
        sound.setVoicePercent(prefs.voiceVolume());
        effectMode = EffectMode.fromKey(prefs.effectMode());
        effectOneHeart = prefs.effectOneHeart();
        classic = new ClassicSession(StratagemCatalog.ALL, random);
        effect = new EffectSession(StratagemCatalog.ALL, random, effectMode, effectOneHeart);
        free = new GameSession(StratagemCatalog.ALL, random);
        setContentDescription("战略配备英雄");
    }

    // ---------------------------------------------------------------- lifecycle

    public void onHostPause() {
        hostPaused = true;
        lastFrameMs = 0L;
        if (isPlayScreen()) {
            pauseGame();
            sound.pauseHeroLoop();
        }
    }

    public void onHostResume() {
        hostPaused = false;
        lastFrameMs = 0L;
        if (isPlayScreen()) sound.resumeHeroLoop();
    }

    public void release() {
        if (released) return;
        released = true;
        sound.release();
    }

    public boolean handleBack() {
        switch (screen) {
            case S_MENU:
                return false;
            case S_PAUSED:
                resumeGame();
                return true;
            case S_CATALOG:
                screen = catalogFrom;
                return true;
            case S_DETAIL:
                screen = S_CATALOG;
                return true;
            case S_SETTINGS:
                screen = settingsFrom;
                return true;
            case S_HELP:
            case S_EFFECT_SETUP:
                screen = S_MENU;
                return true;
            case S_COUNTDOWN:
                screen = S_MENU;
                return true;
            case S_OVER:
                exitToMenu();
                return true;
            default:
                if (isPlayScreen() && systemSwipeCanceled) {
                    systemSwipeCanceled = false;
                    triggerInput(backGestureDirection(),
                            swipeStartX + systemCancelDx, swipeStartY + systemCancelDy);
                    return true;
                }
                pauseGame();
                return true;
        }
    }

    private Direction backGestureDirection() {
        float adx = Math.abs(systemCancelDx);
        float ady = Math.abs(systemCancelDy);
        if (adx < 8f && ady < 8f) return Direction.RIGHT;
        if (adx >= ady) return systemCancelDx >= 0f ? Direction.RIGHT : Direction.LEFT;
        return systemCancelDy >= 0f ? Direction.DOWN : Direction.UP;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        boolean wasLandscape = landscape;
        viewW = w;
        viewH = h;
        boolean newLandscape = w > h;
        if (orientationKnown && newLandscape != wasLandscape && isPlayScreen()) {
            pauseGame();
        }
        orientationKnown = true;
        recomputeGeometry();
    }

    public void setSafeInsets(int leftPx, int topPx, int rightPx, int bottomPx) {
        int left = Math.max(0, leftPx);
        int top = Math.max(0, topPx);
        int right = Math.max(0, rightPx);
        int bottom = Math.max(0, bottomPx);
        if (left == safeLeftPx && top == safeTopPx && right == safeRightPx && bottom == safeBottomPx) {
            return;
        }
        safeLeftPx = left;
        safeTopPx = top;
        safeRightPx = right;
        safeBottomPx = bottom;
        recomputeGeometry();
        invalidate();
    }

    /** Kept for callers that only know the vertical cutout. */
    public void setSafeInsets(int topPx, int bottomPx) {
        setSafeInsets(0, topPx, 0, bottomPx);
    }

    private void recomputeGeometry() {
        if (viewW <= 0 || viewH <= 0) return;
        landscape = viewW > viewH;
        int availW = Math.max(1, viewW - safeLeftPx - safeRightPx);
        int usableH = Math.max(1, viewH - safeTopPx - safeBottomPx);
        if (landscape) {
            designW = LANDSCAPE_W;
            scale = Math.min(availW / LANDSCAPE_W, usableH / 520f);
        } else {
            designW = DESIGN_W;
            scale = Math.min(availW / DESIGN_W, usableH / 760f);
        }
        if (scale <= 0.01f) scale = 0.01f;
        designH = usableH / scale;
        offsetX = safeLeftPx + (availW - designW * scale) * 0.5f;
        offsetY = safeTopPx;
        refreshGestureExclusion();
    }

    public void refreshGestureExclusion() {
        if (Build.VERSION.SDK_INT < 29) return;
        try {
            int h = getHeight();
            int w = getWidth();
            float density = getResources().getDisplayMetrics().density;
            int top = Math.max(0, h - (int) (260f * density));
            setSystemGestureExclusionRects(java.util.Collections.singletonList(
                    new Rect(0, top, w, h)));
        } catch (Throwable ignored) {
            // Some OEMs reject exclusion; the app remains fully usable.
        }
    }

    // ---------------------------------------------------------------- frame loop

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long ms = SystemClock.uptimeMillis();
        float dt = lastFrameMs == 0L ? 0f : (ms - lastFrameMs) / 1000f;
        if (dt > 0.10f) dt = 0.10f;
        lastFrameMs = ms;
        now = ms / 1000f;
        updateFrame(dt);

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);
        drawScreen(canvas);
        canvas.restore();
        postInvalidateOnAnimation();
    }

    private void updateFrame(float dt) {
        if (hostPaused) return;

        if (screen == S_COUNTDOWN) {
            countdown -= dt;
            int digit = (int) Math.ceil(countdown);
            if (digit > 0 && digit != lastCount) {
                lastCount = digit;
                sound.countdown(digit == 1);
            }
            if (countdown <= 0f) beginPlay();
        }

        if (screen == S_CLASSIC) {
            int before = classic.getState();
            classic.update(dt);
            int after = classic.getState();
            if (after != before) {
                if (after == ClassicSession.PLAYING && before == ClassicSession.READY) sound.start();
            }
            if (classic.isOver()) showOver();
        } else if (screen == S_EFFECT) {
            int before = effect.getState();
            effect.update(dt);
            int after = effect.getState();
            if (after != before) {
                if (after == EffectSession.PLAYING && before == EffectSession.INTERMISSION) {
                    sound.start();
                }
            }
            if (effect.isOver()) showOver();
        }

        for (int i = ghosts.size() - 1; i >= 0; i--) {
            if (now - ghosts.get(i).born > GHOST_LIFE) ghosts.remove(i);
        }
    }

    private void drawScreen(Canvas c) {
        switch (screen) {
            case S_MENU:
                drawMenu(c);
                break;
            case S_HELP:
                drawHelp(c);
                break;
            case S_COUNTDOWN:
                drawCountdown(c);
                break;
            case S_CLASSIC:
            case S_EFFECT:
            case S_FREE:
                drawGameScene(c);
                break;
            case S_OVER:
                drawOver(c);
                break;
            case S_PAUSED:
                drawGameScene(c);
                drawPause(c);
                break;
            case S_CATALOG:
                drawCatalog(c);
                break;
            case S_DETAIL:
                drawDetail(c);
                break;
            case S_SETTINGS:
                drawSettings(c);
                break;
            case S_EFFECT_SETUP:
                drawEffectSetup(c);
                break;
            default:
                break;
        }
    }

    // ---------------------------------------------------------------- flow

    private boolean isPlayScreen() {
        return screen == S_CLASSIC || screen == S_EFFECT || screen == S_FREE;
    }

    private void startSelected() {
        if (selectedMode == M_FREE) {
            openCatalog(S_MENU);
        } else if (selectedMode == M_EFFECT) {
            screen = S_EFFECT_SETUP;
        } else {
            startMode(selectedMode, true, 3.0f);
        }
    }

    private void startMode(int m, boolean reset, float seconds) {
        mode = m;
        selectedMode = m;
        if (m == M_CLASSIC) {
            classic = new ClassicSession(StratagemCatalog.ALL, random);
            classic.start();
        } else if (m == M_EFFECT) {
            effect = new EffectSession(StratagemCatalog.ALL, random, effectMode, effectOneHeart);
            effect.start();
        } else {
            free = new GameSession(StratagemCatalog.ALL, random);
            free.start();
        }
        countdown = seconds;
        lastCount = 99;
        revealUntil = 0f;
        finalDuration = 0f;
        inputLockUntilMs = 0L;
        screen = S_COUNTDOWN;
        sound.stopHeroLoop();
    }

    private void beginPlay() {
        screen = mode == M_CLASSIC ? S_CLASSIC : mode == M_EFFECT ? S_EFFECT : S_FREE;
        sound.start();
        sound.startHeroLoop();
        lastFrameMs = 0L;
    }

    private void pauseGame() {
        if (!isPlayScreen()) return;
        pausedFrom = screen;
        screen = S_PAUSED;
        sound.pauseHeroLoop();
    }

    private void resumeGame() {
        if (screen != S_PAUSED) return;
        screen = pausedFrom;
        sound.resumeHeroLoop();
        lastFrameMs = 0L;
    }

    private void restart() {
        startMode(mode, true, 3.0f);
    }

    private void exitToMenu() {
        recordCurrentScore();
        screen = S_MENU;
        selectedMode = mode;
        sound.stopHeroLoop();
    }

    private void startEffect() {
        prefs.setEffectMode(effectMode.key);
        prefs.setEffectOneHeart(effectOneHeart);
        startMode(M_EFFECT, true, 3.0f);
    }

    private void recordCurrentScore() {
        if (!isPlayScreen() && screen != S_OVER) return;
        if (mode == M_CLASSIC && classic != null) {
            prefs.submit(modeKey(M_CLASSIC), classic.getScore());
        } else if (mode == M_EFFECT && effect != null) {
            prefs.submit(modeKey(M_EFFECT), effect.getScore());
        }
    }

    private String modeKey(int m) {
        if (m == M_CLASSIC) return "classic";
        if (m == M_EFFECT) return effectMode.prefKey();
        return "free";
    }

    private String modeZh(int m) {
        if (m == M_CLASSIC) return "经典模式";
        if (m == M_EFFECT) return effectMode.zh;
        return "自由呼叫";
    }

    private String modeEn(int m) {
        if (m == M_CLASSIC) return "CLASSIC";
        if (m == M_EFFECT) return "EFFECT · " + effectMode.en;
        return "FREE CALL";
    }

    private int bestForSelected() {
        if (selectedMode == M_CLASSIC) return prefs.best("classic");
        if (selectedMode == M_EFFECT) return prefs.best(effectMode.prefKey());
        return 0;
    }

    private void showOver() {
        finalMode = mode;
        finalEffectMode = effectMode;
        if (mode == M_CLASSIC) {
            finalScore = classic.getScore();
            finalCompleted = classic.getTotalCompleted();
            finalRound = classic.getRound();
            finalArrows = classic.getTotalArrows();
            finalLines = 0;
            finalSpeed = 0;
        } else {
            finalScore = effect.getScore();
            finalCompleted = effect.getArrowCompleted();
            finalRound = effect.getRound();
            finalArrows = effect.getArrowCompleted();
            finalLines = effect.getLinesCompleted();
            finalDuration = effect.getTotalTimer();
            finalSpeed = effect.getSpeed();
        }
        finalRecord = prefs.submit(modeKey(mode), finalScore);
        finalBest = Math.max(finalScore, prefs.best(modeKey(mode)));
        screen = S_OVER;
        sound.stopHeroLoop();
        sound.gameOver();
    }

    // ---------------------------------------------------------------- input

    private void triggerInput(Direction dir, float x, float y) {
        if (!isPlayScreen()) return;
        if (screen == S_FREE && SystemClock.uptimeMillis() < inputLockUntilMs) return;

        if (screen == S_CLASSIC) {
            ClassicSession.Result r = classic.input(dir);
            if (r == ClassicSession.Result.WRONG) {
                sound.error();
                wrongUntil = now + 0.46f;
                shakeUntil = now + 0.29f;
            } else if (r == ClassicSession.Result.COMPLETE) {
                sound.success();
                sound.callIn(classic.getLastCompleted());
                if (classic.getState() == ClassicSession.ROUND_SCORE) sound.roundComplete();
                successUntil = now + 0.70f;
            } else if (r == ClassicSession.Result.CORRECT) {
                sound.tick();
            }
        } else if (screen == S_EFFECT) {
            EffectSession.Result r = effect.input(dir);
            if (r == EffectSession.Result.WRONG) {
                sound.error();
                wrongUntil = now + 0.46f;
                shakeUntil = now + 0.29f;
                if (effect.isOver()) showOver();
            } else if (r == EffectSession.Result.COMPLETE) {
                sound.success();
                sound.callIn(effect.getLastCompleted());
                if (effect.isIntermission()) sound.roundComplete();
                successUntil = now + 0.70f;
                if (effect.isOver()) showOver();
            } else if (r == EffectSession.Result.CORRECT) {
                sound.tick();
            }
        } else if (screen == S_FREE) {
            GameSession.Result r = free.input(dir);
            if (r == GameSession.Result.WRONG) {
                sound.error();
                wrongUntil = now + 0.46f;
                shakeUntil = now + 0.29f;
            } else if (r == GameSession.Result.COMPLETE) {
                sound.success();
                sound.callIn(free.getLastCompleted());
                revealUntil = now + 2.60f;
                successUntil = now + 0.70f;
                inputLockUntilMs = SystemClock.uptimeMillis() + 2650L;
            } else if (r == GameSession.Result.CORRECT) {
                sound.tick();
            }
        }
        if (screen != S_CLASSIC && screen != S_EFFECT && screen != S_FREE) return;
        pressDir = dir;
        pressUntil = now + PRESS_HOLD;
        lastInputDir = dir;
        lastInputAt = now;
        ghosts.add(new Ghost(dir, x, y, now));
        if (ghosts.size() > 10) ghosts.remove(0);
    }

    private Direction padArrowAt(float x, float y) {
        float dx = x - padCx();
        float dy = y - padCy();
        float dist = (float) Math.hypot(dx, dy);
        float k = padR() / 170f;
        if (dist < 66f * k || dist > padR() + 38f * k) return null;
        return directionFor(dx, dy);
    }

    private static Direction directionFor(float dx, float dy) {
        if (Math.abs(dx) > Math.abs(dy)) return dx >= 0f ? Direction.RIGHT : Direction.LEFT;
        return dy >= 0f ? Direction.DOWN : Direction.UP;
    }

    private void feedSwipe(float x, float y, boolean release) {
        if (!swipeTracking || swipeConsumed) return;
        float dx = x - swipeStartX;
        float dy = y - swipeStartY;
        boolean horizontal = Math.abs(dx) > Math.abs(dy);
        float threshold = horizontal
                ? (release ? SWIPE_H_RELEASE : SWIPE_H)
                : (release ? SWIPE_V_RELEASE : SWIPE_V);
        if (Math.abs(dx) + Math.abs(dy) <= threshold) return;
        swipeConsumed = true;
        triggerInput(directionFor(dx, dy), x, y);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = (event.getX() - offsetX) / scale;
        float y = (event.getY() - offsetY) / scale;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downX = x;
                downY = y;
                swipeTracking = false;
                swipeConsumed = false;
                systemSwipeCanceled = false;
                pauseTouch = false;
                catalogTouch = false;
                listTouch = false;
                catalogDragging = false;
                catalogLastY = y;
                volumeRow = -1;
                if (isPlayScreen()) {
                    pauseTouch = hit(pauseRect(), x, y);
                    catalogTouch = screen == S_FREE && hit(catalogBtnRect(), x, y);
                    if (!pauseTouch && !catalogTouch) playDown(x, y);
                } else if (screen == S_CATALOG) {
                    if (y >= LIST_TOP && !hit(backRect(), x, y) && !hit(catalogBlindRect(), x, y)) {
                        listTouch = true;
                    }
                } else if (screen == S_SETTINGS) {
                    volumeRow = settingsSliderAt(x, y);
                    if (volumeRow >= 0) {
                        updateVolumeFromX(volumeRow, x);
                        return true;
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (volumeRow >= 0) {
                    updateVolumeFromX(volumeRow, x);
                    return true;
                }
                feedSwipe(x, y, false);
                float moved = Math.abs(x - downX) + Math.abs(y - downY);
                if (listTouch && moved > 9f) {
                    catalogScroll -= (y - catalogLastY);
                    catalogLastY = y;
                    catalogDragging = true;
                    clampCatalogScroll();
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (volumeRow >= 0) {
                    updateVolumeFromX(volumeRow, x);
                    volumeRow = -1;
                    return true;
                }
                float moved = Math.abs(x - downX) + Math.abs(y - downY);
                if (isPlayScreen()) {
                    if (pauseTouch && moved < 24f) {
                        pauseGame();
                    } else if (catalogTouch && moved < 24f) {
                        openCatalog(S_FREE);
                    } else {
                        feedSwipe(x, y, true);
                    }
                } else if (moved < 32f) {
                    handleTap(x, y);
                }
                swipeTracking = false;
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                volumeRow = -1;
                if (isPlayScreen() && swipeTracking) {
                    feedSwipe(x, y, true);
                    if (!swipeConsumed) {
                        systemSwipeCanceled = true;
                        systemCancelDx = x - swipeStartX;
                        systemCancelDy = y - swipeStartY;
                    }
                }
                swipeTracking = false;
                return true;
            }
            default:
                return true;
        }
    }

    private void playDown(float x, float y) {
        boolean clickMode = prefs.pad();
        if (clickMode && (x < EDGE_GUARD || x > designW - EDGE_GUARD
                || y < EDGE_GUARD || y > designH - EDGE_GUARD)) {
            return;
        }
        if (clickMode) {
            Direction d = padArrowAt(x, y);
            if (d != null) {
                pressDir = d;
                pressUntil = now + PRESS_HOLD;
                triggerInput(d, x, y);
                return;
            }
        }
        swipeTracking = true;
        swipeStartX = x;
        swipeStartY = y;
    }

    private void handleTap(float x, float y) {
        switch (screen) {
            case S_MENU:
                if (hit(soundRect(), x, y)) {
                    setSoundEnabled(!prefs.sound());
                } else if (hit(settingsRect(), x, y)) {
                    openSettings(S_MENU);
                } else if (hit(helpRect(), x, y)) {
                    screen = S_HELP;
                } else if (hit(menuStartRect(), x, y)) {
                    startSelected();
                } else {
                    for (int i = 0; i < 3; i++) {
                        if (hit(menuTabRect(i), x, y)) {
                            selectedMode = i;
                            return;
                        }
                    }
                }
                return;
            case S_EFFECT_SETUP:
                if (hit(backRect(), x, y)) {
                    screen = S_MENU;
                } else if (hit(effectOneHeartRect(), x, y)) {
                    effectOneHeart = !effectOneHeart;
                    prefs.setEffectOneHeart(effectOneHeart);
                } else if (hit(effectStartRect(), x, y)) {
                    startEffect();
                } else {
                    for (int i = 0; i < EffectMode.values().length; i++) {
                        if (hit(effectRowRect(i), x, y)) {
                            effectMode = EffectMode.values()[i];
                            prefs.setEffectMode(effectMode.key);
                            return;
                        }
                    }
                }
                return;
            case S_HELP:
                if (hit(backRect(), x, y) || hit(helpBackRect(), x, y)) screen = S_MENU;
                return;
            case S_PAUSED:
                if (hit(pauseResumeRect(), x, y)) {
                    resumeGame();
                } else if (hit(pauseRestartRect(), x, y)) {
                    restart();
                } else if (hit(pauseSettingsRect(), x, y)) {
                    openSettings(S_PAUSED);
                } else if (hit(pauseExitRect(), x, y)) {
                    exitToMenu();
                }
                return;
            case S_OVER:
                if (hit(overAgainRect(), x, y)) {
                    startMode(finalMode, true, 3.0f);
                } else if (hit(overMenuRect(), x, y)) {
                    screen = S_MENU;
                    selectedMode = finalMode;
                }
                return;
            case S_CATALOG:
                if (hit(backRect(), x, y)) {
                    screen = catalogFrom;
                } else if (hit(catalogBlindRect(), x, y)) {
                    startMode(M_FREE, true, 3.0f);
                } else if (hit(catalogListRect(), x, y) && !catalogDragging) {
                    int idx = catalogIndexAt(y);
                    if (idx >= 0 && idx < StratagemCatalog.ALL.size()) {
                        detailStratagem = StratagemCatalog.ALL.get(idx);
                        screen = S_DETAIL;
                    }
                }
                return;
            case S_DETAIL:
                if (hit(backRect(), x, y)) {
                    screen = S_CATALOG;
                } else if (hit(detailStartRect(), x, y)) {
                    startMode(M_FREE, true, 3.0f);
                } else if (hit(detailBackRect(), x, y)) {
                    screen = S_CATALOG;
                }
                return;
            case S_SETTINGS:
                if (hit(settingsBackRect(), x, y) || hit(backRect(), x, y)) {
                    volumeRow = -1;
                    screen = settingsFrom;
                } else {
                    for (int i = 0; i < 5; i++) {
                        if (hit(settingsRowRect(i), x, y)) {
                            if (i == 0) setSoundEnabled(!prefs.sound());
                            else if (i == 2) setVoiceEnabled(!prefs.voice());
                            else if (i == 4) setPadEnabled(!prefs.pad());
                            return;
                        }
                    }
                }
                return;
            default:
                return;
        }
    }

    private void setSoundEnabled(boolean on) {
        prefs.setSound(on);
        prefs.setHaptics(on);
        sound.setEnabled(on);
        sound.setHapticsEnabled(on);
        if (on && isPlayScreen()) sound.startHeroLoop();
    }

    private void setVoiceEnabled(boolean on) {
        prefs.setVoice(on);
        sound.setVoiceEnabled(on);
    }

    private void setBgmVolume(int percent) {
        prefs.setBgmVolume(percent);
        sound.setBgmPercent(percent);
    }

    private void setVoiceVolume(int percent) {
        prefs.setVoiceVolume(percent);
        sound.setVoicePercent(percent);
    }

    private int settingsSliderAt(float x, float y) {
        if (screen != S_SETTINGS) return -1;
        if (hit(settingsRowRect(1), x, y)) return 1;
        if (hit(settingsRowRect(3), x, y)) return 3;
        return -1;
    }

    private void updateVolumeFromX(int row, float x) {
        RectF r = settingsRowRect(row);
        float left = r.left + 24f;
        float right = r.right - 24f;
        int percent = Math.round((x - left) / (right - left) * 200f);
        percent = Math.max(0, Math.min(200, percent));
        if (row == 1) {
            if (percent != prefs.bgmVolume()) setBgmVolume(percent);
        } else if (row == 3) {
            if (percent != prefs.voiceVolume()) setVoiceVolume(percent);
        }
    }

    private void setPadEnabled(boolean on) {
        prefs.setPad(on);
    }

    private void openSettings(int from) {
        volumeRow = -1;
        settingsFrom = from;
        screen = S_SETTINGS;
    }

    private void openCatalog(int from) {
        catalogFrom = from;
        catalogScroll = 0f;
        screen = S_CATALOG;
    }

    private void clampCatalogScroll() {
        float max = Math.max(0f, StratagemCatalog.ALL.size() * ROW_H - (designH - LIST_TOP - 38f));
        if (catalogScroll < 0f) catalogScroll = 0f;
        if (catalogScroll > max) catalogScroll = max;
    }

    private int catalogIndexAt(float y) {
        if (y < LIST_TOP) return -1;
        float local = y - LIST_TOP + catalogScroll;
        return (int) (local / ROW_H);
    }

    // ---------------------------------------------------------------- menu

    private float menuShift() {
        return clamp((designH - 800f) * 0.60f, 0f, 140f);
    }

    private void drawMenu(Canvas c) {
        UiKit.background(c, designW, designH);
        drawFrameCorners(c);
        if (landscape) drawMenuLandscape(c);
        else drawMenuPortrait(c);
    }

    private void drawMenuPortrait(Canvas c) {
        float cx = designW * 0.5f;
        float s = menuShift();
        float emblemCy = 112f + s;
        float emblemR = 54f;
        UiKit.ring(c, cx, emblemCy, emblemR + 8f, 0x44FFD24A, 1f);
        UiKit.ring(c, cx, emblemCy, emblemR + 14f, 0x22FFD24A, 0.8f);
        c.save();
        Path clip = new Path();
        clip.addCircle(cx, emblemCy, emblemR, Path.Direction.CW);
        c.clipPath(clip);
        icons.draw(c, "ic_launcher", cx - emblemR, emblemCy - emblemR,
                cx + emblemR, emblemCy + emblemR);
        c.restore();

        UiKit.text(c, "STRATAGEM", cx, 246f + s, 34f, UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, "HERO", cx, 310f + s, 64f, UiKit.GOLD, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, "战略配备英雄", cx, 342f + s, 17f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
        int best = bestForSelected();
        String bestText = selectedMode == M_FREE ? "沙盒" : best == 0 ? "-----" : five(best);
        UiKit.text(c, "历史最高   " + bestText, cx, 376f + s, 12f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
        UiKit.line(c, cx - 71f, 366f + s, cx - 49f, 366f + s, 1f, 0x55FFD24A);
        UiKit.line(c, cx + 49f, 366f + s, cx + 71f, 366f + s, 1f, 0x55FFD24A);

        for (int i = 0; i < 3; i++) drawMenuTab(c, i);

        String d1;
        String d2;
        if (selectedMode == M_CLASSIC) {
            d1 = "每回合 15 秒，完成目标抢回时间。";
            d2 = "先看战备再输入 · 按错不扣时";
        } else if (selectedMode == M_EFFECT) {
            d1 = "五种特殊规则，另有「一错即死」。";
            d2 = "默写 · 长城 · 多行 · 终端";
        } else {
            d1 = "无计时、无分数。";
            d2 = "图鉴查代码 · 盲呼练输入";
        }
        UiKit.text(c, d1, cx, 504f + s, 13f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
        UiKit.text(c, d2, cx, 526f + s, 11f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);

        RectF start = menuStartRect();
        drawPrimary(c, start, selectedMode == M_FREE ? "进 入 图 鉴"
                : selectedMode == M_EFFECT ? "选 择 效 果" : "开 始 演 练");
        drawControlHint(c, start);

        drawSoundIcon(c, soundRect().centerX(), soundRect().centerY(), 17f, prefs.sound());
        drawSettingsIcon(c, settingsRect().centerX(), settingsRect().centerY(), 17f);
        drawHelpIcon(c, helpRect().centerX(), helpRect().centerY(), 17f);

        UiKit.text(c, "v1.1   ·   " + StratagemCatalog.ALL.size() + " 战备", 24f, designH - 22f,
                10f, UiKit.DIM, Paint.Align.LEFT, UiKit.BODY);
        UiKit.text(c, "非官方粉丝作品", designW - 24f, designH - 22f, 10f,
                UiKit.DIM, Paint.Align.RIGHT, UiKit.BODY);
    }

    private void drawMenuLandscape(Canvas c) {
        float brandCx = designW * 0.28f;
        float emblemCy = designH * 0.40f;
        float emblemR = 64f;
        UiKit.ring(c, brandCx, emblemCy, emblemR + 8f, 0x44FFD24A, 1f);
        UiKit.ring(c, brandCx, emblemCy, emblemR + 14f, 0x22FFD24A, 0.8f);
        c.save();
        Path clip = new Path();
        clip.addCircle(brandCx, emblemCy, emblemR, Path.Direction.CW);
        c.clipPath(clip);
        icons.draw(c, "ic_launcher", brandCx - emblemR, emblemCy - emblemR,
                brandCx + emblemR, emblemCy + emblemR);
        c.restore();

        UiKit.text(c, "STRATAGEM", brandCx, emblemCy + 92f, 38f, UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, "HERO", brandCx, emblemCy + 152f, 68f, UiKit.GOLD, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, "战略配备英雄", brandCx, emblemCy + 180f, 18f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
        int best = bestForSelected();
        String bestText = selectedMode == M_FREE ? "沙盒" : best == 0 ? "-----" : five(best);
        UiKit.text(c, "历史最高   " + bestText, brandCx, emblemCy + 208f, 12f,
                UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);

        UiKit.line(c, designW * 0.52f, designH * 0.14f, designW * 0.52f, designH * 0.86f,
                0.8f, 0x33FFD24A);

        for (int i = 0; i < 3; i++) drawMenuTab(c, i);

        String d1;
        String d2;
        if (selectedMode == M_CLASSIC) {
            d1 = "每回合 15 秒，完成目标抢回时间。";
            d2 = "按错只清空输入并锁定 0.4 秒，不扣时间。";
        } else if (selectedMode == M_EFFECT) {
            d1 = "五种特殊规则，另有「一错即死」。";
            d2 = "先选择规则，再进入 3 秒倒计时。";
        } else {
            d1 = "无计时、无分数。";
            d2 = "图鉴查代码 · 盲呼练输入";
        }
        float rightCx = (menuTabRect(0).left + menuTabRect(0).right) * 0.5f;
        UiKit.text(c, d1, rightCx, menuStartRect().top - 34f, 13f,
                UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
        UiKit.text(c, d2, rightCx, menuStartRect().top - 17f, 10f,
                UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);

        RectF start = menuStartRect();
        drawPrimary(c, start, selectedMode == M_FREE ? "进 入 图 鉴"
                : selectedMode == M_EFFECT ? "选 择 效 果" : "开 始 演 练");
        UiKit.text(c, prefs.pad() ? "圆盘按下即输入 · 也可全屏滑动" : "全屏滑动 · 一笔一个方向",
                start.centerX(), start.bottom + 26f, 10f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);

        drawSoundIcon(c, soundRect().centerX(), soundRect().centerY(), 17f, prefs.sound());
        drawSettingsIcon(c, settingsRect().centerX(), settingsRect().centerY(), 17f);
        drawHelpIcon(c, helpRect().centerX(), helpRect().centerY(), 17f);

        UiKit.text(c, "v1.1   ·   " + StratagemCatalog.ALL.size() + " 战备", 40f, designH - 22f,
                10f, UiKit.DIM, Paint.Align.LEFT, UiKit.BODY);
        UiKit.text(c, "非官方粉丝作品", designW - 40f, designH - 22f, 10f,
                UiKit.DIM, Paint.Align.RIGHT, UiKit.BODY);
    }

    private void drawMenuTab(Canvas c, int i) {
        RectF r = menuTabRect(i);
        boolean active = i == selectedMode;
        UiKit.panel(c, r.left, r.top, r.right, r.bottom, 6f,
                active ? UiKit.GOLD : 0xCC10191B,
                active ? UiKit.GOLD_SOFT : 0x44F1EEDC);
        UiKit.text(c, i == 0 ? "经典模式" : i == 1 ? "效果模式" : "自由呼叫",
                r.centerX(), r.top + (r.height() + 9f) * 0.5f, landscape ? 15f : 14f,
                active ? 0xFF101313 : UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY_BOLD);
    }

    private float menuLandscapeTop() {
        return clamp((designH - 306f) * 0.5f - 10f, 108f, designH * 0.30f);
    }

    private RectF menuTabRect(int i) {
        if (landscape) {
            float top = menuLandscapeTop() + i * 58f;
            return new RectF(designW * 0.56f, top, designW - 70f, top + 52f);
        }
        float s = menuShift();
        float w = 126f;
        float gap = 6f;
        float left = 24f + i * (w + gap);
        return new RectF(left, 424f + s, left + w, 466f + s);
    }

    private RectF menuStartRect() {
        if (landscape) {
            float top = menuLandscapeTop() + 230f;
            return new RectF(designW * 0.56f, top, designW - 70f, top + 56f);
        }
        float s = menuShift();
        return new RectF(designW * 0.5f - 150f, 548f + s, designW * 0.5f + 150f, 604f + s);
    }

    private RectF soundRect() {
        if (landscape) return new RectF(designW - 300f, 26f, designW - 244f, 82f);
        return new RectF(20f, 26f, 78f, 82f);
    }

    private RectF settingsRect() {
        if (landscape) return new RectF(designW - 190f, 26f, designW - 134f, 82f);
        return new RectF(299f, 26f, 359f, 82f);
    }

    private RectF helpRect() {
        if (landscape) return new RectF(designW - 80f, 26f, designW - 24f, 82f);
        return new RectF(372f, 26f, 430f, 82f);
    }

    private void drawControlHint(Canvas c, RectF start) {
        float infoH = 66f;
        float footerTop = designH - 42f;
        float top = (start.bottom + footerTop) * 0.5f - infoH * 0.5f;
        top = Math.max(top, start.bottom + 18f);
        top = Math.min(top, footerTop - infoH - 6f);
        if (top < start.bottom + 8f || top + infoH > designH - 30f) return;
        float left = designW * 0.5f - 175f;
        UiKit.panel(c, left, top, left + 350f, top + infoH, 8f, 0x9910191B, 0x33F1EEDC);
        UiKit.text(c, "操 作  /  CONTROLS", designW * 0.5f, top + 18f, 8f, UiKit.GOLD,
                Paint.Align.CENTER, UiKit.BODY_BOLD);
        UiKit.text(c, prefs.pad() ? "按下圆盘即输入  ·  也可在别处滑动" : "全屏滑动  ·  一笔一个方向",
                designW * 0.5f, top + 38f, 11f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
        UiKit.text(c, "音效、语音与四向键盘可在设置中调整", designW * 0.5f, top + 54f, 9f,
                UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
    }

    private void drawFrameCorners(Canvas c) {
        UiKit.bracket(c, 14f, 14f, designW - 14f, designH - 14f, 18f, 0x33FFD24A, 1f);
    }

    private void drawPrimary(Canvas c, RectF r, String label) {
        UiKit.panel(c, r.left, r.top, r.right, r.bottom, 10f, UiKit.GOLD, UiKit.GOLD_SOFT);
        UiKit.text(c, label, r.centerX(), r.top + 36f, 19f, 0xFF101313,
                Paint.Align.CENTER, UiKit.BODY_BOLD);
        UiKit.arrowOutline(c, r.left + 26f, r.centerY(), 15f, Direction.RIGHT, 0x55101313, 1.3f);
        UiKit.arrowOutline(c, r.right - 26f, r.centerY(), 15f, Direction.LEFT, 0x55101313, 1.3f);
    }

    private void drawSoundIcon(Canvas c, float cx, float cy, float size, boolean on) {
        UiKit.ring(c, cx, cy, 25f, 0x33F1EEDC, 1f);
        Path p = new Path();
        p.moveTo(cx - size * 0.55f, cy - size * 0.22f);
        p.lineTo(cx - size * 0.22f, cy - size * 0.22f);
        p.lineTo(cx + size * 0.16f, cy - size * 0.60f);
        p.lineTo(cx + size * 0.16f, cy + size * 0.60f);
        p.lineTo(cx - size * 0.22f, cy + size * 0.22f);
        p.lineTo(cx - size * 0.55f, cy + size * 0.22f);
        p.close();
        textPaint.reset();
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(on ? UiKit.CREAM : UiKit.DIM);
        c.drawPath(p, textPaint);
        if (on) {
            textPaint.setStyle(Paint.Style.STROKE);
            textPaint.setStrokeWidth(1.5f);
            textPaint.setColor(UiKit.GOLD);
            c.drawArc(new RectF(cx + size * 0.05f, cy - size * 0.55f,
                    cx + size * 0.85f, cy + size * 0.55f), -45f, 90f, false, textPaint);
            c.drawArc(new RectF(cx + size * 0.05f, cy - size * 0.88f,
                    cx + size * 1.18f, cy + size * 0.88f), -42f, 84f, false, textPaint);
        } else {
            UiKit.line(c, cx + size * 0.30f, cy - size * 0.34f, cx + size * 0.78f, cy + size * 0.34f, 1.8f, UiKit.RED);
            UiKit.line(c, cx + size * 0.78f, cy - size * 0.34f, cx + size * 0.30f, cy + size * 0.34f, 1.8f, UiKit.RED);
        }
    }

    private void drawHelpIcon(Canvas c, float cx, float cy, float size) {
        UiKit.ring(c, cx, cy, 25f, 0x33F1EEDC, 1f);
        UiKit.text(c, "?", cx, cy + size * 0.55f, size * 1.35f, UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
    }

    private void drawSettingsIcon(Canvas c, float cx, float cy, float size) {
        UiKit.ring(c, cx, cy, 25f, 0x33F1EEDC, 1f);
        float[] ys = {cy - size * 0.48f, cy, cy + size * 0.48f};
        float[] knobs = {cx + size * 0.32f, cx - size * 0.26f, cx + size * 0.10f};
        for (int i = 0; i < 3; i++) {
            UiKit.line(c, cx - size * 0.62f, ys[i], cx + size * 0.62f, ys[i], 1.3f, UiKit.CREAM);
            textPaint.reset();
            textPaint.setAntiAlias(true);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(UiKit.GOLD);
            c.drawCircle(knobs[i], ys[i], 2.3f, textPaint);
        }
    }

    // ---------------------------------------------------------------- effect setup

    private float effectRowsTop() { return landscape ? 104f : 130f; }

    /** Landscape rows shrink on short screens so the hint and start button never collide. */
    private float effectRowHeight() {
        if (!landscape) return 56f;
        float rowsBottom = effectStartRect().top - 26f;
        float available = Math.max(170f, rowsBottom - effectRowsTop());
        return Math.max(34f, Math.min(62f, (available - 4f * 8f) / 5f));
    }

    private RectF effectRowRect(int i) {
        float rowH = effectRowHeight();
        if (landscape) {
            float step = rowH + 8f;
            return new RectF(40f, effectRowsTop() + i * step, designW * 0.55f,
                    effectRowsTop() + i * step + rowH);
        }
        return new RectF(24f, effectRowsTop() + i * 66f, designW - 24f,
                effectRowsTop() + i * 66f + rowH);
    }

    private RectF effectOneHeartRect() {
        if (landscape) {
            return new RectF(designW * 0.58f, 256f, designW - 50f, 308f);
        }
        return new RectF(24f, 464f, designW - 24f, 516f);
    }

    private RectF effectStartRect() {
        return primaryBottomRect();
    }

    private void drawEffectSetup(Canvas c) {
        UiKit.background(c, designW, designH);
        drawTopBar(c, "效果模式", "EFFECT MODE");
        EffectMode[] modes = EffectMode.values();
        for (int i = 0; i < modes.length; i++) {
            drawEffectRow(c, modes[i], effectRowRect(i));
        }

        if (landscape) {
            float left = designW * 0.58f;
            float right = designW - 50f;
            UiKit.text(c, "规 则 说 明", left, 128f, 9f, UiKit.GOLD,
                    Paint.Align.LEFT, UiKit.BODY_BOLD);
            UiKit.line(c, left, 136f, right, 136f, 0.8f, 0x33FFD24A);
            wrap(c, effectMode.description, left, 156f, 11f, UiKit.CREAM, right - left, 18f);
            drawOneHeartRow(c, effectOneHeartRect());
        } else {
            drawOneHeartRow(c, effectOneHeartRect());
            UiKit.text(c, "规 则 说 明", 24f, 542f, 9f, UiKit.GOLD, Paint.Align.LEFT, UiKit.BODY_BOLD);
            wrap(c, effectMode.description, 24f, 556f, 10f, UiKit.MUTED, designW - 48f, 15f);
        }

        float startTop = effectStartRect().top;
        if (startTop - effectRowRect(EffectMode.values().length - 1).bottom >= 30f) {
            UiKit.text(c, "开始后进入 3 秒倒计时", designW * 0.5f, startTop - 14f,
                    9f, UiKit.DIM, Paint.Align.CENTER, UiKit.BODY);
        }
        drawButton(c, effectStartRect(), "开 始 演 练", true);
    }

    private void drawEffectRow(Canvas c, EffectMode m, RectF r) {
        boolean active = m == effectMode;
        UiKit.panel(c, r.left, r.top, r.right, r.bottom, 7f,
                active ? 0xE6FFD24A : 0xCC10191B,
                active ? UiKit.GOLD_SOFT : 0x33F1EEDC);
        if (r.height() < 48f) {
            UiKit.text(c, m.zh, r.left + 18f, r.centerY() + 5f, 15f,
                    active ? 0xFF101313 : UiKit.CREAM, Paint.Align.LEFT, UiKit.BODY_BOLD);
        } else {
            UiKit.text(c, m.zh, r.left + 18f, r.top + 24f, 15f,
                    active ? 0xFF101313 : UiKit.CREAM, Paint.Align.LEFT, UiKit.BODY_BOLD);
            UiKit.text(c, m.en, r.left + 18f, r.top + 42f, 8f,
                    active ? 0x99101313 : UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
        }
        if (active) {
            UiKit.text(c, "●", r.right - 24f, r.top + 31f, 15f,
                    0xFF101313, Paint.Align.CENTER, UiKit.BODY_BOLD);
        }
    }

    private void drawOneHeartRow(Canvas c, RectF r) {
        UiKit.panel(c, r.left, r.top, r.right, r.bottom, 7f, 0xCC10191B,
                effectOneHeart ? UiKit.withAlpha(UiKit.CORAL, 110) : 0x33F1EEDC);
        UiKit.text(c, "一 错 即 死", r.left + 18f, r.top + 23f, 14f,
                effectOneHeart ? UiKit.CORAL : UiKit.CREAM, Paint.Align.LEFT, UiKit.BODY_BOLD);
        UiKit.text(c, "效果模式中按错一次立即结束", r.left + 18f, r.top + 41f, 8f,
                UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
        UiKit.text(c, effectOneHeart ? "开" : "关", r.right - 76f, r.top + 31f, 11f,
                effectOneHeart ? UiKit.CORAL : UiKit.DIM, Paint.Align.RIGHT, UiKit.BODY_BOLD);
        UiKit.toggle(c, r.right - 34f, r.top + 26f, effectOneHeart, UiKit.CORAL);
    }

    // ---------------------------------------------------------------- game scene

    private float padR() {
        return landscape ? 120f : designH < 800f ? 156f : 170f;
    }

    private float padCy() {
        return landscape ? designH - 170f : designH - padR() - 30f;
    }

    private float padCx() {
        return landscape ? designW - 190f : designW * 0.5f;
    }

    private float cardBottom() {
        if (!prefs.pad()) return Math.min(560f, designH - 130f);
        return Math.min(500f, padCy() - padR() - 26f);
    }

    private float stageLeft() {
        return landscape ? 40f : 22f;
    }

    private float stageRight() {
        return landscape ? designW - 370f : designW - 22f;
    }

    private float stageTop() {
        return landscape ? 116f : 126f;
    }

    private float stageBottom() {
        return landscape ? designH - 40f : cardBottom();
    }

    private RectF pauseRect() {
        float cx = designW * 0.5f;
        return new RectF(cx - 26f, 26f, cx + 26f, 78f);
    }

    private RectF catalogBtnRect() {
        if (landscape) return new RectF(designW - 170f, 20f, designW - 60f, 68f);
        return new RectF(designW - 124f, 20f, designW - 24f, 68f);
    }

    private int accentForMode() {
        if (mode == M_CLASSIC) return UiKit.GOLD;
        if (mode == M_EFFECT) return UiKit.CORAL;
        return UiKit.BLUE;
    }

    private void drawGameScene(Canvas c) {
        UiKit.background(c, designW, designH);
        drawHeader(c);
        if (mode == M_CLASSIC) {
            drawClassicStage(c);
        } else if (mode == M_EFFECT) {
            drawEffectStage(c);
        } else {
            drawFreeStage(c);
        }
        if (prefs.pad()) drawPad(c);
        else drawSwipeHint(c);
        drawGhosts(c);
        drawWrongFlash(c);
        if (mode == M_CLASSIC && classic.getState() != ClassicSession.PLAYING) {
            drawClassicLayer(c);
        } else if (mode == M_EFFECT && effect.getState() == EffectSession.INTERMISSION) {
            drawEffectLayer(c);
        }
    }

    private void drawHeader(Canvas c) {
        int accent = accentForMode();
        UiKit.text(c, modeZh(mode), 24f, 24f, 10f, UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
        drawPauseButton(c);

        if (mode == M_FREE) {
            UiKit.panel(c, catalogBtnRect().left, catalogBtnRect().top, catalogBtnRect().right,
                    catalogBtnRect().bottom, 6f, 0xCC10191B, 0x66F1EEDC);
            UiKit.text(c, "图 鉴", catalogBtnRect().centerX(), catalogBtnRect().top + 32f,
                    13f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY_BOLD);
            UiKit.text(c, "FREE CALL", designW * 0.5f, 24f, 11f, accent,
                    Paint.Align.CENTER, UiKit.BODY_BOLD);
            UiKit.text(c, "FREE", 24f, 86f, 38f, UiKit.BLUE, Paint.Align.LEFT, UiKit.DISPLAY);
            UiKit.text(c, "无计时 · 无分数", 24f, 104f, 9f, UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
            UiKit.text(c, "CALL-IN SANDBOX", designW - 24f, 104f, 9f, UiKit.DIM,
                    Paint.Align.RIGHT, UiKit.BODY);
            return;
        }

        String status;
        String count;
        int score = mode == M_CLASSIC ? classic.getScore() : effect.getScore();
        if (mode == M_CLASSIC) {
            status = "ROUND " + two(classic.getRound());
            count = classic.getCompletedThisRound() + " / " + classic.getRoundTargets();
        } else if (effect.getMode() == EffectMode.GREATWALL) {
            status = "GREAT WALL";
            count = effect.getArrowCompleted() + " ARROWS";
        } else if (effect.getMode() == EffectMode.TERMINAL) {
            status = "LINE " + two(effect.getTerminalLineNumber());
            count = effect.getTerminalEntered() + " / " + effect.getTerminalLine().size();
        } else {
            status = "ROUND " + two(effect.getRound());
            count = effect.getCompletedThisRound() + " / " + effect.getRoundTargets();
        }
        UiKit.text(c, status, designW * 0.5f, 24f, 11f, accent, Paint.Align.CENTER, UiKit.BODY_BOLD);
        UiKit.text(c, count, designW - 24f, 24f, 10f, UiKit.MUTED, Paint.Align.RIGHT, UiKit.BODY);

        float time = mode == M_CLASSIC ? classic.getTimeLeft() : effect.getTimeLeft();
        float fraction = mode == M_CLASSIC ? classic.getTimeFraction() : effect.getTimeFraction();
        int timeColor = fraction < 0.25f ? UiKit.RED : fraction < 0.5f ? UiKit.GOLD_SOFT : UiKit.CREAM;
        UiKit.text(c, five(score), 24f, 86f, 40f, UiKit.CREAM, Paint.Align.LEFT, UiKit.DISPLAY);
        UiKit.text(c, effect.getMode() == EffectMode.GREATWALL && mode == M_EFFECT ? "ARROWS" : "SCORE",
                24f, 104f, 9f, UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
        String t = String.format(Locale.US, "%.1fs", Math.max(0f, time));
        UiKit.text(c, t, designW - 24f, 86f, 36f, timeColor, Paint.Align.RIGHT, UiKit.DISPLAY);
        UiKit.text(c, "TIME", designW - 24f, 104f, 9f, UiKit.MUTED, Paint.Align.RIGHT, UiKit.BODY);
        UiKit.progressBar(c, 24f, 112f, designW - 24f, 3f, fraction,
                fraction < 0.25f ? UiKit.RED : UiKit.GOLD);
    }

    private void drawPauseButton(Canvas c) {
        float cx = designW * 0.5f;
        UiKit.ring(c, cx, 52f, 18f, 0x44F1EEDC, 1f);
        textPaint.reset();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(UiKit.CREAM);
        c.drawRect(cx - 5f, 45f, cx - 2f, 59f, textPaint);
        c.drawRect(cx + 2f, 45f, cx + 5f, 59f, textPaint);
    }

    private void drawClassicStage(Canvas c) {
        Stratagem target = classic.getCurrent();
        int entered = classic.getState() == ClassicSession.PLAYING ? classic.getEnteredCount()
                : target == null ? 0 : target.length();
        String status = classic.getState() == ClassicSession.PLAYING ? "STANDBY" : "ROUND CLEAR";
        drawSingleCard(c, target, entered, false,
                classic.getState() != ClassicSession.PLAYING, status);
    }

    private void drawEffectStage(Canvas c) {
        if (effect.getMode() == EffectMode.MULTILINES) {
            drawMultiLanes(c);
            return;
        }
        if (effect.getMode() == EffectMode.GREATWALL) {
            drawWall(c);
            return;
        }
        if (effect.getMode() == EffectMode.TERMINAL) {
            drawTerminal(c);
            return;
        }
        Stratagem target = effect.getCurrent();
        int entered = effect.isIntermission() && target != null ? target.length() : effect.getEnteredCount();
        drawSingleCard(c, target, entered, effect.getMode() == EffectMode.DICTATION,
                effect.isIntermission(), effect.isIntermission() ? "ROUND CLEAR" : "STANDBY");
    }

    private void drawFreeStage(Canvas c) {
        Stratagem last = free.getLastCompleted();
        boolean reveal = last != null && now < revealUntil;
        if (reveal) {
            drawSingleCard(c, last, last.length(), false, true, "CALLED IN");
        } else {
            drawFreeCallCard(c);
        }
    }

    private void drawSingleCard(Canvas c, Stratagem s, int entered, boolean hideFuture,
                                boolean complete, String status) {
        float l = stageLeft();
        float r = stageRight();
        float t = stageTop();
        float b = stageBottom();
        if (b <= t + 70f) return;
        int accent = s == null ? accentForMode() : UiKit.categoryColor(s.category);
        float h = b - t;
        float w = r - l;
        c.save();
        if (now < shakeUntil) c.translate((float) Math.sin(now * 90f) * 7f, 0f);
        UiKit.panel(c, l, t, r, b, 10f, 0xF20A1012, UiKit.withAlpha(accent, 110));
        UiKit.line(c, l + 10f, t + 1.5f, r - 10f, t + 1.5f, 2.4f, accent);
        UiKit.bracket(c, l + 12f, t + 12f, r - 12f, b - 12f, 14f,
                UiKit.withAlpha(accent, 130), 1.2f);
        UiKit.text(c, s == null ? "STANDBY" : UiKit.categoryZh(s.category),
                l + 18f, t + 28f, 10f, accent, Paint.Align.LEFT, UiKit.BODY_BOLD);
        UiKit.text(c, status, r - 18f, t + 28f, 9f, complete ? UiKit.GOLD : UiKit.DIM,
                Paint.Align.RIGHT, UiKit.BODY);

        if (s == null) {
            UiKit.text(c, "STANDBY", (l + r) * 0.5f, t + h * 0.55f, 24f, UiKit.MUTED,
                    Paint.Align.CENTER, UiKit.DISPLAY);
            c.restore();
            return;
        }

        float iconSize = Math.min(landscape ? 180f : 112f, Math.min(h * 0.42f, w * 0.34f));
        float iconCy = t + h * 0.32f;
        UiKit.ring(c, (l + r) * 0.5f, iconCy, iconSize * 0.62f, 0x22F1EEDC, 1f);
        UiKit.ring(c, (l + r) * 0.5f, iconCy, iconSize * 0.56f, UiKit.withAlpha(accent, 40), 0.8f);
        icons.draw(c, s.assetName, (l + r) * 0.5f - iconSize * 0.5f, iconCy - iconSize * 0.5f,
                (l + r) * 0.5f + iconSize * 0.5f, iconCy + iconSize * 0.5f);
        float nameY = t + h * 0.66f;
        UiKit.textFit(c, s.name, (l + r) * 0.5f, nameY, landscape ? 30f : 23f,
                landscape ? 16f : 13f, w - 48f, UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.textFit(c, s.nameZh, (l + r) * 0.5f, nameY + (landscape ? 26f : 23f),
                landscape ? 17f : 15f, 10f, w - 48f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
        drawSlots(c, s, entered, complete, (l + r) * 0.5f, b - (landscape ? 32f : 26f),
                w - 40f, hideFuture);
        c.restore();
    }

    private void drawFreeCallCard(Canvas c) {
        float l = stageLeft();
        float r = stageRight();
        float t = stageTop();
        float b = stageBottom();
        if (b <= t + 70f) return;
        float w = r - l;
        float h = b - t;
        UiKit.panel(c, l, t, r, b, 10f, 0xF20A1012, UiKit.withAlpha(UiKit.BLUE, 100));
        UiKit.line(c, l + 10f, t + 1.5f, r - 10f, t + 1.5f, 2.4f, UiKit.BLUE);
        UiKit.text(c, "自由呼叫", l + 18f, t + 28f, 10f, UiKit.BLUE,
                Paint.Align.LEFT, UiKit.BODY_BOLD);
        UiKit.text(c, "LIVE INPUT", r - 18f, t + 28f, 9f, UiKit.DIM,
                Paint.Align.RIGHT, UiKit.BODY);
        UiKit.text(c, landscape ? "FREE CALL" : "自由呼叫", (l + r) * 0.5f, t + h * 0.40f,
                landscape ? 42f : 32f, UiKit.BLUE, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, "输入任意战备序列", (l + r) * 0.5f, t + h * 0.40f + 28f, 13f,
                UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
        UiKit.text(c, "无计时、无分数 · 完成即呼叫", (l + r) * 0.5f, t + h * 0.40f + 50f, 10f,
                UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
        drawInputSlots(c, free.getInputCode(), (l + r) * 0.5f, b - 26f, w - 40f);
    }

    private void drawMultiLanes(Canvas c) {
        for (int i = 0; i < EffectSession.MULTI_LANES; i++) {
            drawLane(c, i, effect.getLanes()[i], multiLaneRect(i));
        }
        UiKit.text(c, "牌堆剩余 " + effect.getPoolRemaining(), stageRight() - 16f,
                stageTop() - 6f, 8f, UiKit.DIM, Paint.Align.RIGHT, UiKit.BODY);
    }

    private RectF multiLaneRect(int i) {
        float l = stageLeft();
        float r = stageRight();
        float t = stageTop();
        float b = stageBottom();
        if (landscape) {
            float gap = 6f;
            float h = (b - t - gap * 3f) / 4f;
            return new RectF(l, t + i * (h + gap), r, t + i * (h + gap) + h);
        }
        float gap = 8f;
        float w = (r - l - gap) * 0.5f;
        float h = (b - t - gap) * 0.5f;
        float x = l + (i % 2) * (w + gap);
        float y = t + (i / 2) * (h + gap);
        return new RectF(x, y, x + w, y + h);
    }

    private void drawLane(Canvas c, int index, EffectSession.Lane lane, RectF box) {
        Stratagem s = lane.getTarget();
        boolean done = lane.isDone();
        int accent = s == null ? UiKit.CORAL : UiKit.categoryColor(s.category);
        if (lane.getWrongFlash() > 0f) accent = UiKit.RED;
        UiKit.panel(c, box.left, box.top, box.right, box.bottom, 7f,
                done ? 0xAA0A1012 : 0xE60A1012,
                done ? 0x33F1EEDC : UiKit.withAlpha(accent, 120));
        UiKit.line(c, box.left + 5f, box.top + 5f, box.left + 5f, box.bottom - 5f, 3f, accent);
        float icon = Math.min(landscape ? 54f : 42f, box.height() * 0.40f);
        float iconCy = box.top + icon * 0.65f + 4f;
        if (s != null) {
            icons.draw(c, s.assetName, box.left + 10f, iconCy - icon * 0.5f,
                    box.left + 10f + icon, iconCy + icon * 0.5f);
        }
        float nameLeft = box.left + 16f + icon;
        float nameRight = landscape ? box.left + box.width() * 0.46f : box.right - 10f;
        float nameMax = Math.max(40f, nameRight - nameLeft);
        if (s != null) {
            UiKit.textFit(c, s.name, nameLeft, box.top + 22f, landscape ? 15f : 13f, 9f,
                    nameMax, done ? UiKit.DIM : UiKit.CREAM, Paint.Align.LEFT, UiKit.BODY_BOLD);
            UiKit.textFit(c, s.nameZh, nameLeft, box.top + 38f, landscape ? 11f : 10f, 8f,
                    nameMax, UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
        }
        if (done) {
            UiKit.text(c, "DONE", box.right - 10f, box.top + 22f, 9f, UiKit.GOLD,
                    Paint.Align.RIGHT, UiKit.BODY_BOLD);
        }
        if (s != null) {
            float slotsCx = landscape ? box.left + box.width() * 0.75f : box.centerX();
            float slotsWidth = landscape ? box.width() * 0.46f : box.width() - 16f;
            drawSlots(c, s, lane.getEnteredCount(), false, slotsCx,
                    box.bottom - (landscape ? 27f : 18f), slotsWidth, false);
        }
    }

    private void drawWall(Canvas c) {
        float l = stageLeft();
        float r = stageRight();
        float t = stageTop();
        float b = stageBottom();
        UiKit.panel(c, l, t, r, b, 10f, 0xF20A1012, UiKit.withAlpha(UiKit.CORAL, 100));
        UiKit.text(c, "GREAT WALL", (l + r) * 0.5f, t + 34f, landscape ? 28f : 20f,
                UiKit.CORAL, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, "连续箭头 · 从最左侧开始，按错不清进度", (l + r) * 0.5f, t + 56f,
                9f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
        List<Direction> wall = effect.getWall();
        for (int i = 0; i < wall.size(); i++) {
            RectF cell = wallCellRect(i, l, r, t, b);
            boolean first = i == 0;
            UiKit.panel(c, cell.left, cell.top, cell.right, cell.bottom, 5f,
                    first ? 0xE6FFD24A : 0xCC10191B,
                    first ? UiKit.GOLD_SOFT : 0x44F1EEDC);
            float size = Math.min(cell.width() * 0.30f, cell.height() * 0.34f);
            UiKit.arrow(c, cell.centerX(), cell.centerY(), size, wall.get(i),
                    first ? 0xFF101313 : UiKit.CREAM);
        }
        UiKit.text(c, "已输入 " + effect.getArrowCompleted() + " 个 · 保持连击", (l + r) * 0.5f,
                b - 18f, 10f, UiKit.GOLD, Paint.Align.CENTER, UiKit.BODY_BOLD);
    }

    private RectF wallCellRect(int i, float l, float r, float t, float b) {
        float gap = 8f;
        int columns = 4;
        int rows = 2;
        float cellW = (r - l - gap * (columns - 1)) / columns;
        float gridTop = t + 78f;
        float gridBottom = b - 34f;
        float available = Math.max(2f * 40f + gap, gridBottom - gridTop);
        float cellH = Math.min(landscape ? 180f : 100f, (available - gap) / rows);
        float gridHeight = cellH * rows + gap;
        float top = gridTop + Math.max(0f, (available - gridHeight) * 0.5f);
        float x = l + (i % columns) * (cellW + gap);
        float y = top + (i / columns) * (cellH + gap);
        return new RectF(x, y, x + cellW, y + cellH);
    }

    private void drawTerminal(Canvas c) {
        float l = stageLeft();
        float r = stageRight();
        float t = stageTop();
        float b = stageBottom();
        UiKit.panel(c, l, t, r, b, 10f, 0xF20A1012, UiKit.withAlpha(UiKit.GREEN, 90));
        UiKit.text(c, "STRATAGEM TERMINAL", l + 18f, t + 30f, 11f, UiKit.GREEN,
                Paint.Align.LEFT, UiKit.MONO);
        UiKit.text(c, "LINE " + two(effect.getTerminalLineNumber()), r - 18f, t + 30f,
                11f, UiKit.GREEN, Paint.Align.RIGHT, UiKit.MONO);
        UiKit.line(c, l + 18f, t + 40f, r - 18f, t + 40f, 0.8f, 0x559BE58B);
        UiKit.text(c, "> 输入这一行", l + 18f, t + 62f, 10f, UiKit.MUTED,
                Paint.Align.LEFT, UiKit.MONO);
        drawDirectionSlots(c, effect.getTerminalLine(), effect.getTerminalEntered(),
                (l + r) * 0.5f, t + (b - t) * 0.48f, r - l - 60f);
        UiKit.text(c, "已完成 " + effect.getLinesCompleted() + " 行 · 行长度会逐渐增加",
                (l + r) * 0.5f, b - 34f, 10f, UiKit.GREEN, Paint.Align.CENTER, UiKit.BODY);
        UiKit.text(c, "按错重置当前行并扣 0.75 秒", (l + r) * 0.5f, b - 16f, 9f,
                UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
    }

    private void drawClassicLayer(Canvas c) {
        float l = stageLeft();
        float r = stageRight();
        float t = stageTop();
        float b = stageBottom();
        UiKit.panel(c, l, t, r, b, 10f, 0xF70A1012, 0x66FFD24A);
        if (classic.getState() == ClassicSession.ROUND_SCORE) {
            UiKit.text(c, "ROUND CLEAR", (l + r) * 0.5f, t + 38f, 11f,
                    UiKit.GOLD, Paint.Align.CENTER, UiKit.BODY_BOLD);
            UiKit.text(c, "第 " + two(classic.getRound()) + " 回合完成", (l + r) * 0.5f, t + 76f,
                    22f, UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
            UiKit.text(c, "+" + classic.getLastRoundScore(), (l + r) * 0.5f, t + 128f,
                    38f, UiKit.GOLD, Paint.Align.CENTER, UiKit.DISPLAY);
            float row = t + 154f;
            drawOverlayRow(c, l, r, row, "回合奖励", "+" + classic.getRoundBonus());
            drawOverlayRow(c, l, r, row + 22f, "剩余时间", "+" + classic.getTimeBonus());
            drawOverlayRow(c, l, r, row + 44f, "完美回合", classic.isPerfectRound()
                    ? "+" + classic.getPerfectBonus() : "—");
            UiKit.text(c, "总分 " + five(classic.getScore()), (l + r) * 0.5f, row + 78f,
                    12f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
            drawOverlayBar(c, l, r, b, classic.getStateTimer() / ClassicSession.ROUND_SCORE_SECONDS);
        } else {
            UiKit.text(c, "ROUND READY", (l + r) * 0.5f, t + 48f, 11f,
                    UiKit.GOLD, Paint.Align.CENTER, UiKit.BODY_BOLD);
            UiKit.text(c, "下一回合", (l + r) * 0.5f, t + 100f, 28f,
                    UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
            UiKit.text(c, "第 " + two(classic.getRound() + 1) + " 回合 · "
                            + (int) Math.max(3, Math.min(30, (classic.getRound() + 5) / 1.3f)) + " 个目标",
                    (l + r) * 0.5f, t + 132f, 11f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
            UiKit.text(c, "时间上限更短 · 目标更多", (l + r) * 0.5f, t + 166f,
                    10f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
            drawOverlayBar(c, l, r, b, classic.getStateTimer() / ClassicSession.ROUND_READY_SECONDS);
        }
    }

    private void drawEffectLayer(Canvas c) {
        float l = stageLeft();
        float r = stageRight();
        float t = stageTop();
        float b = stageBottom();
        UiKit.panel(c, l, t, r, b, 10f, 0xF70A1012, 0x66FF8E7A);
        UiKit.text(c, "ROUND CLEAR", (l + r) * 0.5f, t + 38f, 11f,
                UiKit.CORAL, Paint.Align.CENTER, UiKit.BODY_BOLD);
        UiKit.text(c, "第 " + two(effect.getRound()) + " 回合完成", (l + r) * 0.5f, t + 76f,
                22f, UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, "+" + effect.getLastRoundScore(), (l + r) * 0.5f, t + 128f,
                38f, UiKit.GOLD, Paint.Align.CENTER, UiKit.DISPLAY);
        float row = t + 154f;
        drawOverlayRow(c, l, r, row, "剩余时间", "+" + effect.getLastTimeBonus());
        drawOverlayRow(c, l, r, row + 22f, "完美回合", effect.getLastPerfectBonus() > 0
                ? "+" + effect.getLastPerfectBonus() : "—");
        UiKit.text(c, "总分 " + five(effect.getScore()), (l + r) * 0.5f, row + 58f,
                12f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
        drawOverlayBar(c, l, r, b, effect.getIntermissionLeft() / EffectSession.INTERMISSION_SECONDS);
    }

    private void drawOverlayRow(Canvas c, float l, float r, float y, String label, String value) {
        UiKit.text(c, label, (l + r) * 0.5f - 120f, y, 10f, UiKit.MUTED,
                Paint.Align.LEFT, UiKit.BODY);
        UiKit.text(c, value, (l + r) * 0.5f + 120f, y, 10f, UiKit.CREAM,
                Paint.Align.RIGHT, UiKit.BODY_BOLD);
        UiKit.line(c, (l + r) * 0.5f - 120f, y + 7f, (l + r) * 0.5f + 120f, y + 7f,
                0.6f, 0x22F1EEDC);
    }

    private void drawOverlayBar(Canvas c, float l, float r, float b, float fraction) {
        UiKit.progressBar(c, l + 30f, b - 26f, r - 30f, 3f, Math.max(0f, Math.min(1f, fraction)),
                UiKit.GOLD);
    }

    private void drawSlots(Canvas c, Stratagem s, int entered, boolean complete, float cx,
                           float cy, float maxWidth, boolean hideFuture) {
        int n = s.sequence.length;
        float gap = landscape ? 6f : 4f;
        float slot = Math.min(landscape ? 42f : 30f, (maxWidth - (n - 1) * gap) / n);
        slot = Math.max(13f, slot);
        float total = n * slot + (n - 1) * gap;
        float x = cx - total * 0.5f;
        for (int i = 0; i < n; i++) {
            boolean done = complete || i < entered;
            boolean next = !complete && i == entered;
            boolean visible = done || next || !hideFuture;
            int edge = done ? UiKit.GOLD : next ? UiKit.GOLD_SOFT : 0x44F1EEDC;
            int fill = done ? 0x33FFD24A : 0xCC0B1416;
            UiKit.panel(c, x, cy - slot * 0.5f, x + slot, cy + slot * 0.5f, 3f, fill, edge);
            if (visible) {
                UiKit.arrow(c, x + slot * 0.5f, cy, slot * 0.56f, s.sequence[i],
                        done ? UiKit.GOLD : next ? UiKit.CREAM : UiKit.DIM);
            } else {
                UiKit.text(c, "·", x + slot * 0.5f, cy + 3f, slot * 0.5f,
                        UiKit.DIM, Paint.Align.CENTER, UiKit.DISPLAY);
            }
            x += slot + gap;
        }
    }

    private void drawInputSlots(Canvas c, String code, float cx, float cy, float maxWidth) {
        int n = Math.max(4, code.length());
        float gap = landscape ? 6f : 4f;
        float slot = Math.min(landscape ? 42f : 30f, (maxWidth - (n - 1) * gap) / n);
        slot = Math.max(13f, slot);
        float total = n * slot + (n - 1) * gap;
        float x = cx - total * 0.5f;
        for (int i = 0; i < n; i++) {
            boolean filled = i < code.length();
            UiKit.panel(c, x, cy - slot * 0.5f, x + slot, cy + slot * 0.5f, 3f,
                    filled ? 0x33FFD24A : 0x00000000, filled ? UiKit.GOLD : 0x33F1EEDC);
            if (filled) {
                UiKit.arrow(c, x + slot * 0.5f, cy, slot * 0.56f,
                        Direction.fromCode(code.charAt(i)), UiKit.GOLD);
            } else {
                UiKit.text(c, "·", x + slot * 0.5f, cy + 3f, slot * 0.5f,
                        UiKit.DIM, Paint.Align.CENTER, UiKit.DISPLAY);
            }
            x += slot + gap;
        }
    }

    private void drawDirectionSlots(Canvas c, List<Direction> line, int entered, float cx,
                                    float cy, float maxWidth) {
        int n = line.size();
        if (n == 0) return;
        float gap = landscape ? 6f : 5f;
        float slot = Math.min(landscape ? 50f : 38f, (maxWidth - (n - 1) * gap) / n);
        slot = Math.max(14f, slot);
        float total = n * slot + (n - 1) * gap;
        float x = cx - total * 0.5f;
        for (int i = 0; i < n; i++) {
            boolean done = i < entered;
            boolean next = i == entered;
            UiKit.panel(c, x, cy - slot * 0.5f, x + slot, cy + slot * 0.5f, 3f,
                    done ? 0x339BE58B : 0xCC0B1416,
                    done ? UiKit.GREEN : next ? UiKit.GOLD_SOFT : 0x44F1EEDC);
            UiKit.arrow(c, x + slot * 0.5f, cy, slot * 0.56f, line.get(i),
                    done ? UiKit.GREEN : next ? UiKit.CREAM : UiKit.DIM);
            x += slot + gap;
        }
    }

    private void drawPad(Canvas c) {
        float cx = padCx();
        float cy = padCy();
        float r = padR();
        float k = r / 170f;
        int accent = accentForMode();
        UiKit.ring(c, cx, cy, r + 20f, 0x0DFFD24A, 1f);
        UiKit.ring(c, cx, cy, r + 13f, 0x22F1EEDC, 0.8f);
        UiKit.ring(c, cx, cy, r, 0x33F1EEDC, 1f);
        UiKit.ring(c, cx, cy, 62f * k, 0x33F1EEDC, 1f);

        RectF arc = new RectF(cx - r, cy - r, cx + r, cy + r);
        Direction[] dirs = {Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT};
        float[] angles = {-90f, 0f, 90f, 180f};
        for (int i = 0; i < 4; i++) {
            boolean pressed = pressDir == dirs[i] && now < pressUntil;
            boolean flash = lastInputDir == dirs[i] && now - lastInputAt < FLASH_HOLD;
            textPaint.reset();
            textPaint.setStyle(Paint.Style.STROKE);
            textPaint.setStrokeWidth(54f * k);
            textPaint.setStrokeCap(Paint.Cap.BUTT);
            textPaint.setColor(pressed ? UiKit.GOLD
                    : flash ? UiKit.lerpColor(accent, 0xFF131E21, 0.45f)
                    : (now < wrongUntil ? 0x552B1717 : 0xFF131E21));
            c.drawArc(arc, angles[i] - 37f, 74f, false, textPaint);
            float ar = r - 27f * k;
            double rad = Math.toRadians(angles[i]);
            float ax = cx + (float) Math.cos(rad) * ar;
            float ay = cy + (float) Math.sin(rad) * ar;
            UiKit.arrow(c, ax, ay, 34f * k, dirs[i], pressed ? 0xFF101313 : UiKit.CREAM);
        }
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(i * 15);
            float r1 = r + 17f * k;
            float r2 = (i % 6 == 0) ? r + 26f * k : r + 21f * k;
            UiKit.line(c, cx + (float) Math.cos(a) * r1, cy + (float) Math.sin(a) * r1,
                    cx + (float) Math.cos(a) * r2, cy + (float) Math.sin(a) * r2,
                    i % 6 == 0 ? 1.4f : 0.8f, i % 6 == 0 ? 0x66FFD24A : 0x33F1EEDC);
        }

        float panel = 110f * k;
        UiKit.panel(c, cx - panel * 0.5f, cy - panel * 0.5f, cx + panel * 0.5f,
                cy + panel * 0.5f, panel * 0.5f, 0xFF081012, 0x55F1EEDC);
        String main = padMain();
        String sub = padSub();
        float prog = padProgress();
        if (prog > 0f) {
            textPaint.reset();
            textPaint.setStyle(Paint.Style.STROKE);
            textPaint.setStrokeWidth(2.2f);
            textPaint.setColor(accent);
            c.drawArc(new RectF(cx - panel * 0.42f, cy - panel * 0.42f, cx + panel * 0.42f,
                    cy + panel * 0.42f), -90f, 360f * prog, false, textPaint);
        }
        UiKit.text(c, main, cx, cy + 7f * k + 2f, 24f * k + 2f, accent,
                Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, sub, cx, cy + 27f * k, 8f * k + 1f, UiKit.DIM,
                Paint.Align.CENTER, UiKit.BODY);
    }

    private String padMain() {
        if (mode == M_FREE) return String.valueOf(free.getInputCode().length());
        if (mode == M_CLASSIC) {
            if (classic.getState() != ClassicSession.PLAYING) return "OK";
            Stratagem s = classic.getCurrent();
            return s == null ? "OK" : classic.getEnteredCount() + "/" + s.length();
        }
        if (effect.getMode() == EffectMode.GREATWALL) return String.valueOf(effect.getArrowCompleted());
        if (effect.getMode() == EffectMode.TERMINAL) {
            return effect.getTerminalEntered() + "/" + effect.getTerminalLine().size();
        }
        if (effect.getMode() == EffectMode.MULTILINES) {
            EffectSession.Lane lane = activeLane();
            if (lane == null || lane.getTarget() == null) return "OK";
            return lane.getEnteredCount() + "/" + lane.getTarget().length();
        }
        Stratagem s = effect.getCurrent();
        return s == null ? "OK" : effect.getEnteredCount() + "/" + s.length();
    }

    private String padSub() {
        if (mode == M_FREE) return "INPUT";
        if (mode == M_EFFECT && effect.getMode() == EffectMode.GREATWALL) return "ARROWS";
        if (mode == M_EFFECT && effect.getMode() == EffectMode.TERMINAL) return "LINE";
        if (mode == M_EFFECT && effect.getMode() == EffectMode.MULTILINES) return "LANE";
        return "INPUT";
    }

    private float padProgress() {
        if (mode == M_CLASSIC) return classic.getProgress();
        if (mode == M_FREE) return free.getProgress();
        if (effect.getMode() == EffectMode.MULTILINES) {
            EffectSession.Lane lane = activeLane();
            return lane == null ? 0f : lane.getProgress();
        }
        if (effect.getMode() == EffectMode.TERMINAL) {
            int n = effect.getTerminalLine().size();
            return n == 0 ? 0f : effect.getTerminalEntered() / (float) n;
        }
        if (effect.getMode() == EffectMode.GREATWALL) return 0f;
        return effect.getProgress();
    }

    private EffectSession.Lane activeLane() {
        EffectSession.Lane first = null;
        for (EffectSession.Lane lane : effect.getLanes()) {
            if (lane.isDone() || lane.getTarget() == null) continue;
            if (first == null) first = lane;
            if (lane.getEnteredCount() > 0) return lane;
        }
        return first;
    }

    private void drawSwipeHint(Canvas c) {
        float cx = landscape ? designW - 190f : designW * 0.5f;
        float y = landscape ? designH - 90f : cardBottom() + 34f;
        UiKit.text(c, "全屏滑动输入", cx, y, 14f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY_BOLD);
        UiKit.text(c, "一笔一个方向 · 可在设置中重新开启圆盘", cx, y + 18f, 9f,
                UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
    }

    private void drawGhosts(Canvas c) {
        for (Ghost g : ghosts) {
            float age = (now - g.born) / GHOST_LIFE;
            if (age < 0f || age > 1f) continue;
            float k = 1f - age;
            float drift = 14f * (1f - k);
            float dx = 0f;
            float dy = 0f;
            switch (g.dir) {
                case UP: dy = -drift; break;
                case RIGHT: dx = drift; break;
                case DOWN: dy = drift; break;
                default: dx = -drift; break;
            }
            int alpha = (int) (150f * k);
            UiKit.arrow(c, g.x + dx, g.y + dy, 34f, g.dir, Color.argb(alpha, 255, 210, 74));
        }
    }

    private void drawWrongFlash(Canvas c) {
        if (now >= wrongUntil) return;
        int alpha = (int) (40f * (wrongUntil - now) / 0.46f);
        textPaint.reset();
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(7f);
        textPaint.setColor(Color.argb(Math.max(0, alpha), 255, 93, 82));
        c.drawRect(5f, 5f, designW - 5f, designH - 5f, textPaint);
    }

    // ---------------------------------------------------------------- countdown / pause

    private void drawCountdown(Canvas c) {
        UiKit.background(c, designW, designH);
        drawFrameCorners(c);
        int digit = Math.max(1, (int) Math.ceil(countdown));
        float cx = designW * 0.5f;
        UiKit.text(c, modeZh(mode), cx, designH * 0.30f, 18f, UiKit.GOLD,
                Paint.Align.CENTER, UiKit.BODY_BOLD);
        UiKit.text(c, modeEn(mode), cx, designH * 0.30f + 22f, 10f, UiKit.MUTED,
                Paint.Align.CENTER, UiKit.BODY);
        float r = Math.min(104f, designH * 0.18f);
        UiKit.ring(c, cx, designH * 0.50f, r, 0x33FFD24A, 1f);
        UiKit.ring(c, cx, designH * 0.50f, r + 12f, 0x18F1EEDC, 1f);
        UiKit.text(c, String.valueOf(digit), cx, designH * 0.50f + r * 0.40f,
                r * 1.08f, UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
        float frac = 1f - Math.max(0f, Math.min(1f, countdown / 3f));
        textPaint.reset();
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(5f);
        textPaint.setColor(UiKit.GOLD);
        c.drawArc(new RectF(cx - r - 10f, designH * 0.50f - r - 10f,
                cx + r + 10f, designH * 0.50f + r + 10f), -90f, 360f * frac, false, textPaint);
        UiKit.text(c, "准备输入", cx, designH * 0.50f + r + 60f, 13f,
                UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
        String hint = mode == M_CLASSIC ? "完成目标抢回时间 · 按错不扣时"
                : mode == M_EFFECT ? "按效果规则输入 · 留意时间上限"
                : "输入完整序列即呼叫";
        UiKit.text(c, hint, cx, designH * 0.50f + r + 84f, 10f,
                UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
    }

    private void drawPause(Canvas c) {
        textPaint.reset();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xD9030607);
        c.drawRect(0f, 0f, designW, designH, textPaint);
        float cy = designH * 0.50f;
        float top = cy - 175f;
        float panelW = Math.min(344f, designW - 80f);
        float left = (designW - panelW) * 0.5f;
        UiKit.panel(c, left, top, left + panelW, top + 350f, 10f, 0xFA0A1012, 0x66FFD24A);
        UiKit.text(c, "战术暂停", designW * 0.5f, top + 42f, 25f,
                UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, "TACTICAL PAUSE", designW * 0.5f, top + 58f, 9f,
                UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
        String score = mode == M_CLASSIC ? five(classic.getScore())
                : mode == M_EFFECT ? five(effect.getScore()) : "FREE";
        UiKit.text(c, "得分 " + score, designW * 0.5f, top + 88f, 11f,
                UiKit.GOLD, Paint.Align.CENTER, UiKit.BODY);
        drawButton(c, pauseResumeRect(), "继 续 训 练", true);
        drawButton(c, pauseRestartRect(), "重 新 开 始", false);
        drawButton(c, pauseSettingsRect(), "设 置", false);
        drawButton(c, pauseExitRect(), "退 出 任 务", false);
    }

    private void drawButton(Canvas c, RectF r, String label, boolean primary) {
        UiKit.button(c, label, r.left, r.top, r.right, r.bottom, primary, true);
    }

    private float pauseTop() { return designH * 0.5f - 175f; }
    private RectF pauseResumeRect() { return pauseButtonRect(0); }
    private RectF pauseRestartRect() { return pauseButtonRect(1); }
    private RectF pauseSettingsRect() { return pauseButtonRect(2); }
    private RectF pauseExitRect() { return pauseButtonRect(3); }

    private RectF pauseButtonRect(int index) {
        float panelW = Math.min(344f, designW - 80f);
        float left = (designW - panelW) * 0.5f + 30f;
        float top = pauseTop() + 112f + index * 56f;
        return new RectF(left, top, left + panelW - 60f, top + 48f);
    }

    // ---------------------------------------------------------------- results

    private RectF primaryBottomRect() {
        float w = Math.min(300f, designW - 60f);
        return new RectF(designW * 0.5f - w * 0.5f, designH - 154f,
                designW * 0.5f + w * 0.5f, designH - 102f);
    }

    private RectF secondaryBottomRect() {
        float w = Math.min(300f, designW - 60f);
        return new RectF(designW * 0.5f - w * 0.5f, designH - 90f,
                designW * 0.5f + w * 0.5f, designH - 38f);
    }

    private RectF overAgainRect() { return primaryBottomRect(); }
    private RectF overMenuRect() { return secondaryBottomRect(); }

    private void drawOver(Canvas c) {
        UiKit.background(c, designW, designH);
        drawFrameCorners(c);
        float cx = designW * 0.5f;
        float base = landscape ? 32f : 64f;
        UiKit.text(c, "MISSION END", cx, base, 12f, UiKit.CORAL,
                Paint.Align.CENTER, UiKit.BODY_BOLD);
        UiKit.text(c, "训练结束", cx, base + 38f, 30f, UiKit.CREAM,
                Paint.Align.CENTER, UiKit.DISPLAY);
        String modeTitle = finalMode == M_EFFECT ? "效果 · " + finalEffectMode.zh : modeZh(finalMode);
        UiKit.text(c, modeTitle, cx, base + 58f, 10f, UiKit.MUTED,
                Paint.Align.CENTER, UiKit.BODY);
        if (finalEffectMode != null && finalMode == M_EFFECT && effectOneHeart) {
            UiKit.text(c, "一错即死", cx, base + 74f, 9f, UiKit.CORAL,
                    Paint.Align.CENTER, UiKit.BODY_BOLD);
        }
        UiKit.text(c, "最终得分", cx, base + 94f, 12f, UiKit.MUTED,
                Paint.Align.CENTER, UiKit.BODY);
        UiKit.text(c, five(finalScore), cx, base + 144f, 52f, UiKit.CREAM,
                Paint.Align.CENTER, UiKit.DISPLAY);

        float statY = base + 180f;
        if (finalMode == M_CLASSIC) {
            drawStatLine(c, cx, statY, "到达回合", String.valueOf(finalRound));
            drawStatLine(c, cx, statY + 25f, "完成目标", String.valueOf(finalCompleted));
            drawStatLine(c, cx, statY + 50f, "完成箭头", String.valueOf(finalArrows));
        } else if (finalEffectMode == EffectMode.GREATWALL) {
            drawStatLine(c, cx, statY, "完成箭头", String.valueOf(finalArrows));
            drawStatLine(c, cx, statY + 25f, "坚持时间", String.format(Locale.US, "%.1fs", finalDuration));
            drawStatLine(c, cx, statY + 50f, "速度", finalSpeed + " / 分");
        } else if (finalEffectMode == EffectMode.TERMINAL) {
            drawStatLine(c, cx, statY, "通过行数", String.valueOf(finalLines));
            drawStatLine(c, cx, statY + 25f, "完成箭头", String.valueOf(finalArrows));
            drawStatLine(c, cx, statY + 50f, "速度", finalSpeed + " / 分");
        } else {
            drawStatLine(c, cx, statY, "到达回合", String.valueOf(finalRound));
            drawStatLine(c, cx, statY + 25f, "完成箭头", String.valueOf(finalArrows));
            drawStatLine(c, cx, statY + 50f, "速度", finalSpeed + " / 分");
        }
        drawStatLine(c, cx, statY + 75f, "历史最高", finalBest == 0 ? "-----" : five(finalBest));

        if (finalRecord) {
            UiKit.text(c, "★  新 纪 录  ★", cx, statY + 112f, 13f, UiKit.GOLD,
                    Paint.Align.CENTER, UiKit.BODY_BOLD);
        }
        drawButton(c, overAgainRect(), "再 次 部 署", true);
        drawButton(c, overMenuRect(), "返 回 主 菜 单", false);
    }

    private void drawStatLine(Canvas c, float cx, float y, String label, String value) {
        float half = Math.min(150f, designW * 0.5f - 40f);
        UiKit.text(c, label, cx - half, y, 11f, UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
        UiKit.text(c, value, cx + half, y, 11f, UiKit.CREAM, Paint.Align.RIGHT, UiKit.BODY_BOLD);
        UiKit.line(c, cx - half, y + 8f, cx + half, y + 8f, 0.7f, 0x22F1EEDC);
    }

    // ---------------------------------------------------------------- catalog / detail

    private RectF catalogBlindRect() {
        if (landscape) return new RectF(designW - 170f, 28f, designW - 60f, 76f);
        return new RectF(designW - 120f, 28f, designW - 20f, 76f);
    }

    private RectF catalogListRect() {
        return new RectF(0f, LIST_TOP, designW, designH);
    }

    private void drawCatalog(Canvas c) {
        UiKit.background(c, designW, designH);
        drawTopBar(c, "战备图鉴", StratagemCatalog.ALL.size() + " CALL-INS");
        RectF blind = catalogBlindRect();
        UiKit.panel(c, blind.left, blind.top, blind.right, blind.bottom, 6f,
                0xCCFFD24A, UiKit.GOLD_SOFT);
        UiKit.text(c, "盲 呼", blind.centerX(), blind.top + 32f, 13f,
                0xFF101313, Paint.Align.CENTER, UiKit.BODY_BOLD);

        float listBottom = designH - 38f;
        float listH = listBottom - LIST_TOP;
        clampCatalogScroll();
        c.save();
        c.clipRect(0f, LIST_TOP, designW, listBottom);
        int first = Math.max(0, (int) (catalogScroll / ROW_H) - 1);
        for (int i = first; i < StratagemCatalog.ALL.size(); i++) {
            float y = LIST_TOP + i * ROW_H - catalogScroll;
            if (y > listBottom) break;
            if (y + ROW_H < LIST_TOP) continue;
            drawCatalogRow(c, i, y);
        }
        c.restore();
        float max = Math.max(1f, StratagemCatalog.ALL.size() * ROW_H - listH);
        if (max > 0f) {
            float barH = Math.max(30f, listH * listH / (StratagemCatalog.ALL.size() * ROW_H));
            float barY = LIST_TOP + (listH - barH) * (catalogScroll / max);
            UiKit.panel(c, designW - 12f, barY, designW - 8f, barY + barH, 2f,
                    0x66FFD24A, 0);
        }
        textPaint.reset();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xF2030607);
        c.drawRect(0f, listBottom, designW, designH, textPaint);
        UiKit.line(c, 24f, listBottom + 1f, designW - 24f, listBottom + 1f, 0.8f, 0x22F1EEDC);
        UiKit.text(c, "共 " + StratagemCatalog.ALL.size() + " 项 · 点击查看呼叫代码",
                designW * 0.5f, designH - 12f, 9f, UiKit.DIM, Paint.Align.CENTER, UiKit.BODY);
    }

    private void drawCatalogRow(Canvas c, int index, float y) {
        Stratagem s = StratagemCatalog.ALL.get(index);
        int accent = UiKit.categoryColor(s.category);
        float left = 14f;
        float right = designW - 14f;
        float top = y + 2f;
        float bottom = y + ROW_H - 2f;
        boolean alt = (index & 1) == 0;
        UiKit.panel(c, left, top, right, bottom, 5f,
                alt ? 0xB80A1113 : 0x8C081012, 0x22F1EEDC);
        UiKit.line(c, left, top + 4f, left, bottom - 4f, 3f, accent);
        icons.draw(c, s.assetName, left + 12f, top + 9f, left + 58f, bottom - 9f);
        float nameMax = Math.max(120f, right - (left + 70f) - 180f);
        UiKit.textFit(c, s.name, left + 70f, top + 26f, 15f, 10f, nameMax,
                UiKit.CREAM, Paint.Align.LEFT, UiKit.BODY_BOLD);
        UiKit.textFit(c, s.nameZh, left + 70f, top + 44f, 11f, 9f, nameMax,
                UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
        UiKit.text(c, UiKit.categoryZh(s.category), right - 12f, top + 17f, 8f,
                accent, Paint.Align.RIGHT, UiKit.BODY_BOLD);
        drawMiniCode(c, s, right - 12f, top + 38f);
    }

    private void drawMiniCode(Canvas c, Stratagem s, float right, float y) {
        int n = s.sequence.length;
        float step = n > 7 ? 10f : 12f;
        float x = right - (n - 1) * step;
        for (int i = 0; i < n; i++) {
            UiKit.arrow(c, x, y, n > 7 ? 9f : 11f, s.sequence[i], 0xAAFFD24A);
            x += step;
        }
    }

    private void drawDetail(Canvas c) {
        UiKit.background(c, designW, designH);
        drawTopBar(c, "战备详情", "CALL-IN DATA");
        Stratagem s = detailStratagem;
        if (s == null) {
            UiKit.text(c, "无数据", designW * 0.5f, 300f, 20f,
                    UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
            return;
        }
        int accent = UiKit.categoryColor(s.category);
        if (landscape) {
            float iconCx = designW * 0.25f;
            float iconCy = designH * 0.40f;
            float iconSize = Math.min(190f, designH * 0.40f);
            UiKit.ring(c, iconCx, iconCy, iconSize * 0.58f, UiKit.withAlpha(accent, 100), 1.2f);
            UiKit.ring(c, iconCx, iconCy, iconSize * 0.66f, UiKit.withAlpha(accent, 40), 0.8f);
            icons.draw(c, s.assetName, iconCx - iconSize * 0.5f, iconCy - iconSize * 0.5f,
                    iconCx + iconSize * 0.5f, iconCy + iconSize * 0.5f);
            float rightX = designW * 0.47f;
            UiKit.text(c, UiKit.categoryZh(s.category) + " / " + s.category, rightX, 120f,
                    10f, accent, Paint.Align.LEFT, UiKit.BODY_BOLD);
            UiKit.textFit(c, s.name, rightX, 154f, 27f, 14f, designW - 60f - rightX,
                    UiKit.CREAM, Paint.Align.LEFT, UiKit.DISPLAY);
            UiKit.textFit(c, s.nameZh, rightX, 178f, 16f, 10f, designW - 60f - rightX,
                    UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
            drawSlots(c, s, s.sequence.length, true, designW * 0.66f, 232f,
                    designW * 0.48f, false);
            UiKit.text(c, "输入这段序列即可呼叫", designW * 0.66f, 260f, 10f,
                    UiKit.DIM, Paint.Align.CENTER, UiKit.BODY);
            drawDetailData(c, s, accent, rightX, 282f, designW - 50f,
                    primaryBottomRect().top - 18f);
        } else {
            float cx = designW * 0.5f;
            UiKit.ring(c, cx, 190f, 84f, UiKit.withAlpha(accent, 100), 1.2f);
            UiKit.ring(c, cx, 190f, 96f, UiKit.withAlpha(accent, 40), 0.8f);
            icons.draw(c, s.assetName, cx - 80f, 110f, cx + 80f, 270f);
            UiKit.text(c, UiKit.categoryZh(s.category) + " / " + s.category, cx, 294f, 10f,
                    accent, Paint.Align.CENTER, UiKit.BODY_BOLD);
            UiKit.textFit(c, s.name, cx, 332f, 27f, 14f, designW - 60f,
                    UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
            UiKit.textFit(c, s.nameZh, cx, 358f, 16f, 10f, designW - 60f,
                    UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
            drawSlots(c, s, s.sequence.length, true, cx, 420f, designW - 100f, false);
            UiKit.text(c, "输入这段序列即可呼叫", cx, 458f, 11f,
                    UiKit.DIM, Paint.Align.CENTER, UiKit.BODY);
            drawDetailData(c, s, accent, 50f, 494f, designW - 50f,
                    primaryBottomRect().top - 28f);
        }
        drawButton(c, detailStartRect(), "开 始 盲 呼", true);
        drawButton(c, detailBackRect(), "返 回 图 鉴", false);
    }

    private void drawDetailData(Canvas c, Stratagem s, int accent, float left, float top,
                                float right, float bottom) {
        if (bottom - top < 78f) return;
        UiKit.panel(c, left, top, right, bottom, 8f, 0x9910191B, 0x33F1EEDC);
        UiKit.text(c, "呼 叫 数 据  /  CALL DATA", (left + right) * 0.5f, top + 22f, 8f,
                UiKit.GOLD, Paint.Align.CENTER, UiKit.BODY_BOLD);
        float row = top + 42f;
        detailRow(c, left + 24f, right - 24f, row, "战备编号", "#" + s.id);
        detailRow(c, left + 24f, right - 24f, row + 20f, "序列长度", s.sequence.length + " 步");
        detailRow(c, left + 24f, right - 24f, row + 40f, "所属类别",
                UiKit.categoryZh(s.category) + " / " + s.category);
        if (bottom - top > 190f) {
            float split = top + 98f;
            UiKit.line(c, left + 24f, split, right - 24f, split, 0.8f, 0x22F1EEDC);
            UiKit.text(c, "呼 叫 流 程  /  CALL PROCEDURE", (left + right) * 0.5f, split + 22f,
                    8f, accent, Paint.Align.CENTER, UiKit.BODY_BOLD);
            float line = split + 46f;
            UiKit.text(c, "1   记住这段四向序列", left + 34f, line, 11f,
                    UiKit.CREAM, Paint.Align.LEFT, UiKit.BODY);
            UiKit.text(c, "2   在自由呼叫中滑动输入", left + 34f, line + 22f, 11f,
                    UiKit.CREAM, Paint.Align.LEFT, UiKit.BODY);
            UiKit.text(c, "3   完整匹配即可完成呼叫", left + 34f, line + 44f, 11f,
                    UiKit.CREAM, Paint.Align.LEFT, UiKit.BODY);
        }
    }

    private void detailRow(Canvas c, float left, float right, float y, String label, String value) {
        UiKit.text(c, label, left, y, 11f, UiKit.MUTED, Paint.Align.LEFT, UiKit.BODY);
        UiKit.textFit(c, value, right, y, 11f, 9f, right - left - 120f,
                UiKit.CREAM, Paint.Align.RIGHT, UiKit.BODY_BOLD);
        UiKit.line(c, left, y + 10f, right, y + 10f, 0.6f, 0x1AF1EEDC);
    }

    private RectF detailStartRect() { return primaryBottomRect(); }
    private RectF detailBackRect() { return secondaryBottomRect(); }
    private RectF backRect() { return new RectF(16f, 28f, 94f, 76f); }

    private void drawTopBar(Canvas c, String title, String sub) {
        UiKit.panel(c, 16f, 28f, 94f, 76f, 6f, 0xCC10191B, 0x44F1EEDC);
        UiKit.text(c, "< 返回", 55f, 60f, 12f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY_BOLD);
        UiKit.text(c, title, designW * 0.5f, 56f, 20f, UiKit.CREAM, Paint.Align.CENTER, UiKit.DISPLAY);
        UiKit.text(c, sub, designW * 0.5f, 72f, 8f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
    }

    // ---------------------------------------------------------------- settings / help

    private float settingsRowHeight() {
        if (!landscape) return 76f;
        // Keep five rows, three notes and the back button above each other even
        // on ultra-wide screens where designH is close to 520.
        float available = designH - 258f;
        return Math.max(44f, Math.min(68f, available / 5f));
    }

    private RectF settingsRowRect(int i) {
        if (landscape) {
            float h = settingsRowHeight();
            float top = 124f + i * (h + 4f);
            return new RectF(50f, top, designW - 50f, top + h);
        }
        return new RectF(50f, 168f + i * 94f, designW - 50f, 244f + i * 94f);
    }

    private RectF settingsBackRect() {
        if (!landscape) return secondaryBottomRect();
        float w = Math.min(300f, designW - 60f);
        return new RectF(designW * 0.5f - w * 0.5f, designH - 56f,
                designW * 0.5f + w * 0.5f, designH - 14f);
    }

    private void drawSettings(Canvas c) {
        UiKit.background(c, designW, designH);
        drawTopBar(c, "设 置", "SETTINGS");
        UiKit.text(c, "声 音 与 输 入", 50f, landscape ? 110f : 138f, 9f, UiKit.GOLD,
                Paint.Align.LEFT, UiKit.BODY_BOLD);
        UiKit.line(c, 50f, landscape ? 118f : 146f, designW - 50f,
                landscape ? 118f : 146f, 0.8f, 0x33FFD24A);
        float rowH = settingsRowHeight();
        for (int i = 0; i < 5; i++) {
            RectF r = settingsRowRect(i);
            boolean switchRow = i == 0 || i == 2 || i == 4;
            boolean on = i == 0 ? prefs.sound() : i == 2 ? prefs.voice()
                    : i == 4 ? prefs.pad() : false;
            int percent = i == 1 ? prefs.bgmVolume() : i == 3 ? prefs.voiceVolume() : 0;
            boolean highlighted = switchRow ? on : percent > 0;
            int accent = highlighted ? UiKit.GOLD : 0x66F1EEDC;
            UiKit.panel(c, r.left, r.top, r.right, r.bottom, 8f, 0xCC10191B,
                    highlighted ? UiKit.withAlpha(UiKit.GOLD, 90) : 0x33F1EEDC);
            String label = i == 0 ? "音效 / 触感" : i == 1 ? "背景音乐"
                    : i == 2 ? "语音播报" : i == 3 ? "语音音量" : "四向键盘";
            String subtitle = i == 0 ? "按钮音、结算音与震动反馈"
                    : i == 1 ? "0% - 200%；100% 为原音量，200% 为两倍"
                    : i == 2 ? "任务控制语音，独立于音效开关"
                    : i == 3 ? "0% - 200%；100% 为原音量，200% 为两倍"
                    : "关闭后只用滑动，按下圆盘不再响应";
            float labelY = r.top + (landscape
                    ? rowH * (switchRow ? 0.46f : 0.36f)
                    : switchRow ? 30f : 28f);
            float subtitleY = r.top + (landscape
                    ? rowH * (switchRow ? 0.78f : 0.62f)
                    : switchRow ? 50f : 44f);
            UiKit.text(c, label, r.left + 24f, labelY, 15f, UiKit.CREAM,
                    Paint.Align.LEFT, UiKit.BODY_BOLD);
            UiKit.text(c, subtitle, r.left + 24f, subtitleY, 9f, UiKit.MUTED,
                    Paint.Align.LEFT, UiKit.BODY);
            if (switchRow) {
                UiKit.text(c, on ? "开" : "关", r.right - 100f,
                        r.top + (landscape ? rowH * 0.62f : 44f), 12f,
                        accent, Paint.Align.RIGHT, UiKit.BODY_BOLD);
                UiKit.toggle(c, r.right - 60f,
                        r.top + (landscape ? rowH * 0.58f : 38f), on, UiKit.GOLD);
            } else {
                UiKit.text(c, percent + "%", r.right - 24f,
                        r.top + (landscape ? rowH * 0.36f : 30f), 12f, accent,
                        Paint.Align.RIGHT, UiKit.BODY_BOLD);
                float trackY = r.top + (landscape ? rowH * 0.82f : 62f);
                UiKit.slider(c, r.left + 24f, trackY, r.right - 24f,
                        landscape ? 6f : 8f, percent / 200f, UiKit.GOLD);
            }
        }
        float noteY = 646f;
        boolean notesFit = true;
        if (landscape) {
            float rowsBottom = settingsRowRect(4).bottom;
            float backTop = settingsBackRect().top;
            noteY = Math.min(rowsBottom + 24f, backTop - 48f);
            notesFit = noteY >= rowsBottom + 8f;
        }
        if (notesFit) {
            UiKit.text(c, "设置会立即保存；音量 100% 为原音量，200% 为两倍",
                    designW * 0.5f, noteY, 10f, UiKit.CREAM, Paint.Align.CENTER, UiKit.BODY);
            UiKit.text(c, "横屏随系统自动旋转；游玩中旋转会自动暂停", designW * 0.5f, noteY + 20f,
                    9f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
            UiKit.text(c, "滑动一笔只出一个方向，斜向按主轴判定", designW * 0.5f, noteY + 40f,
                    9f, UiKit.MUTED, Paint.Align.CENTER, UiKit.BODY);
        }
        drawButton(c, settingsBackRect(), "返 回", false);
    }

    private RectF helpBackRect() { return secondaryBottomRect(); }

    private void drawHelp(Canvas c) {
        UiKit.background(c, designW, designH);
        drawTopBar(c, "作战手册", "FIELD MANUAL");
        if (landscape) {
            float colW = designW * 0.5f - 70f;
            float leftX = 40f;
            float rightX = designW * 0.5f + 30f;
            float y1 = 106f;
            y1 = section(c, "输入", leftX, y1, colW, prefs.pad()
                    ? "按下底部圆盘的四个扇区即输入，不必等抬手；圆盘以外也可以滑动，一笔只出一个方向。"
                    : "四向键盘已关闭，使用全屏滑动输入，一笔只出一个方向。斜向滑动按主轴判定，不会作废。");
            y1 = section(c, "经典模式", leftX, y1 + 4f, colW,
                    "每回合 15 秒，完成目标按箭头数得分并抢回时间；按错只清空输入并锁 0.4 秒，不扣时间。回合末结算时间与完美奖励。");
            y1 = section(c, "效果模式", leftX, y1 + 4f, colW,
                    "无特殊效果 8 秒起；默写隐藏箭头且时间 ×6；多行四线共享牌堆；长城 8 箭 15 秒；终端 20 秒长序列。");
            section(c, "一错即死", leftX, y1 + 4f, colW,
                    "效果模式可开启：按错一次立即结束。与所选效果共用最高分记录。");
            float y2 = 106f;
            y2 = section(c, "自由呼叫", rightX, y2, colW,
                    "无计时、无分数；输入完整序列即呼叫，右上图鉴查看全部 " + StratagemCatalog.ALL.size() + " 条代码。");
            y2 = section(c, "横屏", rightX, y2 + 4f, colW,
                    "跟随系统自动旋转；横竖屏都有完整布局。游玩中旋转会自动暂停，倒计时不受影响。");
            section(c, "界面与素材", rightX, y2 + 4f, colW,
                    "顶部中央按钮暂停；四向键盘可在设置中关闭。非官方粉丝作品，战备数据、图标与音效版权归 Arrowhead / Sony 及原作者。");
        } else {
            float y = 106f;
            y = section(c, "输入", 24f, y, designW - 48f, prefs.pad()
                    ? "按下底部圆盘的四个扇区即输入，不必等抬手；圆盘以外也可以滑动，一笔只出一个方向。"
                    : "四向键盘已关闭，使用全屏滑动输入，一笔只出一个方向。斜向滑动按主轴判定，不会作废。");
            y = section(c, "经典模式", 24f, y + 4f, designW - 48f,
                    "每回合 15 秒，完成目标按箭头数得分并抢回时间；按错只清空输入并锁 0.4 秒，不扣时间。回合末结算时间与完美奖励。");
            y = section(c, "效果模式", 24f, y + 4f, designW - 48f,
                    "无特殊效果 8 秒起；默写隐藏箭头且时间 ×6；多行四线共享牌堆；长城 8 箭 15 秒；终端 20 秒长序列。");
            y = section(c, "一错即死", 24f, y + 4f, designW - 48f,
                    "效果模式可开启：按错一次立即结束。与所选效果共用最高分记录。");
            y = section(c, "自由呼叫", 24f, y + 4f, designW - 48f,
                    "无计时、无分数；输入完整序列即呼叫，右上图鉴查看全部 " + StratagemCatalog.ALL.size() + " 条代码。");
            y = section(c, "横屏", 24f, y + 4f, designW - 48f,
                    "跟随系统自动旋转；横竖屏都有完整布局。游玩中旋转会自动暂停，倒计时不受影响。");
            section(c, "界面与素材", 24f, y + 4f, designW - 48f,
                    "顶部中央按钮暂停；四向键盘可在设置中关闭。非官方粉丝作品，战备数据、图标与音效版权归 Arrowhead / Sony 及原作者。");
        }
        drawButton(c, helpBackRect(), "返 回 主 菜 单", false);
    }

    private float section(Canvas c, String title, float x, float y, float maxWidth, String body) {
        UiKit.text(c, title, x, y, 14f, UiKit.GOLD, Paint.Align.LEFT, UiKit.BODY_BOLD);
        UiKit.line(c, x, y + 6f, x + maxWidth, y + 6f, 0.8f, 0x33FFD24A);
        float after = wrap(c, body, x, y + 24f, landscape ? 11f : 10.5f, UiKit.CREAM,
                maxWidth, landscape ? 17f : 16f);
        return after + 8f;
    }

    private float wrap(Canvas c, String text, float x, float y, float size, int color,
                       float maxWidth, float lineHeight) {
        textPaint.reset();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(UiKit.BODY);
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        float yy = y;
        int start = 0;
        while (start < text.length()) {
            int end = start;
            while (end < text.length()) {
                float w = textPaint.measureText(text, start, end + 1);
                if (w > maxWidth && end > start) break;
                end++;
            }
            if (end == start) end++;
            if (end < text.length() && HANGING_PUNCT.indexOf(text.charAt(end)) >= 0) {
                end++;
            }
            String line = text.substring(start, Math.min(end, text.length()));
            c.drawText(line, x, yy, textPaint);
            yy += lineHeight;
            start = end;
        }
        return yy;
    }

    // ---------------------------------------------------------------- helpers

    private boolean hit(RectF r, float x, float y) {
        return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom;
    }

    private static String two(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static String five(int n) {
        return String.format(Locale.US, "%05d", Math.max(0, n));
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }
}
