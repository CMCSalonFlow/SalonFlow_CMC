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
        Salon salon = branch != null ? branch.getSalon() : null;

        List<InvoiceItemDto> items = (booking.getItems() != null)
                ? booking.getItems().stream().map(this::toItemDto).toList()
                : List.of();

        double subTotal = booking.getTotalPrice() != null ? booking.getTotalPrice().doubleValue() : 0.0;
        double tax = 0;
        double total = subTotal + tax;

        String customerName = "Khách hàng";
        String customerPhone = "";
        if (booking.getCustomer() != null) {
            if (booking.getCustomer().getFullName() != null && !booking.getCustomer().getFullName().isBlank()) {
                customerName = booking.getCustomer().getFullName();
            } else if (booking.getCustomer().getUsername() != null && !booking.getCustomer().getUsername().isBlank()) {
                customerName = booking.getCustomer().getUsername();
            } else if (booking.getCustomer().getEmail() != null) {
                customerName = booking.getCustomer().getEmail();
            }
            if (booking.getCustomer().getPhone() != null) {
                customerPhone = booking.getCustomer().getPhone();
            }
        }

        LocalDateTime bookingTime = LocalDateTime.now();
        if (booking.getBookingDate() != null && booking.getStartTime() != null) {
            bookingTime = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        }

        return InvoiceDto.builder()
                .salonName(salon != null ? salon.getName() : "SalonFlow")
                .salonAddress(branch != null ? branch.getAddress() : "")
                .salonPhone(branch != null ? branch.getPhone() : "")
                .salonLogo(null)
                .bookingId(booking.getId())
                .bookingTime(bookingTime)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .items(items)
                .subTotal(subTotal)
                .tax(tax)
                .total(total)
                .build();
    }

    private InvoiceItemDto toItemDto(BookingItem item) {

        String serviceName = "Dịch vụ";

        if (item.getService() != null && item.getService().getName() != null) {
            serviceName = item.getService().getName();
        } else if (item.getBundle() != null && item.getBundle().getName() != null) {
            serviceName = item.getBundle().getName();
        }

        double price = 0.0;
        if (item.getPrice() != null && item.getPrice().doubleValue() > 0) {
            price = item.getPrice().doubleValue();
        } else if (item.getService() != null && item.getService().getPrice() != null) {
            price = item.getService().getPrice().doubleValue();
        } else if (item.getBundle() != null && item.getBundle().getPrice() != null) {
            price = item.getBundle().getPrice().doubleValue();
        }

        return InvoiceItemDto.builder()
                .serviceName(serviceName)
                .quantity(1)
                .unitPrice(price)
                .totalPrice(price)
                .build();
    }

}