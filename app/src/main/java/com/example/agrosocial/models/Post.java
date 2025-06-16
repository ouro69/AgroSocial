package com.example.agrosocial.models;

public class Post {
    private String id;
    private String content;
    private String timestamp;
    private String username;

    public Post(String id, String content, String timestamp, String username) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }
}