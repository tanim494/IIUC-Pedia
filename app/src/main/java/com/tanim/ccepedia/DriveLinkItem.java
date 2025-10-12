package com.tanim.ccepedia;

public class DriveLinkItem {
    private String id;
    private String title;
    private String url;

    public DriveLinkItem() {
    }

    public DriveLinkItem(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
