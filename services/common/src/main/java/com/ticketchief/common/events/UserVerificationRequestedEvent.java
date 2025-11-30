package com.ticketchief.common.events;

public record UserVerificationRequestedEvent(
        String type,
        String userId,
        String email,
        String token
) {
    public UserVerificationRequestedEvent(String userId, String email, String token) {
        this("user.email.verification.requested", userId, email, token);
    }
}
