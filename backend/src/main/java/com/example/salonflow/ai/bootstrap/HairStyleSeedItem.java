package com.example.salonflow.ai.bootstrap;

import com.example.salonflow.entity.enums.hair.HairGender;

public record HairStyleSeedItem(
        HairGender gender,
        String code,
        String name,
        String imagePrefix,
        int sortOrder,
        double popularityScore
) {
}
