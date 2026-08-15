package com.example.salonflow.repository.projection;

import java.math.BigDecimal;

public interface NearbyBranchProjection {
    Long getBranchId();
    String getBranchName();
    String getBranchPhone();
    String getBranchEmail();
    String getAddress();
    Double getLatitude();
    Double getLongitude();

    Long getSalonId();
    String getSalonName();
    String getSalonDescription();
    String getLogoUrl();

    Double getDistanceMeters();
    BigDecimal getRatingAverage();
    Integer getRatingCount();
}
