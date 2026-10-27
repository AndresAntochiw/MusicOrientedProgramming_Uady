package com.audy.api;

import com.audy.models.Track;
import com.audy.models.Artist;
import java.util.List;

public class SpotifyAPIClient {
    private String clientId = "ID";
    private String clientSecret = "SECRET";
    private String accessToken;

    public void authenticate() {
        // We need to see how it works with the Spotify API, but for now, we can just simulate the authentication process.
        this.accessToken = "SIMULATED_ACCESS_TOKEN";
    }

    public Track searchTrack(String query) {
        return null;
    }
    
    public Artist getArtistInformation(String artistId) {
        return null;
    }

    public List<String> getArtistGenres(String artistId) {
        return null;
    }

    public List<Track> fetchRecommendations(List<String> seedGenres, int minPop, int maxPop) {
        return null;
    }
}