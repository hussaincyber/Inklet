package com.inklet.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.inklet.app.R;
import com.inklet.app.adapters.NoteAdapter;
import com.inklet.app.models.Note;
import com.inklet.app.utils.NoteStorage;
import com.inklet.app.utils.ProfileManager;
import com.inklet.app.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {

    private static final String NOTESHARE_URL = "https://inkletnoteshare.netlify.app";
    private static final String PREFS_BANNER   = "inklet_banner";
    private static final String KEY_BANNER_DISMISSED = "banner_dismissed";

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private List<Note> allNotes = new ArrayList<>();
    private TextView tvEmpty;

    private static final int D_BG       = 0xFF0D0D0D;
    private static final int D_TEXT     = 0xFFF0F0F0;
    private static final int D_SUB      = 0xFF888888;
    private static final int D_DIVIDER  = 0xFF2A2A2A;
    private static final int D_FAB_BG   = 0xFFFFFFFF;
    private static final int D_FAB_ICON = 0xFF0D0D0D;

    private static final int L_BG       = 0xFFFDF6EC;
    private static final int L_TEXT     = 0xFF1A1008;
    private static final int L_SUB      = 0xFF6B4C2A;
    private static final int L_DIVIDER  = 0xFFE8D5B7;
    private static final int L_FAB_BG   = 0xFF2C1A0E;
    private static final int L_FAB_ICON = 0xFFFDF6EC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ThemeManager.isDarkMode(this)) setTheme(R.style.Theme_Inklet_Dark);
        else setTheme(R.style.Theme_Inklet);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applyTheme();
        setupBanner();

        TextView tvGreeting = findViewById(R.id.tv_greeting);
        recyclerView        = findViewById(R.id.recycler_notes);
        tvEmpty             = findViewById(R.id.tv_empty);
        FloatingActionButton fab = findViewById(R.id.fab_new_note);
        EditText etSearch   = findViewById(R.id.et_search);
        ImageButton btnToggle = findViewById(R.id.btn_theme_toggle);
        ImageButton btnLink   = findViewById(R.id.btn_noteshare);

        String name = ProfileManager.getDisplayName(this);
        tvGreeting.setText("Hello, " + name + ".");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(new ArrayList<>(), this, ThemeManager.isDarkMode(this));
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> openEditor(null));

        btnToggle.setOnClickListener(v -> {
            ThemeManager.toggleTheme(this);
            recreate();
        });

        btnLink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(NOTESHARE_URL));
            startActivity(intent);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }
        });
    }

    private void setupBanner() {
        boolean dismissed = getSharedPreferences(PREFS_BANNER, MODE_PRIVATE)
                .getBoolean(KEY_BANNER_DISMISSED, false);

        View banner      = findViewById(R.id.banner_noteshare);
        View btnClose    = findViewById(R.id.btn_banner_close);
        View btnVisit    = findViewById(R.id.btn_banner_visit);

        if (dismissed || banner == null) {
            if (banner != null) banner.setVisibility(View.GONE);
            return;
        }

        banner.setVisibility(View.VISIBLE);

        btnClose.setOnClickListener(v -> {
            banner.setVisibility(View.GONE);
            getSharedPreferences(PREFS_BANNER, MODE_PRIVATE)
                    .edit().putBoolean(KEY_BANNER_DISMISSED, true).apply();
        });

        btnVisit.setOnClickListener(v -> {
            banner.setVisibility(View.GONE);
            getSharedPreferences(PREFS_BANNER, MODE_PRIVATE)
                    .edit().putBoolean(KEY_BANNER_DISMISSED, true).apply();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(NOTESHARE_URL)));
        });
    }

    private void applyTheme() {
        boolean dark = ThemeManager.isDarkMode(this);
        int bg      = dark ? D_BG      : L_BG;
        int text    = dark ? D_TEXT    : L_TEXT;
        int sub     = dark ? D_SUB     : L_SUB;
        int div     = dark ? D_DIVIDER : L_DIVIDER;
        int fabBg   = dark ? D_FAB_BG  : L_FAB_BG;
        int fabIco  = dark ? D_FAB_ICON: L_FAB_ICON;

        View root         = findViewById(R.id.root_main);
        LinearLayout cont = findViewById(R.id.main_content);
        View toolbar      = findViewById(R.id.toolbar);
        TextView tvLogo   = findViewById(R.id.tv_logo_main);
        TextView tvGreet  = findViewById(R.id.tv_greeting);
        EditText etSearch = findViewById(R.id.et_search);
        TextView tvEmpty  = findViewById(R.id.tv_empty);
        ImageButton btnTog  = findViewById(R.id.btn_theme_toggle);
        ImageButton btnLink = findViewById(R.id.btn_noteshare);
        FloatingActionButton fab = findViewById(R.id.fab_new_note);
        ImageView woodLeft  = findViewById(R.id.wood_left);
        ImageView woodRight = findViewById(R.id.wood_right);

        root.setBackgroundColor(bg);
        cont.setBackgroundColor(bg);
        toolbar.setBackgroundColor(bg);
        tvLogo.setTextColor(text);
        tvGreet.setTextColor(text);
        etSearch.setTextColor(text);
        etSearch.setHintTextColor(sub);
        etSearch.setBackgroundResource(dark ? R.drawable.bg_search_dark : R.drawable.bg_search_light);
        if (tvEmpty  != null) tvEmpty.setTextColor(sub);
        if (btnTog   != null) btnTog.setColorFilter(text);
        if (btnLink  != null) btnLink.setColorFilter(text);
        fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fabBg));
        fab.setColorFilter(fabIco);

        woodLeft.setVisibility(dark ? View.GONE : View.VISIBLE);
        woodRight.setVisibility(dark ? View.GONE : View.VISIBLE);

        // Banner theme
        View banner = findViewById(R.id.banner_noteshare);
        if (banner != null) {
            banner.setBackgroundColor(dark ? 0xFF1C1C1C : 0xFF2C1A0E);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        allNotes = NoteStorage.loadAllNotes(this);
        adapter.setNotes(allNotes);
        tvEmpty.setVisibility(allNotes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void filterNotes(String query) {
        if (query.isEmpty()) { adapter.setNotes(allNotes); return; }
        List<Note> filtered = new ArrayList<>();
        for (Note note : allNotes) {
            if (note.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                note.getPreviewText().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(note);
            }
        }
        adapter.setNotes(filtered);
    }

    private void openEditor(Note note) {
        Intent intent = new Intent(this, EditorActivity.class);
        if (note != null) intent.putExtra("note_id", note.getId());
        startActivity(intent);
    }

    @Override public void onNoteClick(Note note) { openEditor(note); }

    @Override
    public void onNoteDelete(Note note) {
        NoteStorage.deleteNote(this, note.getId());
        loadNotes();
    }
}
