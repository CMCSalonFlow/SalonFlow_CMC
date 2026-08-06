package com.example.salonflow.ai.bootstrap;

import java.util.List;

public final class HairStyleSeedCatalog {

    private HairStyleSeedCatalog() {
    }

    public static List<HairStyleSeedItem> all() {
        return List.of(
                item("BUZZ_CUT", "Buzz Cut", "Buzz Cut", 1, 0.90),
                item("CREW_CUT", "Crew Cut", "Crew Cut", 2, 0.85),
                item("FAUX_HAWK", "Faux Hawk", "Faux Hawk", 3, 0.78),
                item("FRENCH_CROP", "French Crop", "French Crop", 4, 0.82),
                item("LAYER_TWO_BLOCK", "Layer Two Block", "layer-two-block", 5, 0.88),
                item("LONG_CURLY_HAIR", "Long Curly Hair", "Long Curly Hair", 6, 0.75),
                item("LONG_LAYERED_HAIR", "Long Layered Hair", "Long Layered Hair", 7, 0.74),
                item("LONG_QUIFF", "Long Quiff", "Long Quiff", 8, 0.71),
                item("MAN_BUN", "Man Bun", "Man Bun", 9, 0.72),
                item("MOHAWK", "Mohawk", "Mohawk", 10, 0.68),
                item("MULLET_NAM", "Mullet Nam", "mullet-nam", 11, 0.67),
                item("POMPADOUR", "Pompadour", "Pompadour", 12, 0.83),
                item("SHORT_LAYERED_HAIR", "Short Layered Hair", "Short Layered Hair", 13, 0.76),
                item("SHORT_QUIFF", "Short Quiff", "Short Quiff", 14, 0.79),
                item("SIDE_PART", "Side Part", "Side Part", 15, 0.86),
                item("SLICKED_BACK_UNDERCUT", "Slicked Back Undercut", "Slicked Back Undercut", 16, 0.82),
                item("TOP_KNOT", "Top Knot", "Top Knot", 17, 0.70),
                item("BUTTERFLY_CUT", "Butterfly Cut", "Butterfly Cut", 18, 0.88),
                item("CURLY_LOB", "Curly Lob", "Curly Lob", 19, 0.84),
                item("FRENCH_END_CURLS", "French End Curls", "French End Curls", 20, 0.73),
                item("HIGH_LAYERED_CUT", "High Layered Cut", "High Layered Cut", 21, 0.82),
                item("HIME_CUT", "Hime Cut", "Hime Cut", 22, 0.77),
                item("HIPPIE_PERM", "Hippie Perm", "Hippie Perm", 23, 0.69),
                item("KOREAN_VOLUME_PERM", "Korean Volume Perm", "Korean Volume Perm", 24, 0.91),
                item("LARGE_BARREL_CURLS", "Large Barrel Curls", "Large Barrel Curls", 25, 0.80),
                item("LAYER_WOLF_CUT", "Layer Wolf Cut", "Layer Wolf Cut", 26, 0.90),
                item("LAYERED_TOMBOY_MULLET", "Layered Tomboy Mullet", "Layered Tomboy Mullet", 27, 0.72),
                item("LOB_CUT", "Lob Cut", "Lob Cut", 28, 0.87),
                item("LONG_LOOSE_WAVES", "Long Loose Waves", "Long Loose Waves", 29, 0.86),
                item("LONG_STRAIGHT_HAIR", "Long Straight Hair", "Long Straight Hair", 30, 0.93),
                item("PIXIE_CUT", "Pixie Cut", "Pixie Cut", 31, 0.89),
                item("SHOULDER_LENGTH_FLIPPED_OUT_HAIR", "Shoulder-Length Flipped-Out Hair", "Shoulder-Length Flipped-Out Hair", 32, 0.76),
                item("SHOULDER_LENGTH_LAYERED_MULLET", "Shoulder-Length Layered Mullet", "Shoulder-Length Layered Mullet", 33, 0.68),
                item("SHOULDER_LENGTH_SHAG_CUT", "Shoulder-Length Shag Cut", "Shoulder-Length Shag Cut", 34, 0.79),
                item("TOC_BOB", "Toc Bob", "toc-bob", 35, 0.74),
                item("TOC_TEM_PIXIE", "Toc Tem Pixie", "toc-tem-pixie", 36, 0.81),
                item("VICTORIA_BECKHAM_BOB", "Victoria Beckham Bob", "Victoria Beckham Bob", 37, 0.84)
        );
    }

    private static HairStyleSeedItem item(
            String code,
            String name,
            String imagePrefix,
            int sortOrder,
            double popularityScore
    ) {
        return new HairStyleSeedItem(code, name, imagePrefix, sortOrder, popularityScore);
    }
}
