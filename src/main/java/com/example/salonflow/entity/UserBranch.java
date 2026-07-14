package com.example.salonflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBranch extends BaseEntity {

    @EmbeddedId
    private UserBranchId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("branchId")
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "assigned_at")
    private Instant assignedAt;
}