package com.stratagemhero.mobile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/** Boots the real activity on the oldest and newest supported API levels. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 34}, qualifiers = "w450dp-h800dp-xxhdpi")
public class ActivitySmokeTest {

    @Test
    public void activityBootsWithContentView() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        assertNotNull(activity);
        View content = activity.findViewById(android.R.id.content);
        assertNotNull(content);
        assertTrue(((ViewGroup) content).getChildCount() >= 1);
        controller.pause().resume().destroy();
    }
}
