package com.inklet.app.utils;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class RichTextSerializer {

    public static String toJson(SpannableStringBuilder ssb) {
        try {
            JSONObject root = new JSONObject();
            root.put("text", ssb.toString());

            JSONArray spans = new JSONArray();
            Object[] allSpans = ssb.getSpans(0, ssb.length(), Object.class);
            for (Object span : allSpans) {
                int start = ssb.getSpanStart(span);
                int end = ssb.getSpanEnd(span);
                JSONObject s = new JSONObject();
                s.put("start", start);
                s.put("end", end);

                if (span instanceof StyleSpan) {
                    int style = ((StyleSpan) span).getStyle();
                    if (style == Typeface.BOLD) s.put("type", "bold");
                    else if (style == Typeface.ITALIC) s.put("type", "italic");
                    else if (style == Typeface.BOLD_ITALIC) s.put("type", "bolditalic");
                    else continue;
                } else if (span instanceof UnderlineSpan) {
                    s.put("type", "underline");
                } else if (span instanceof StrikethroughSpan) {
                    s.put("type", "strikethrough");
                } else if (span instanceof ForegroundColorSpan) {
                    s.put("type", "fgcolor");
                    s.put("color", ((ForegroundColorSpan) span).getForegroundColor());
                } else if (span instanceof BackgroundColorSpan) {
                    s.put("type", "bgcolor");
                    s.put("color", ((BackgroundColorSpan) span).getBackgroundColor());
                } else if (span instanceof RelativeSizeSpan) {
                    s.put("type", "size");
                    s.put("size", ((RelativeSizeSpan) span).getSizeChange());
                } else if (span instanceof TypefaceSpan) {
                    s.put("type", "font");
                    s.put("font", ((TypefaceSpan) span).getFamily());
                } else if (span instanceof BulletSpan) {
                    s.put("type", "bullet");
                } else {
                    continue;
                }
                spans.put(s);
            }
            root.put("spans", spans);
            return root.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"text\":\"\",\"spans\":[]}";
        }
    }

    public static SpannableStringBuilder fromJson(String json) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        if (json == null || json.isEmpty()) return ssb;
        try {
            JSONObject root = new JSONObject(json);
            String text = root.optString("text", "");
            ssb.append(text);

            JSONArray spans = root.optJSONArray("spans");
            if (spans == null) return ssb;

            for (int i = 0; i < spans.length(); i++) {
                JSONObject s = spans.getJSONObject(i);
                int start = s.getInt("start");
                int end = s.getInt("end");
                if (start < 0 || end > ssb.length() || start >= end) continue;

                String type = s.getString("type");
                switch (type) {
                    case "bold":
                        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "italic":
                        ssb.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "bolditalic":
                        ssb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "underline":
                        ssb.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "strikethrough":
                        ssb.setSpan(new StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "fgcolor":
                        ssb.setSpan(new ForegroundColorSpan(s.getInt("color")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "bgcolor":
                        ssb.setSpan(new BackgroundColorSpan(s.getInt("color")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "size":
                        ssb.setSpan(new RelativeSizeSpan((float) s.getDouble("size")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "font":
                        ssb.setSpan(new TypefaceSpan(s.getString("font")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case "bullet":
                        ssb.setSpan(new BulletSpan(16), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ssb;
    }
}
