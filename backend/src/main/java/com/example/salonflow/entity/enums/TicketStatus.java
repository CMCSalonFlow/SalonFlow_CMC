package com.example.salonflow.entity.enums;

public enum TicketStatus {
    OPEN("Mới tiếp nhận"),
    IN_PROGRESS("Đang xử lý"),
    RESOLVED("Đã giải quyết"),
    CLOSED("Đã đóng");

    private final String description;

    TicketStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
