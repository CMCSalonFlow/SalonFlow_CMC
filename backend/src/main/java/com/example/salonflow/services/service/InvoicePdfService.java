package com.example.salonflow.services.service;

import com.example.salonflow.entity.Booking;

public interface InvoicePdfService {

    String generateInvoice(Booking booking) throws Exception;

}