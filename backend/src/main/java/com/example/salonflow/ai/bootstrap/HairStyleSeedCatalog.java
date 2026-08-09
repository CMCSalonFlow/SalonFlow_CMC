package com.example.salonflow.ai.bootstrap;

import com.example.salonflow.entity.enums.hair.HairGender;

import java.util.List;
import java.util.Optional;

public final class HairStyleSeedCatalog {

    private HairStyleSeedCatalog() {
    }

    public static List<HairStyleSeedItem> all() {
        return List.of(
                item(HairGender.MEN, "BUZZ_CUT", "Buzz Cut", "Buzz Cut", 1, 0.90),
                item(HairGender.MEN, "CREW_CUT", "Crew Cut", "Crew Cut", 2, 0.85),
                item(HairGender.MEN, "FAUX_HAWK", "Faux Hawk", "Faux Hawk", 3, 0.78),
                item(HairGender.MEN, "FRENCH_CROP", "French Crop", "French Crop", 4, 0.82),
                item(HairGender.MEN, "LAYER_TWO_BLOCK", "Layer Two Block", "layer-two-block", 5, 0.88),
                item(HairGender.MEN, "LONG_CURLY_HAIR", "Long Curly Hair", "Long Curly Hair", 6, 0.75),
                item(HairGender.MEN, "LONG_LAYERED_HAIR", "Long Layered Hair", "Long Layered Hair", 7, 0.74),
                item(HairGender.MEN, "LONG_QUIFF", "Long Quiff", "Long Quiff", 8, 0.71),
                item(HairGender.MEN, "MAN_BUN", "Man Bun", "Man Bun", 9, 0.72),
                item(HairGender.MEN, "MOHAWK", "Mohawk", "Mohawk", 10, 0.68),
                item(HairGender.MEN, "MULLET_NAM", "Mullet Nam", "mullet-nam", 11, 0.67),
                item(HairGender.MEN, "POMPADOUR", "Pompadour", "Pompadour", 12, 0.83),
                item(HairGender.MEN, "SHORT_LAYERED_HAIR", "Short Layered Hair", "Short Layered Hair", 13, 0.76),
                item(HairGender.MEN, "SHORT_QUIFF", "Short Quiff", "Short Quiff", 14, 0.79),
                item(HairGender.MEN, "SIDE_PART", "Side Part", "Side Part", 15, 0.86),
                item(HairGender.MEN, "SLICKED_BACK_UNDERCUT", "Slicked Back Undercut", "Slicked Back Undercut", 16, 0.82),
                item(HairGender.MEN, "TOP_KNOT", "Top Knot", "Top Knot", 17, 0.70),
                item(HairGender.WOMEN, "BUTTERFLY_CUT", "Butterfly Cut", "Butterfly Cut", 18, 0.88),
                item(HairGender.WOMEN, "CURLY_LOB", "Curly Lob", "Curly Lob", 19, 0.84),
                item(HairGender.WOMEN, "FRENCH_END_CURLS", "French End Curls", "French End Curls", 20, 0.73),
                item(HairGender.WOMEN, "HIGH_LAYERED_CUT", "High Layered Cut", "High Layered Cut", 21, 0.82),
                item(HairGender.WOMEN, "HIME_CUT", "Hime Cut", "Hime Cut", 22, 0.77),
                item(HairGender.WOMEN, "HIPPIE_PERM", "Hippie Perm", "Hippie Perm", 23, 0.69),
                item(HairGender.WOMEN, "KOREAN_VOLUME_PERM", "Korean Volume Perm", "Korean Volume Perm", 24, 0.91),
                item(HairGender.WOMEN, "LARGE_BARREL_CURLS", "Large Barrel Curls", "Large Barrel Curls", 25, 0.80),
                item(HairGender.WOMEN, "LAYER_WOLF_CUT", "Layer Wolf Cut", "Layer Wolf Cut", 26, 0.90),
                item(HairGender.WOMEN, "LAYERED_TOMBOY_MULLET", "Layered Tomboy Mullet", "Layered Tomboy Mullet", 27, 0.72),
                item(HairGender.WOMEN, "LOB_CUT", "Lob Cut", "Lob Cut", 28, 0.87),
                item(HairGender.WOMEN, "LONG_LOOSE_WAVES", "Long Loose Waves", "Long Loose Waves", 29, 0.86),
                item(HairGender.WOMEN, "LONG_STRAIGHT_HAIR", "Long Straight Hair", "Long Straight Hair", 30, 0.93),
                item(HairGender.WOMEN, "PIXIE_CUT", "Pixie Cut", "Pixie Cut", 31, 0.89),
                item(HairGender.WOMEN, "SHOULDER_LENGTH_FLIPPED_OUT_HAIR", "Shoulder-Length Flipped-Out Hair", "Shoulder-Length Flipped-Out Hair", 32, 0.76),
                item(HairGender.WOMEN, "SHOULDER_LENGTH_LAYERED_MULLET", "Shoulder-Length Layered Mullet", "Shoulder-Length Layered Mullet", 33, 0.68),
                item(HairGender.WOMEN, "SHOULDER_LENGTH_SHAG_CUT", "Shoulder-Length Shag Cut", "Shoulder-Length Shag Cut", 34, 0.79),
                item(HairGender.WOMEN, "TOC_BOB", "Toc Bob", "toc-bob", 35, 0.74),
                item(HairGender.WOMEN, "TOC_TEM_PIXIE", "Toc Tem Pixie", "toc-tem-pixie", 36, 0.81),
                item(HairGender.WOMEN, "VICTORIA_BECKHAM_BOB", "Victoria Beckham Bob", "Victoria Beckham Bob", 37, 0.84)
        );
    }

    public static Optional<HairGender> genderForCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return all().stream()
                .filter(item -> item.code().equalsIgnoreCase(code))
                .map(HairStyleSeedItem::gender)
                .findFirst();
    }

    private static HairStyleSeedItem item(
            HairGender gender,
            String code,
            String name,
            String imagePrefix,
            int sortOrder,
            double popularityScore
    ) {
        return new HairStyleSeedItem(gender, code, name, imagePrefix, sortOrder, popularityScore);
    }
}
