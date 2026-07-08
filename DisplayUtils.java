package com.inklet.app.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Inklet is a phone-first app, but it can also run on Chromebooks, tablets,
 * Samsung DeX, and Android's desktop windowing mode. On those larger/resizable
 * windows we cap the main content to a comfortable reading width and center
 * it, instead of letting text stretch edge-to-edge across a huge window.
 *
 * This re-applies on every onCreate and on onConfigurationChanged, so it keeps
 * up as a freeform desktop window is dragged wider or narrower.
 */
public class DisplayUtils {

    private static final int WIDE_BREAKPOINT_DP = 600;
    private static final int MAX_CONTENT_WIDTH_DP = 760;

    public static void applyResponsiveWidth(Activity activity, View content) {
        if (activity == null || content == null) return;

        int screenWidthDp = activity.getResources().getConfiguration().screenWidthDp;
        float density = activity.getResources().getDisplayMetrics().density;
        ViewGroup.LayoutParams lp = content.getLayoutParams();
        if (lp == null) return;

        if (screenWidthDp >= WIDE_BREAKPOINT_DP) {
            lp.width = Math.round(MAX_CONTENT_WIDTH_DP * density);
            if (lp instanceof RelativeLayout.LayoutParams) {
                ((RelativeLayout.LayoutParams) lp).addRule(RelativeLayout.CENTER_HORIZONTAL);
            }
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        content.setLayoutParams(lp);
    }
}
