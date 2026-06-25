package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "service_bundle_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceBundleItem {

    @EmbeddedId
    private ServiceBundleItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bundleId")
    @JoinColumn(name = "bundle_id")
    private ServiceBundle bundle;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    private Service service;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
