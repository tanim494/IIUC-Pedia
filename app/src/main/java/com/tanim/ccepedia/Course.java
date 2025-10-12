package com.tanim.ccepedia;

public class Course {
    private final String id;
    private final String title;

    public Course(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() { return id; }

    public String getTitle() { return title; }
}