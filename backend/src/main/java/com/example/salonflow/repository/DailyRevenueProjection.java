package com.example.salonflow.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRevenueProjection {
    LocalDate getDate();
    BigDecimal getRevenue();
}
