package com.inklet.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.inklet.app.utils.NoteStorage;
import com.inklet.app.utils.RichTextSerializer;
import com.inklet.app.utils.ThemeManager;

import java.io.*;

public class EditorActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private TextView tvSaveStatus;
    private Note currentNote;
    private final Handler autoSaveHandler = new Handler();
    private Runnable autoSaveRunnable;

    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_PERMISSION = 102;
    private int listCounter = 0;

    // Theme color sets
    private static final int D_BG      = 0xFF111111;
    private static final int D_TOOLBAR = 0xFF0D0D0D;
    private static final int D_TEXT    = 0xFFF0F0F0;
    private static final int D_HINT    = 0xFF555555;
    private static final int D_DIVIDER = 0xFF2A2A2A;
    private static final int D_FORMAT  = 0xFF161616;

    private static final int L_BG      = 0xFFFFFDF8;
    private static final int L_TOOLBAR = 0xFFFDF6EC;
    private static final int L_TEXT    = 0xFF1A1008;
    private static final int L_HINT    = 0xFFBBAA88;
    private static final int L_DIVIDER = 0xFFE8D5B7;
    private static final int L_FORMAT  = 0xFFFDF6EC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ThemeManager.isDarkMode(this)) setTheme(R.style.Theme_Inklet_Dark);
        else setTheme(R.style.Theme_Inklet);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        tvSaveStatus = findViewById(R.id.tv_save_status);

        applyTheme();
        setupToolbar();
        setupFormatButtons();

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
        ImageView woodLeft  = findViewById(R.id.wood_left);
        ImageView woodRight = findViewById(R.id.wood_right);

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
                R.id.btn_size, R.id.btn_font, R.id.btn_text_color, R.id.btn_bg_color,
                R.id.btn_bullet, R.id.btn_number, R.id.btn_image, R.id.btn_clear_format};
        for (int id : fmtIds) {
            ImageButton btn = findViewById(id);
            if (btn != null) btn.setColorFilter(text);
        }

        // Wood panels: light only
        woodLeft.setVisibility(dark ? View.GONE : View.VISIBLE);
        woodRight.setVisibility(dark ? View.GONE : View.VISIBLE);
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
        findViewById(R.id.btn_bold).setOnClickListener(v -> applyStyleSpan(Typeface.BOLD));
        findViewById(R.id.btn_italic).setOnClickListener(v -> applyStyleSpan(Typeface.ITALIC));
        findViewById(R.id.btn_underline).setOnClickListener(v -> applySpan(new UnderlineSpan()));
        findViewById(R.id.btn_strike).setOnClickListener(v -> applySpan(new StrikethroughSpan()));
        findViewById(R.id.btn_size).setOnClickListener(v -> showFontSizeDialog());
        findViewById(R.id.btn_font).setOnClickListener(v -> showFontDialog());
        findViewById(R.id.btn_text_color).setOnClickListener(v -> showColorDialog(false));
        findViewById(R.id.btn_bg_color).setOnClickListener(v -> showColorDialog(true));
        findViewById(R.id.btn_bullet).setOnClickListener(v -> insertBullet());
        findViewById(R.id.btn_number).setOnClickListener(v -> insertNumbered());
        findViewById(R.id.btn_image).setOnClickListener(v -> pickImage());
        findViewById(R.id.btn_clear_format).setOnClickListener(v -> clearFormatting());
    }

    private void applyStyleSpan(int style) {
        int s = etContent.getSelectionStart(), e = etContent.getSelectionEnd();
        if (s == e) return;
        etContent.getText().setSpan(new StyleSpan(style), s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void applySpan(Object span) {
        int s = etContent.getSelectionStart(), e = etContent.getSelectionEnd();
        if (s == e) return;
        etContent.getText().setSpan(span, s, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void showFontSizeDialog() {
        String[] sizes = {"Small (0.75x)", "Normal (1.0x)", "Large (1.25x)", "X-Large (1.5x)", "XX-Large (2.0x)"};
        float[] vals   = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        new AlertDialog.Builder(this).setTitle("Font Size")
            .setItems(sizes, (d, w) -> applySpan(new RelativeSizeSpan(vals[w]))).show();
    }

    private void showFontDialog() {
        // Arial maps to sans-serif on Android. Additional choices available.
        String[] fonts  = {"sans-serif", "serif", "monospace", "sans-serif-condensed", "sans-serif-medium", "serif-monospace"};
        String[] labels = {"Arial / Sans-Serif", "Serif", "Monospace", "Condensed", "Medium", "Serif Mono"};
        new AlertDialog.Builder(this).setTitle("Font Family")
            .setItems(labels, (d, w) -> applySpan(new TypefaceSpan(fonts[w]))).show();
    }

    private void showColorDialog(boolean isBg) {
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
                if (isBg) applySpan(new BackgroundColorSpan(colors[w]));
                else      applySpan(new ForegroundColorSpan(colors[w]));
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
    }

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
            Bitmap bmp = BitmapFactory.decodeStream(is);

            String imgDir = NoteStorage.getImagesDir(this);
            String imgName = "img_" + System.currentTimeMillis() + ".jpg";
            File imgFile = new File(imgDir, imgName);
            FileOutputStream fos = new FileOutputStream(imgFile);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();

            int maxW = 600;
            float scale = (float) maxW / bmp.getWidth();
            int h = (int)(bmp.getHeight() * scale);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, maxW, h, true);

            int cursor = etContent.getSelectionStart();
            Editable editable = etContent.getText();
            editable.insert(cursor, " ");

            android.graphics.drawable.Drawable d = new android.graphics.drawable.BitmapDrawable(getResources(), scaled);
            d.setBounds(0, 0, scaled.getWidth(), scaled.getHeight());
            editable.setSpan(new ImageSpan(d, imgFile.getAbsolutePath()), cursor, cursor + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            etContent.setSelection(cursor + 1);
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Failed to insert image", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFormatting() {
        int s = etContent.getSelectionStart(), e = etContent.getSelectionEnd();
        if (s == e) return;
        for (Object span : etContent.getText().getSpans(s, e, Object.class))
            if (!(span instanceof ImageSpan)) etContent.getText().removeSpan(span);
    }

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
    @Override protected void onDestroy() { super.onDestroy(); autoSaveHandler.removeCallbacks(autoSaveRunnable); }
}
