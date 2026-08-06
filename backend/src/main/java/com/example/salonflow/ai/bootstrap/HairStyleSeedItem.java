package com.example.salonflow.ai.bootstrap;

public record HairStyleSeedItem(
        String code,
        String name,
        String imagePrefix,
        int sortOrder,
        double popularityScore
) {
}
