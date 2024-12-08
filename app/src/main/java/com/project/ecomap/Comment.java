package com.project.ecomap;

import com.google.firebase.Timestamp;

public class Comment {

    String cQuestionId, commenterId, comment, cProfileImageUrl;
    Timestamp cTimestamp;

    public Comment() {}

    public Comment(String cQuestionId, String commenterId, String comment, String cProfileImageUrl, Timestamp cTimestamp) {
        this.cQuestionId = cQuestionId;
        this.commenterId = commenterId;
        this.comment = comment;
        this.cProfileImageUrl = cProfileImageUrl;
        this.cTimestamp = cTimestamp;
    }

    public String getQuestionId() {
        return cQuestionId;
    }
    public void setQuestionId(String questionId) {
        this.cQuestionId = questionId;
    }

    public String getCommenterId() {
        return commenterId;
    }
    public void setCommenterId(String commenterId) {
        this.commenterId = commenterId;
    }

    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCProfileImageUrl() {
        return cProfileImageUrl;
    }
    public void setCProfileImageUrl(String profileImageUrl) {

        this.cProfileImageUrl = profileImageUrl;
    }

    public Timestamp getCTimestamp() {
        return cTimestamp;
    }
    public void setCTimestamp(Timestamp timestamp) {
        this.cTimestamp = timestamp;
    }
}