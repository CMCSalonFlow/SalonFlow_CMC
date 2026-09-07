package com.example.salonflow.services.service;

import com.example.salonflow.dto.booking.LockSlotRequest;
import com.example.salonflow.dto.booking.LockSlotResponse;

public interface SlotLockService {

    /**
     * Lock slot bằng Redis SETNX (TTL: 300 giây - 5 phút).
     * Hỗ trợ cả user đăng nhập và khách vãng lai thông qua holderId.
     */
    LockSlotResponse lockSlot(String holderId, LockSlotRequest request);

    default LockSlotResponse lockSlot(Long customerId, LockSlotRequest request) {
        return lockSlot(customerId != null ? "user:" + customerId : "guest:anonymous", request);
    }

    /**
     * Unlock slot (hủy lock).
     * Chỉ holder đang giữ lock mới được unlock.
     */
    void unlockSlot(String holderId, String slotKey);

    default void unlockSlot(Long customerId, String slotKey) {
        unlockSlot(customerId != null ? "user:" + customerId : "guest:anonymous", slotKey);
    }

    /**
     * Xóa lock bắt buộc (dùng khi booking đã hoàn tất tạo thành công).
     */
    void forceUnlock(String slotKey);

    /**
     * Kiểm tra slot có đang bị lock không.
     */
    boolean isSlotLocked(String slotKey);

    /**
     * Lấy thời gian còn lại của lock (giây).
     */
    Long getSlotTtl(String slotKey);
}

