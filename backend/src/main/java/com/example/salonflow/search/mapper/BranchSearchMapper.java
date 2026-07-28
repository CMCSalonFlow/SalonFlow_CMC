package com.example.salonflow.search.mapper;

import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.SalonService;
import com.example.salonflow.search.document.BranchSearchDocument;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
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

        BranchSearchDocument document = new BranchSearchDocument();
        document.setBranchId(branch.getId());
        document.setSortId(branch.getId());
        document.setSalonId(branch.getSalon().getId());
        document.setSalonName(branch.getSalon().getName());
        document.setBranchName(branch.getName());
        document.setAddress(branch.getAddress());
        document.setLatitude(branch.getLatitude());
        document.setLongitude(branch.getLongitude());
        document.setLocation(
                branch.getLatitude() != null && branch.getLongitude() != null
                        ? new GeoPoint(branch.getLatitude(), branch.getLongitude())
                        : null
        );
        document.setServiceIds(
                services.stream()
                        .map(SalonService::getId)
                        .toList()
        );
        document.setServices(
                services.stream()
                        .map(SalonService::getName)
                        .toList()
        );
        document.setMinPrice(minPrice);
        document.setMaxPrice(maxPrice);
        document.setAverageRating(branch.getRatingAverage() != null ? branch.getRatingAverage().doubleValue() : 0d);
        document.setActive(branch.getIsActive());
        return document;
    }

}
