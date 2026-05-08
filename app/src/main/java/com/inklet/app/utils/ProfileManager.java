package com.inklet.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

public class ProfileManager {
    private static final String PREFS_NAME = "inklet_profile";
    private static final String KEY_PROFILE_ID = "profile_id";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    public static String getProfileId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY_PROFILE_ID, null);
        if (id == null) {
            id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == null || id.isEmpty()) id = "inklet_" + System.currentTimeMillis();
            prefs.edit().putString(KEY_PROFILE_ID, id).apply();
        }
        return id;
    }

    public static boolean isFirstLaunch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public static void markLaunched(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }

    public static String getDisplayName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_DISPLAY_NAME, "You");
    }

    public static void setDisplayName(Context context, String name) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DISPLAY_NAME, name).apply();
    }
}
