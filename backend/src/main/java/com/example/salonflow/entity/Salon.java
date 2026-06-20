package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "salons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon extends BaseEntity {

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

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    @Column(columnDefinition = "TEXT")
    private String address;

    private Double latitude;

    private Double longitude;

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