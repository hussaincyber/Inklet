package com.inklet.app.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;

import com.inklet.app.models.Note;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NoteStorage {

    private static final String NOTES_DIR = "inklet_notes";
    private static final String INDEX_FILE = "notes_index.json";

    private static File getNotesDir(Context context) {
        File dir = new File(context.getFilesDir(), NOTES_DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static List<Note> loadAllNotes(Context context) {
        List<Note> notes = new ArrayList<>();
        try {
            File indexFile = new File(getNotesDir(context), INDEX_FILE);
            if (!indexFile.exists()) return notes;

            String json = readFile(indexFile);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Note note = new Note(
                    obj.getString("id"),
                    obj.getString("title"),
                    obj.optString("contentJson", ""),
                    obj.getLong("createdAt"),
                    obj.getLong("updatedAt"),
                    obj.optString("previewText", "")
                );
                notes.add(note);
            }
            notes.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return notes;
    }

    public static void saveNote(Context context, Note note) {
        try {
            // Load full content for the note content file
            File noteFile = new File(getNotesDir(context), note.getId() + ".json");
            JSONObject obj = new JSONObject();
            obj.put("id", note.getId());
            obj.put("title", note.getTitle());
            obj.put("contentJson", note.getContentJson());
            obj.put("createdAt", note.getCreatedAt());
            obj.put("updatedAt", note.getUpdatedAt());
            obj.put("previewText", note.getPreviewText());
            writeFile(noteFile, obj.toString());

            // Update index
            updateIndex(context, note);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Note loadNote(Context context, String noteId) {
        try {
            File noteFile = new File(getNotesDir(context), noteId + ".json");
            if (!noteFile.exists()) return null;
            String json = readFile(noteFile);
            JSONObject obj = new JSONObject(json);
            return new Note(
                obj.getString("id"),
                obj.getString("title"),
                obj.optString("contentJson", ""),
                obj.getLong("createdAt"),
                obj.getLong("updatedAt"),
                obj.optString("previewText", "")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deleteNote(Context context, String noteId) {
        try {
            File noteFile = new File(getNotesDir(context), noteId + ".json");
            noteFile.delete();

            // Remove from index
            List<Note> notes = loadAllNotes(context);
            notes.removeIf(n -> n.getId().equals(noteId));
            saveIndex(context, notes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateIndex(Context context, Note updatedNote) {
        try {
            List<Note> notes = loadAllNotes(context);
            boolean found = false;
            for (int i = 0; i < notes.size(); i++) {
                if (notes.get(i).getId().equals(updatedNote.getId())) {
                    notes.set(i, updatedNote);
                    found = true;
                    break;
                }
            }
            if (!found) notes.add(updatedNote);
            saveIndex(context, notes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveIndex(Context context, List<Note> notes) {
        try {
            JSONArray array = new JSONArray();
            for (Note note : notes) {
                JSONObject obj = new JSONObject();
                obj.put("id", note.getId());
                obj.put("title", note.getTitle());
                obj.put("contentJson", note.getContentJson());
                obj.put("createdAt", note.getCreatedAt());
                obj.put("updatedAt", note.getUpdatedAt());
                obj.put("previewText", note.getPreviewText());
                array.put(obj);
            }
            File indexFile = new File(getNotesDir(context), INDEX_FILE);
            writeFile(indexFile, array.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String generateId() {
        return "note_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 9999);
    }

    public static String getImagesDir(Context context) {
        File imgDir = new File(getNotesDir(context), "images");
        if (!imgDir.exists()) imgDir.mkdirs();
        return imgDir.getAbsolutePath();
    }

    private static String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static void writeFile(File file, String content) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}
