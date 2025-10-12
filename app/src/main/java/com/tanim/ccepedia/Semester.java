package com.tanim.ccepedia;

public class Semester {
    private final String title;
    private final String id;
    private final String status;

    public Semester(String title, String id, String status) {
        this.title = title;
        this.id = id;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getNumber() {
        String[] parts = title.split(" ");
        return parts.length > 1 ? parts[1] : "N/A";
    }
}