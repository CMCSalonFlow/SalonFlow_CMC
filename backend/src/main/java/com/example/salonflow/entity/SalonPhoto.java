package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "salon_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}