package com.audy;

import com.audy.api.SpotifyAPIClient;
import com.audy.services.DiscoveryEngine;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Audy...");
        
        DiscoveryEngine engine = new DiscoveryEngine();
        System.out.println("Engine successfully initialized.");
        
        SpotifyAPIClient api = new SpotifyAPIClient();
        api.authenticate();
    }
}