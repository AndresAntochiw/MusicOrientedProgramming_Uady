package com.audy.services;

import com.audy.api.APIClientException;
import com.audy.api.MusicBrainzAPIClient;
import com.audy.api.SpotifyAPIClient;
import com.audy.models.Artist;
import com.audy.models.Track;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


// Logic engine implementing discovery, debounce, and filtering rules.
public class DiscoveryEngine {
    private final SpotifyAPIClient spotifyApi;
    private final MusicBrainzAPIClient musicBrainzApi;
    private long lastKeystrokeTimestamp = 0;

    private static final long DEBOUNCE_DELAY_MS = 300; // RNF-04: >= 300 ms wait
    private static final int MIN_RECOMMENDATIONS = 10; // RNF-03: At least 10 results

    public DiscoveryEngine(SpotifyAPIClient spotifyApi, MusicBrainzAPIClient musicBrainzApi) {
        this.spotifyApi = spotifyApi;
        this.musicBrainzApi = musicBrainzApi;
    }

    // Handles predictive search suggestions while typing (RF-02) with a 300ms debounce (RNF-04).
    public List<Track> getPredictiveSuggestions(String partialText) throws APIClientException {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastKeystrokeTimestamp < DEBOUNCE_DELAY_MS) {
            return new ArrayList<>();
        }
        lastKeystrokeTimestamp = currentTime;

        if (partialText == null || partialText.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return spotifyApi.searchTracks(partialText.trim(), 5);
    }

    // Executes full search when the user presses Enter (RF-03).
    public List<Track> searchMatches(String query) throws APIClientException {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return spotifyApi.searchTracks(query.trim(), 20);
    }

    // Generates filtered recommendations (RF-05, RF-06, RF-07, RF-08)
    // strictly enforcing RNF-03 (no reference track, no duplicates, >= 10 results).
    public List<Track> generateRecommendations(Track referenceTrack) throws APIClientException {
        if (referenceTrack == null || referenceTrack.getArtist() == null) {
            throw new APIClientException("Invalid reference track selected.", null, false);
        }

        // RF-06: Retrieve artist metadata and genres
        Artist fullArtist = spotifyApi.getArtistMetadata(referenceTrack.getArtist().getId());
        Set<String> seedGenres = new LinkedHashSet<>();

        if (fullArtist != null && fullArtist.getGenres() != null) {
            seedGenres.addAll(fullArtist.getGenres());
        }
        if (referenceTrack.getGenres() != null) {
            seedGenres.addAll(referenceTrack.getGenres());
        }

        // Fallback to MusicBrainz if Spotify provides no genres for the artist/track
        if (seedGenres.isEmpty()) {
            List<String> fallbackTags = musicBrainzApi.fetchTrackTags(
                    referenceTrack.getTitle(),
                    referenceTrack.getArtist().getName()
            );
            seedGenres.addAll(fallbackTags);
        }

        // RF-07: Calculate adaptive popularity bounds based on reference track popularity
        int refPop = referenceTrack.getPopularity();
        int minPop;
        int maxPop;

        if (refPop > 70) {
            minPop = 25;
            maxPop = 65;
        } else if (refPop >= 30) {
            minPop = Math.max(15, refPop - 20);
            maxPop = Math.min(75, refPop + 15);
        } else {
            minPop = 10;
            maxPop = 50;
        }

        // Request extra tracks (limit = 30) to ensure at least 10 remain after deduplication (RNF-03)
        List<Track> rawCandidates = spotifyApi.fetchRecommendations(
                new ArrayList<>(seedGenres), minPop, maxPop, 30
        );

        // RNF-03: Filter out reference song and eliminate duplicates
        Set<Track> uniqueRecommendations = new LinkedHashSet<>();
        for (Track candidate : rawCandidates) {
            if (!candidate.equals(referenceTrack)) {
                uniqueRecommendations.add(candidate);
            }
        }

        // RNF-03: If fewer than 10 results, widen the popularity filter to fetch additional candidates
        if (uniqueRecommendations.size() < MIN_RECOMMENDATIONS) {
            List<Track> extraCandidates = spotifyApi.fetchRecommendations(
                    new ArrayList<>(seedGenres), 5, 85, 30
            );
            for (Track extra : extraCandidates) {
                if (!extra.equals(referenceTrack)) {
                    uniqueRecommendations.add(extra);
                }
                if (uniqueRecommendations.size() >= MIN_RECOMMENDATIONS) {
                    break;
                }
            }
        }

        return new ArrayList<>(uniqueRecommendations);
    }
}