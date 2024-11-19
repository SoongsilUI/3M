package com.example.mmm;

public class Image {
    private String questionId, imageUri;

    public Image(String questionId, String imageUri) {
        this.questionId = questionId;
        this.imageUri = imageUri;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUriUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
