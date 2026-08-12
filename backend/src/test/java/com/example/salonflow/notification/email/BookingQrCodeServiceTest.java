package com.example.salonflow.notification.email;

import com.example.salonflow.entity.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BookingQrCodeServiceTest {

    private BookingQrCodeService qrCodeService;
    private BookingQrSignatureService signatureService;

    @BeforeEach
    void setUp() {
        signatureService = Mockito.mock(BookingQrSignatureService.class);
        qrCodeService = new BookingQrCodeService(signatureService);
        ReflectionTestUtils.setField(qrCodeService, "frontendUrl", "http://localhost:5173/");
    }

    @Test
    void generateDataUrl_shouldReturnExternalQrApiUrl() {
        Booking booking = Booking.builder().id(123L).build();
        when(signatureService.generateSignature(booking)).thenReturn("test-sig");

        String qrUrl = qrCodeService.generateDataUrl(booking);

        assertThat(qrUrl).isNotBlank();
        assertThat(qrUrl).startsWith("https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=");

        String expectedContent = "http://localhost:5173/check-in?bookingId=123&signature=test-sig";
        String expectedDataPart = URLEncoder.encode(expectedContent, StandardCharsets.UTF_8);
        
        assertThat(qrUrl).contains(expectedDataPart);
    }

    @Test
    void generateDataUrl_whenFrontendUrlIsEmpty_shouldReturnFallbackContent() {
        ReflectionTestUtils.setField(qrCodeService, "frontendUrl", "");
        Booking booking = Booking.builder().id(123L).build();
        when(signatureService.generateSignature(booking)).thenReturn("test-sig");

        String qrUrl = qrCodeService.generateDataUrl(booking);

        assertThat(qrUrl).isNotBlank();
        assertThat(qrUrl).startsWith("https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=");

        String expectedContent = "SalonFlow|bookingId=123|signature=test-sig";
        String expectedDataPart = URLEncoder.encode(expectedContent, StandardCharsets.UTF_8);
        
        assertThat(qrUrl).contains(expectedDataPart);
    }
}
