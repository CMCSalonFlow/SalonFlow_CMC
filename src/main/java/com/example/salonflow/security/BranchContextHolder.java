package com.example.salonflow.security;

public final class BranchContextHolder {

    private static final ThreadLocal<Long>
            CURRENT_BRANCH = new ThreadLocal<>();

    private BranchContextHolder() {
    }

    public static void setBranchId(
            Long branchId
    ) {
        CURRENT_BRANCH.set(branchId);
    }

    public static Long getBranchId() {
        return CURRENT_BRANCH.get();
    }

    public static void clear() {
        CURRENT_BRANCH.remove();
    }
}