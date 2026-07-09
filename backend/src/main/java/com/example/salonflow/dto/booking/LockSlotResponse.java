package com.example.salonflow.dto.booking;

import lombok.Builder;
import lombok.Data;

/**
 * Response sau khi lock slot thành công.
 * ttlSeconds: thời gian còn lại của lock (giây) — FE dùng để countdown.
 */
@Data
@Builder
public class LockSlotResponse {

    private String slotKey;
    private Long ttlSeconds;
    private String message;
}
