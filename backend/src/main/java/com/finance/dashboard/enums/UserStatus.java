package com.finance.dashboard.enums;

/**
 * Admins can deactivate users without deleting them.
 * INACTIVE users cannot log in (blocked at the authentication layer).
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE
}