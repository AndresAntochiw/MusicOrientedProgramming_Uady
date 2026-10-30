package com.audy.models;

import java.util.List;
import java.util.Objects;

// (RF-05, RF-09)
//Represents a song and its metadata within the system.
public class Track {
    private String id;
    private String title;
    private Artist artist;
    private List<String> genres;
    private int popularity;
    private String previewUrl;
    private String playerUrl;
    private String coverImage;

    public Track(String id, String title, Artist artist, List<String> genres,
                 int popularity, String previewUrl, String playerUrl, String coverImage) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.genres = genres;
        this.popularity = popularity;
        this.previewUrl = previewUrl;
        this.playerUrl = playerUrl;
        this.coverImage = coverImage;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Artist getArtist() { return artist; }
    public void setArtist(Artist artist) { this.artist = artist; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public int getPopularity() { return popularity; }
    public void setPopularity(int popularity) { this.popularity = popularity; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getPlayerUrl() { return playerUrl; }
    public void setPlayerUrl(String playerUrl) { this.playerUrl = playerUrl; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    //  Ensures no duplicate tracks appear in recommendations (RNF-03) by checking both Spotify ID and normalized Title + Artist name.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        if (id != null && track.id != null && id.equals(track.id)) return true;
        String thisArtist = (artist != null && artist.getName() != null) ? artist.getName().toLowerCase() : "";
        String otherArtist = (track.artist != null && track.artist.getName() != null) ? track.artist.getName().toLowerCase() : "";
        return Objects.equals(title != null ? title.toLowerCase() : "", track.title != null ? track.title.toLowerCase() : "")
                && Objects.equals(thisArtist, otherArtist);
    }

    @Override
    public int hashCode() {
        String normTitle = title != null ? title.toLowerCase() : "";
        String normArtist = (artist != null && artist.getName() != null) ? artist.getName().toLowerCase() : "";
        return Objects.hash(normTitle, normArtist);
    }
}