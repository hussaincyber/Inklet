package com.inklet.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.*;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.inklet.app.R;
import com.inklet.app.models.Note;
import com.inklet.app.utils.DisplayUtils;
import com.inklet.app.utils.NoteStorage;
import com.inklet.app.utils.ResizableImageSpan;
import com.inklet.app.utils.RichTextSerializer;
import com.inklet.app.utils.ThemeManager;

import java.io.*;
import java.util.function.Supplier;

public class EditorActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private TextView tvSaveStatus;
    private TextView tvFormatFeedback;
    private Note currentNote;
    private final Handler autoSaveHandler = new Handler();
    private Runnable autoSaveRunnable;
    private final Handler feedbackHandler = new Handler();
    private Runnable feedbackHideRunnable;

    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_PERMISSION = 102;
    private static final int MAX_SOURCE_IMAGE_DIM = 2000;
    private int listCounter = 0;

    // Theme color sets
    private static final int D_BG      = 0xFF111111;
    private static final int D_TOOLBAR = 0xFF0D0D0D;
    private static final int D_TEXT    = 0xFFF0F0F0;
    private static final int D_HINT    = 0xFF555555;
    private static final int D_DIVIDER = 0xFF2A2A2A;
    private static final int D_FORMAT  = 0xFF161616;
    private static final int D_CODE_BG = 0xFF2A2A2A;

    private static final int L_BG      = 0xFFFFFDF8;
    private static final int L_TOOLBAR = 0xFFFDF6EC;
    private static final int L_TEXT    = 0xFF1A1008;
    private static final int L_HINT    = 0xFFBBAA88;
    private static final int L_DIVIDER = 0xFFE8D5B7;
    private static final int L_FORMAT  = 0xFFFDF6EC;
    private static final int L_CODE_BG = 0xFFEFE6D8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ThemeManager.isDarkMode(this)) setTheme(R.style.Theme_Inklet_Dark);
        else setTheme(R.style.Theme_Inklet);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        tvSaveStatus = findViewById(R.id.tv_save_status);
        tvFormatFeedback = findViewById(R.id.tv_format_feedback);

        applyTheme();
        setupToolbar();
        setupFormatButtons();
        setupImageTapToResize();
        DisplayUtils.applyResponsiveWidth(this, findViewById(R.id.editor_content));

        String noteId = getIntent().getStringExtra("note_id");
        if (noteId != null) {
            currentNote = NoteStorage.loadNote(this, noteId);
            if (currentNote != null) {
                etTitle.setText(currentNote.getTitle());
                SpannableStringBuilder ssb = RichTextSerializer.fromJson(currentNote.getContentJson());
                etContent.setText(ssb);
                etContent.setSelection(ssb.length());
            }
        } else {
            currentNote = new Note();
            currentNote.setId(NoteStorage.generateId());
            currentNote.setCreatedAt(System.currentTimeMillis());
        }

        setupAutoSave();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayUtils.applyResponsiveWidth(this, findViewById(R.id.editor_content));
    }

    private void applyTheme() {
        boolean dark = ThemeManager.isDarkMode(this);
        int bg      = dark ? D_BG      : L_BG;
        int toolbar = dark ? D_TOOLBAR : L_TOOLBAR;
        int text    = dark ? D_TEXT    : L_TEXT;
        int hint    = dark ? D_HINT    : L_HINT;
        int divider = dark ? D_DIVIDER : L_DIVIDER;
        int format  = dark ? D_FORMAT  : L_FORMAT;

        View root        = findViewById(R.id.root_editor);
        View editorCont  = findViewById(R.id.editor_content);
        View editorToolbar = findViewById(R.id.editor_toolbar);
        View formatScroll = findViewById(R.id.format_scroll);

        root.setBackgroundColor(bg);
        editorCont.setBackgroundColor(bg);
        editorToolbar.setBackgroundColor(toolbar);
        formatScroll.setBackgroundColor(format);
        etContent.setBackgroundColor(bg);
        etTitle.setTextColor(text);
        etTitle.setHintTextColor(divider);
        etContent.setTextColor(text);
        etContent.setHintTextColor(hint);
        tvSaveStatus.setTextColor(hint);

        // tint all toolbar/format icons
        int[] toolbarIconIds = {R.id.btn_back, R.id.btn_save, R.id.btn_delete};
        for (int id : toolbarIconIds) {
            ImageButton btn = findViewById(id);
            if (btn != null) btn.setColorFilter(text);
        }
        int[] fmtIds = {R.id.btn_bold, R.id.btn_italic, R.id.btn_underline, R.id.btn_strike,
                R.id.btn_size, R.id.btn_code_block, R.id.btn_text_color, R.id.btn_bg_color,
                R.id.btn_bullet, R.id.btn_number, R.id.btn_image, R.id.btn_clear_format};
        for (int id : fmtIds) {
            ImageButton btn = findViewById(id);
            if (btn != null) btn.setColorFilter(text);
        }
    }

    private void setupToolbar() {
        findViewById(R.id.btn_back).setOnClickListener(v -> { saveNote(); finish(); });
        findViewById(R.id.btn_save).setOnClickListener(v -> { saveNote(); showSaveStatus("Saved"); });
        findViewById(R.id.btn_delete).setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Delete this note? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    if (currentNote.getCreatedAt() != 0) NoteStorage.deleteNote(this, currentNote.getId());
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show()
        );
    }

    private void setupFormatButtons() {
        findViewById(R.id.btn_bold).setOnClickListener(v -> toggleBoldItalic(true));
        findViewById(R.id.btn_italic).setOnClickListener(v -> toggleBoldItalic(false));
        findViewById(R.id.btn_underline).setOnClickListener(v ->
                toggleSimpleSpan(UnderlineSpan.class, UnderlineSpan::new, "Underline"));
        findViewById(R.id.btn_strike).setOnClickListener(v ->
                toggleSimpleSpan(StrikethroughSpan.class, StrikethroughSpan::new, "Strikethrough"));
        findViewById(R.id.btn_size).setOnClickListener(v -> showFontSizeDialog());
        findViewById(R.id.btn_code_block).setOnClickListener(v -> insertCodeBlock());
        findViewById(R.id.btn_text_color).setOnClickListener(v -> showColorDialog(false));
        findViewById(R.id.btn_bg_color).setOnClickListener(v -> showColorDialog(true));
        findViewById(R.id.btn_bullet).setOnClickListener(v -> insertBullet());
        findViewById(R.id.btn_number).setOnClickListener(v -> insertNumbered());
        findViewById(R.id.btn_image).setOnClickListener(v -> pickImage());
        findViewById(R.id.btn_clear_format).setOnClickListener(v -> clearFormatting());
    }

    // ---- Formatting with real on/off feedback ----

    private void toggleBoldItalic(boolean toggleBold) {
        int s = etContent.getSelectionStart(), e = etContent.getSelectionEnd();
        String label = toggleBold ? "Bold" : "Italic";
        if (s == e) { showFormatFeedback(label + ": select text first"); return; }

        Editable ed = etContent.getText();
        boolean curBold = false, curItalic = false;
        for (StyleSpan sp : ed.getSpans(s, e, StyleSpan.class)) {
            int ss = ed.getSpanStart(sp), se = ed.getSpanEnd(sp);
            if (ss <= s && se >= e) {
                int style = sp.getStyle();
                if (style == Typeface.BOLD) curBold = true;
                else if (style == Typeface.ITALIC) curItalic = true;
                else if (style == Typeface.BOLD_ITALIC) { curBold = true; curItalic = true; }
            }
        }
        for (StyleSpan sp : ed.getSpans(s, e, StyleSpan.class)) ed.removeSpan(sp);

        boolean newBold   = toggleBold ? !curBold   : curBold;
        boolean newItalic = toggleBold ? curItalic  : !curItalic;

        int styleToApply = -1;
        if (newBold && newItalic) styleToApply = Typeface.BOLD_ITALIC;
        else if (newBold) styleToApply = Typeface.BOLD;
        else if (newItalic) styleToApply = Typeface.ITALIC;

        if (styleToApply != -1) ed.setSpan(new StyleSpan(styleToApply), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        boolean nowOn = toggleBold ? newBold : newItalic;
        showFormatFeedback(label + ": " + (nowOn ? "On" : "Off"));
    }

    private <T extends CharacterStyle> void toggleSimpleSpan(Class<T> cls, Supplier<T> factory, String label) {
        int s = etContent.getSelectionStart(), e = etContent.getSelectionEnd();
        if (s == e) { showFormatFeedback(label + ": select text first"); return; }

        Editable ed = etContent.getText();
        T[] existing = ed.getSpans(s, e, cls);
        boolean covers = false;
        for (T sp : existing) {
            int ss = ed.getSpanStart(sp), se = ed.getSpanEnd(sp);
            if (ss <= s && se >= e) covers = true;
            ed.removeSpan(sp);
        }
        if (!covers) ed.setSpan(factory.get(), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        showFormatFeedback(label + ": " + (covers ? "Off" : "On"));
    }

    private void showFontSizeDialog() {
        int s = etContent.getSelectionStart(), e = etContent.getSelectionEnd();
        if (s == e) { showFormatFeedback("Size: select text first"); return; }

        String[] sizes = {"Small (0.75x)", "Normal (1.0x)", "Large (1.25x)", "X-Large (1.5x)", "XX-Large (2.0x)"};
        float[] vals   = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        new AlertDialog.Builder(this).setTitle("Font Size")
            .setItems(sizes, (d, w) -> {
                // Remove any existing size spans in this exact range first so sizes
                // replace each other instead of compounding (this was the bug where
                // repeated size changes made text balloon or shrink unpredictably).
                Editable ed = etContent.getText();
                for (RelativeSizeSpan sp : ed.getSpans(s, e, RelativeSizeSpan.class)) ed.removeSpan(sp);
                ed.setSpan(new RelativeSizeSpan(vals[w]), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                showFormatFeedback("Size: " + sizes[w]);
            }).show();
    }

    private void showColorDialog(boolean isBg) {
        int s = etContent.getSelectionStart(), e = etContent.getSelectionEnd();
        String label = isBg ? "Highlight" : "Text color";
        if (s == e) { showFormatFeedback(label + ": select text first"); return; }

        int[] colors = {
            Color.BLACK, Color.WHITE, Color.GRAY, Color.DKGRAY,
            Color.RED,   Color.parseColor("#FF6B6B"),
            Color.BLUE,  Color.parseColor("#4ECDC4"),
            Color.GREEN, Color.parseColor("#95E1D3"),
            Color.YELLOW,Color.parseColor("#F38181"),
            Color.parseColor("#AA96DA"), Color.parseColor("#FFAAA5")
        };
        String[] names = {"Black","White","Gray","Dark Gray","Red","Coral","Blue","Teal","Green","Mint","Yellow","Salmon","Lavender","Pink"};
        new AlertDialog.Builder(this).setTitle(isBg ? "Highlight Color" : "Text Color")
            .setItems(names, (d, w) -> {
                Editable ed = etContent.getText();
                if (isBg) {
                    for (BackgroundColorSpan sp : ed.getSpans(s, e, BackgroundColorSpan.class)) ed.removeSpan(sp);
                    ed.setSpan(new BackgroundColorSpan(colors[w]), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    for (ForegroundColorSpan sp : ed.getSpans(s, e, ForegroundColorSpan.class)) ed.removeSpan(sp);
                    ed.setSpan(new ForegroundColorSpan(colors[w]), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                showFormatFeedback(label + ": " + names[w]);
            }).show();
    }

    private void insertBullet() {
        listCounter = 0;
        int cursor = etContent.getSelectionStart();
        Editable e = etContent.getText();
        int lineStart = cursor;
        while (lineStart > 0 && e.charAt(lineStart - 1) != '\n') lineStart--;
        int lineEnd = e.toString().indexOf('\n', cursor);
        if (lineEnd == -1) lineEnd = e.length();
        e.setSpan(new BulletSpan(20, Color.BLACK), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        etContent.setSelection(lineEnd);
        showFormatFeedback("Bullet added");
    }

    private void insertNumbered() {
        listCounter++;
        int cursor = etContent.getSelectionStart();
        Editable e = etContent.getText();
        int lineStart = cursor;
        while (lineStart > 0 && e.charAt(lineStart - 1) != '\n') lineStart--;
        String prefix = listCounter + ". ";
        e.insert(lineStart, prefix);
        etContent.setSelection(lineStart + prefix.length());
        showFormatFeedback("Numbered item added");
    }

    /** Repurposes what used to be the font-family picker: inserts/wraps a monospace, highlighted code block. */
    private void insertCodeBlock() {
        Editable ed = etContent.getText();
        int s = etContent.getSelectionStart(), e = etContent.getSelectionEnd();
        boolean dark = ThemeManager.isDarkMode(this);
        int codeBg = dark ? D_CODE_BG : L_CODE_BG;

        if (s == e) {
            String placeholder = "code";
            ed.insert(s, placeholder);
            int end = s + placeholder.length();
            applyCodeStyle(ed, s, end, codeBg);
            etContent.setSelection(s, end);
            showFormatFeedback("Code block inserted");
        } else {
            applyCodeStyle(ed, s, e, codeBg);
            etContent.setSelection(e);
            showFormatFeedback("Code block applied");
        }
    }

    private void applyCodeStyle(Editable ed, int s, int e, int bgColor) {
        for (TypefaceSpan sp : ed.getSpans(s, e, TypefaceSpan.class)) ed.removeSpan(sp);
        for (BackgroundColorSpan sp : ed.getSpans(s, e, BackgroundColorSpan.class)) ed.removeSpan(sp);
        ed.setSpan(new TypefaceSpan("monospace"), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ed.setSpan(new BackgroundColorSpan(bgColor), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void clearFormatting() {
        int s = etContent.getSelectionStart(), e = etContent.getSelectionEnd();
        if (s == e) { showFormatFeedback("Clear format: select text first"); return; }
        for (Object span : etContent.getText().getSpans(s, e, Object.class))
            if (!(span instanceof ImageSpan)) etContent.getText().removeSpan(span);
        showFormatFeedback("Formatting cleared");
    }

    // ---- Images: pick, insert (full-res source kept on disk), tap-to-resize ----

    private void pickImage() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{perm}, REQUEST_PERMISSION);
            return;
        }
        startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQUEST_IMAGE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQUEST_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) pickImage();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQUEST_IMAGE_PICK || res != RESULT_OK || data == null) return;
        try {
            Uri uri = data.getData();
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            if (original == null) throw new IOException("Could not decode picked image");

            // Keep a reasonably high-resolution source on disk (not a tiny pre-shrunk
            // copy) so later resizes have real detail to re-sample from.
            Bitmap source = original;
            int ow = original.getWidth(), oh = original.getHeight();
            if (Math.max(ow, oh) > MAX_SOURCE_IMAGE_DIM) {
                float scale = MAX_SOURCE_IMAGE_DIM / (float) Math.max(ow, oh);
                source = Bitmap.createScaledBitmap(original, Math.round(ow * scale), Math.round(oh * scale), true);
            }

            String imgDir = NoteStorage.getImagesDir(this);
            String imgName = "img_" + System.currentTimeMillis() + ".jpg";
            File imgFile = new File(imgDir, imgName);
            try (FileOutputStream fos = new FileOutputStream(imgFile)) {
                source.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }

            int contentWidth = etContent.getWidth() > 0
                    ? etContent.getWidth() - etContent.getPaddingStart() - etContent.getPaddingEnd()
                    : dpToPx(320);
            int initialWidth = Math.min(source.getWidth(), Math.round(contentWidth * 0.92f));
            insertImageAtCursor(imgFile.getAbsolutePath(), initialWidth);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Failed to insert image", Toast.LENGTH_SHORT).show();
        }
    }

    private void insertImageAtCursor(String path, int displayWidth) {
        ResizableImageSpan span = ResizableImageSpan.create(path, displayWidth);
        if (span == null) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            return;
        }
        int cursor = etContent.getSelectionStart();
        if (cursor < 0) cursor = etContent.getText().length();
        Editable editable = etContent.getText();
        editable.insert(cursor, " ");
        editable.setSpan(span, cursor, cursor + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        etContent.setSelection(cursor + 1);
        showFormatFeedback("Image inserted \u2014 tap it to resize");
    }

    private void setupImageTapToResize() {
        etContent.setOnClickListener(v -> {
            Editable ed = etContent.getText();
            int offset = etContent.getSelectionStart();
            int from = Math.max(0, offset - 1);
            int to = Math.min(ed.length(), offset + 1);
            ResizableImageSpan[] spans = ed.getSpans(from, to, ResizableImageSpan.class);
            if (spans.length > 0) showResizeDialog(spans[0]);
        });
    }

    private void showResizeDialog(ResizableImageSpan span) {
        Editable ed = etContent.getText();
        int start = ed.getSpanStart(span);
        int end = ed.getSpanEnd(span);
        if (start < 0 || end < 0) return;
        String path = span.getImagePath();

        String[] labels = {"Small", "Medium", "Large", "Full width"};
        float[] fractions = {0.35f, 0.55f, 0.8f, 1.0f};
        new AlertDialog.Builder(this).setTitle("Resize Image")
            .setItems(labels, (d, w) -> {
                int contentWidth = etContent.getWidth() > 0
                        ? etContent.getWidth() - etContent.getPaddingStart() - etContent.getPaddingEnd()
                        : dpToPx(320);
                int newWidth = Math.round(contentWidth * fractions[w]);
                ResizableImageSpan newSpan = ResizableImageSpan.create(path, newWidth);
                if (newSpan == null) return;
                ed.removeSpan(span);
                ed.setSpan(newSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                etContent.invalidate();
                showFormatFeedback("Image resized: " + labels[w]);
            }).show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ---- Feedback pill ----

    private void showFormatFeedback(String msg) {
        if (tvFormatFeedback == null) return;
        tvFormatFeedback.setText(msg);
        tvFormatFeedback.setVisibility(View.VISIBLE);
        feedbackHandler.removeCallbacks(feedbackHideRunnable);
        feedbackHandler.postDelayed(feedbackHideRunnable = () -> tvFormatFeedback.setVisibility(View.GONE), 1300);
    }

    // ---- Autosave / persistence ----

    private void setupAutoSave() {
        etContent.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                autoSaveHandler.removeCallbacks(autoSaveRunnable);
                autoSaveHandler.postDelayed(autoSaveRunnable = () -> {
                    saveNote(); showSaveStatus("Auto-saved");
                }, 2000);
            }
        });
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            String c = etContent.getText().toString();
            title = c.isEmpty() ? "Untitled" : c.substring(0, Math.min(40, c.length()));
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(etContent.getText());
        String contentJson = RichTextSerializer.toJson(ssb);
        String preview = etContent.getText().toString().substring(0, Math.min(100, etContent.getText().length()));
        currentNote.setTitle(title);
        currentNote.setContentJson(contentJson);
        currentNote.setUpdatedAt(System.currentTimeMillis());
        currentNote.setPreviewText(preview);
        NoteStorage.saveNote(this, currentNote);
    }

    private void showSaveStatus(String msg) {
        tvSaveStatus.setText(msg);
        tvSaveStatus.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> tvSaveStatus.setVisibility(View.GONE), 1500);
    }

    @Override protected void onPause() { super.onPause(); saveNote(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        feedbackHandler.removeCallbacks(feedbackHideRunnable);
    }
}
