package com.example.salonflow.repository;

import com.example.salonflow.entity.ZaloToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZaloTokenRepository extends JpaRepository<ZaloToken, Long> {
    Optional<ZaloToken> findByOaId(String oaId);
    Optional<ZaloToken> findFirstByOrderByIdAsc();
}
