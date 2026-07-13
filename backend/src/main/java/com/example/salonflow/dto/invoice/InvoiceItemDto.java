package com.example.salonflow.dto.invoice;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemDto {

    private String serviceName;

    private Integer quantity;

    private Double unitPrice;

    private Double totalPrice;
}