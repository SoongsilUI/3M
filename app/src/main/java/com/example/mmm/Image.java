package com.example.mmm;

public class Image {
    private String questionId, imageURL;

    public Image(String questionId, String imageURL) {
        this.questionId = questionId;
        this.imageURL = imageURL;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getImageURL() {
        return this.imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
}
