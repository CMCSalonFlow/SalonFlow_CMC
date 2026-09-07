package com.example.salonflow.security;

import com.example.salonflow.exception.InvalidTokenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<CustomUserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal customUserPrincipal) {
            return Optional.of(customUserPrincipal);
        }

        return Optional.empty();
    }

    public static Optional<Long> getCurrentUserIdOptional() {
        return getCurrentUserPrincipal().map(CustomUserPrincipal::getId);
    }

    public static Optional<String> getCurrentUserEmailOptional() {
        return getCurrentUserPrincipal().map(CustomUserPrincipal::getEmail);
    }

    public static Long getCurrentUserId() {
        return getCurrentUserIdOptional()
                .orElseThrow(() -> new InvalidTokenException("User is not authenticated"));
    }

    public static String getCurrentUserEmail() {
        return getCurrentUserEmailOptional()
                .orElseThrow(() -> new InvalidTokenException("User is not authenticated"));
    }

    public static Long getCurrentBranchId() {

        Long branchId =
                BranchContextHolder
                        .getBranchId();

        if (branchId == null) {

                throw new IllegalStateException(
                        "No branch selected"
                );
        }

        return branchId;
    }
}