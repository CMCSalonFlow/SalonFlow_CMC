package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "salons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owner
    @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(
                name = "owner_id",
                nullable = false,
                unique = true
        )
        private User owner;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    private String phone;

    private String email;

    private String website;

    @Column(columnDefinition = "TEXT")
    private String address;

    private Double latitude;

    private Double longitude;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "salon",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<SalonHour> hours = new ArrayList<>();

    @OneToMany(
            mappedBy = "salon",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<SalonPhoto> photos = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "salon")
    private List<Branch> branches = new ArrayList<>();
}