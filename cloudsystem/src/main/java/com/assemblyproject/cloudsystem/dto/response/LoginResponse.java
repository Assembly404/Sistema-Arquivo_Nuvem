package com.assemblyproject.cloudsystem.dto.response;

public record LoginResponse(
        AuthUserData user,
        String token,
        String tokenType,
        long expiresIn
) {
}

