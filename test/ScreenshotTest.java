package com.stratagemhero.mobile;

import static org.junit.Assert.assertNotNull;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Renders every screen in both orientations with the real Robolectric native
 * canvas, then writes PNGs to {@code app/shots} for visual inspection.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w450dp-h800dp-xxhdpi")
public class ScreenshotTest {

    private static final int PW = 1080;
    private static final int PH = 1920;
    private static final int LW = 1920;
    private static final int LH = 1080;
    private static final int SW = 2400;
    private static final int SH = 1080;
    private static final int TW = 1080;
    private static final int TH = 2400;

    private File dir;

    @Before
    public void resetSettings() {
        Application app = ApplicationProvider.getApplicationContext();
        Prefs prefs = new Prefs(app);
        prefs.setSound(true);
        prefs.setHaptics(true);
        prefs.setVoice(true);
        prefs.setBgmVolume(100);
        prefs.setVoiceVolume(100);
        prefs.setPad(true);
        prefs.setEffectMode(EffectMode.NONE.key);
        prefs.setEffectOneHeart(false);
        dir = new File("shots");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
    }

    @Test
    public void portraitScreens() throws Exception {
        GameView view = newView(PW, PH);
        capture(view, "01-menu.png", PW, PH);

        set(view, "screen", 13);
        capture(view, "02-effect-setup.png", PW, PH);
        set(view, "effectOneHeart", true);
        capture(view, "03-effect-setup-oneheart.png", PW, PH);
        set(view, "effectOneHeart", false);

        start(view, 0);
        enterFirstClassic(view);
        capture(view, "04-classic.png", PW, PH);

        for (EffectMode m : EffectMode.values()) {
            startEffect(view, m);
            enterFirstEffect(view);
            capture(view, "05-effect-" + m.key + ".png", PW, PH);
        }

        start(view, 2);
        capture(view, "06-free.png", PW, PH);
        showCalledIn(view);
        capture(view, "07-free-called.png", PW, PH);

        invoke(view, "openCatalog", 0);
        capture(view, "08-catalog.png", PW, PH);
        setDetail(view, "ge_e711_extraction_drill");
        capture(view, "09-detail.png", PW, PH);

        set(view, "screen", 12);
        capture(view, "10-settings.png", PW, PH);
        set(view, "screen", 1);
        capture(view, "11-help.png", PW, PH);

        start(view, 0);
        invoke(view, "pauseGame");
        capture(view, "12-pause.png", PW, PH);
        invoke(view, "resumeGame");
        set(view, "countdown", 2.2f);
        set(view, "screen", 2);
        capture(view, "13-countdown.png", PW, PH);

        showClassicOver(view);
        capture(view, "14-over-classic.png", PW, PH);
        showGreatWallOver(view);
        capture(view, "15-over-greatwall.png", PW, PH);
    }

    @Test
    public void landscapeScreens() throws Exception {
        GameView view = newView(LW, LH);
        capture(view, "20-landscape-menu.png", LW, LH);

        set(view, "screen", 13);
        capture(view, "21-landscape-effect-setup.png", LW, LH);

        start(view, 0);
        enterFirstClassic(view);
        capture(view, "22-landscape-classic.png", LW, LH);

        for (EffectMode m : EffectMode.values()) {
            startEffect(view, m);
            enterFirstEffect(view);
            capture(view, "23-landscape-" + m.key + ".png", LW, LH);
        }

        start(view, 2);
        capture(view, "24-landscape-free.png", LW, LH);
        showCalledIn(view);
        capture(view, "25-landscape-free-called.png", LW, LH);

        startEffect(view, EffectMode.MULTILINES);
        invoke(view, "pauseGame");
        capture(view, "26-landscape-pause.png", LW, LH);

        invoke(view, "openCatalog", 0);
        capture(view, "27-landscape-catalog.png", LW, LH);
        setDetail(view, "ge_e711_extraction_drill");
        capture(view, "28-landscape-detail.png", LW, LH);

        set(view, "screen", 12);
        capture(view, "29-landscape-settings.png", LW, LH);
        set(view, "screen", 1);
        capture(view, "30-landscape-help.png", LW, LH);

        showGreatWallOver(view);
        capture(view, "31-landscape-over.png", LW, LH);
    }

    @Test
    public void shortLandscapeScreens() throws Exception {
        GameView view = newView(SW, SH);
        capture(view, "40-short-menu.png", SW, SH);

        set(view, "screen", 13);
        capture(view, "41-short-effect-setup.png", SW, SH);

        start(view, 0);
        enterFirstClassic(view);
        capture(view, "42-short-classic.png", SW, SH);

        startEffect(view, EffectMode.MULTILINES);
        enterFirstEffect(view);
        capture(view, "43-short-multilines.png", SW, SH);

        startEffect(view, EffectMode.GREATWALL);
        enterFirstEffect(view);
        capture(view, "44-short-greatwall.png", SW, SH);

        startEffect(view, EffectMode.TERMINAL);
        enterFirstEffect(view);
        capture(view, "45-short-terminal.png", SW, SH);

        start(view, 2);
        capture(view, "46-short-free.png", SW, SH);

        set(view, "screen", 1);
        capture(view, "47-short-help.png", SW, SH);

        set(view, "screen", 12);
        capture(view, "48-short-settings.png", SW, SH);
    }

    @Test
    public void tallPortraitScreens() throws Exception {
        GameView view = newView(TW, TH);
        capture(view, "50-tall-menu.png", TW, TH);

        set(view, "screen", 1);
        capture(view, "51-tall-help.png", TW, TH);

        set(view, "screen", 13);
        capture(view, "52-tall-effect-setup.png", TW, TH);

        start(view, 0);
        capture(view, "53-tall-classic.png", TW, TH);

        showGreatWallOver(view);
        capture(view, "54-tall-over.png", TW, TH);
    }

    @Test
    public void cutoutSafeAreaScreens() throws Exception {
        GameView portrait = newView(PW, PH);
        portrait.setSafeInsets(32, 96, 24, 48);
        capture(portrait, "60-cutout-portrait-menu.png", PW, PH);

        GameView landscape = newView(SW, SH);
        landscape.setSafeInsets(96, 24, 96, 24);
        capture(landscape, "61-cutout-landscape-menu.png", SW, SH);
        set(landscape, "screen", 13);
        capture(landscape, "62-cutout-landscape-effect-setup.png", SW, SH);
    }

    private static GameView newView(int w, int h) {
        Application app = ApplicationProvider.getApplicationContext();
        GameView view = new GameView(app);
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, w, h);
        return view;
    }

    private static void start(GameView view, int mode) throws Exception {
        invoke(view, "startMode", mode, true, 0f);
        invoke(view, "beginPlay");
    }

    private static void startEffect(GameView view, EffectMode mode) throws Exception {
        set(view, "effectMode", mode);
        start(view, 1);
    }

    private static void enterFirstClassic(GameView view) throws Exception {
        ClassicSession classic = (ClassicSession) get(view, "classic");
        Stratagem target = classic.getCurrent();
        if (target != null && target.sequence.length > 0) {
            invoke(view, "triggerInput", target.sequence[0], 400f, 500f);
        }
    }

    private static void enterFirstEffect(GameView view) throws Exception {
        EffectSession effect = (EffectSession) get(view, "effect");
        Direction dir = null;
        if (effect.getMode() == EffectMode.MULTILINES) {
            for (EffectSession.Lane lane : effect.getLanes()) {
                if (!lane.isDone() && lane.getTarget() != null && lane.getTarget().sequence.length > 0) {
                    dir = lane.getTarget().sequence[0];
                    break;
                }
            }
        } else if (effect.getMode() == EffectMode.GREATWALL) {
            if (!effect.getWall().isEmpty()) dir = effect.getWall().get(0);
        } else if (effect.getMode() == EffectMode.TERMINAL) {
            if (!effect.getTerminalLine().isEmpty()) dir = effect.getTerminalLine().get(0);
        } else {
            Stratagem target = effect.getCurrent();
            if (target != null && target.sequence.length > 0) dir = target.sequence[0];
        }
        if (dir != null) invoke(view, "triggerInput", dir, 400f, 500f);
    }

    private static void showCalledIn(GameView view) throws Exception {
        GameSession free = (GameSession) get(view, "free");
        Field field = GameSession.class.getDeclaredField("lastCompleted");
        field.setAccessible(true);
        field.set(free, StratagemCatalog.ALL.get(0));
        set(view, "revealUntil", 1000000000f);
    }

    private static void setDetail(GameView view, String code) throws Exception {
        Stratagem stratagem = StratagemCatalog.find(code);
        assertNotNull(stratagem);
        set(view, "detailStratagem", stratagem);
        set(view, "screen", 11);
    }

    private static void showClassicOver(GameView view) throws Exception {
        set(view, "finalMode", 0);
        set(view, "finalEffectMode", EffectMode.NONE);
        set(view, "finalScore", 12480);
        set(view, "finalCompleted", 37);
        set(view, "finalRound", 8);
        set(view, "finalArrows", 213);
        set(view, "finalLines", 0);
        set(view, "finalSpeed", 0);
        set(view, "finalBest", 15000);
        set(view, "finalRecord", true);
        set(view, "screen", 8);
    }

    private static void showGreatWallOver(GameView view) throws Exception {
        startEffect(view, EffectMode.GREATWALL);
        EffectSession effect = (EffectSession) get(view, "effect");
        for (int i = 0; i < 12 && !effect.getWall().isEmpty(); i++) {
            invoke(view, "triggerInput", effect.getWall().get(0), 500f, 500f);
        }
        set(view, "finalMode", 1);
        set(view, "finalEffectMode", EffectMode.GREATWALL);
        set(view, "finalScore", 40);
        set(view, "finalCompleted", 40);
        set(view, "finalRound", 1);
        set(view, "finalArrows", 40);
        set(view, "finalLines", 0);
        set(view, "finalDuration", 42.3f);
        set(view, "finalSpeed", 57);
        set(view, "finalBest", 60);
        set(view, "finalRecord", false);
        set(view, "screen", 8);
    }

    private static Object get(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void invoke(Object target, String name, Object... args) throws Exception {
        for (Method m : target.getClass().getDeclaredMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            m.setAccessible(true);
            m.invoke(target, args);
            return;
        }
        throw new NoSuchMethodException(name + "/" + args.length);
    }

    private void capture(GameView view, String name, int w, int h) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        File out = new File(dir, name);
        FileOutputStream stream = new FileOutputStream(out);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        stream.close();
        System.out.println("SHOT " + out.getAbsolutePath() + " " + bitmap.getWidth() + "x" + bitmap.getHeight());
    }
}
