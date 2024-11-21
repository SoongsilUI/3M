package com.project.ecomap;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class Question implements Serializable {

    private String questionId, qTimeStampString;
    private String author, content, imageUrl, title;
    private Timestamp qTimestamp;

    public Question(){}
    public Question(String questionId, String author, String content, String imageUrl, String qTimeStampString, String title, Timestamp timestamp) {
        this.author = author;
        this.content = content;
        this.imageUrl = imageUrl;
        this.qTimestamp = timestamp;
        this.qTimeStampString = qTimeStampString;
        this.questionId = questionId;
        this.title = title;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /*public String getQTimestampString() {
        return qTimeStampString;
    }

    public void setQTimestampString(Timestamp timestamp) {
        this.qTimestamp = timestamp;

    }*/

    public Timestamp getQTimestamp() {
        return qTimestamp;
    }

    public void setQTimestamp(Timestamp timestamp) {
        this.qTimestamp = timestamp;
    }

    public String getTimeStampString() {
        return qTimeStampString;
    }

    public void setTimeStampString(String qTimeStampString) {
        this.qTimeStampString = qTimeStampString;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}