package com.example.salonflow.entity.enums;

public enum TicketCategory {
    TECHNICAL("Sự cố kỹ thuật"),
    BILLING("Thanh toán & Hóa đơn"),
    ACCOUNT("Tài khoản & Phân quyền"),
    SALON_OPERATION("Vận hành Salon"),
    OTHER("Khác");

    private final String description;

    TicketCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
