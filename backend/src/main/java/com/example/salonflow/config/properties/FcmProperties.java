package com.example.salonflow.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification.fcm")
public class FcmProperties {

    private boolean enabled;
    private String projectId;
    private String serviceAccountJson;
    private String serviceAccountPath;
    private String webLink;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getServiceAccountJson() {
        return serviceAccountJson;
    }

    public void setServiceAccountJson(String serviceAccountJson) {
        this.serviceAccountJson = serviceAccountJson;
    }

    public String getServiceAccountPath() {
        return serviceAccountPath;
    }

    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }

    public String getWebLink() {
        return webLink;
    }

    public void setWebLink(String webLink) {
        this.webLink = webLink;
    }

    public boolean hasServiceAccountJson() {
        return serviceAccountJson != null && !serviceAccountJson.isBlank();
    }

    public boolean hasServiceAccountPath() {
        return serviceAccountPath != null && !serviceAccountPath.isBlank();
    }
}
