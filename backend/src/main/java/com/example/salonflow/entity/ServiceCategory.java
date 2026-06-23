package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private String icon;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}