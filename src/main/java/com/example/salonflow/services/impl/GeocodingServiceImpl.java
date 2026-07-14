package com.example.salonflow.services.impl;

import com.example.salonflow.services.service.GeocodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@Slf4j
public class GeocodingServiceImpl implements GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public double[] getCoordinates(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        try {
            java.net.URI uri = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/search")
                    .queryParam("format", "json")
                    .queryParam("q", address.trim())
                    .queryParam("limit", 1)
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SalonFlow-App (contact@salonflow.site)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map[].class);
            Map[] results = response.getBody();

            if (results != null && results.length > 0) {
                Map<String, Object> firstResult = (Map<String, Object>) results[0];
                double lat = Double.parseDouble(firstResult.get("lat").toString());
                double lon = Double.parseDouble(firstResult.get("lon").toString());
                log.info("Geocoding success for address [{}]: lat={}, lon={}", address, lat, lon);
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            log.error("Geocoding failed for address [{}]: {}", address, e.getMessage());
        }
        return null;
    }
}
