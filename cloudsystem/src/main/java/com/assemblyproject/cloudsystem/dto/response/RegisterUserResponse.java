package com.assemblyproject.cloudsystem.dto.response;

public record RegisterUserResponse(
        AuthUserData user,
        String token,
        String tokenType,
        long expiresIn
) {
}

