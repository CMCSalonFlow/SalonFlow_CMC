package com.example.salonflow.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        CustomUserPrincipal principal =
                (CustomUserPrincipal)
                        authentication.getPrincipal();

        return principal.getId();
    }

    public static String getCurrentUserEmail() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        CustomUserPrincipal principal =
                (CustomUserPrincipal)
                        authentication.getPrincipal();

        return principal.getEmail();
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