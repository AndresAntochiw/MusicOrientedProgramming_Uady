package com.audy.api;

import com.audy.models.Artist;
import com.audy.models.Track;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Mmodule for Spotify Web API communication (RNF-05).
public class SpotifyAPIClient {
    private static final String AUTH_URL = "https://accounts.spotify.com/api/token";
    private static final String API_BASE_URL = "https://api.spotify.com/v1";

    private final String clientId;
    private final String clientSecret;
    private String accessToken;
    private final HttpClient httpClient;

    public SpotifyAPIClient(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public SpotifyAPIClient() {
        this(
            System.getenv().getOrDefault("SPOTIFY_CLIENT_ID", "YOUR_CLIENT_ID"),
            System.getenv().getOrDefault("SPOTIFY_CLIENT_SECRET", "YOUR_CLIENT_SECRET")
        );
    }

    public void authenticate() throws APIClientException {
        try {
            String credentials = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_URL))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new APIClientException(
                        "Spotify authentication failed (HTTP " + response.statusCode() + "). Check Client ID/Secret.",
                        null, false
                );
            }

            this.accessToken = extractJsonString(response.body(), "access_token");
        } catch (APIClientException e) {
            throw e;
        } catch (Exception e) {
            throw new APIClientException("Network error during Spotify authentication.", e, true);
        }
    }

    private void ensureAuthenticated() throws APIClientException {
        if (this.accessToken == null || this.accessToken.isEmpty()) {
            authenticate();
        }
    }

    // Searches tracks on Spotify (RF-02 predictive suggestions & RF-03 Enter search).
    public List<Track> searchTracks(String query, int limit) throws APIClientException {
        ensureAuthenticated();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = API_BASE_URL + "/search?q=" + encodedQuery + "&type=track&limit=" + limit;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new APIClientException("Spotify search error: HTTP " + response.statusCode(), null, true);
            }

            return parseTracksFromSearchJson(response.body());
        } catch (APIClientException e) {
            throw e;
        } catch (Exception e) {
            throw new APIClientException("Connection failed while searching tracks on Spotify.", e, true);
        }
    }

    // Retrieves detailed metadata for a single track (RF-05, RF-09).
    public Track getTrackMetadata(String trackId) throws APIClientException {
        List<Track> tracks = searchTracks("spotifysong:" + trackId, 1);
        return tracks.isEmpty() ? null : tracks.get(0);
    }

    // Retrieves artist information and their associated genres (RF-06).
    public Artist getArtistMetadata(String artistId) throws APIClientException {
        ensureAuthenticated();
        try {
            String url = API_BASE_URL + "/artists/" + artistId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new APIClientException("Failed to fetch artist metadata: HTTP " + response.statusCode(), null, true);
            }

            String name = extractJsonString(response.body(), "name");
            List<String> genres = extractJsonArrayStrings(response.body(), "genres");
            return new Artist(artistId, name, genres);
        } catch (APIClientException e) {
            throw e;
        } catch (Exception e) {
            throw new APIClientException("Network error while retrieving artist metadata.", e, true);
        }
    }

    // Fetches recommendations by genre and popularity bounds (RF-07, RF-08).
    // Includes an automatic fallback to genre-based search if /recommendations doesn't work.
    public List<Track> fetchRecommendations(List<String> seedGenres, int minPop, int maxPop, int limit) throws APIClientException {
        ensureAuthenticated();
        if (seedGenres == null || seedGenres.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<String> maxFiveSeeds = seedGenres.subList(0, Math.min(5, seedGenres.size()));
            String joinedGenres = URLEncoder.encode(String.join(",", maxFiveSeeds), StandardCharsets.UTF_8);

            String url = String.format(
                    "%s/recommendations?seed_genres=%s&min_popularity=%d&max_popularity=%d&limit=%d",
                    API_BASE_URL, joinedGenres, minPop, maxPop, limit
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseTracksFromSearchJson(response.body());
            }

            // Fallback
            List<Track> fallbackResults = new ArrayList<>();
            for (String genre : maxFiveSeeds) {
                List<Track> genreTracks = searchTracks("genre:\"" + genre + "\"", 25);
                for (Track t : genreTracks) {
                    if (t.getPopularity() >= minPop && t.getPopularity() <= maxPop) {
                        fallbackResults.add(t);
                    }
                }
                if (fallbackResults.size() >= limit) break;
            }
            return fallbackResults;

        } catch (APIClientException e) {
            throw e;
        } catch (Exception e) {
            throw new APIClientException("Error generating recommendations from Spotify.", e, true);
        }
    }

    // --- JSON Extraction Helpers ---

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private List<String> extractJsonArrayStrings(String json, String arrayKey) {
        List<String> list = new ArrayList<>();
        Pattern blockPattern = Pattern.compile("\"" + arrayKey + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher blockMatcher = blockPattern.matcher(json);
        if (blockMatcher.find()) {
            Matcher itemMatcher = Pattern.compile("\"([^\"]+)\"").matcher(blockMatcher.group(1));
            while (itemMatcher.find()) {
                list.add(itemMatcher.group(1));
            }
        }
        return list;
    }

    private List<Track> parseTracksFromSearchJson(String json) {
        List<Track> tracks = new ArrayList<>();
        // Splits track items by "type" : "track" blocks
        String[] blocks = json.split("\"type\"\\s*:\\s*\"track\"");
        for (int i = 0; i < blocks.length - 1; i++) {
            String block = blocks[i];
            String id = extractLastMatch(block, "\"id\"\\s*:\\s*\"([^\"]+)\"");
            String title = extractLastMatch(block, "\"name\"\\s*:\\s*\"([^\"]+)\"");
            String popStr = extractFirstMatch(blocks[i + 1], "\"popularity\"\\s*:\\s*(\\d+)");
            int popularity = popStr.isEmpty() ? 50 : Integer.parseInt(popStr);
            String previewUrl = extractFirstMatch(blocks[i + 1], "\"preview_url\"\\s*:\\s*\"([^\"]+)\"");
            String playerUrl = "https://open.spotify.com/track/" + id;

            Artist artist = new Artist("unknown_artist_id", "Spotify Artist", new ArrayList<>());
            if (!id.isEmpty() && !title.isEmpty()) {
                tracks.add(new Track(id, title, artist, new ArrayList<>(), popularity, previewUrl, playerUrl, ""));
            }
        }
        return tracks;
    }

    private String extractLastMatch(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        String last = "";
        while (m.find()) last = m.group(1);
        return last;
    }

    private String extractFirstMatch(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : "";
    }
}