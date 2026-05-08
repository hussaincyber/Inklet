package com.inklet.app.models;

import java.io.Serializable;

public class Note implements Serializable {
    private String id;
    private String title;
    private String contentJson; // stores SpannableString as JSON
    private long createdAt;
    private long updatedAt;
    private String previewText;

    public Note() {}

    public Note(String id, String title, String contentJson, long createdAt, long updatedAt, String previewText) {
        this.id = id;
        this.title = title;
        this.contentJson = contentJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.previewText = previewText;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getPreviewText() { return previewText; }
    public void setPreviewText(String previewText) { this.previewText = previewText; }
}
