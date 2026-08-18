package com.familycareai.identity.event;

public record UserRegisteredEvent(
        String email,
        String firstName,
        String lastName,
        String role
) {}
