package com.example.salonflow.services.impl;

import com.example.salonflow.dto.invoice.InvoiceDto;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.mapper.InvoiceMapper;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.services.service.InvoicePdfService;
import com.example.salonflow.services.service.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private final ObjectMapper objectMapper;

    private final InvoiceMapper invoiceMapper;

    private final MediaService mediaService;

    private final BookingRepository bookingRepository;

    @Override
    public String generateInvoice(Booking booking) throws Exception {

        // Tạo thư mục tạm
        Path tempDir = Files.createTempDirectory("invoice-");

        Path jsonFile = tempDir.resolve("invoice.json");
        Path pdfFile = tempDir.resolve("invoice.pdf");

        // Mapping Booking -> InvoiceDto
        InvoiceDto invoice = invoiceMapper.toDto(booking);

        // Ghi JSON
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

       // Upload PDF lên MinIO
        String objectName = mediaService.uploadInvoice(
        pdfFile.toFile(),
        booking.getId()
);

        // Chỉ lưu objectName vào DB
        booking.setInvoiceUrl(objectName);
        booking.setInvoiceGeneratedAt(LocalDateTime.now());

        bookingRepository.save(booking);

        // Xóa file tạm
        Files.deleteIfExists(pdfFile);
        Files.deleteIfExists(jsonFile);
        Files.deleteIfExists(tempDir);

        // Trả về objectName
        return objectName;

    }
}