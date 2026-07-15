package com.example.salonflow.mapper;

import com.example.salonflow.dto.invoice.InvoiceDto;
import com.example.salonflow.dto.invoice.InvoiceItemDto;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.BookingItem;
import com.example.salonflow.entity.Branch;
import com.example.salonflow.entity.Salon;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class InvoiceMapper {

    public InvoiceDto toDto(Booking booking) {

        Branch branch = booking.getBranch();
        Salon salon = branch.getSalon();

        List<InvoiceItemDto> items = booking.getItems()
                .stream()
                .map(this::toItemDto)
                .toList();

        double subTotal = booking.getTotalPrice().doubleValue();

        double tax = 0;

        double total = subTotal + tax;

        return InvoiceDto.builder()

                // salon
                .salonName(salon.getName())
                .salonAddress(branch.getAddress())
                .salonPhone(branch.getPhone())
                .salonLogo(null)

                // booking
                .bookingId(booking.getId())
                .bookingTime(LocalDateTime.of(
                        booking.getBookingDate(),
                        booking.getStartTime()
                ))

                // customer
                .customerName(booking.getCustomer().getFullName())
                .customerPhone(booking.getCustomer().getPhone())

                // services
                .items(items)

                // money
                .subTotal(subTotal)
                .tax(tax)
                .total(total)

                .build();
    }

    private InvoiceItemDto toItemDto(BookingItem item) {

        String serviceName;

        if (item.getService() != null) {
            serviceName = item.getService().getName();
        } else {
            serviceName = item.getBundle().getName();
        }

        return InvoiceItemDto.builder()
                .serviceName(serviceName)
                .quantity(1)
                .unitPrice(item.getPrice().doubleValue())
                .totalPrice(item.getPrice().doubleValue())
                .build();
    }

}