package com.inklet.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

/**
 * An ImageSpan that remembers where its source file lives on disk and what size
 * it's currently displayed at, so notes can persist resized images and re-render
 * them later without stretching an already-shrunk bitmap.
 *
 * Resizing always re-decodes from the original source file at the new target
 * width (with an appropriate inSampleSize), rather than scaling a previously
 * cached, possibly-downscaled copy. That means repeated resizes don't compound
 * quality loss the way a normal raster resize chain would - the only ceiling
 * on sharpness is the resolution of the original photo itself.
 */
public class ResizableImageSpan extends ImageSpan {

    private final String imagePath;
    private final int displayWidth;
    private final int displayHeight;

    public ResizableImageSpan(Drawable drawable, String imagePath, int displayWidth, int displayHeight) {
        super(drawable);
        this.imagePath = imagePath;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
    }

    public String getImagePath() { return imagePath; }
    public int getDisplayWidth() { return displayWidth; }
    public int getDisplayHeight() { return displayHeight; }

    /**
     * Decodes the source file at (approximately) the requested display width,
     * preserving aspect ratio. Always re-samples from the original file.
     */
    public static Bitmap decodeAtWidth(String path, int targetWidth) {
        if (targetWidth <= 0) targetWidth = 1;

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int targetHeight = Math.max(1, Math.round(targetWidth * (bounds.outHeight / (float) bounds.outWidth)));

        int sample = 1;
        while ((bounds.outWidth / (sample * 2)) >= targetWidth) sample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap decoded = BitmapFactory.decodeFile(path, opts);
        if (decoded == null) return null;

        if (decoded.getWidth() == targetWidth && decoded.getHeight() == targetHeight) return decoded;
        return Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true);
    }

    /** Builds a ready-to-use span for the given source file at the requested display width. */
    public static ResizableImageSpan create(String path, int targetWidth) {
        Bitmap bmp = decodeAtWidth(path, targetWidth);
        if (bmp == null) return null;
        Drawable d = new BitmapDrawable(bmp);
        d.setBounds(0, 0, bmp.getWidth(), bmp.getHeight());
        return new ResizableImageSpan(d, path, bmp.getWidth(), bmp.getHeight());
    }
}
