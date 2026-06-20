package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salon_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalonPhoto extends BaseEntity {

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
}