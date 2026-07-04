package com.example.salonflow.search.mapper;

import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.SalonService;
import com.example.salonflow.search.document.BranchSearchDocument;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class BranchSearchMapper {

    public BranchSearchDocument toDocument(
            Branch branch,
            List<SalonService> services
    ) {

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        if (!services.isEmpty()) {

            minPrice = services.stream()
                    .map(SalonService::getPrice)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            maxPrice = services.stream()
                    .map(SalonService::getPrice)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
        }

        return BranchSearchDocument.builder()
                .branchId(branch.getId())
                .salonId(branch.getSalon().getId())
                .salonName(branch.getSalon().getName())
                .branchName(branch.getName())
                .address(branch.getAddress())
                .latitude(branch.getLatitude())
                .longitude(branch.getLongitude())
                .serviceIds(
                        services.stream()
                                .map(SalonService::getId)
                                .toList()
                )
                .services(
                        services.stream()
                                .map(SalonService::getName)
                                .toList()
                )
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .averageRating(0d)
                .active(branch.getIsActive())
                .build();
    }

}