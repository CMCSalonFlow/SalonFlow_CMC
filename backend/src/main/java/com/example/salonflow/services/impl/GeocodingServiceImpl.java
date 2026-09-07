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

        String raw = address.trim();

        // 1. Thử địa chỉ gốc qua Photon
        double[] coords = queryPhoton(raw);
        if (coords != null) return coords;

        // 2. Thử gắn thêm ", Việt Nam" nếu chưa có
        if (!raw.toLowerCase().contains("việt nam") && !raw.toLowerCase().contains("vietnam")) {
            coords = queryPhoton(raw + ", Việt Nam");
            if (coords != null) return coords;
        }

        // 3. Thử bỏ số nhà (ví dụ "74 Phố Lụa, ..." -> "Phố Lụa, ...")
        String withoutNumber = raw.replaceFirst("^\\d+[A-Za-z]?\\s*(/\\s*\\d+[A-Za-z]?\\s*)*(ngõ|ngách|hẻm)?\\s*", "").trim();
        if (!withoutNumber.isEmpty() && !withoutNumber.equalsIgnoreCase(raw)) {
            coords = queryPhoton(withoutNumber + (withoutNumber.toLowerCase().contains("việt nam") ? "" : ", Việt Nam"));
            if (coords != null) return coords;
        }

        // 4. Thử tìm theo phường/quận/huyện (bỏ đoạn đầu)
        String[] parts = raw.split(",");
        if (parts.length > 2) {
            StringBuilder broader = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (broader.length() > 0) broader.append(", ");
                broader.append(parts[i].trim());
            }
            if (!broader.toString().toLowerCase().contains("việt nam")) {
                broader.append(", Việt Nam");
            }
            coords = queryPhoton(broader.toString());
            if (coords != null) return coords;
        }

        log.warn("Photon geocoding exhausted all attempts for address: {}", address);
        return null;
    }

    private double[] queryPhoton(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        try {
            java.net.URI uri = UriComponentsBuilder.fromHttpUrl("https://photon.komoot.io/api/")
                    .queryParam("q", query.trim())
                    .queryParam("limit", 1)
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SalonFlow-App (contact@salonflow.site)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            Map body = response.getBody();
            if (body != null && body.get("features") instanceof java.util.List<?> features && !features.isEmpty()) {
                Map<String, Object> firstFeature = (Map<String, Object>) features.get(0);
                Map<String, Object> geometry = (Map<String, Object>) firstFeature.get("geometry");
                if (geometry != null && geometry.get("coordinates") instanceof java.util.List<?> coords && coords.size() >= 2) {
                    double lon = Double.parseDouble(coords.get(0).toString());
                    double lat = Double.parseDouble(coords.get(1).toString());
                    log.info("Photon geocoding success for query [{}]: lat={}, lon={}", query, lat, lon);
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception e) {
            log.warn("Photon geocoding query [{}] failed: {}", query, e.getMessage());
        }
        return null;
    }
}
