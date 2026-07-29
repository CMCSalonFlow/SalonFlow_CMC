package com.example.salonflow.services.impl;

import com.example.salonflow.dto.invoice.InvoiceDto;
import com.example.salonflow.entity.Booking;
import com.example.salonflow.mapper.InvoiceMapper;
import com.example.salonflow.repository.BookingRepository;
import com.example.salonflow.services.service.InvoicePdfService;
import com.example.salonflow.services.service.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
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

        try {
            // Mapping Booking -> InvoiceDto
            InvoiceDto invoice = invoiceMapper.toDto(booking);

            // Ghi JSON
            objectMapper.writeValue(jsonFile.toFile(), invoice);

            boolean pdfGenerated = false;

            // 1. Thử gọi Node.js render PDF nếu có script
            File scriptFile = new File("scripts/renderInvoice.js");
            if (!scriptFile.exists()) {
                scriptFile = new File("backend/scripts/renderInvoice.js");
            }

            if (scriptFile.exists()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            "node",
                            scriptFile.getAbsolutePath(),
                            jsonFile.toAbsolutePath().toString(),
                            pdfFile.toAbsolutePath().toString()
                    );
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                    if (exitCode == 0 && Files.exists(pdfFile)) {
                        pdfGenerated = true;
                        log.info("Invoice PDF rendered successfully via NodeJS for booking {}", booking.getId());
                    } else {
                        log.warn("NodeJS render script failed with exit code {}: {}", exitCode, processOutput);
                    }
                } catch (Exception e) {
                    log.warn("NodeJS execution failed, falling back to Java PDF generator: {}", e.getMessage());
                }
            }

            // 2. Fallback: Nếu NodeJS không chạy hoặc không có, tự sinh PDF trực tiếp bằng Java
            if (!pdfGenerated || !Files.exists(pdfFile)) {
                createPdfDirectly(invoice, pdfFile);
                log.info("Invoice PDF rendered directly via Java fallback for booking {}", booking.getId());
            }

            if (!Files.exists(pdfFile)) {
                throw new RuntimeException("Invoice PDF file creation failed.");
            }

            // 3. Upload PDF lên MinIO
            String objectName = mediaService.uploadInvoice(
                    pdfFile.toFile(),
                    booking.getId()
            );

            // Lưu invoiceUrl & invoiceGeneratedAt vào Booking DB
            booking.setInvoiceUrl(objectName);
            booking.setInvoiceGeneratedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            log.info("Invoice created and saved for booking {}: {}", booking.getId(), objectName);
            return objectName;
        } finally {
            // Xóa file tạm
            try {
                Files.deleteIfExists(pdfFile);
                Files.deleteIfExists(jsonFile);
                Files.deleteIfExists(tempDir);
            } catch (Exception ignored) {}
        }
    }

    private void createPdfDirectly(InvoiceDto invoice, Path pdfFile) throws Exception {
        String salonName = removeAccents(invoice.getSalonName() != null ? invoice.getSalonName() : "SalonFlow Hair Salon");
        String salonPhone = removeAccents(invoice.getSalonPhone() != null ? invoice.getSalonPhone() : "0900000000");
        String salonAddress = removeAccents(invoice.getSalonAddress() != null ? invoice.getSalonAddress() : "Ha Noi, Viet Nam");
        String customerName = removeAccents(invoice.getCustomerName() != null ? invoice.getCustomerName() : "Khach hang");
        String customerPhone = removeAccents(invoice.getCustomerPhone() != null ? invoice.getCustomerPhone() : "");
        Long bookingId = invoice.getBookingId() != null ? invoice.getBookingId() : 0L;
        Double totalVal = invoice.getTotal() != null ? invoice.getTotal() : (invoice.getSubTotal() != null ? invoice.getSubTotal() : 0.0);
        String totalFormatted = String.format("%,.0f VND", totalVal);

        java.util.List<String> linesList = new java.util.ArrayList<>();
        linesList.add("============================================================");
        linesList.add("                  HOA DON THANH TOAN SALONFLOW              ");
        linesList.add("============================================================");
        linesList.add(String.format("  Ma Hoa Don    : BK-%d", bookingId));
        linesList.add(String.format("  Salon         : %s", salonName));
        linesList.add(String.format("  Dia chi       : %s", salonAddress));
        linesList.add(String.format("  So Dien Thoai : %s", salonPhone));
        linesList.add("------------------------------------------------------------");
        linesList.add(String.format("  Khach Hang    : %s", customerName));
        linesList.add(String.format("  SDT Khach     : %s", customerPhone.isBlank() ? "N/A" : customerPhone));
        linesList.add("------------------------------------------------------------");
        linesList.add("  DANH SACH DICH VU SU DUNG:");

        if (invoice.getItems() != null) {
            int idx = 1;
            for (var item : invoice.getItems()) {
                String name = removeAccents(item.getServiceName() != null ? item.getServiceName() : "Dich vu");
                Double priceVal = item.getTotalPrice() != null ? item.getTotalPrice() : (item.getUnitPrice() != null ? item.getUnitPrice() : 0.0);
                String priceFormatted = String.format("%,.0f VND", priceVal);
                linesList.add(String.format("  [%d] %-30s : %s", idx++, name, priceFormatted));
            }
        }

        linesList.add("------------------------------------------------------------");
        linesList.add(String.format("  TONG CONG THANH TOAN: %s", totalFormatted));
        linesList.add("============================================================");
        linesList.add("          CAM ON QUY KHACH VA HEN GAP LAI QUA SALONFLOW!     ");
        linesList.add("============================================================");

        StringBuilder streamLines = new StringBuilder();
        for (String l : linesList) {
            String safeLine = l.replace("(", "").replace(")", "");
            streamLines.append("(").append(safeLine).append(") Tj T* ");
        }

        String streamContent = "BT /F1 11 Tf 40 780 Td 16 TL " + streamLines.toString().trim() + " ET";
        byte[] streamBytes = streamContent.getBytes(StandardCharsets.UTF_8);
        int streamLen = streamBytes.length;

        String pdfString = "%PDF-1.4\n" +
                "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
                "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n" +
                "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n" +
                "5 0 obj\n<< /Length " + streamLen + " >>\nstream\n" +
                streamContent + "\nendstream\nendobj\n" +
                "xref\n0 6\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n00000000115 00000 n \n0000000244 00000 n \n0000000315 00000 n \ntrailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + (315 + streamLen + 70) + "\n%%EOF";

        Files.write(pdfFile, pdfString.getBytes(StandardCharsets.UTF_8));
    }

    private String removeAccents(String src) {
        if (src == null) return "";
        String normalized = java.text.Normalizer.normalize(src, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .replace("Đ", "D")
                .replace("đ", "d");
    }
}