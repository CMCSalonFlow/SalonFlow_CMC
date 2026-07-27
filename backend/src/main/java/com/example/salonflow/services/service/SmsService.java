package com.example.salonflow.services.service;

/**
 * Service gửi SMS qua ESMS (US-037).
 */
public interface SmsService {

    /**
     * Gửi SMS nhắc hẹn tới khách hàng.
     * @param phone   Số điện thoại khách hàng (VD: 0987654321)
     * @param message Nội dung SMS (tối đa 160 ký tự)
     * @return true nếu gửi thành công
     */
    boolean sendSms(String phone, String message);
}
