package com.project.ecomap;

public class Notification {
    private String title;
    private String time;
    private String questionId;

    public Notification(String title, String time, String questionId) {
        this.title = title;
        this.time = time;
        this.questionId = questionId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }
}
