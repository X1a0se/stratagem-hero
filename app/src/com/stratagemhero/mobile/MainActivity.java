package com.stratagemhero.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

public final class MainActivity extends Activity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        gameView = new GameView(this);
        setContentView(gameView);
        gameView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int left = 0;
                int top = 0;
                int right = 0;
                int bottom = 0;
                if (Build.VERSION.SDK_INT >= 28) {
                    DisplayCutout cutout = insets.getDisplayCutout();
                    if (cutout != null) {
                        left = cutout.getSafeInsetLeft();
                        top = cutout.getSafeInsetTop();
                        right = cutout.getSafeInsetRight();
                        bottom = cutout.getSafeInsetBottom();
                    }
                }
                gameView.setSafeInsets(left, top, right, bottom);
                return insets;
            }
        });
        gameView.requestApplyInsets();
        hideSystemUi();
        registerBackHandler();
    }

    private void registerBackHandler() {
        if (Build.VERSION.SDK_INT >= 33) {
            Back33.register(this);
        }
    }

    private void handleBackOrFinish() {
        if (gameView == null || !gameView.handleBack()) finish();
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= 30) {
            SystemUi30.hide(this);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
            if (gameView != null) gameView.refreshGestureExclusion();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.onHostResume();
        hideSystemUi();
    }

    @Override
    protected void onPause() {
        if (gameView != null) gameView.onHostPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) gameView.release();
        super.onDestroy();
    }

    /**
     * API 33 classes live in their own class so Android 8-12 never has to
     * resolve them while loading MainActivity.
     */
    private static final class Back33 {
        static void register(final MainActivity activity) {
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    new OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            activity.handleBackOrFinish();
                        }
                    });
        }
    }

    /** API 30 immersive-mode entry point, isolated for the same reason. */
    private static final class SystemUi30 {
        static void hide(MainActivity activity) {
            android.view.Window window = activity.getWindow();
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT < 33) {
            handleBackOrFinish();
        } else {
            super.onBackPressed();
        }
    }
}
