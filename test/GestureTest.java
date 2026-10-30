package com.stratagemhero.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Drives the real touch path in portrait and landscape: menu, pad, swipes, modes, pause. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w450dp-h800dp-xxhdpi")
public class GestureTest {
    private static final int PW = 1080;
    private static final int PH = 1920;
    private static final int LW = 1920;
    private static final int LH = 1080;

    private Application app;
    private Prefs prefs;

    @Before
    public void resetSettings() {
        app = ApplicationProvider.getApplicationContext();
        prefs = new Prefs(app);
        prefs.setSound(true);
        prefs.setHaptics(true);
        prefs.setVoice(true);
        prefs.setBgmVolume(100);
        prefs.setVoiceVolume(100);
        prefs.setPad(true);
        prefs.setEffectMode("none");
        prefs.setEffectOneHeart(false);
    }

    @Test
    public void classicStartCompleteAndPause() throws Exception {
        GameView view = newView(PW, PH);
        assertEquals("menu is first", 0, getInt(view, "screen"));

        tap(view, 225f, 576f);
        assertEquals("menu start opens countdown", 2, getInt(view, "screen"));
        invoke(view, "beginPlay");
        assertEquals("countdown enters classic play", 3, getInt(view, "screen"));

        ClassicSession classic = (ClassicSession) get(view, "classic");
        assertNotNull(classic.getCurrent());
        assertEquals("classic starts playing", ClassicSession.PLAYING, classic.getState());
        finishClassicTarget(view, classic);
        assertEquals("one target completed by pad taps", 1, classic.getCompletedThisRound());
        assertTrue("completion awards score", classic.getScore() > 0);
        assertTrue("completion records arrows", classic.getTotalArrows() > 0);

        tap(view, 225f, 52f);
        assertEquals("pause button opens pause sheet", 9, getInt(view, "screen"));
        tap(view, 225f, 361f);
        assertEquals("resume returns to classic play", 3, getInt(view, "screen"));
    }

    @Test
    public void swipeUsesWatchThresholds() throws Exception {
        GameView view = newView(PW, PH);
        startPlay(view, 0);
        ClassicSession classic = (ClassicSession) get(view, "classic");
        Direction first = classic.getCurrent().sequence[0];

        float[] start = {225f, 250f};
        float[] shortEnd = offset(start, first, 6f);
        swipe(view, start[0], start[1], shortEnd[0], shortEnd[1]);
        assertEquals("short drag is ignored", 0, classic.getEnteredCount());

        float[] diagonal = diagonalPoint(start, first);
        swipe(view, start[0], start[1], (start[0] + diagonal[0]) * 0.5f,
                (start[1] + diagonal[1]) * 0.5f, diagonal[0], diagonal[1]);
        assertEquals("diagonal reads its main axis", 1, classic.getEnteredCount());

        completeWithSwipes(view, classic, start);
        assertEquals("four strokes complete a target", 1, classic.getCompletedThisRound());
    }

    @Test
    public void oneStrokeEntersOneDirection() throws Exception {
        GameView view = newView(PW, PH);
        startPlay(view, 0);
        ClassicSession classic = (ClassicSession) get(view, "classic");
        Direction first = classic.getCurrent().sequence[0];

        float[] start = {225f, 250f};
        float[] end = offset(start, first, 160f);
        float[] mid = offset(start, first, 80f);
        swipe(view, start[0], start[1], mid[0], mid[1], end[0], end[1],
                end[0] + 20f, end[1] + 20f);
        assertEquals("one long stroke enters one direction", 1, classic.getEnteredCount());

        Direction second = classic.getCurrent().sequence[classic.getEnteredCount()];
        float[] end2 = offset(start, second, 70f);
        swipe(view, start[0], start[1], end2[0], end2[1]);
        assertEquals("lifting allows the next direction", 2, classic.getEnteredCount());
        completeWithSwipes(view, classic, start);
        assertEquals("swipes finish the target", 1, classic.getCompletedThisRound());
    }

    @Test
    public void padPressFiresBeforeTheFingerLifts() throws Exception {
        GameView view = newView(PW, PH);
        startPlay(view, 0);
        ClassicSession classic = (ClassicSession) get(view, "classic");
        Direction first = classic.getCurrent().sequence[0];
        float[] p = padPoint(view, first, padRadius(view) * 0.72f);
        float[] screen = toScreen(view, p[0], p[1]);

        long t = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, screen[0], screen[1], 0);
        view.dispatchTouchEvent(down);
        down.recycle();
        assertEquals("pressing the arc pad already counts", 1, classic.getEnteredCount());

        MotionEvent up = MotionEvent.obtain(t, t + 30, MotionEvent.ACTION_UP, screen[0], screen[1], 0);
        view.dispatchTouchEvent(up);
        up.recycle();
        assertEquals("lifting the finger does not repeat it", 1, classic.getEnteredCount());
    }

    @Test
    public void padOffUsesSwipesOnly() throws Exception {
        prefs.setPad(false);
        GameView view = newView(PW, PH);
        startPlay(view, 0);
        ClassicSession classic = (ClassicSession) get(view, "classic");
        Direction first = classic.getCurrent().sequence[0];

        float[] pad = padPoint(view, first, padRadius(view) * 0.72f);
        tap(view, pad[0], pad[1]);
        assertEquals("pad taps do nothing when the pad is off", 0, classic.getEnteredCount());

        completeWithSwipes(view, classic, new float[]{225f, 250f});
        assertEquals("swipes remain enough with the pad off", 1, classic.getCompletedThisRound());
    }

    @Test
    public void effectSetupPersistsModeAndStarts() throws Exception {
        GameView view = newView(PW, PH);
        tap(view, 219f, 445f);
        assertEquals("effect tab selects effect mode", 1, getInt(view, "selectedMode"));

        tap(view, 225f, 576f);
        assertEquals("effect start opens setup", 13, getInt(view, "screen"));
        assertEquals("setup starts on none", EffectMode.NONE, get(view, "effectMode"));

        tap(view, 225f, 224f);
        assertEquals("dictation row selected", EffectMode.DICTATION, get(view, "effectMode"));
        assertEquals("dictation is persisted", "dictation", prefs.effectMode());

        tap(view, 225f, 490f);
        assertEquals("one heart toggled on", Boolean.TRUE, get(view, "effectOneHeart"));
        assertTrue("one heart is persisted", prefs.effectOneHeart());

        tap(view, 225f, 672f);
        assertEquals("effect start begins countdown", 2, getInt(view, "screen"));
        invoke(view, "beginPlay");
        assertEquals("effect mode enters effect play", 4, getInt(view, "screen"));
        EffectSession effect = (EffectSession) get(view, "effect");
        assertEquals("effect uses dictation", EffectMode.DICTATION, effect.getMode());
        assertEquals("dictation timer is six times", 48f, effect.getTimeMax(), 0.01f);
    }

    @Test
    public void settingsToggleAndReturn() throws Exception {
        GameView view = newView(PW, PH);

        tap(view, 329f, 54f);
        assertEquals("gear opens settings", 12, getInt(view, "screen"));

        tap(view, 225f, 206f);
        assertFalse("sound row turns sound off", prefs.sound());
        tap(view, 225f, 206f);
        assertTrue("sound row turns sound back on", prefs.sound());

        tap(view, 225f, 394f);
        assertFalse("voice row turns voice off", prefs.voice());
        tap(view, 225f, 582f);
        assertFalse("pad row turns the pad off", prefs.pad());
        Prefs stored = new Prefs(app);
        assertFalse("voice setting is persisted", stored.voice());
        assertFalse("pad setting is persisted", stored.pad());

        tap(view, 225f, 736f);
        assertEquals("settings back returns to the menu", 0, getInt(view, "screen"));

        startPlay(view, 0);
        tap(view, 225f, 52f);
        assertEquals("pause opens", 9, getInt(view, "screen"));
        tap(view, 225f, 473f);
        assertEquals("pause settings row opens settings", 12, getInt(view, "screen"));
        tap(view, 225f, 736f);
        assertEquals("settings back returns to pause", 9, getInt(view, "screen"));
        tap(view, 225f, 361f);
        assertEquals("resume returns to play", 3, getInt(view, "screen"));
    }

    @Test
    public void settingsVolumeSlidersPersist() throws Exception {
        GameView view = newView(PW, PH);
        tap(view, 329f, 54f);
        assertEquals("gear opens settings", 12, getInt(view, "screen"));

        swipe(view, 225f, 324f, 300f, 324f, 376f, 324f);
        assertEquals("bgm slider reaches 200 percent", 200, prefs.bgmVolume());
        swipe(view, 225f, 512f, 300f, 512f, 376f, 512f);
        assertEquals("voice slider reaches 200 percent", 200, prefs.voiceVolume());

        Prefs stored = new Prefs(app);
        assertEquals("bgm volume is persisted", 200, stored.bgmVolume());
        assertEquals("voice volume is persisted", 200, stored.voiceVolume());

        swipe(view, 225f, 324f, 100f, 324f, 74f, 324f);
        assertEquals("bgm slider reaches 0 percent", 0, prefs.bgmVolume());
        tap(view, 225f, 324f);
        assertEquals("bgm slider returns to 100 percent", 100, prefs.bgmVolume());
        tap(view, 225f, 512f);
        assertEquals("voice slider returns to 100 percent", 100, prefs.voiceVolume());
    }

    @Test
    public void landscapeLayoutAndPauseWork() throws Exception {
        GameView view = newView(LW, LH);
        assertTrue("wide viewport is landscape", (Boolean) get(view, "landscape"));
        assertEquals("landscape design width", 1280f, getFloat(view, "designW"), 0.01f);
        assertEquals("landscape design height", 720f, getFloat(view, "designH"), 0.01f);

        tap(view, 963f, 455f);
        assertEquals("landscape start opens countdown", 2, getInt(view, "screen"));
        invoke(view, "beginPlay");
        assertEquals("landscape enters classic", 3, getInt(view, "screen"));

        ClassicSession classic = (ClassicSession) get(view, "classic");
        finishClassicTarget(view, classic);
        assertEquals("landscape pad completes a target", 1, classic.getCompletedThisRound());

        tap(view, 640f, 52f);
        assertEquals("landscape pause opens", 9, getInt(view, "screen"));
        tap(view, 640f, 321f);
        assertEquals("landscape resume", 3, getInt(view, "screen"));
    }

    @Test
    public void rotatingDuringPlayAutoPauses() throws Exception {
        GameView view = newView(PW, PH);
        startPlay(view, 0);
        assertEquals("portrait play", 3, getInt(view, "screen"));
        view.measure(View.MeasureSpec.makeMeasureSpec(LW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(LH, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, LW, LH);
        view.draw(new Canvas(Bitmap.createBitmap(LW, LH, Bitmap.Config.ARGB_8888)));
        assertEquals("rotation while playing pauses", 9, getInt(view, "screen"));
        assertTrue("rotated viewport is landscape", (Boolean) get(view, "landscape"));
    }

    private GameView newView(int w, int h) {
        GameView view = new GameView(app);
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, w, h);
        view.draw(new Canvas(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)));
        return view;
    }

    private static void startPlay(GameView view, int mode) throws Exception {
        Method start = GameView.class.getDeclaredMethod("startMode", int.class, boolean.class, float.class);
        start.setAccessible(true);
        start.invoke(view, mode, true, 0f);
        invoke(view, "beginPlay");
    }

    private static void finishClassicTarget(GameView view, ClassicSession classic) throws Exception {
        int guard = 0;
        while (classic.getState() == ClassicSession.PLAYING && classic.getCurrent() != null
                && guard++ < 40) {
            int entered = classic.getEnteredCount();
            Stratagem target = classic.getCurrent();
            if (entered >= target.length()) break;
            tapDirection(view, target.sequence[entered]);
            if (classic.getCurrent() != target || classic.getState() != ClassicSession.PLAYING) break;
            if (classic.getEnteredCount() != entered + 1) break;
        }
    }

    private static void completeWithSwipes(GameView view, ClassicSession classic, float[] start)
            throws Exception {
        int guard = 0;
        while (classic.getState() == ClassicSession.PLAYING && classic.getCurrent() != null
                && guard++ < 40) {
            Stratagem target = classic.getCurrent();
            int entered = classic.getEnteredCount();
            if (entered >= target.length()) break;
            float[] end = offset(start, target.sequence[entered], 70f);
            swipe(view, start[0], start[1], end[0], end[1]);
            if (classic.getCurrent() != target || classic.getState() != ClassicSession.PLAYING) break;
            if (classic.getEnteredCount() != entered + 1) break;
        }
    }

    private static void tapDirection(GameView view, Direction dir) throws Exception {
        float[] p = padPoint(view, dir, padRadius(view) * 0.72f);
        tap(view, p[0], p[1]);
    }

    private static float[] padPoint(GameView view, Direction dir, float radius) throws Exception {
        boolean land = (Boolean) get(view, "landscape");
        float cx;
        float cy;
        if (land) {
            cx = getFloat(view, "designW") - 190f;
            cy = getFloat(view, "designH") - 170f;
        } else {
            cx = getFloat(view, "designW") * 0.5f;
            float r = padRadius(view);
            cy = getFloat(view, "designH") - r - 30f;
        }
        switch (dir) {
            case UP:
                return new float[]{cx, cy - radius};
            case RIGHT:
                return new float[]{cx + radius, cy};
            case DOWN:
                return new float[]{cx, cy + radius};
            default:
                return new float[]{cx - radius, cy};
        }
    }

    private static float padRadius(GameView view) throws Exception {
        boolean land = (Boolean) get(view, "landscape");
        if (land) return 120f;
        return getFloat(view, "designH") < 800f ? 156f : 170f;
    }

    private static float[] offset(float[] start, Direction dir, float amount) {
        switch (dir) {
            case UP:
                return new float[]{start[0], start[1] - amount};
            case RIGHT:
                return new float[]{start[0] + amount, start[1]};
            case DOWN:
                return new float[]{start[0], start[1] + amount};
            default:
                return new float[]{start[0] - amount, start[1]};
        }
    }

    private static float[] diagonalPoint(float[] start, Direction dir) {
        switch (dir) {
            case UP:
                return new float[]{start[0] + 10f, start[1] - 34f};
            case RIGHT:
                return new float[]{start[0] + 34f, start[1] + 10f};
            case DOWN:
                return new float[]{start[0] - 10f, start[1] + 34f};
            default:
                return new float[]{start[0] - 34f, start[1] - 10f};
        }
    }

    private static void tap(GameView view, float designX, float designY) throws Exception {
        float[] screen = toScreen(view, designX, designY);
        long t = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, screen[0], screen[1], 0);
        view.dispatchTouchEvent(down);
        down.recycle();
        MotionEvent up = MotionEvent.obtain(t, t + 30, MotionEvent.ACTION_UP, screen[0], screen[1], 0);
        view.dispatchTouchEvent(up);
        up.recycle();
    }

    /** Dispatches DOWN at the first point, MOVE through the middle ones, UP at the last. */
    private static void swipe(GameView view, float... points) throws Exception {
        if (points.length < 4 || points.length % 2 != 0) throw new IllegalArgumentException("need x,y pairs");
        long t = SystemClock.uptimeMillis();
        float[] p0 = toScreen(view, points[0], points[1]);
        MotionEvent down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, p0[0], p0[1], 0);
        view.dispatchTouchEvent(down);
        down.recycle();
        int pairs = points.length / 2;
        for (int i = 1; i < pairs; i++) {
            float[] p = toScreen(view, points[i * 2], points[i * 2 + 1]);
            MotionEvent move = MotionEvent.obtain(t, t + i * 16L, MotionEvent.ACTION_MOVE,
                    p[0], p[1], 0);
            view.dispatchTouchEvent(move);
            move.recycle();
        }
        int last = pairs - 1;
        float[] pl = toScreen(view, points[last * 2], points[last * 2 + 1]);
        MotionEvent up = MotionEvent.obtain(t, t + pairs * 16L, MotionEvent.ACTION_UP,
                pl[0], pl[1], 0);
        view.dispatchTouchEvent(up);
        up.recycle();
    }

    private static float[] toScreen(GameView view, float designX, float designY) throws Exception {
        float scale = getFloat(view, "scale");
        float ox = getFloat(view, "offsetX");
        float oy = getFloat(view, "offsetY");
        return new float[]{ox + designX * scale, oy + designY * scale};
    }

    private static int getInt(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(target);
    }

    private static float getFloat(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getFloat(target);
    }

    private static Object get(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void invoke(Object target, String name) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(target);
    }
}
