package com.inklet.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.inklet.app.R;
import com.inklet.app.models.Note;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteDelete(Note note);
    }

    private List<Note> notes;
    private final OnNoteClickListener listener;
    private final boolean darkMode;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public NoteAdapter(List<Note> notes, OnNoteClickListener listener, boolean darkMode) {
        this.notes = notes;
        this.listener = listener;
        this.darkMode = darkMode;
    }

    public void setNotes(List<Note> notes) { this.notes = notes; notifyDataSetChanged(); }

    @NonNull @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder h, int position) {
        Note note = notes.get(position);
        h.tvTitle.setText(note.getTitle().isEmpty() ? "Untitled" : note.getTitle());
        h.tvPreview.setText(note.getPreviewText().isEmpty() ? "Empty note" : note.getPreviewText());
        h.tvDate.setText(sdf.format(new Date(note.getUpdatedAt())));

        // Apply theme to card
        if (darkMode) {
            h.card.setCardBackgroundColor(Color.parseColor("#1C1C1C"));
            h.tvTitle.setTextColor(Color.parseColor("#F0F0F0"));
            h.tvPreview.setTextColor(Color.parseColor("#888888"));
            h.tvDate.setTextColor(Color.parseColor("#444444"));
            h.btnDelete.setColorFilter(Color.parseColor("#555555"));
        } else {
            h.card.setCardBackgroundColor(Color.parseColor("#FFFCF5"));
            h.tvTitle.setTextColor(Color.parseColor("#1A1008"));
            h.tvPreview.setTextColor(Color.parseColor("#6B4C2A"));
            h.tvDate.setTextColor(Color.parseColor("#C4A882"));
            h.btnDelete.setColorFilter(Color.parseColor("#B89878"));
        }

        h.itemView.setOnClickListener(v -> listener.onNoteClick(note));
        h.btnDelete.setOnClickListener(v -> listener.onNoteDelete(note));
    }

    @Override public int getItemCount() { return notes.size(); }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvTitle, tvPreview, tvDate;
        ImageButton btnDelete;
        NoteViewHolder(View view) {
            super(view);
            card      = view.findViewById(R.id.card_note);
            tvTitle   = view.findViewById(R.id.tv_note_title);
            tvPreview = view.findViewById(R.id.tv_note_preview);
            tvDate    = view.findViewById(R.id.tv_note_date);
            btnDelete = view.findViewById(R.id.btn_note_delete);
        }
    }
}
