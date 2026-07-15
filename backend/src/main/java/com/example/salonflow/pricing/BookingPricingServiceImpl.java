package com.example.salonflow.pricing;

import com.example.salonflow.entity.SalonService;
import com.example.salonflow.entity.ServiceBundle;
import com.example.salonflow.entity.ServiceBundleItem;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingPricingServiceImpl
        implements BookingPricingService {

    private final ServiceRepository serviceRepository;

    @Override
    public BookingPricingResult calculate(
            Long branchId,
            List<Long> serviceIds,
            ServiceBundle bundle
    ) {

        if (bundle != null) {
            return calculateBundle(branchId, bundle);
        }

        return calculateServices(branchId, serviceIds);
    }

    private BookingPricingResult calculateServices(
            Long branchId,
            List<Long> serviceIds
    ) {

        List<SalonService> services =
                serviceRepository.findAllById(serviceIds);

        if (services.size() != serviceIds.size()) {
            throw new BusinessException(
                    "Một số dịch vụ không tồn tại."
            );
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal deposit = BigDecimal.ZERO;
        int duration = 0;

        for (SalonService service : services) {

            if (!service.getBranch().getId().equals(branchId)) {
                throw new BusinessException(
                        "Service "
                                + service.getName()
                                + " không thuộc chi nhánh."
                );
            }

            totalPrice =
                    totalPrice.add(service.getPrice());

            duration +=
                    service.getDurationMinutes();

            if (Boolean.TRUE.equals(service.getDepositRequired())) {

                BigDecimal itemDeposit =
                        service.getPrice()
                                .multiply(service.getDepositPercentage())
                                .divide(
                                        BigDecimal.valueOf(100),
                                        2,
                                        RoundingMode.HALF_UP
                                );

                deposit =
                        deposit.add(itemDeposit);

            }

        }

        return BookingPricingResult.builder()
                .services(services)
                .totalDurationMinutes(duration)
                .totalPrice(totalPrice)
                .depositAmount(deposit)
                .remainingAmount(totalPrice.subtract(deposit))
                .build();
    }

    private BookingPricingResult calculateBundle(
            Long branchId,
            ServiceBundle bundle
    ) {

        if (!bundle.getBranch().getId().equals(branchId)) {
            throw new BusinessException(
                    "Bundle không thuộc chi nhánh."
            );
        }

        List<SalonService> services =
                bundle.getItems()
                        .stream()
                        .map(ServiceBundleItem::getService)
                        .toList();

        BigDecimal originalPrice = BigDecimal.ZERO;
        BigDecimal deposit = BigDecimal.ZERO;

        for (SalonService service : services) {

            originalPrice =
                    originalPrice.add(service.getPrice());

            if (Boolean.TRUE.equals(service.getDepositRequired())) {

                BigDecimal itemDeposit =
                        service.getPrice()
                                .multiply(service.getDepositPercentage())
                                .divide(
                                        BigDecimal.valueOf(100),
                                        2,
                                        RoundingMode.HALF_UP
                                );

                deposit =
                        deposit.add(itemDeposit);

            }

        }

        BigDecimal finalDeposit = deposit;

        if (originalPrice.compareTo(BigDecimal.ZERO) > 0
                && bundle.getPrice() != null) {

            BigDecimal discountRate =
                    bundle.getPrice()
                            .divide(
                                    originalPrice,
                                    4,
                                    RoundingMode.HALF_UP
                            );

            finalDeposit =
                    deposit.multiply(discountRate)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );
        }

        return BookingPricingResult.builder()
                .services(new ArrayList<>(services))
                .totalDurationMinutes(
                        bundle.getTotalDurationMinutes()
                )
                .totalPrice(bundle.getPrice())
                .depositAmount(finalDeposit)
                .remainingAmount(
                        bundle.getPrice()
                                .subtract(finalDeposit)
                )
                .build();

    }

}