package com.stratagemhero.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

/** Resolves the VectorDrawable call-in icons by their asset name. */
final class IconBank {
    private final Context context;
    private final Map<String, Integer> ids = new HashMap<>();
    private final SparseArray<Drawable> cache = new SparseArray<>();

    IconBank(Context context) {
        this.context = context.getApplicationContext();
    }

    private int id(String name) {
        if (name == null || name.isEmpty()) return 0;
        Integer cached = ids.get(name);
        if (cached != null) return cached;
        int res = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        ids.put(name, res);
        return res;
    }

    Drawable get(String name) {
        int res = id(name);
        if (res == 0) return null;
        Drawable d = cache.get(res);
        if (d == null) {
            try {
                d = context.getDrawable(res);
            } catch (Exception ignored) {
                return null;
            }
            if (d != null) cache.put(res, d);
        }
        return d;
    }

    void draw(Canvas canvas, String name, float left, float top, float right, float bottom) {
        Drawable d = get(name);
        if (d == null) return;
        d.setBounds(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
        d.draw(canvas);
    }
}
