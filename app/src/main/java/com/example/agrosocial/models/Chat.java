package com.example.agrosocial.models;

public class Chat {
    private String id;
    private String title; // например, имя собеседника
    private String lastMessageAt;

    public Chat(String id, String title, String lastMessageAt) {
        this.id = id;
        this.title = title;
        this.lastMessageAt = lastMessageAt;
    }

    // Геттеры
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getLastMessageAt() { return lastMessageAt; }
}

