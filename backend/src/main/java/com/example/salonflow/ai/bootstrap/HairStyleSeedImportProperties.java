package com.example.salonflow.ai.bootstrap;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.hair-style-import")
public class HairStyleSeedImportProperties {

    private boolean enabled = false;
    private String manZipPath;
    private String womenZipPath;
    private String objectPrefix = "hair-styles";
}
