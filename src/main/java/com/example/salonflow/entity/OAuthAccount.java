package com.example.salonflow.entity;

import com.example.salonflow.entity.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "oauth_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_oauth_accounts_provider_user",
                        columnNames = {"provider", "provider_user_id"}
                ),
                @UniqueConstraint(
                        name = "uk_oauth_accounts_user_provider",
                        columnNames = {"user_id", "provider"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(nullable = false)
    private String email;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
