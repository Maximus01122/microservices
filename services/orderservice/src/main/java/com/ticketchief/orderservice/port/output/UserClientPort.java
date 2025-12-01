package com.ticketchief.orderservice.port.output;

/**
 * Outside port for fetching user information from the user service.
 */
public interface UserClientPort {
    /**
     * Fetch the user's email by userId. Return null if not found or on error.
     */
    String getUserEmail(String userId);
}
