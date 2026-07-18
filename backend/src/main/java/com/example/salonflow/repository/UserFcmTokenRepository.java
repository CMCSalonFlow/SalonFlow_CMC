package com.example.salonflow.repository;

import com.example.salonflow.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    Optional<UserFcmToken> findByToken(String token);

    Optional<UserFcmToken> findByUserIdAndToken(Long userId, String token);

    List<UserFcmToken> findByUserIdAndIsActiveTrue(Long userId);

    List<UserFcmToken> findByUserId(Long userId);
}
