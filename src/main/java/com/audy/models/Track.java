package com.audy.models;

public class Track {
    private String id;
    private String title;
    private String previewUrl;
    private String coverImage;
    private Artist artist;
    private int popularity;

    public Track(String id, String title, String previewUrl, String coverImage, Artist artist, int popularity) {
        this.id = id;
        this.title = title;
        this.previewUrl = previewUrl;
        this.coverImage = coverImage;
        this.artist = artist;
        this.popularity = popularity;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public Artist getArtist() { return artist; }
    public void setArtist(Artist artist) { this.artist = artist; }

    public int getPopularity() { return popularity; }
    public void setPopularity(int popularity) { this.popularity = popularity; }
}