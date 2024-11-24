package com.project.ecomap.Models;

public class ProfileModel {
    private String userId;
    private String username;
    private String email;
    private String password;
    private String path;

    public ProfileModel() {

    }
    public ProfileModel(String userId, String username, String email, String password, String path) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.path = path;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
