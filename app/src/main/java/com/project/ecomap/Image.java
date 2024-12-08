package com.project.ecomap;

public class Image {
    private long likedCount;
    private String imageUrl, markerId;
    private String title;

    public Image() {
    }

    public Image(String imageUrl, String markerId, String title, long likedCount) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.likedCount = likedCount;
        this.markerId = markerId;

    }

    public String getMarkerId() {
        return markerId;
    }

    public void setMarkerId(String markerId) {
        this.markerId = markerId;
    }

    public long getLikedCount() {
        return likedCount;
    }

    public void setLikedCount(long likedCount) {
        this.likedCount = likedCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


}
