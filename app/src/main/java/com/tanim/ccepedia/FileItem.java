package com.tanim.ccepedia;

public class FileItem {
    private final String id;
    private final String fileName;
    private final String url;
    private final String uploader;
    private String uploaderStudentId;

    public FileItem(String id, String fileName, String url, String uploader) {
        this.id = id;
        this.fileName = fileName;
        this.url = url;
        this.uploader = uploader;
    }

    public String getId() { return id; }
    public String getFileName() { return fileName; }
    public String getUrl() { return url; }
    public String getUploader() { return uploader; }

    public String getUploaderStudentId() {
        return uploaderStudentId;
    }

    public void setUploaderStudentId(String uploaderStudentId) {
        this.uploaderStudentId = uploaderStudentId;
    }
}