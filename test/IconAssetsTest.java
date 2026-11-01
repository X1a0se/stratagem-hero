package com.stratagemhero.mobile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Loads and draws every catalog icon so a missing or malformed vector cannot fail silently. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w450dp-h800dp-xxhdpi")
public class IconAssetsTest {

    @Test
    public void allCatalogIconsLoadAndRender() {
        Application app = ApplicationProvider.getApplicationContext();
        for (Stratagem s : StratagemCatalog.ALL) {
            assertNotNull("asset name for " + s.id, s.assetName);
            int res = app.getResources().getIdentifier(
                    s.assetName, "drawable", app.getPackageName());
            assertTrue("missing drawable " + s.assetName + " for " + s.id, res != 0);
            Drawable drawable = app.getDrawable(res);
            assertNotNull("unreadable drawable " + s.assetName + " for " + s.id, drawable);
            Bitmap bitmap = Bitmap.createBitmap(126, 126, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, 126, 126);
            drawable.draw(canvas);
            bitmap.recycle();
        }
    }
}
