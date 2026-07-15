package com.example.salonflow.pricing;

import com.example.salonflow.entity.ServiceBundle;

import java.util.List;

public interface BookingPricingService {

    BookingPricingResult calculate(
            Long branchId,
            List<Long> serviceIds,
            ServiceBundle bundle
    );

}