package com.example.salonflow.services.service;

public interface GeocodingService {
    /**
     * Resolves an address into a double array of [latitude, longitude].
     * Returns null if resolution fails or no results are found.
     */
    double[] getCoordinates(String address);
}
