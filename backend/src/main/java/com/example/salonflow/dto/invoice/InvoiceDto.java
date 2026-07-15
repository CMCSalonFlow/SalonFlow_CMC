package com.example.salonflow.dto.invoice;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDto {

    // Salon
    private String salonName;

    private String salonAddress;

    private String salonPhone;

    private String salonLogo;

    // Booking
    private Long bookingId;

    private LocalDateTime bookingTime;

    // Customer
    private String customerName;

    private String customerPhone;

    // Danh sách dịch vụ
    private List<InvoiceItemDto> items;

    // Tiền
    private Double subTotal;

    private Double tax;

    private Double total;
}