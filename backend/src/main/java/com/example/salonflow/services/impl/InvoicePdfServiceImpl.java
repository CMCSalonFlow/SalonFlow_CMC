package com.example.salonflow.services.impl;

import com.example.salonflow.dto.invoice.InvoiceDto;
import com.example.salonflow.dto.invoice.InvoiceItemDto;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.entity.BookingItem;
import com.example.salonflow.services.service.InvoicePdfService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private final ObjectMapper objectMapper;

    @Override
    public String generateInvoice(Booking booking) throws Exception {

        // Tạo thư mục tạm
        Path tempDir = Files.createTempDirectory("invoice-");

        Path jsonFile = tempDir.resolve("invoice.json");
        Path pdfFile = tempDir.resolve("invoice.pdf");

        // Booking -> InvoiceDto
        InvoiceDto invoice = buildInvoice(booking);

        // Ghi JSON cho NodeJS đọc
        objectMapper.writeValue(jsonFile.toFile(), invoice);

        // Gọi NodeJS render PDF
        ProcessBuilder pb = new ProcessBuilder(
                "node",
                "scripts/renderInvoice.js",
                jsonFile.toString(),
                pdfFile.toString()
        );

        pb.directory(new File("."));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Generate invoice PDF failed.");
        }

        if (!Files.exists(pdfFile)) {
            throw new RuntimeException("Invoice PDF not found.");
        }

        return pdfFile.toAbsolutePath().toString();
    }

    /**
     * Booking -> InvoiceDto
     */
    private InvoiceDto buildInvoice(Booking booking) {

        List<InvoiceItemDto> items = booking.getItems()
                .stream()
                .map(this::mapItem)
                .toList();

        double subTotal = items.stream()
                .mapToDouble(InvoiceItemDto::getTotalPrice)
                .sum();

        // Chưa áp dụng VAT
        double tax = 0;

        double total = subTotal;

        return InvoiceDto.builder()

                // ===== Salon =====
                .salonName(
                        booking.getBranch()
                                .getSalon()
                                .getName()
                )

                .salonAddress(
                        booking.getBranch()
                                .getAddress()
                )

                .salonPhone(
                        booking.getBranch()
                                .getPhone()
                )

                // Sau này lấy logo từ DB hoặc MinIO
                .salonLogo(null)

                // ===== Booking =====
                .bookingId(
                        booking.getId()
                )

                .bookingTime(
                        booking.getCreatedAt() == null
                                ? LocalDateTime.now()
                                : booking.getCreatedAt()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                )

                // ===== Customer =====
                .customerName(
                        booking.getCustomer()
                                .getFullName()
                )

                .customerPhone(
                        booking.getCustomer()
                                .getPhone()
                )

                // ===== Services =====
                .items(items)

                // ===== Money =====
                .subTotal(subTotal)

                .tax(tax)

                .total(total)

                .build();
    }

    /**
     * BookingItem -> InvoiceItemDto
     */
    private InvoiceItemDto mapItem(BookingItem item) {

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