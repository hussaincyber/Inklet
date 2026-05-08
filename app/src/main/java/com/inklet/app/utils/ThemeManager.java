package com.inklet.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {
    private static final String PREFS = "inklet_theme";
    private static final String KEY_DARK = "is_dark";

    public static boolean isDarkMode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK, false);
    }

    public static void setDarkMode(Context context, boolean dark) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DARK, dark).apply();
    }

    public static void toggleTheme(Context context) {
        setDarkMode(context, !isDarkMode(context));
    }
}
