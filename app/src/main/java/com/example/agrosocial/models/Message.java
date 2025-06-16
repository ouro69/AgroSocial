package com.example.agrosocial.models;

public class Message {
    private String id;
    private String senderId;
    private String content;
    private String createdAt;

    public Message(String id, String senderId, String content, String createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
