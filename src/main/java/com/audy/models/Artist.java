package com.audy.models;

import java.util.List;

public class Artist {
    private String id;
    private String name;
    private List<String> genres;

    public Artist(String id, String name, List<String> genres) {
        this.id = id;
        this.name = name;
        this.genres = genres;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }
}