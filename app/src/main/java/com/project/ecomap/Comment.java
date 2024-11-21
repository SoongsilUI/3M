package com.project.ecomap;

import com.google.firebase.Timestamp;

public class Comment {

    String cQuestionId, commenter, comment, cProfileImageURL;
    Timestamp cTimestamp;

    public Comment() {}

    public Comment(String cQuestionId, String commenter, String comment, String cProfileImageURL, Timestamp cTimestamp) {
        this.cQuestionId = cQuestionId;
        this.commenter = commenter;
        this.comment = comment;
        this.cProfileImageURL = cProfileImageURL;
        this.cTimestamp = cTimestamp;
    }

    public String getQuestionId() {
        return cQuestionId;
    }
    public void setQuestionId(String questionId) {
        this.cQuestionId = questionId;
    }

    public String getCommenter() {
        return commenter;
    }
    public void setCommenter(String commenter) {
        this.commenter = commenter;
    }

    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCProfileImageURL() {
        return cProfileImageURL;
    }
    public void setCProfileImageURL(String profileImageURL) {
        this.cProfileImageURL = profileImageURL;
    }

    public Timestamp getCTimestamp() {
        return cTimestamp;
    }
    public void setCTimestamp(Timestamp timestamp) {
        this.cTimestamp = timestamp;
    }
}
