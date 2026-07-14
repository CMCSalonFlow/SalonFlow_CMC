package com.example.salonflow.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class ServiceBundleItemId implements Serializable {

    private Long bundleId;

    private Long serviceId;
}
