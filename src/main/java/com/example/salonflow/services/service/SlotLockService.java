package com.example.salonflow.services.service;

import com.example.salonflow.dto.booking.LockSlotRequest;
import com.example.salonflow.dto.booking.LockSlotResponse;

public interface SlotLockService {

    /**
     * Lock slot bằng Redis SETNX.
     * Nếu slot đã bị lock → throw 409 Conflict.
     * TTL: 600 giây (10 phút).
     */
    LockSlotResponse lockSlot(Long customerId, LockSlotRequest request);

    /**
     * Unlock slot (hủy lock).
     * Chỉ user đang giữ lock mới được unlock.
     */
    void unlockSlot(Long customerId, String slotKey);

    /**
     * Kiểm tra slot có đang bị lock không.
     * Trả về true nếu slot đang bị lock.
     */
    boolean isSlotLocked(String slotKey);

    /**
     * Lấy thời gian còn lại của lock (giây).
     * Trả về -1 nếu key không tồn tại.
     */
    Long getSlotTtl(String slotKey);
}
