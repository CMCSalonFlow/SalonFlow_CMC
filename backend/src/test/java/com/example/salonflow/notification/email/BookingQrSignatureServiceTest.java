package com.example.salonflow.notification.email;

import com.example.salonflow.entity.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BookingQrSignatureServiceTest {

    private BookingQrSignatureService service;

    @BeforeEach
    void setUp() {
        service = new BookingQrSignatureService();
        ReflectionTestUtils.setField(service, "secret", "test-booking-qr-secret");
    }

    @Test
    void generateSignature_shouldVerifySameBookingId() {
        Booking booking = Booking.builder().id(100L).build();

        String signature = service.generateSignature(booking);

        assertThat(signature).isNotBlank();
        assertThat(service.verify(100L, signature)).isTrue();
    }

    @Test
    void verify_shouldRejectTamperedBookingIdOrSignature() {
        Booking booking = Booking.builder().id(100L).build();
        String signature = service.generateSignature(booking);

        assertThat(service.verify(101L, signature)).isFalse();
        assertThat(service.verify(100L, signature + "x")).isFalse();
    }
}
