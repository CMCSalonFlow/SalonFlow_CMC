package com.example.salonflow.search.dto;

import java.math.BigDecimal;

public class BranchSearchRequest {

    private String q;
    private String service;
    private Long serviceId;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private Double ratingMin;
    private Double latitude;
    private Double longitude;
    private Double radius = 20d;
    private String cursor;
    private Integer size = 20;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(BigDecimal priceMin) {
        this.priceMin = priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(BigDecimal priceMax) {
        this.priceMax = priceMax;
    }

    public Double getRatingMin() {
        return ratingMin;
    }

    public void setRatingMin(Double ratingMin) {
        this.ratingMin = ratingMin;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getRadius() {
        return radius;
    }

    public void setRadius(Double radius) {
        this.radius = radius;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
