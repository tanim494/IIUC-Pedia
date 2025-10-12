package com.tanim.ccepedia;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class CommunityMessage {
    private String userEmail;
    private String userStudentId;
    private String userName;
    private String messageText;
    private Date timestamp;

    public CommunityMessage() {
    }

    public CommunityMessage(String userStudentId, String userEmail, String userName, String messageText) {
        this.userStudentId = userStudentId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.messageText = messageText;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserStudentId() {
        return userStudentId;
    }

    public String getUserName() {
        return userName;
    }

    public String getMessageText() {
        return messageText;
    }

    @ServerTimestamp
    public Date getTimestamp() { return timestamp; }

    public void setUserStudentId(String userStudentId) {
        this.userStudentId = userStudentId;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}