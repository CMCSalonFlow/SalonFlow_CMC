package com.example.salonflow.entity.enums;

public enum TicketPriority {
    P1("Khẩn cấp (SLA < 4h)", 4),
    P2("Cao (SLA < 24h)", 24),
    P3("Bình thường (SLA < 72h)", 72);

    private final String description;
    private final int slaHours;

    TicketPriority(String description, int slaHours) {
        this.description = description;
        this.slaHours = slaHours;
    }

    public String getDescription() {
        return description;
    }

    public int getSlaHours() {
        return slaHours;
    }
}
